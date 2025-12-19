package com.example.mentor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentor.dao.entity.Reservation;
import com.example.mentor.dao.entity.User;
import com.example.mentor.dao.mapper.ReservationMapper;
import com.example.mentor.dto.Result;
import com.example.mentor.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdminReservationController {

    private final ReservationMapper reservationMapper;
    private final com.example.mentor.dao.mapper.UserMapper userMapper;
    private final AdminAuthService adminAuthService;

    // 管理员查询课程预约名单（含用户昵称头像）
    @GetMapping("/reservations/course/{courseId}")
    public Result<Map<String, Object>> getCourseReservations(
            @PathVariable("courseId") Long courseId,
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
        List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getCourseId, courseId)
                .eq(Reservation::getStatus, "reserved"));
        // 批量查询用户信息以渲染昵称与头像
        Set<Long> userIds = reservations.stream().map(Reservation::getUserId).collect(Collectors.toSet());
        Map<Long, com.example.mentor.dao.entity.User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<com.example.mentor.dao.entity.User> users = userMapper.selectBatchIds(new ArrayList<>(userIds));
            for (com.example.mentor.dao.entity.User u : users) {
                userMap.put(u.getId(), u);
            }
        }

        List<Map<String, Object>> items = reservations.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("reservation_id", r.getId());
            m.put("status", r.getStatus());
            m.put("checkin_status", r.getCheckinStatus());
            m.put("created_at", r.getCreatedAt());
            com.example.mentor.dao.entity.User u = userMap.get(r.getUserId());
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", r.getUserId());
            user.put("nickname", u != null ? u.getNickname() : null);
            user.put("avatar", u != null ? u.getAvatar() : null);
            m.put("user", user);
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", items);
        data.put("total", items.size());
        return Result.buildSuccess(data);
    }

    /**
     * 管理员签到核销
     * POST /api/checkin
     * 支持两种方式：
     * 1. 通过 reservation_id 直接签到
     * 2. 通过 course_id + user_id 签到
     */
    @PostMapping("/checkin")
    public Result<Map<String, Object>> checkin(
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

        Object ridObj = body.get("reservation_id");
        Object courseIdObj = body.get("course_id");
        Object userIdObj = body.get("user_id");
        String method = (String) body.getOrDefault("method", "manual");

        Reservation reservation = null;

        // 方式1：通过 reservation_id 签到
        if (ridObj != null) {
            Long reservationId = Long.valueOf(ridObj.toString());
            reservation = reservationMapper.selectById(reservationId);
        }
        // 方式2：通过 course_id + user_id 签到
        else if (courseIdObj != null && userIdObj != null) {
            Long courseId = Long.valueOf(courseIdObj.toString());
            Long userId = Long.valueOf(userIdObj.toString());

            reservation = reservationMapper.selectOne(
                    new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getCourseId, courseId)
                            .eq(Reservation::getUserId, userId)
                            .eq(Reservation::getStatus, "reserved"));
        } else {
            return Result.buildFailure(400, "必须提供 reservation_id 或 (course_id + user_id)");
        }

        if (reservation == null) {
            return Result.buildFailure(404, "预约记录不存在或状态异常");
        }

        // 检查是否已签到
        if ("checked_in".equals(reservation.getCheckinStatus())) {
            return Result.buildFailure(409, "该预约已签到，请勿重复签到");
        }

        // 检查预约状态
        if (!"reserved".equals(reservation.getStatus())) {
            return Result.buildFailure(403, "预约状态异常，无法签到");
        }

        // 更新签到状态
        reservation.setCheckinStatus("checked_in");
        reservation.setUpdatedAt(new java.util.Date());
        reservationMapper.updateById(reservation);

        // 查询用户信息用于返回
        User user = userMapper.selectById(reservation.getUserId());

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("reservation_id", reservation.getId());
        result.put("course_id", reservation.getCourseId());
        result.put("user_id", reservation.getUserId());
        result.put("checkin_status", reservation.getCheckinStatus());
        result.put("method", method);
        result.put("checkin_time", reservation.getUpdatedAt());

        if (user != null) {
            result.put("user_info", Map.of(
                    "nickname", user.getNickname(),
                    "real_name", user.getRealName() != null ? user.getRealName() : "",
                    "avatar", user.getAvatar()));
        }

        return Result.buildSuccess(result);
    }

    /**
     * 管理员批量签到
     * POST /api/checkin/batch
     */
    @PostMapping("/checkin/batch")
    public Result<Map<String, Object>> batchCheckin(
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

        List<?> idObjs = (List<?>) body.get("reservation_ids");
        if (idObjs == null || idObjs.isEmpty()) {
            return Result.buildFailure(400, "reservation_ids不能为空");
        }
        List<Long> reservationIds = idObjs.stream().map(obj -> ((Number) obj).longValue()).collect(Collectors.toList());

        List<Map<String, Object>> successList = new java.util.ArrayList<>();
        List<Map<String, Object>> failureList = new java.util.ArrayList<>();

        for (Long reservationId : reservationIds) {
            try {
                Reservation reservation = reservationMapper.selectById(reservationId);

                if (reservation == null) {
                    failureList.add(Map.of(
                            "reservation_id", reservationId,
                            "reason", "预约记录不存在"));
                    continue;
                }

                if ("checked_in".equals(reservation.getCheckinStatus())) {
                    failureList.add(Map.of(
                            "reservation_id", reservationId,
                            "reason", "已签到"));
                    continue;
                }

                if (!"reserved".equals(reservation.getStatus())) {
                    failureList.add(Map.of(
                            "reservation_id", reservationId,
                            "reason", "预约状态异常"));
                    continue;
                }

                reservation.setCheckinStatus("checked_in");
                reservation.setUpdatedAt(new java.util.Date());
                reservationMapper.updateById(reservation);

                successList.add(Map.of(
                        "reservation_id", reservationId,
                        "user_id", reservation.getUserId()));

            } catch (Exception e) {
                failureList.add(Map.of(
                        "reservation_id", reservationId,
                        "reason", e.getMessage()));
            }
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success_count", successList.size());
        result.put("failure_count", failureList.size());
        result.put("success_list", successList);
        result.put("failure_list", failureList);

        return Result.buildSuccess(result);
    }
}
