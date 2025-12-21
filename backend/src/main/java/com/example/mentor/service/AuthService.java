package com.example.mentor.service;

import com.example.mentor.dao.entity.User;

public interface AuthService {
    // 可继续支持微信登录(备选)
    String loginByWeChatCode(String code);

    // 手机号+密码注册
    String registerByPhone(String phone, String password);

    // 手机号+密码登录
    String loginByPhone(String phone, String password);

    User getUserInfoByToken(String token);
}