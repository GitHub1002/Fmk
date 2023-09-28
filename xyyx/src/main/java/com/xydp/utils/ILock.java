package com.xydp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author 付淇
 * @version 1.0
 * 定义关于项目集群多个JVM使用synchronized锁失效问题时，通过redis中setnx实现多集群互斥功能的规格
 */
public interface ILock {
    /**
     * 获取锁方法
     */
    boolean tryLock(String name,Long timeOut);

    /**
     * 释放互斥锁
     */
    void unLock();
}
