package com.example.mentor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentor.dao.entity.Card;
import com.example.mentor.dao.entity.User;
import com.example.mentor.dao.mapper.CardMapper;
import com.example.mentor.dto.Result;
import com.example.mentor.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserCardController {

    private final CardMapper cardMapper;
    private final AuthService authService;
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd");

    // GET /api/user/cards -> 返回用户的导师卡列表
    @GetMapping("/cards")
    public ResponseEntity<Result<?>> cards(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String xAuthToken) {
        String token = parseToken(authorization, xAuthToken);
        if (token == null) {
            return ResponseEntity.ok(Result.buildSuccess(Collections.emptyList()));
        }
        User u;
        try {
            u = authService.getUserInfoByToken(token);
        } catch (Exception e) {
            return ResponseEntity.ok(Result.buildSuccess(Collections.emptyList()));
        }
        List<Card> cards = cardMapper.selectList(new LambdaQueryWrapper<Card>().eq(Card::getUserId, u.getId()));
        List<Map<String, Object>> list = new ArrayList<>();
        Date today = new Date();
        for (Card c : cards) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("card_number", c.getCardNumber());
            m.put("card_type", c.getCardType());
            m.put("status", c.getStatus());
            m.put("start_date", c.getStartDate() == null ? "" : DF.format(c.getStartDate()));
            m.put("end_date", c.getEndDate() == null ? "" : DF.format(c.getEndDate()));
            m.put("real_name_image", c.getRealNameImage());
            // 可选：是否当前有效
            boolean active = "active".equalsIgnoreCase(c.getStatus())
                    && ("lifetime".equals(c.getCardType()) || (c.getStartDate() != null && c.getEndDate() != null
                            && !today.before(c.getStartDate()) && !today.after(c.getEndDate())));
            m.put("active", active);
            list.add(m);
        }
        return ResponseEntity.ok(Result.buildSuccess(list));
    }

    private String parseToken(String authorization, String xAuthToken) {
        if (authorization != null && authorization.startsWith("Bearer "))
            return authorization.substring(7);
        if (xAuthToken != null && !xAuthToken.isEmpty())
            return xAuthToken;
        if (authorization != null && authorization.startsWith("Basic "))
            return null;
        return authorization;
    }
}
