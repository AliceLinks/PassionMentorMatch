package com.example.mentor.service;

import com.example.mentor.dao.entity.User;

public interface AuthService {
    String loginByWeChatCode(String code);
    User getUserInfoByToken(String token);
}