package com.xydp.config;

import com.xydp.utils.LoginInterceptor;
import com.xydp.utils.TokenRefreshInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author 付淇
 * @version 1.0
 */
@Configuration
public class WebConfig extends WebMvcConfigurationSupport {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns(
                "/shop/**",
                "/voucher/**",
                "/shop-type/**",
                "/upload/**",
                "/blog/hot",
                "/user/code",
                "/user/login"
        ).order(1);
        registry.addInterceptor(new TokenRefreshInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
    }

    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        //创建消息转换器对象
        MappingJackson2HttpMessageConverter MessageConverter = new MappingJackson2HttpMessageConverter();
        //设置对象转换器，底层使用JackSon将java对象转换Json
        MessageConverter.setObjectMapper(new JacksonObjectMapper());
        //将上面的消息转换器对象添加到mvc框架中默认的转换器集合中
        //位置必须设置为 => 0，才可以保证使用的是我们自定义的转换器
        converters.add(0,MessageConverter);

    }
}
