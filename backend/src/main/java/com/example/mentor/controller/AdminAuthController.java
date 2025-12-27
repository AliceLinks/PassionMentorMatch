package com.example.mentor.controller;

import com.example.mentor.dto.Result;
import com.example.mentor.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        String pwd = body == null ? null : body.get("password");
        if (pwd == null)
            return Result.buildFailure(400, "密码不能为空");
        try {
            String token = adminAuthService.login(pwd);
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            return Result.buildSuccess(data);
        } catch (RuntimeException e) {
            return Result.buildFailure(401, e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestBody Map<String, String> body) {
        if (token == null || token.isEmpty())
            return Result.buildFailure(401, "未登录");
        String oldPwd = body == null ? null : body.get("old_password");
        String newPwd = body == null ? null : body.get("new_password");
        if (oldPwd == null || newPwd == null)
            return Result.buildFailure(400, "请求体不完整");
        try {
            adminAuthService.changePassword(token, oldPwd, newPwd);
            return Result.buildSuccess("OK");
        } catch (RuntimeException e) {
            return Result.buildFailure(400, e.getMessage());
        }
    }
}
