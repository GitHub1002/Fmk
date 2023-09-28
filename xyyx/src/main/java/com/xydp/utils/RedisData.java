package com.xydp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author 付淇
 * @version 1.0
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
