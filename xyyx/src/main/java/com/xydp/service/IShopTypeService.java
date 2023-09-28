package com.xydp.service;

import com.xydp.dto.Result;
import com.xydp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author 付淇
 * @version 1.0
 */
public interface IShopTypeService extends IService<ShopType> {

    Result queryTypeList();
}
