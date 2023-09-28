package com.xydp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author 付淇
 * @version 1.0
 */
@Component
public class RedisIdWorker {
    // 2023-03-29 17:04:00
    private static final long BEGIN_TIMESTAMP = 1680109440L;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成全局唯一ID
     * @param keyPrefix 业务前缀
     * @return
     */
    public long nextId(String keyPrefix) {
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowEpochSecond = now.toEpochSecond(ZoneOffset.UTC);
        long subTime  = nowEpochSecond - BEGIN_TIMESTAMP;

        // 2.生成序列号
        // 2.1获取当前天数，精确到天 那么就是一天一个Key，格式使用：做分割的好处是redis中：可以进行分类方便我们后期做不同日期的信息统计
        String dateFormat = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2 将当前天数拼接到自增key中
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + dateFormat);
        //3.拼接返回
        return subTime << 32 | count;
    }


//    public static void main(String[] args) {
//        LocalDateTime of = LocalDateTime.of(2023, 3, 29, 17, 4);
//        long epochSecond = of.toEpochSecond(ZoneOffset.UTC);
//        System.out.printf("当前时间" + epochSecond + "秒");
//    }
}
