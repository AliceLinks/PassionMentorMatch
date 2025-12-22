    package com.example.mentor.service;

    import com.example.mentor.dao.entity.User;

    public interface AuthService {
        String loginByWeChatCode(String code);
        String loginByPhoneAndPassword(String phone, String password);
        User getUserInfoByToken(String token);
        void register(String phone, String password, String realName, String avatar);
    }