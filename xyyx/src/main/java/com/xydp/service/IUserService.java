package com.xydp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xydp.dto.LoginFormDTO;
import com.xydp.dto.Result;
import com.xydp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * @author 付淇
 * @version 1.0
 */
public interface IUserService extends IService<User> {
    /**
     * 发送手机验证码
     * @param phone
     * @param session
     * @return
     */
    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);
}
