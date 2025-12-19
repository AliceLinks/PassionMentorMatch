package com.example.mentor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentor.config.AuthProperties;
import com.example.mentor.dao.entity.Admin;
import com.example.mentor.dao.entity.AdminToken;
import com.example.mentor.dao.mapper.AdminMapper;
import com.example.mentor.dao.mapper.AdminTokenMapper;
import com.example.mentor.exception.BizException;
import com.example.mentor.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminMapper adminMapper;
    private final AdminTokenMapper adminTokenMapper;
    private final AuthProperties authProperties;

    @Override
    public String login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BizException(400, "用户名或密码不能为空");
        }

        // 查询管理员
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getUsername, username));

        if (admin == null) {
            throw new BizException(401, "用户名或密码错误");
        }

        // 检查状态
        if (!"active".equals(admin.getStatus())) {
            throw new BizException(403, "账号已被禁用");
        }

        // 验证密码（MD5加密）
        String passwordHash = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!passwordHash.equals(admin.getPasswordHash())) {
            throw new BizException(401, "用户名或密码错误");
        }

        // 生成token
        String token = UUID.randomUUID().toString().replace("-", "");
        Date expireAt = Date.from(Instant.now().plus(authProperties.getTokenTtlDays(), ChronoUnit.DAYS));

        // 删除旧token
        adminTokenMapper.delete(
                new LambdaQueryWrapper<AdminToken>()
                        .eq(AdminToken::getAdminId, admin.getId()));

        // 保存新token
        AdminToken adminToken = new AdminToken();
        adminToken.setAdminId(admin.getId());
        adminToken.setToken(token);
        adminToken.setExpireAt(expireAt);
        adminToken.setCreatedAt(new Date());
        adminTokenMapper.insert(adminToken);

        // 更新最后登录时间
        admin.setLastLogin(new Date());
        adminMapper.updateById(admin);

        log.info("管理员登录成功: username={}, role={}", username, admin.getRole());
        return token;
    }

    @Override
    public Admin getAdminByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BizException(401, "未提供认证令牌");
        }

        // 查询token
        AdminToken adminToken = adminTokenMapper.selectOne(
                new LambdaQueryWrapper<AdminToken>()
                        .eq(AdminToken::getToken, token));

        if (adminToken == null) {
            throw new BizException(401, "认证令牌无效");
        }

        // 检查过期
        if (adminToken.getExpireAt().before(new Date())) {
            throw new BizException(401, "认证令牌已过期");
        }

        // 查询管理员信息
        Admin admin = adminMapper.selectById(adminToken.getAdminId());
        if (admin == null) {
            throw new BizException(401, "管理员不存在");
        }

        // 检查状态
        if (!"active".equals(admin.getStatus())) {
            throw new BizException(403, "账号已被禁用");
        }

        return admin;
    }

    @Override
    public boolean verifyAdminPermission(String token) {
        try {
            getAdminByToken(token);
            return true; // 只要是有效的管理员，就有所有权限
        } catch (BizException e) {
            log.warn("权限验证失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String extractToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }

        // 支持 "Bearer token" 格式
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }

        // 直接返回token
        return authorization;
    }

}
