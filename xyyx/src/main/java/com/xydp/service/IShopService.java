package com.xydp.service;

import com.xydp.dto.Result;
import com.xydp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author 付淇
 * @version 1.0
 */
public interface IShopService extends IService<Shop> {

    Result queryShopById(Long id);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
