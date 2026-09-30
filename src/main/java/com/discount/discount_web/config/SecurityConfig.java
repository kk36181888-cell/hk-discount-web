package com.discount.discount_web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. 授權規則：全部網址放行 (因為後台已經有我哋嘅 JWT Interceptor 把關)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            
            // 2. 停用 Spring Security 預設嘅登入畫面同 HTTP Basic 驗證，避免同 DayBuy 自己嘅 Login 衝突
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

            // 💡 註解：Spring Security 預設已經自動開啟咗 CSRF 防護同 Security Headers。
            // 只要你前端嘅 form 係用 th:action，Thymeleaf 就會自動注入防偽造 Token。

        return http.build();
    }
}