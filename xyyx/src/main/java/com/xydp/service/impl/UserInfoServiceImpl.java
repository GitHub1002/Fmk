package com.xydp.service.impl;

import com.xydp.entity.UserInfo;
import com.xydp.mapper.UserInfoMapper;
import com.xydp.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 付淇
 * @version 1.0
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
