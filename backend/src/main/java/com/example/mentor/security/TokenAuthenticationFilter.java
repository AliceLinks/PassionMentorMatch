package com.example.mentor.security;

import com.example.mentor.dao.entity.User;
import com.example.mentor.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 读取 Bearer Token -> 查询用户 -> 注入认证上下文
 */
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 已有认证则跳过
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = extractToken(request);
            if (StringUtils.hasText(token)) {
                try {
                    User user = authService.getUserInfoByToken(token);
                    // 简单授予 ROLE_USER
                    var auth = new UsernamePasswordAuthenticationToken(
                            user, token, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception ignored) {
                    // token 无效 -> 保持匿名，后续统一由 AuthenticationEntryPoint 返回 401 JSON
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (StringUtils.hasText(h) && h.startsWith("Bearer ")) {
            return h.substring(7);
        }
        String q = request.getParameter("token");
        if (StringUtils.hasText(q)) {
            return q;
        }
        return null;
    }
}