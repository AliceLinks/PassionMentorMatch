package com.example.mentor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mentor.dao.entity.Reservation;
import com.example.mentor.dao.entity.UserToken;
import com.example.mentor.dao.mapper.ReservationMapper;
import com.example.mentor.dao.mapper.UserTokenMapper;
import com.example.mentor.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Slf4j
public class ReservationController {

    private final ReservationMapper reservationMapper;
    private final UserTokenMapper userTokenMapper;
    private final com.example.mentor.dao.mapper.CourseMapper courseMapper;
    private final com.example.mentor.dao.mapper.CardMapper cardMapper;

    // GET /api/reservations/my?page=1&page_size=20
    @GetMapping("/my")
    public ResponseEntity<Result<?>> myReservations(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String xAuthToken,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize
    ) {
        String token = parseToken(authorization, xAuthToken);
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Result.buildFailure(401, "UNAUTHORIZED", "未提供token"));
        }

        UserToken ut = userTokenMapper.selectOne(new LambdaQueryWrapper<UserToken>().eq(UserToken::getToken, token));
        if (ut == null) {
            return ResponseEntity.status(401).body(Result.buildFailure(401, "UNAUTHORIZED", "登录失效"));
        }

        Page<Reservation> p = new Page<>(Math.max(page, 1), Math.max(pageSize, 1));
        Page<Reservation> result = reservationMapper.selectPage(p,
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, ut.getUserId())
                        .orderByDesc(Reservation::getCreatedAt)
        );

        java.util.List<Reservation> reservations = result.getRecords();
        java.util.Set<Long> courseIds = new java.util.HashSet<>();
        for (Reservation r : reservations) {
            if (r.getCourseId() != null) courseIds.add(r.getCourseId());
        }
        java.util.Map<Long, com.example.mentor.dao.entity.Course> courseMap = new java.util.HashMap<>();
        if (!courseIds.isEmpty()) {
            java.util.List<com.example.mentor.dao.entity.Course> courses = courseMapper.selectBatchIds(new java.util.ArrayList<>(courseIds));
            for (com.example.mentor.dao.entity.Course c : courses) { courseMap.put(c.getId(), c); }
        }

        java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
        for (Reservation r : reservations) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("status", r.getStatus());
            m.put("checkin_status", r.getCheckinStatus());
            m.put("created_at", r.getCreatedAt());

            com.example.mentor.dao.entity.Course c = courseMap.get(r.getCourseId());
            if (c != null) {
                java.util.Map<String, Object> cm = new java.util.LinkedHashMap<>();
                cm.put("id", c.getId());
                cm.put("course_date", c.getCourseDate());
                cm.put("start_time", c.getStartTime());
                cm.put("end_time", c.getEndTime());
                cm.put("teacher", c.getTeacher());
                cm.put("dance_type", c.getDanceType());
                cm.put("capacity", c.getCapacity());
                cm.put("current_participants", c.getCurrentParticipants());
                m.put("course", cm);
            }
            items.add(m);
        }

        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("data", items);
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("total", result.getTotal());
        meta.put("page", result.getCurrent());
        meta.put("page_size", result.getSize());
        resp.put("meta", meta);
        return ResponseEntity.ok(Result.buildSuccess(resp));
    }

    // POST /api/reservations  body: { course_id: number }
    @PostMapping("")
    public ResponseEntity<Result<?>> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String xAuthToken,
            @RequestBody java.util.Map<String, Object> body
    ) {
        String token = parseToken(authorization, xAuthToken);
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Result.buildFailure(401, "UNAUTHORIZED", "未提供token"));
        }
        UserToken ut = userTokenMapper.selectOne(new LambdaQueryWrapper<UserToken>().eq(UserToken::getToken, token));
        if (ut == null) {
            return ResponseEntity.status(401).body(Result.buildFailure(401, "UNAUTHORIZED", "登录失效"));
        }
        Long courseId = null;
        if (body != null && body.get("course_id") != null) {
            courseId = Long.valueOf(String.valueOf(body.get("course_id")));
        }
        if (courseId == null) {
            return ResponseEntity.badRequest().body(Result.buildFailure(400, "BAD_REQUEST", "缺少course_id"));
        }

        // 校验课程存在
        com.example.mentor.dao.entity.Course course = courseMapper.selectById(courseId);
        if (course == null) {
            return ResponseEntity.badRequest().body(Result.buildFailure(400, "BAD_REQUEST", "课程不存在"));
        }
        // 校验导师卡有效
        java.util.Date today = new java.util.Date();
        java.util.List<com.example.mentor.dao.entity.Card> cards = cardMapper.selectList(
                new LambdaQueryWrapper<com.example.mentor.dao.entity.Card>().eq(com.example.mentor.dao.entity.Card::getUserId, ut.getUserId())
        );
        boolean hasValidCard = false;
        for (com.example.mentor.dao.entity.Card c : cards) {
            boolean active = "active".equalsIgnoreCase(c.getStatus()) && ("lifetime".equals(c.getCardType())
                    || (c.getStartDate() != null && c.getEndDate() != null && !today.before(c.getStartDate()) && !today.after(c.getEndDate())));
            if (active) { hasValidCard = true; break; }
        }
        if (!hasValidCard) {
            return ResponseEntity.status(403).body(Result.buildFailure(403, "FORBIDDEN", "无有效导师卡"));
        }

        // 冲突校验：同一天时间段存在预约则拒绝
        // 使用标准日期格式避免 MySQL Incorrect DATE value 错误
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String courseDateStr = course.getCourseDate() == null ? null : df.format(course.getCourseDate());
        String startTimeStr = course.getStartTime() == null ? null : course.getStartTime().toString();
        String endTimeStr = course.getEndTime() == null ? null : course.getEndTime().toString();

        long conflicts = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
            .eq(Reservation::getUserId, ut.getUserId())
            .eq(Reservation::getStatus, "reserved")
            .inSql(Reservation::getCourseId,
                "SELECT id FROM courses WHERE course_date = '" + courseDateStr + "' " +
                "AND NOT (end_time <= '" + startTimeStr + "' OR start_time >= '" + endTimeStr + "')")
        );
        if (conflicts > 0) {
            return ResponseEntity.status(409).body(Result.buildFailure(409, "TIME_CONFLICT", "时间冲突，已有预约"));
        }

        // 容量校验
        if (course.getCurrentParticipants() >= course.getCapacity()) {
            return ResponseEntity.status(409).body(Result.buildFailure(409, "FULL", "名额已满"));
        }

        // 插入预约并更新人数（简化实现：非事务）
        // 处理唯一索引(user_id, course_id)：若存在历史记录则更新为reserved
        Reservation existing = reservationMapper.selectOne(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, ut.getUserId())
                .eq(Reservation::getCourseId, courseId));
        Reservation r;
        if (existing != null) {
            if ("reserved".equalsIgnoreCase(existing.getStatus())) {
                // 幂等：已是预约状态直接返回
                r = existing;
            } else {
                existing.setStatus("reserved");
                existing.setCheckinStatus("not_checked");
                reservationMapper.updateById(existing);
                r = existing;
            }
        } else {
            r = new Reservation();
            r.setUserId(ut.getUserId());
            r.setCourseId(courseId);
            r.setStatus("reserved");
            r.setCheckinStatus("not_checked");
            reservationMapper.insert(r);
        }

        // 更新课程人数
        long count = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getCourseId, courseId)
                .eq(Reservation::getStatus, "reserved")
        );
        course.setCurrentParticipants(Math.toIntExact(count));
        courseMapper.updateById(course);

        return ResponseEntity.ok(Result.buildSuccess(java.util.Collections.singletonMap("reservation_id", r.getId())));
    }

    // DELETE /api/reservations/:id 取消预约
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<?>> cancel(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String xAuthToken,
            @PathVariable("id") Long id
    ) {
        String token = parseToken(authorization, xAuthToken);
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Result.buildFailure(401, "UNAUTHORIZED", "未提供token"));
        }
        UserToken ut = userTokenMapper.selectOne(new LambdaQueryWrapper<UserToken>().eq(UserToken::getToken, token));
        if (ut == null) {
            return ResponseEntity.status(401).body(Result.buildFailure(401, "UNAUTHORIZED", "登录失效"));
        }

        Reservation r = reservationMapper.selectById(id);
        if (r == null || !ut.getUserId().equals(r.getUserId())) {
            // 幂等处理：直接返回成功
            return ResponseEntity.ok(Result.buildSuccess("OK"));
        }
        r.setStatus("cancelled");
        reservationMapper.updateById(r);
        // 回写课程人数
        long count = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getCourseId, r.getCourseId())
                .eq(Reservation::getStatus, "reserved")
        );
        com.example.mentor.dao.entity.Course course = courseMapper.selectById(r.getCourseId());
        if (course != null) { course.setCurrentParticipants(Math.toIntExact(count)); courseMapper.updateById(course); }

        return ResponseEntity.ok(Result.buildSuccess("OK"));
    }

    private String parseToken(String authorization, String xAuthToken) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        if (xAuthToken != null && !xAuthToken.isEmpty()) {
            return xAuthToken;
        }
        return (authorization != null && !authorization.startsWith("Basic ")) ? authorization : null;
    }
}
