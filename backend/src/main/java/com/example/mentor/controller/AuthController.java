package com.example.mentor.controller;

import com.example.mentor.dao.entity.User;
import com.example.mentor.dao.mapper.UserMapper;
import com.example.mentor.dto.Result;
import com.example.mentor.dto.request.LoginRequest;
import com.example.mentor.dto.request.RegisterRequest;
import com.example.mentor.dto.request.UpdateProfileRequest;
import com.example.mentor.dto.response.LoginResponse;
import com.example.mentor.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    /** 1.1 用户注册（手机号+密码） */
    @PostMapping("/register")
    public ResponseEntity<Result<?>> register(@RequestBody RegisterRequest req) {
        try {
            String token = authService.registerByPhone(req.getPhone(), req.getPassword());
            User user = authService.getUserInfoByToken(token);
            return ResponseEntity.ok(Result.buildSuccess(new LoginResponse(token, user)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.buildFailure(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.buildFailure(500, "注册失败：" + e.getMessage()));
        }
    }

    /** 1.2 登录（手机号+密码） */
    @PostMapping("/login")
    public ResponseEntity<Result<?>> login(@RequestBody LoginRequest req) {
        try {
            String token = authService.loginByPhone(req.getPhone(), req.getPassword());
            User user = authService.getUserInfoByToken(token);
            return ResponseEntity.ok(Result.buildSuccess(new LoginResponse(token, user)));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            int code = 400;
            if ("账号被禁用".equals(msg)) {
                code = 403;
            } else if ("用户名或密码错误".equals(msg)) {
                code = 401;
            }
            return ResponseEntity.ok(Result.buildFailure(code, msg));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.buildFailure(500, "登录失败：" + e.getMessage()));
        }
    }

    /** 1.3 获取当前登录用户信息 */
    @GetMapping("/profile")
    public ResponseEntity<Result<?>> getUserInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenParam,
            HttpServletRequest request
    ) {
        String token = extractToken(authorization, tokenParam, request);
        try {
            User user = authService.getUserInfoByToken(token);
            return ResponseEntity.ok(Result.buildSuccess(user));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.buildFailure(401, "登录失效"));
        }
    }

    /** 1.4 更新用户信息 */
    @PutMapping("/profile")
    public ResponseEntity<Result<?>> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenParam,
            @RequestBody UpdateProfileRequest body,
            HttpServletRequest request
    ) {
        String token = extractToken(authorization, tokenParam, request);
        try {
            User user = authService.getUserInfoByToken(token);

            if (StringUtils.hasText(body.getRealName())) {
                user.setRealName(body.getRealName());
            }
            if (StringUtils.hasText(body.getPhone())) {
                user.setPhone(body.getPhone());
            }
            if (StringUtils.hasText(body.getAvatar())) {
                user.setAvatar(body.getAvatar());
            }

            userMapper.updateById(user);
            return ResponseEntity.ok(Result.buildSuccess(user));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.buildFailure(401, "登录失效"));
        }
    }

    /** 接口文档 8.1 ：token 提取逻辑 */
    private String extractToken(String authorization, String tokenParam, HttpServletRequest request) {
        String token = null;
        if (StringUtils.hasText(authorization)) {
            if (authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            } else {
                token = authorization;
            }
        }
        if (!StringUtils.hasText(token)) {
            token = tokenParam;
        }
        if (!StringUtils.hasText(token) && request.getCookies() != null) {
            for (var c : request.getCookies()) {
                if ("token".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        return token;
    }
}