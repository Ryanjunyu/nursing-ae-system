package com.his.ae.security;

import com.his.ae.common.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** JWT 认证拦截器：校验令牌并放入当前用户上下文（登录接口放行） */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if ("OPTIONS".equals(request.getMethod()) || uri.contains("/api/auth/login")) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BizException(401, "未登录或令牌缺失");
        }
        String token = auth.substring(7);
        try {
            LoginUser u = new LoginUser();
            u.setUserId(jwtUtil.getUserId(token));
            u.setUsername(jwtUtil.getUsername(token));
            u.setRole(jwtUtil.getRole(token));
            u.setDeptId(jwtUtil.getDeptId(token));
            CurrentUserHolder.set(u);
            request.setAttribute("loginUser", u);
            return true;
        } catch (Exception e) {
            throw new BizException(401, "令牌无效或已过期");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserHolder.clear();
    }
}
