package com.xydp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.xydp.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class XyDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(XyDianPingApplication.class, args);
    }

}
