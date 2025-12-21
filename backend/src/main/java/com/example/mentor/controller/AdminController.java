package com.example.mentor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mentor.dao.entity.Admin;
import com.example.mentor.dao.entity.Card;
import com.example.mentor.dao.entity.User;
import com.example.mentor.dao.mapper.CardMapper;
import com.example.mentor.dao.mapper.UserMapper;
import com.example.mentor.dto.Result;
import com.example.mentor.exception.BizException;
import com.example.mentor.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final CardMapper cardMapper;
    private final AdminAuthService adminAuthService;

    /**
     * 管理员登录
     * POST /api/admin/login
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        try {
            String phone = body.getOrDefault("phone", "");
            String password = body.getOrDefault("password", "");

            String token = adminAuthService.login(phone, password); 
            Admin admin = adminAuthService.getAdminByToken(token);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("role", admin.getRole());
            data.put("username", admin.getUsername());

            return Result.buildSuccess(data);
        } catch (BizException e) {
            return Result.buildFailure(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.buildFailure(500, "登录失败：" + e.getMessage());
        }
    }

    /**
     * 管理员分页检索学员
     * GET
     * /api/admin/users?page=1&page_size=20&q=关键词&has_valid_card=true&card_status=active
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(value = "page_size", defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String q,
            @RequestParam(value = "has_valid_card", required = false) Boolean hasValidCard,
            @RequestParam(value = "card_status", required = false) String cardStatus,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // 校验管理员权限
        try {
            String token = adminAuthService.extractToken(authorization);
            if (!adminAuthService.verifyAdminPermission(token)) {
                return Result.buildFailure(403, "权限不足");
            }
        } catch (Exception e) {
            return Result.buildFailure(401, "未授权");
        }

        if (pageSize > 100)
            pageSize = 100;

        Page<User> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索：昵称、真实姓名、手机号
        if (StringUtils.hasText(q)) {
            wrapper.and(w -> w.like(User::getNickname, q)
                    .or().like(User::getRealName, q)
                    .or().like(User::getPhone, q));
        }

        IPage<User> userPage = userMapper.selectPage(pageParam, wrapper);

        // 查询用户的卡片信息
        List<Long> userIds = userPage.getRecords().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        Map<Long, List<Card>> userCardsMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<Card> cards = cardMapper.selectList(
                    new LambdaQueryWrapper<Card>().in(Card::getUserId, userIds));
            for (Card card : cards) {
                userCardsMap.computeIfAbsent(card.getUserId(), k -> new ArrayList<>()).add(card);
            }
        }

        // 组装返回数据
        List<Map<String, Object>> userList = userPage.getRecords().stream()
                .map(user -> {
                    Map<String, Object> userMap = new LinkedHashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("openid", user.getOpenid());
                    userMap.put("nickname", user.getNickname());
                    userMap.put("real_name", user.getRealName());
                    userMap.put("phone", user.getPhone());
                    userMap.put("avatar", user.getAvatar());
                    userMap.put("status", user.getStatus());

                    List<Card> cards = userCardsMap.getOrDefault(user.getId(), new ArrayList<>());
                    userMap.put("cards", cards.stream().map(this::cardToMap).collect(Collectors.toList()));

                    // 判断是否有有效卡
                    boolean hasValid = cards.stream().anyMatch(this::isCardValid);
                    userMap.put("has_valid_card", hasValid);

                    return userMap;
                })
                .filter(userMap -> {
                    // 筛选条件：是否有有效卡
                    if (hasValidCard != null) {
                        return hasValidCard.equals(userMap.get("has_valid_card"));
                    }
                    return true;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", userList);
        result.put("meta", Map.of(
                "total", userPage.getTotal(),
                "page", page,
                "page_size", pageSize));

        return Result.buildSuccess(result);
    }

    /**
     * 管理员发放导师卡
     * POST /api/admin/cards
     */
    @PostMapping("/cards")
    @Transactional
    public Result<Map<String, Object>> issueCard(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // 校验管理员权限
        try {
            String token = adminAuthService.extractToken(authorization);
            if (!adminAuthService.verifyAdminPermission(token)) {
                return Result.buildFailure(403, "权限不足");
            }
        } catch (Exception e) {
            return Result.buildFailure(401, "未授权");
        }

        Object userIdObj = body.get("user_id");
        String cardType = (String) body.get("card_type");
        String faceImageUrl = (String) body.get("face_image_url");

        if (userIdObj == null || !StringUtils.hasText(cardType)) {
            return Result.buildFailure(400, "user_id和card_type不能为空");
        }

        Long userId = Long.valueOf(userIdObj.toString());

        // 校验用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.buildFailure(404, "用户不存在");
        }

        // 校验卡片类型
        if (!Arrays.asList("semester", "year", "lifetime").contains(cardType)) {
            return Result.buildFailure(400, "卡片类型无效，仅支持：semester、year、lifetime");
        }

        // 生成卡号
        String cardNumber = generateCardNumber();

        // 计算有效期
        Date now = new Date();
        Date startDate = now;
        Date endDate = calculateEndDate(cardType, now);

        // 创建卡片
        Card card = new Card();
        card.setUserId(userId);
        card.setCardNumber(cardNumber);
        card.setCardType(cardType);
        card.setStatus("active");
        card.setStartDate(startDate);
        card.setEndDate(endDate);
        card.setCreatedAt(now);
        card.setUpdatedAt(now);
        card.setFaceImageUrl(faceImageUrl);

        cardMapper.insert(card);

        Map<String, Object> result = new HashMap<>();
        result.put("card_id", card.getId());
        result.put("card_number", cardNumber);
        result.put("card_type", cardType);
        result.put("status", "active");
        result.put("start_date", startDate);
        result.put("end_date", endDate);
        result.put("face_image_url", faceImageUrl);
        result.put("user", Map.of(
                "id", user.getId(),
                "nickname", user.getNickname(),
                "real_name", user.getRealName()));

        return Result.buildSuccess(result);
    }

    /**
     * 管理员批量发卡
     * POST /api/admin/cards/batch
     */
    @PostMapping("/cards/batch")
    @Transactional
    public Result<Map<String, Object>> issueCardsBatch(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // 校验管理员权限
        try {
            String token = adminAuthService.extractToken(authorization);
            if (!adminAuthService.verifyAdminPermission(token)) {
                return Result.buildFailure(403, "权限不足");
            }
        } catch (Exception e) {
            return Result.buildFailure(401, "未授权");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cardRequests = (List<Map<String, Object>>) body.get("cards");

        if (cardRequests == null || cardRequests.isEmpty()) {
            return Result.buildFailure(400, "cards列表不能为空");
        }

        List<Map<String, Object>> successList = new ArrayList<>();
        List<Map<String, Object>> failureList = new ArrayList<>();

        for (Map<String, Object> req : cardRequests) {
            try {
                Object userIdObj = req.get("user_id");
                String cardType = (String) req.get("card_type");
                String faceImageUrl = (String) req.get("face_image_url");

                if (userIdObj == null || !StringUtils.hasText(cardType)) {
                    failureList.add(Map.of(
                            "user_id", userIdObj,
                            "reason", "user_id或card_type为空"));
                    continue;
                }

                Long userId = Long.valueOf(userIdObj.toString());
                User user = userMapper.selectById(userId);
                if (user == null) {
                    failureList.add(Map.of(
                            "user_id", userId,
                            "reason", "用户不存在"));
                    continue;
                }

                if (!Arrays.asList("semester", "year", "lifetime").contains(cardType)) {
                    failureList.add(Map.of(
                            "user_id", userId,
                            "reason", "卡片类型无效"));
                    continue;
                }

                String cardNumber = generateCardNumber();
                Date now = new Date();
                Date startDate = now;
                Date endDate = calculateEndDate(cardType, now);

                Card card = new Card();
                card.setUserId(userId);
                card.setCardNumber(cardNumber);
                card.setCardType(cardType);
                card.setStatus("active");
                card.setStartDate(startDate);
                card.setEndDate(endDate);
                card.setCreatedAt(now);
                card.setUpdatedAt(now);
                card.setFaceImageUrl(faceImageUrl);

                cardMapper.insert(card);

                successList.add(Map.of(
                        "user_id", userId,
                        "card_id", card.getId(),
                        "card_number", cardNumber,
                        "face_image_url", faceImageUrl));

            } catch (Exception e) {
                failureList.add(Map.of(
                        "user_id", req.get("user_id"),
                        "reason", e.getMessage()));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success_count", successList.size());
        result.put("failure_count", failureList.size());
        result.put("success_list", successList);
        result.put("failure_list", failureList);

        return Result.buildSuccess(result);
    }

    // ============ 辅助方法 ============

    /**
     * 生成卡号
     */
    private String generateCardNumber() {
        return "CARD" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    /**
     * 根据卡片类型计算有效期
     */
    private Date calculateEndDate(String cardType, Date startDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        switch (cardType) {
            case "semester":
                // 学期卡：6个月
                calendar.add(Calendar.MONTH, 6);
                break;
            case "year":
                // 年卡：12个月
                calendar.add(Calendar.YEAR, 1);
                break;
            case "lifetime":
                // 终身卡：100年
                calendar.add(Calendar.YEAR, 100);
                break;
            default:
                throw new IllegalArgumentException("未知的卡片类型：" + cardType);
        }

        return calendar.getTime();
    }

    /**
     * 判断卡片是否有效
     */
    private boolean isCardValid(Card card) {
        if (!"active".equals(card.getStatus())) {
            return false;
        }
        if ("lifetime".equals(card.getCardType())) {
            return true;
        }
        Date now = new Date();
        return (card.getStartDate() == null || !now.before(card.getStartDate())) &&
                (card.getEndDate() == null || !now.after(card.getEndDate()));
    }

    /**
     * 将Card对象转换为Map
     */
    private Map<String, Object> cardToMap(Card card) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", card.getId());
        map.put("card_number", card.getCardNumber());
        map.put("card_type", card.getCardType());
        map.put("status", card.getStatus());
        map.put("start_date", card.getStartDate());
        map.put("end_date", card.getEndDate());
        map.put("is_valid", isCardValid(card));
        map.put("face_image_url", card.getFaceImageUrl());
        return map;
    }
}
