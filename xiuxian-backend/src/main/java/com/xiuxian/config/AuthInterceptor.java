package com.xiuxian.config;

import com.xiuxian.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 鉴权拦截器：校验 Authorization: Bearer <token>，将 userId 放入请求属性供控制器使用。
 * 在 WebConfig 中注册，保护 /api/**（放行 /api/auth/**）。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return unauthorized(response);
        }
        try {
            Long userId = jwtUtil.verify(auth.substring(7).trim());
            request.setAttribute("userId", userId);
            return true;
        } catch (AuthException e) {
            return unauthorized(response);
        }
    }

    private boolean unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"未登录或登录已失效\"}");
        return false;
    }
}
