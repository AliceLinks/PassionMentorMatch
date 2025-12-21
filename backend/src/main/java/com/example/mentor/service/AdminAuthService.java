package com.example.mentor.service;

import com.example.mentor.dao.entity.Admin;

public interface AdminAuthService {
    /**
     * 管理员登录
     * 
     * @param username 手机号   
     * @param password 密码
     * @return token
     */
    String login(String username, String password);

    /**
     * 根据token获取管理员信息
     * 
     * @param token token
     * @return 管理员信息
     */
    Admin getAdminByToken(String token);

    /**
     * 验证管理员权限
     * 
     * @param token token
     * @return 是否为有效管理员
     */
    boolean verifyAdminPermission(String token);

    /**
     * 从Authorization header中提取token
     * 
     * @param authorization Authorization header
     * @return token
     */
    String extractToken(String authorization);
}
