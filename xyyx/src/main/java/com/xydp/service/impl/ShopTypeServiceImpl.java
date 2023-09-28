package com.xydp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xydp.dto.Result;
import com.xydp.entity.ShopType;
import com.xydp.mapper.ShopTypeMapper;
import com.xydp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.xydp.utils.RedisConstants.CACHE_SHOPTYPE_KEY;

/**
 * @author 付淇
 * @version 1.0
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        // 先从Redis中查询所有店铺类型
        String s = stringRedisTemplate.opsForValue().get(CACHE_SHOPTYPE_KEY);
        // 判断是否存在
        if (StrUtil.isNotBlank(s)) {
            // 存在则直接返回
            List<ShopType> shopTypes = JSONUtil.toList(JSONUtil.parseArray(s), ShopType.class);
            return Result.ok(shopTypes);
        }
        // 如果不存在则从mysql中查询
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        // 从mysql中查询到数据后存入redis中
        stringRedisTemplate.opsForValue().set(CACHE_SHOPTYPE_KEY,JSONUtil.toJsonStr(shopTypes));
        // 返回结果
        return Result.ok(shopTypes);

    }
}
