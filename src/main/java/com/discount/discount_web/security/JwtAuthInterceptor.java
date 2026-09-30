package com.discount.discount_web.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("DAYBUY_ADMIN_TOKEN".equals(cookie.getName())) {
                    try {
                        jwtUtil.validateTokenAndGetUsername(cookie.getValue());
                        return true; // Token 合法，放行入 CMS
                    } catch (Exception e) {
                        break; // Token 無效或已過期
                    }
                }
            }
        }
        
        // 冇 Cookie 或者無效，踢返去登入頁
        response.sendRedirect("/daybuy-hq/login");
        return false;
    }
}