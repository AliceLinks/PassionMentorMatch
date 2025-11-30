package com.example.mentor.controller;

import com.example.mentor.dao.entity.User;
import com.example.mentor.dto.request.LoginRequest;
import com.example.mentor.dto.response.LoginResponse;
import com.example.mentor.dto.Result;
import com.example.mentor.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/user/login -> 返回 token + user
    @PostMapping("/login")
    public ResponseEntity<Result<?>> login(@RequestBody LoginRequest loginRequest) {
        String token = authService.loginByWeChatCode(loginRequest.getCode());
        User user = authService.getUserInfoByToken(token);
        return ResponseEntity.ok(Result.buildSuccess(new LoginResponse(token, user)));
    }

    // GET /api/user/profile
    @GetMapping("/profile")
    public ResponseEntity<Result<?>> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = (authorization != null && authorization.startsWith("Bearer ")) ? authorization.substring(7) : authorization;
        return ResponseEntity.ok(Result.buildSuccess(authService.getUserInfoByToken(token)));
    }
}