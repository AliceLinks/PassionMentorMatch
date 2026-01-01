package com.example.mentor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentor.config.WeChatProperties;
import com.example.mentor.config.AuthProperties;
import com.example.mentor.dao.entity.User;
import com.example.mentor.dao.entity.UserToken;
import com.example.mentor.dao.mapper.UserMapper;
import com.example.mentor.dao.mapper.UserTokenMapper;
import com.example.mentor.dto.response.WeChatSessionResponse;
import com.example.mentor.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final WeChatProperties weChatProps;
    private final AuthProperties authProps;
    private final RestTemplate restTemplate;
    private final UserMapper userMapper;
    private final UserTokenMapper userTokenMapper;

    @Override
    public void register(String phone, String password, String realName, String avatar) {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("手机号和密码不能为空");
        }
        User exist = userMapper.selectByPhone(phone);
        if (exist != null) {
            throw new RuntimeException("手机号已注册");
        }
        // MD5加密
        String encryptedPwd = org.apache.commons.codec.digest.DigestUtils.md5Hex(password);
        User user = new User();
        user.setPhone(phone);
        user.setPasswordHash(encryptedPwd); // passwordHash字段
        user.setStatus("active");
        user.setRealName(realName);
        user.setAvatar(avatar);
        userMapper.insert(user);
    }

    @Override
    public String loginByPhoneAndPassword(String phone, String password) {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("手机号和密码不能为空");
        }
        User user = userMapper.selectByPhone(phone);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String encryptedPwd = org.apache.commons.codec.digest.DigestUtils.md5Hex(password);
        if (!encryptedPwd.equals(user.getPasswordHash())) {
            throw new RuntimeException("密码错误");
        }
        // 生成 token 并保存
        String token = UUID.randomUUID().toString();
        UserToken userToken = new UserToken();
        userToken.setUserId(user.getId());
        userToken.setToken(token);
        userToken.setExpiredAt(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
        userTokenMapper.insert(userToken);
        return token;
    }

    @Override
    public String loginByWeChatCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("code不能为空");
        }
        if (!StringUtils.hasText(weChatProps.getAppid()) || !StringUtils.hasText(weChatProps.getSecret())) {
            throw new IllegalArgumentException("未配置微信凭证(WECHAT_APPID/WECHAT_SECRET)");
        }
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                weChatProps.getAppid(), weChatProps.getSecret(), code
        );
        String body;
        try {
            body = restTemplate.getForObject(url, String.class);
        } catch (Exception ex) {
            log.warn("调用微信接口失败: {}", ex.getMessage(), ex);
            throw new RuntimeException("微信接口调用失败");
        }
        if (!StringUtils.hasText(body)) {
            throw new RuntimeException("微信接口返回空");
        }
        log.debug("微信返回: {}", body);
        WeChatSessionResponse wx;
        try {
            wx = new ObjectMapper().readValue(body, WeChatSessionResponse.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("解析微信返回失败", e);
        }
        if (wx.getErrcode() != null && wx.getErrcode() != 0) {
            String msg = String.format("微信登录失败: errcode=%s, errmsg=%s", String.valueOf(wx.getErrcode()), wx.getErrmsg());
            throw new RuntimeException(msg);
        }
        String openid = wx.getOpenid();
        if (!StringUtils.hasText(openid)) {
            throw new RuntimeException("未获取到openid");
        }

        // 字段名与实体对齐：openid
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setStatus("active");
            userMapper.insert(user);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        Date expireAt = Date.from(Instant.now().plus(authProps.getTokenTtlDays(), ChronoUnit.DAYS));

        // 单用户只保留一个有效 token
        userTokenMapper.delete(new LambdaQueryWrapper<UserToken>().eq(UserToken::getUserId, user.getId()));

        UserToken ut = new UserToken();
        ut.setUserId(user.getId());
        ut.setToken(token);
        ut.setExpiredAt(expireAt);
        userTokenMapper.insert(ut);

        return token;
    }

    @Override
    public User getUserInfoByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("未提供token");
        }
        UserToken ut = userTokenMapper.selectOne(new LambdaQueryWrapper<UserToken>().eq(UserToken::getToken, token));
        if (ut == null || ut.getExpiredAt().before(new Date())) {
            throw new RuntimeException("登录失效");
        }
        return userMapper.selectById(ut.getUserId());
    }
}
