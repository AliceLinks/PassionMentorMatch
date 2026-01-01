package com.example.mentor.controller;

import com.example.mentor.dao.entity.User;
import com.example.mentor.dto.request.LoginRequest;
import com.example.mentor.dto.response.LoginResponse;
import com.example.mentor.dto.Result;
import com.example.mentor.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    // POST /api/user/register -> 注册新用户
    @PostMapping("/register")
    public ResponseEntity<Result<?>> register(@RequestBody com.example.mentor.dto.request.RegisterRequest registerRequest) {
        try {
            authService.register(
                registerRequest.getPhone(),
                registerRequest.getPassword(),
                registerRequest.getRealName(),
                registerRequest.getAvatar()
            );
            return ResponseEntity.ok(Result.buildSuccess("注册成功"));
        } catch (IllegalArgumentException e) {
            log.warn("Register bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Result.buildFailure(400, "400", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("Register failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Result.buildFailure(400, "REGISTER_FAILED", e.getMessage()));
        }
    }

    private final AuthService authService;
    private final com.example.mentor.dao.mapper.UserMapper userMapper;

    // POST /api/user/login -> 返回 token + user
    @PostMapping("/login")
    public ResponseEntity<Result<?>> login(@RequestBody LoginRequest loginRequest) {
        try {
            String token = authService.loginByPhoneAndPassword(loginRequest.getPhone(), loginRequest.getPassword());
            User user = authService.getUserInfoByToken(token);
            return ResponseEntity.ok(Result.buildSuccess(new LoginResponse(token, user)));
        } catch (IllegalArgumentException e) {
            log.warn("Login bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Result.buildFailure(400, "400", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("Login failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Result.buildFailure(400, "LOGIN_FAILED", e.getMessage()));
        }
    }

    // GET /api/user/profile
    @GetMapping("/profile")
    public ResponseEntity<Result<?>> getUserInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String xAuthToken) {
        String token = parseToken(authorization, xAuthToken);
        try {
            User user = authService.getUserInfoByToken(token);
            return ResponseEntity.ok(Result.buildSuccess(user));
        } catch (IllegalArgumentException e) {
            // 缺少 token 语义上应返回 401，便于前端自动跳转登录
            if ("未提供token".equals(e.getMessage())) {
                log.warn("Profile unauthorized: {}", e.getMessage());
                return ResponseEntity.status(401).body(Result.buildFailure(401, "UNAUTHORIZED", e.getMessage()));
            }
            log.warn("Profile bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Result.buildFailure(400, "400", e.getMessage()));
        } catch (RuntimeException e) {
            // 登录失效等
            log.warn("Profile unauthorized runtime: {}", e.getMessage());
            return ResponseEntity.status(401).body(Result.buildFailure(401, "UNAUTHORIZED", e.getMessage()));
        }
    }

    // PUT /api/user/profile  更新昵称/头像/姓名/手机号
    @PutMapping("/profile")
    public ResponseEntity<Result<?>> updateUserProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String xAuthToken,
            @RequestBody java.util.Map<String, Object> body) {
        String token = parseToken(authorization, xAuthToken);
        try {
            User user = authService.getUserInfoByToken(token);
            if (body == null) {
                return ResponseEntity.badRequest().body(Result.buildFailure(400, "BAD_REQUEST", "请求体为空"));
            }

            // 允许更新的字段，兼容 realName（驼峰）和 real_name（下划线）
            Object avatar = body.get("avatar");
            Object realName = body.get("realName");
            Object real_name = body.get("real_name");
            Object phone = body.get("phone");

            if (avatar != null) user.setAvatar(String.valueOf(avatar));
            // 优先 realName，其次 real_name
            if (realName != null) {
                user.setRealName(String.valueOf(realName));
            } else if (real_name != null) {
                user.setRealName(String.valueOf(real_name));
            }
            if (phone != null) user.setPhone(String.valueOf(phone));

            // 简单校验：手机号长度
            if (user.getPhone() != null && user.getPhone().length() > 32) {
                return ResponseEntity.badRequest().body(Result.buildFailure(400, "BAD_REQUEST", "手机号不合法"));
            }

            log.info("[updateUserProfile] before update, real_name={}", user.getRealName());
            // 持久化
            userMapper.updateById(user);
            log.info("[updateUserProfile] after update, real_name={}", user.getRealName());
            return ResponseEntity.ok(Result.buildSuccess(user));
        } catch (IllegalArgumentException e) {
            if ("未提供token".equals(e.getMessage())) {
                log.warn("Update profile unauthorized: {}", e.getMessage());
                return ResponseEntity.status(401).body(Result.buildFailure(401, "UNAUTHORIZED", e.getMessage()));
            }
            log.warn("Update profile bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Result.buildFailure(400, "400", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("Update profile unauthorized runtime: {}", e.getMessage());
            return ResponseEntity.status(401).body(Result.buildFailure(401, "UNAUTHORIZED", e.getMessage()));
        }
    }

    private String parseToken(String authorization, String xAuthToken) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        if (xAuthToken != null && !xAuthToken.isEmpty()) {
            return xAuthToken;
        }
        return (authorization != null && !authorization.startsWith("Basic ")) ? authorization : null;
    }
}