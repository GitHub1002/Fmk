package com.xydp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xydp.dto.LoginFormDTO;
import com.xydp.dto.Result;
import com.xydp.dto.UserDTO;
import com.xydp.entity.User;
import com.xydp.mapper.UserMapper;
import com.xydp.service.IUserService;
import com.xydp.utils.RegexUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xydp.utils.RedisConstants.*;
import static com.xydp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * @author 付淇
 * @version 1.0
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送手机验证码
     *
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 校验手机号是否合法
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 如果不符合返回错误信息
            return Result.fail("手机号格式错误！");
        }

        // 生成符合验证码
        String code = RandomUtil.randomNumbers(6);

        //将验证码存入Redis中 设置过期时间为2分钟
        stringRedisTemplate.opsForValue().set( LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 发送验证码
        log.debug("发送短信验证码成功，验证码为：" + code);

        // 返回正确信息
        return Result.ok(code);

    }

    /**
     * 登录方法
     *
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 校验手机号是否正确
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 如果不符合返回错误信息
            return Result.fail("手机号格式错误！");
        }

        // 通过Redis获取验证码校验验证码是否一致
        String checkCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (checkCode == null || !checkCode.equals(code)) {
            return Result.fail("验证码信息错误！");
        }

        // 一致进行下一步查询当前手机号对应用户信息
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = getOne(queryWrapper);

        // 判断当前用户信息是否为空，为空则创建用户
        if (user == null) {
            // 用户信息不存在创建并保存
            user = creatUserWithPhone(phone);
        }

        // 生成随机的token令牌通过UUID雪花算法的方式
        String token = UUID.randomUUID().toString(true);

        // 将user对象转换为Map通过BeanUtil.beanToMap()方法实现转换;
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((name,value) -> value.toString()));

        // 存储 通过putAll方法一次性将 对象的字段全部存入，put方法只能单字段存入
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token,stringObjectMap);

        // 设置存储有效期(session的有效期一般是30分钟，让redis中的HashSet中的key过期值也是30分钟)
        // 如果不设置过期信息内存会占用过多，导致项目整体卡顿效率下降
        stringRedisTemplate.expire(LOGIN_USER_KEY + token,LOGIN_USER_TTL,TimeUnit.MINUTES);

        // 返回token
        return Result.ok(LOGIN_USER_KEY + token);
    }

    private User creatUserWithPhone(String phone) {
        // 创建用户
        User user = new User();
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(5));
        user.setPhone(phone);

        // 向数据库保存用户
        save(user);
        return user;
    }
}
