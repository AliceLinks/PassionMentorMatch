package com.example.mentor.service.impl;

import com.example.mentor.dao.entity.AdminAccount;
import com.example.mentor.dao.entity.AdminToken;
import com.example.mentor.dao.mapper.AdminAccountMapper;
import com.example.mentor.dao.mapper.AdminTokenMapper;
import com.example.mentor.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {
    private final AdminAccountMapper adminAccountMapper;
    private final AdminTokenMapper adminTokenMapper;

    @Override
    public String login(String password) {
        // read the first admin account (minimal single-admin design)
        AdminAccount acc = adminAccountMapper.selectById(1L);
        if (acc == null) {
            throw new RuntimeException("管理员未初始化");
        }
        String md5 = DigestUtils.md5Hex(password == null ? "" : password);
        if (!md5.equalsIgnoreCase(acc.getPasswordHash())) {
            throw new RuntimeException("密码错误");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        AdminToken at = new AdminToken();
        at.setToken(token);
        at.setExpireAt(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
        adminTokenMapper.insert(at);
        return token;
    }

    @Override
    public void changePassword(String token, String oldPassword, String newPassword) {
        if (!validateToken(token))
            throw new RuntimeException("未登录或已过期");
        AdminAccount acc = adminAccountMapper.selectById(1L);
        if (acc == null)
            throw new RuntimeException("管理员未初始化");
        String oldMd5 = DigestUtils.md5Hex(oldPassword == null ? "" : oldPassword);
        if (!oldMd5.equalsIgnoreCase(acc.getPasswordHash()))
            throw new RuntimeException("原密码错误");
        String newMd5 = DigestUtils.md5Hex(newPassword == null ? "" : newPassword);
        acc.setPasswordHash(newMd5);
        adminAccountMapper.updateById(acc);
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty())
            return false;
        AdminToken at = adminTokenMapper
                .selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AdminToken>()
                        .eq(AdminToken::getToken, token));
        if (at == null)
            return false;
        return at.getExpireAt() != null && at.getExpireAt().after(new Date());
    }
}
