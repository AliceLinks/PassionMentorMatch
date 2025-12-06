package com.example.mentor.controller;

import com.example.mentor.dto.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    // 占位：管理员登录，后续接入真实账号体系与权限
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        if (username.isEmpty() || password.isEmpty()) {
            return Result.buildFailure(400, "用户名或密码不能为空");
        }
        // 简化：接受任意非空用户名密码，返回占位token与角色
        Map<String, Object> data = new HashMap<>();
        data.put("token", "admin-token-placeholder");
        data.put("role", "ADMIN");
        data.put("username", username);
        return Result.buildSuccess(data);
    }
}
