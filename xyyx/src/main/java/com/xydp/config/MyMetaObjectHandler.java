package com.xydp.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 配置自动注入类
 * @author 付淇
 * @version 1.0
 */
@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    //当程序调用添加方法时会调用该方法
    @Override
    public void insertFill(MetaObject metaObject) {
        metaObject.setValue("create_time", LocalDateTime.now());

    }
    //当程序调用修改方法时会调用该方法
    @Override
    public void updateFill(MetaObject metaObject) {
        metaObject.setValue("update_time", LocalDateTime.now());
    }
}
