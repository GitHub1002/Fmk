package com.xydp.service;

import com.xydp.dto.Result;
import com.xydp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 付淇
 * @version 1.0
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 使用秒杀卷方法
     * @param voucherId
     * @return
     */
    Result seckill(Long voucherId);

    void getResult(VoucherOrder voucherId);
}
