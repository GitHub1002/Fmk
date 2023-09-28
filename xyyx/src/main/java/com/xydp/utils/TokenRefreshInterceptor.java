package com.xydp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.xydp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xydp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.xydp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * @author 付淇
 * @version 1.0
 */
public class TokenRefreshInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public TokenRefreshInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的token
        String token = request.getHeader("authorization");

        // 判断token是否存在
        if (StrUtil.isBlank(token)){
            return true;
        }
        // token存在则基于redis获取user用户,通过entries方法直接获取当前hashset中全部的字段信息，如果单单通过get方法必须指定字段名称无法实现批量获取
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(token);

        // 判断当前session中打的user数据是否为空
        if (userMap.isEmpty()){
            return true;
        }
        // 将查询到的hash数据重新转换为User对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

        // 如果有数据则存入threadLocal中传递保存
        UserHolder.saveUser(userDTO);

        // 刷新Token过期时间
        stringRedisTemplate.expire(LOGIN_USER_KEY + token,LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
         // 因为ThreadLocal的底层是ThreadLocalMap，当前线程ThreadLocal作为Key是弱引用
         // 而user作为value是强引用，jvm不会把强引用数据回收，所以value数据没有释放会引发内存泄漏问题
         // 所以这里我们选择手动释放
        UserHolder.removeUser();
    }
}
