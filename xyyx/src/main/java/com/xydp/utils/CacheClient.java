package com.xydp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.xydp.utils.RedisConstants.CACHE_NULL_TTL;
import static com.xydp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * @author 付淇
 * @version 1.0
 */
@Slf4j
@Component
public class CacheClient {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //封装缓存常规插入方法
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    //封装缓存逻辑时间插入方法
    public void setLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        //设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);

        // 设置逻辑过期时间（逻过期时 = 当前时间 + 后台设置的指定时间）
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

        //将封装好逻辑过期时间的redisData 存入redis 中
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    // 预防缓存穿透
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type,
                                          Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        // 创建redis缓存来搜索数据
        String key = keyPrefix + id;

        // 首先在redis中搜索是否有相关数据
        String Json = stringRedisTemplate.opsForValue().get(key);

        // 判断当前在redis中查找的缓存数据是否存在
        if (StrUtil.isNotBlank(Json)) {
            // 存在则直接返回
            return JSONUtil.toBean(Json, type);
        }

        // 预防缓存穿透问题，对找不到的信息给与“”空字符串赋值处理
        if (Json != null) {
            return null;
        }

        // 不存在则使用mysql进行查找
        R r = dbFallback.apply(id);

        // 判断查找指定id对象是否存在
        if (r == null) {
            // 预防缓存穿透问题，对找不到的数据信息给与“”空字符串赋值处理
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        // 存在存则入redis数据库中
        this.set(key, r, time, unit);

        // 返回结果
        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type,
                                            Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String Json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(Json)) {
            return null;
        }
        //JSONUtil.toBean方法的返回值类型其实是 JSONObject
        RedisData redisData = JSONUtil.toBean(Json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            //未过期返回当前缓存对象
            return r;
        }

        //已过期
        //获取锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean lock = tryLock(lockKey);
        if (lock) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R r1 = dbFallback.apply(id);
                    //写入缓存
                    this.set(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    this.unlock(lockKey);
                }
            });
        }
        return r;
    }

    private boolean tryLock(String key) {
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
