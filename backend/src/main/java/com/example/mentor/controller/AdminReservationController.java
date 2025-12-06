package com.example.mentor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentor.dao.entity.Reservation;
import com.example.mentor.dao.entity.User;
import com.example.mentor.dao.mapper.ReservationMapper;
import com.example.mentor.dto.Result;
import com.example.mentor.service.AuthService;
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
    private final AuthService authService;

    // 管理员查询课程预约名单（含用户昵称头像）
    @GetMapping("/reservations/course/{courseId}")
    public Result<Map<String, Object>> getCourseReservations(
            @PathVariable("courseId") Long courseId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // TODO: 校验管理员权限
        List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getCourseId, courseId)
                .eq(Reservation::getStatus, "reserved"));
        // 批量查询用户信息以渲染昵称与头像
        Set<Long> userIds = reservations.stream().map(Reservation::getUserId).collect(Collectors.toSet());
        Map<Long, com.example.mentor.dao.entity.User> userMap = new HashMap<>();
        if(!userIds.isEmpty()){
            List<com.example.mentor.dao.entity.User> users = userMapper.selectBatchIds(new ArrayList<>(userIds));
            for(com.example.mentor.dao.entity.User u: users){ userMap.put(u.getId(), u); }
        }

        List<Map<String, Object>> items = reservations.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("reservation_id", r.getId());
            m.put("status", r.getStatus());
            m.put("checkin_status", r.getCheckinStatus());
            m.put("created_at", r.getCreatedAt());
            com.example.mentor.dao.entity.User u = userMap.get(r.getUserId());
            Map<String,Object> user = new LinkedHashMap<>();
            user.put("id", r.getUserId());
            user.put("nickname", u!=null ? u.getNickname() : null);
            user.put("avatar", u!=null ? u.getAvatar() : null);
            m.put("user", user);
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", items);
        data.put("total", items.size());
        return Result.buildSuccess(data);
    }

    // 管理员签到核销（占位）
    @PostMapping("/checkin")
    public Result<String> checkin(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // TODO: 校验管理员权限
        Object ridObj = body.get("reservation_id");
        if (ridObj == null) return Result.buildFailure(400, "reservation_id不能为空");
        Long reservationId = Long.valueOf(ridObj.toString());
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null) return Result.buildFailure(404, "预约记录不存在");
        r.setCheckinStatus("checked");
        reservationMapper.updateById(r);
        return Result.buildSuccess("签到成功（占位）");
    }
}
