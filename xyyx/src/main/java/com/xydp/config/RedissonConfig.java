package com.xydp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 付淇
 * @version 1.0
 */
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        // 创建配置
        Config config = new Config();
        // 添加redis地址，这里添加的是单个redis节点的地址，也可以使用config.useClusterServers()添加集群地址
        config.useSingleServer().setAddress("redis://192.168.79.128:6379").setPassword("123456");
        // 创建客户端
        return Redisson.create(config);
    }
}
