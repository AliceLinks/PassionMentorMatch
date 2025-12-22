package com.example.mentor.config;

import com.example.mentor.dto.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.mentor.security.TokenAuthenticationFilter;
import com.example.mentor.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationEntryPoint jsonAuthenticationEntryPoint,
                                                   AuthService authService) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    // 放行用户注册与登录接口（匿名访问）
                    .requestMatchers(HttpMethod.POST,
                            "/api/user/register",
                            "/api/user/login",
                            "/api/admin/login",
                            "/api/image/upload"   
                    ).permitAll()
                    // 其它 /api/** 需要登录
                    .requestMatchers("/api/**").authenticated()
                    // 其余静态资源等全部放行
                    .anyRequest().permitAll()
            )
            //把 TokenAuthenticationFilter 挂到 UsernamePasswordAuthenticationFilter 之前
            .addFilterBefore(new TokenAuthenticationFilter(authService),
                    UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e.authenticationEntryPoint(jsonAuthenticationEntryPoint));

        return http.build();
    }
}