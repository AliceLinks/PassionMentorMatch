package com.example.mentor.config;

import com.example.mentor.dto.Result;
import com.example.mentor.security.TokenAuthenticationFilter;
import com.example.mentor.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    // 未认证时返回 JSON，且不发送 WWW-Authenticate
    @Bean
    public AuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            // 不设置 WWW-Authenticate，避免微信弹窗
            String body = new ObjectMapper().writeValueAsString(Result.buildFailure(401, "401", "未认证/登录失效"));
            response.getWriter().write(body);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationEntryPoint entryPoint,
            AuthService authService) throws Exception {

        http
            // 关闭 CSRF（后端 API）
            .csrf(csrf -> csrf.disable())
            // 关闭默认的 HTTP Basic
            .httpBasic(basic -> basic.disable())
            // 显式关闭表单登录
            .formLogin(form -> form.disable())
            // 关闭默认登出入口，避免误拦截
            .logout(logout -> logout.disable())
            // 使用无状态会话
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 权限规则
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/user/login").permitAll()
                // 公开课程周列表（如果需要匿名访问）
                .requestMatchers("/api/courses/week").permitAll()
                .anyRequest().authenticated()
            )
            // 未认证返回自定义 JSON（不带 Basic 挑战）
            .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint));

        // 注册 Token 过滤器
        http.addFilterBefore(new TokenAuthenticationFilter(authService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}