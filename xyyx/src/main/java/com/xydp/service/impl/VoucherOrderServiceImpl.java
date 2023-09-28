package com.xydp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xydp.dto.Result;
import com.xydp.entity.SeckillVoucher;
import com.xydp.entity.VoucherOrder;
import com.xydp.mapper.VoucherOrderMapper;
import com.xydp.service.ISeckillVoucherService;
import com.xydp.service.IVoucherOrderService;
import com.xydp.service.IVoucherService;
import com.xydp.utils.RedisIdWorker;
import com.xydp.utils.SimpleRedisData;
import com.xydp.utils.UserHolder;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.awt.AppContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author 付淇
 * @version 1.0
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private IVoucherService voucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    // 声明当前类代理对象
    private IVoucherOrderService proxy;
    // 声明阻塞队列利用数组阻塞队列实现 大小为1024*1024
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue(1024 * 1024);
    // 声明线程池
    private static ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();
    // 声明lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    // 取出阻塞队列中的下单信息，要在当前类初始化时启动
    private class VoucherOrderHandler implements Runnable{
        String queueName = "seckill.order";
        @Override
        public void run() {
            while (true){
                try {
                    // 从Stream消息队列中取出下单信息
                    List<MapRecord<String, Object, Object>> seckill = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    if (seckill == null || seckill.size() == 0){
                        // 没有消息，继续循环
                        continue;
                    }
                    // 解析消息中的订单信息
                    MapRecord<String, Object, Object> mapRecord = seckill.get(0);
                    Map<Object, Object> value = mapRecord.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 如果获取成功，可以下单
                    handleVoucherOrder(voucherOrder);
                    // 使用ACK确认消息已经被消费
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "seckill", mapRecord.getId());
                } catch (Exception e) {
                    log.error("处理订单异常：" + e);
                    handlePendingList();
                }
            }
        }

        private void handlePendingList() {
            while (true){
                try {
                    // 从Stream消息队列中取出下单信息
                    List<MapRecord<String, Object, Object>> seckill = stringRedisTemplate.opsForStream().read(
                            Consumer.from("seckill", "seckill-consumer"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    if (seckill == null || seckill.size() == 0){
                        // 没有消息，继续循环
                        break;
                    }
                    // 解析消息中的订单信息
                    MapRecord<String, Object, Object> mapRecord = seckill.get(0);
                    Map<Object, Object> value = mapRecord.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 如果获取成功，可以下单
                    handleVoucherOrder(voucherOrder);
                    // 使用ACK确认消息已经被消费
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "seckill", mapRecord.getId());
                } catch (Exception e) {
                    log.error("处理订单异常：" + e);
                }
            }
        }

    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        // 通过Redisson获取用户的锁
        RLock lock = redissonClient.getLock("lock:or der:" + userId);
        // tryLock（）方法的参数1：等待时间，参数2：锁的过期时间，参数3：时间单位
        // 如果不写参数的话，那么如果获取不到锁的话，会直接返回false不会等待
        boolean isLock = lock.tryLock();
        // 判断是否获取到锁
        if (!isLock){
            // 获取不到锁，返回错误信息
            log.error("不允许重复下单！当前用户id：" + userId);
            return;
        }
        try {
            // 通过AopContext.currentProxy()获取代理对象，从而调用代理对象的方法
            // 调用代理对象的方法这样的话防止事务注解失效
            proxy.getResult(voucherOrder);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    @Override
    public Result seckill(Long voucherId) {
        // 获取用户id
        Long userId = UserHolder.getUser().getId();
        // 生成全局订单id
        Long order = redisIdWorker.nextId("order");
        // 获取当前类代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 执行lua脚本
        Long execute = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.EMPTY_LIST,
                voucherId.toString(), userId.toString(), order.toString()
        );
        // 判断结果是否为0
        // 不为0，代表没有购买资格
        if (execute != 0){
            return Result.fail(execute == 1 ? "库存不足，无法下单！" : "当前用户已购买不能重复下单！");
        }

        // 返回订单id
        return Result.ok(order);

    }

    @Transactional//事物的提交是在函数结束的时候由spring提交
    public void getResult(VoucherOrder voucher) {
        Long id = voucher.getUserId();
        //查询订单
        int count = query().eq("user_id", id).eq("voucher_id", voucher.getVoucherId()).count();
        if (count > 0) {
            log.error("当前用户已经购买过该代金卷，不能重复购买！");
        }
        //扣减库存
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id", voucher.getVoucherId()).gt("stock", 0).update();
        if (!success) {
            log.error("扣减库存失败！");
        }

        //保存订单信息
        save(voucher);
    }
}
