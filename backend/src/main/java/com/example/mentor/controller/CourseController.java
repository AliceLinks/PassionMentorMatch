package com.example.mentor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentor.dao.entity.Course;
import com.example.mentor.dao.entity.Reservation;
import com.example.mentor.dao.entity.User;
import com.example.mentor.dao.mapper.CourseMapper;
import com.example.mentor.dao.mapper.ReservationMapper;
import com.example.mentor.dto.Result;
import com.example.mentor.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseMapper courseMapper;
    private final ReservationMapper reservationMapper;
    private final AuthService authService;

    // GET /api/courses/week?week_start=YYYY-MM-DD
    @GetMapping("/week")
        public ResponseEntity<Result<?>> weekCourses(
            @RequestParam("week_start") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weekStart,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String xAuthToken) {
        LocalDate end = weekStart.plusDays(6);
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
            .between(Course::getCourseDate,
                Date.from(weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            .orderByAsc(Course::getCourseDate, Course::getStartTime));

        // Determine reserved courses for current user (optional token)
        String token = parseToken(authorization, xAuthToken);
        final Set<Long> reservedIds = getReservedIds(token, courses);

        // 构建课程列表并附带当前用户的预约ID（若已预约）
        java.util.Map<Long, Long> reservationIdByCourse = getReservationIdMap(token, courses);
        List<Map<String, Object>> list = courses.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("course_date", formatDate(c.getCourseDate()));
            m.put("start_time", c.getStartTime() == null ? null : c.getStartTime().toString());
            m.put("end_time", c.getEndTime() == null ? null : c.getEndTime().toString());
            m.put("teacher", c.getTeacher());
            m.put("dance_type", c.getDanceType());
            m.put("capacity", c.getCapacity());
            m.put("current_participants", c.getCurrentParticipants());
            m.put("status", c.getStatus());
            m.put("week_number", c.getWeekNumber());
            m.put("reserved", reservedIds.contains(c.getId()));
            Long rid = reservationIdByCourse.get(c.getId());
            if (rid != null) { m.put("reservation_id", rid); }
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("courses", list);
        return ResponseEntity.ok(Result.buildSuccess(data));
    }

    private Set<Long> getReservedIds(String token, List<Course> courses) {
        if (token == null || courses == null || courses.isEmpty()) return Collections.emptySet();
        try {
            User u = authService.getUserInfoByToken(token);
            if (u == null) return Collections.emptySet();
            List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
            if (courseIds.isEmpty()) return Collections.emptySet();
                List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                    .eq(Reservation::getUserId, u.getId())
                    .in(Reservation::getCourseId, courseIds)
                    .eq(Reservation::getStatus, "reserved"));
                return reservations.stream().map(Reservation::getCourseId).collect(Collectors.toSet());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private java.util.Map<Long, Long> getReservationIdMap(String token, List<Course> courses) {
        java.util.Map<Long, Long> map = new java.util.HashMap<>();
        if (token == null || courses == null || courses.isEmpty()) return map;
        try {
            User u = authService.getUserInfoByToken(token);
            if (u == null) return map;
            List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
            if (courseIds.isEmpty()) return map;
            List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                    .eq(Reservation::getUserId, u.getId())
                    .in(Reservation::getCourseId, courseIds)
                    .eq(Reservation::getStatus, "reserved"));
            for (Reservation r : reservations) { map.put(r.getCourseId(), r.getId()); }
        } catch (Exception ignored) {}
        return map;
    }

    private String parseToken(String authorization, String xAuthToken) {
        if (authorization != null && authorization.startsWith("Bearer ")) return authorization.substring(7);
        if (xAuthToken != null && !xAuthToken.isEmpty()) return xAuthToken;
        if (authorization != null && authorization.startsWith("Basic ")) return null;
        return authorization;
    }

    private String formatDate(Date d) {
        if (d == null) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return String.format("%04d-%02d-%02d", y, m, day);
    }

    // 批量发布课程（占位），后续补充权限与入库逻辑
    // 注意：类级别已是 /api/courses，这里只需 /batch，避免成为 /api/courses/courses/batch
    @PostMapping("/batch")
    public Result<?> batchPublish(@RequestBody List<Map<String, Object>> items) {
        int success = 0;
        int failed = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        if (items == null) {
            return Result.buildFailure(400, "请求体不能为空，需为数组");
        }

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> it = items.get(i);
            try {
                String courseDate = String.valueOf(it.get("course_date"));
                String startTime = String.valueOf(it.get("start_time"));
                String endTime = String.valueOf(it.get("end_time"));
                String teacher = String.valueOf(it.get("teacher"));
                String danceType = String.valueOf(it.get("dance_type"));
                Integer capacity = Integer.valueOf(String.valueOf(it.get("capacity")));

                // 基础校验，避免非法数据进入数据库
                if (courseDate == null || startTime == null || endTime == null || teacher == null || danceType == null || capacity == null) {
                    throw new IllegalArgumentException("必填字段缺失");
                }
                if (startTime.compareTo(endTime) >= 0) {
                    throw new IllegalArgumentException("结束时间必须晚于开始时间");
                }
                if (capacity <= 0) {
                    throw new IllegalArgumentException("容量必须为正整数");
                }

                // 解析日期与时间
                java.time.LocalDate cd = java.time.LocalDate.parse(courseDate);
                java.sql.Date sqlDate = java.sql.Date.valueOf(cd);
                String startStr = startTime.length() == 5 ? startTime + ":00" : startTime;
                String endStr = endTime.length() == 5 ? endTime + ":00" : endTime;
                java.sql.Time sqlStart = java.sql.Time.valueOf(startStr);
                java.sql.Time sqlEnd = java.sql.Time.valueOf(endStr);

                // 构建实体并入库
                Course c = new Course();
                c.setCourseDate(sqlDate);
                c.setStartTime(sqlStart);
                c.setEndTime(sqlEnd);
                c.setTeacher(teacher);
                c.setDanceType(danceType);
                c.setCapacity(capacity);
                c.setCurrentParticipants(0);
                c.setStatus("published");
                courseMapper.insert(c);

                success++;
            } catch (Exception ex) {
                failed++;
                Map<String, Object> err = new HashMap<>();
                err.put("index", i + 1);
                err.put("message", ex.getMessage());
                errors.add(err);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);
        resp.put("failed", failed);
        if (!errors.isEmpty()) {
            resp.put("errors", errors);
        }
        return Result.buildSuccess(resp);
    }

    // 管理员取消课程
    @PostMapping("/{id}/cancel")
    public Result<?> cancelCourse(@PathVariable("id") Long id) {
        try {
            if (id == null) {
                return Result.buildFailure(400, "课程ID不能为空");
            }
            Course c = courseMapper.selectById(id);
            if (c == null) {
                return Result.buildFailure(404, "课程不存在");
            }
            if ("cancelled".equalsIgnoreCase(c.getStatus())) {
                return Result.buildSuccess(Collections.singletonMap("message", "课程已是取消状态"));
            }
            c.setStatus("cancelled");
            courseMapper.updateById(c);

            // 同步更新该课程的预约为取消状态（仅更新状态字段，避免覆盖其它字段为null）
            com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Reservation> uw = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
            uw.eq("course_id", id).eq("status", "reserved").set("status", "cancelled");
            reservationMapper.update(null, uw);

            return Result.buildSuccess(Collections.singletonMap("message", "已取消课程并取消相关预约"));
        } catch (Exception e) {
            return Result.buildFailure(500, "取消失败: " + e.getMessage());
        }
    }

    // 管理员编辑课程信息（部分字段可更新）
    @PutMapping("/{id}")
    public Result<?> editCourse(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        if (id == null) return Result.buildFailure(400, "课程ID不能为空");
        Course c = courseMapper.selectById(id);
        if (c == null) return Result.buildFailure(404, "课程不存在");
        // 允许更新的字段：日期/时间/教师/舞种/容量/状态
        try {
            if (body.containsKey("course_date")) {
                java.time.LocalDate cd = java.time.LocalDate.parse(String.valueOf(body.get("course_date")));
                c.setCourseDate(java.sql.Date.valueOf(cd));
            }
            if (body.containsKey("start_time")) {
                String s = String.valueOf(body.get("start_time"));
                c.setStartTime(java.sql.Time.valueOf(s.length()==5? s+":00" : s));
            }
            if (body.containsKey("end_time")) {
                String s = String.valueOf(body.get("end_time"));
                c.setEndTime(java.sql.Time.valueOf(s.length()==5? s+":00" : s));
            }
            if (body.containsKey("teacher")) c.setTeacher(String.valueOf(body.get("teacher")));
            if (body.containsKey("dance_type")) c.setDanceType(String.valueOf(body.get("dance_type")));
            if (body.containsKey("capacity")) c.setCapacity(Integer.valueOf(String.valueOf(body.get("capacity"))));
            if (body.containsKey("status")) c.setStatus(String.valueOf(body.get("status")));
            // 简单校验
            if (c.getStartTime()!=null && c.getEndTime()!=null && c.getStartTime().after(c.getEndTime())) {
                return Result.buildFailure(400, "结束时间必须晚于开始时间");
            }
            courseMapper.updateById(c);
            return Result.buildSuccess(Collections.singletonMap("message", "课程已更新"));
        } catch (Exception e) {
            return Result.buildFailure(400, "参数错误: "+e.getMessage());
        }
    }
}
