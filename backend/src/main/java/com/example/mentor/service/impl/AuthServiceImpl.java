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
public class AuthServiceImpl implements AuthService {

    private final WeChatProperties weChatProps;
    private final AuthProperties authProps;
    private final RestTemplate restTemplate;
    private final UserMapper userMapper;
    private final UserTokenMapper userTokenMapper;

    @Override
    public String loginByWeChatCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("code不能为空");
        }
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                weChatProps.getAppid(), weChatProps.getSecret(), code
        );
        String body = restTemplate.getForObject(url, String.class);
        if (!StringUtils.hasText(body)) {
            throw new RuntimeException("微信接口返回空");
        }
        WeChatSessionResponse wx;
        try {
            wx = new ObjectMapper().readValue(body, WeChatSessionResponse.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("解析微信返回失败", e);
        }
        if (wx.getErrcode() != null && wx.getErrcode() != 0) {
            throw new RuntimeException("微信登录失败: " + wx.getErrmsg());
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
            user.setNickname("游客");
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
        ut.setExpireAt(expireAt);
        userTokenMapper.insert(ut);

        return token;
    }

    @Override
    public User getUserInfoByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("未提供token");
        }
        UserToken ut = userTokenMapper.selectOne(new LambdaQueryWrapper<UserToken>().eq(UserToken::getToken, token));
        if (ut == null || ut.getExpireAt().before(new Date())) {
            throw new RuntimeException("登录失效");
        }
        return userMapper.selectById(ut.getUserId());
    }
}
