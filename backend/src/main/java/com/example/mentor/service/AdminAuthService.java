package com.example.mentor.service;

public interface AdminAuthService {
    /**
     * 验证密码并返回 token
     */
    String login(String password);

    /**
     * 基于 token 修改密码
     */
    void changePassword(String token, String oldPassword, String newPassword);

    /**
     * 验证 token 是否有效
     */
    boolean validateToken(String token);
}
