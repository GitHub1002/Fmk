package com.xydp.service;

import com.xydp.dto.Result;
import com.xydp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author 付淇
 * @version 1.0
 */
public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);
}
