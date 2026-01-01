package com.example.mentor.controller;

import com.example.mentor.dao.entity.Card;
import com.example.mentor.dao.entity.User;
import com.example.mentor.dao.mapper.CardMapper;
import com.example.mentor.dao.mapper.UserMapper;
import com.example.mentor.dto.Result;
import com.example.mentor.dto.request.CardIssueRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Date;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
public class AdminCardController {
    private final CardMapper cardMapper;
    private final UserMapper userMapper;

    // 管理员发放导师卡
    @PostMapping
    public ResponseEntity<Result<?>> issueCard(@RequestBody CardIssueRequest req) {
        // 通过手机号查找用户
        if (req.getPhone() == null || req.getPhone().isEmpty()) {
            return ResponseEntity.badRequest().body(Result.buildFailure(400, "PHONE_REQUIRED", "手机号不能为空"));
        }
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>().eq(User::getPhone,
                        req.getPhone()));
        if (user == null) {
            return ResponseEntity.badRequest().body(Result.buildFailure(400, "USER_NOT_FOUND", "用户不存在"));
        }
        // 同步实名头像到用户表
        if (req.getRealNameImage() != null && !req.getRealNameImage().isEmpty()) {
            user.setRealNameImage(req.getRealNameImage());
            userMapper.updateById(user);
        }
        String type = req.getCardType();
        long millisInDay = 24L * 60 * 60 * 1000;
        Date endDate;
        if ("semester".equalsIgnoreCase(type)) {
            endDate = new Date(req.getStartDate().getTime() + 183L * millisInDay); // 6个月约183天
        } else if ("annual".equalsIgnoreCase(type)) {
            endDate = new Date(req.getStartDate().getTime() + 365L * millisInDay); // 1年
        } else if ("lifetime".equalsIgnoreCase(type)) {
            endDate = new Date(req.getStartDate().getTime() + 36500L * millisInDay); // 100年
        } else {
            return ResponseEntity.badRequest().body(Result.buildFailure(400, "CARD_TYPE_INVALID", "卡类型无效"));
        }
        // 检查该用户是否已有卡
        Card existingCard = cardMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Card>().eq("user_id", user.getId()));
        Card card;
        String action;
        if (existingCard != null) {
            // 更新卡信息
            existingCard.setCardType(type);
            existingCard.setStatus("active");
            existingCard.setStartDate(req.getStartDate());
            existingCard.setEndDate(endDate);
            existingCard.setUpdatedAt(new Date());
            existingCard.setRealNameImage(req.getRealNameImage());
            cardMapper.updateById(existingCard);
            card = existingCard;
            action = "updated";
        } else {
            // 新建卡
            card = new Card();
            card.setUserId(user.getId());
            String cardNumber = "CARD" + System.currentTimeMillis() + user.getId();
            card.setCardNumber(cardNumber);
            card.setCardType(type);
            card.setStatus("active");
            card.setStartDate(req.getStartDate());
            card.setEndDate(endDate);
            card.setCreatedAt(new Date());
            card.setUpdatedAt(new Date());
            card.setRealNameImage(req.getRealNameImage());
            cardMapper.insert(card);
            action = "created";
        }
        java.util.Map<String, Object> resultData = new java.util.HashMap<>();
        resultData.put("action", action);
        resultData.put("card", card);
        return ResponseEntity.ok(Result.buildSuccess(resultData));
    }
}
