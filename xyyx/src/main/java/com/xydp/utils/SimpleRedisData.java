package com.xydp.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author 付淇
 * @version 1.0
 */
public class SimpleRedisData implements ILock{
    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String PreKey = "lock:";
    private static final String PreValue = UUID.fastUUID().toString(true);

    public SimpleRedisData(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(String name,Long timeOut) {
        // 1.获取当先线程的id用于拼接value值
        String threadId = Thread.currentThread().getName();
        // 2.通过redisTemplate中的setnx方法设置锁
        Boolean aBoolean = stringRedisTemplate.opsForValue()
                .setIfAbsent(PreKey + name, PreValue + threadId, timeOut, TimeUnit.SECONDS);
        //因为返回值是包装类所以使用工具类拆箱返回防止空指针问题
        return BooleanUtil.isTrue(aBoolean);
    }

    @Override
    public void unLock() {
        // 1.获取当先线程的id用于拼接value值
        String threadId = PreValue + Thread.currentThread().getName();
        String value = stringRedisTemplate.opsForValue().get(PreKey + name);
        if (threadId.equals(value)){
            //2.判断当前线程和调用释放操作的线程为同一个，防止因为单个的业务阻塞释放了其他线程的锁
            stringRedisTemplate.delete(PreKey + name);
        }

    }
}
