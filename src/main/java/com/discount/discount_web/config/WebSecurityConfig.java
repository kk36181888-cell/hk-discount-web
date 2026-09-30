package com.discount.discount_web.config;

import com.discount.discount_web.security.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 攔截所有 /daybuy-hq/ 開頭嘅路徑
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/daybuy-hq/**")
                // 排除登入頁，否則會無限 Loop
                .excludePathPatterns("/daybuy-hq/login", "/daybuy-hq/doLogin");
    }
}