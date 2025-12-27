
package com.example.mentor.controller;

import com.example.mentor.dao.entity.Reservation;
import com.example.mentor.dao.entity.User;
import com.example.mentor.dao.mapper.ReservationMapper;
import com.example.mentor.dao.mapper.UserMapper;
import com.example.mentor.dao.mapper.CourseMapper;
import com.example.mentor.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdminReservationController {
    private final CourseMapper courseMapper;
    private final ReservationMapper reservationMapper;
    private final UserMapper userMapper;

    // GET /api/admin/courses/week?week_start=YYYY-MM-DD
    @GetMapping("/admin/courses/week")
    public Result<?> weekCourses(
            @RequestParam("week_start") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weekStart) {
        LocalDate end = weekStart.plusDays(6);
        Date startDate = Date.from(weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date today = new Date();

        List<com.example.mentor.dao.entity.Course> courses = courseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.mentor.dao.entity.Course>()
                        .between(com.example.mentor.dao.entity.Course::getCourseDate, startDate, endDate)
                        .orderByAsc(com.example.mentor.dao.entity.Course::getCourseDate,
                                com.example.mentor.dao.entity.Course::getStartTime));

        // 只返回还没有上的课程（课程日期大于等于今天且未取消）
        List<Map<String, Object>> list = new ArrayList<>();
        for (com.example.mentor.dao.entity.Course c : courses) {
            if (c == null)
                continue;
            if (c.getCourseDate() == null)
                continue;
            if (c.getCourseDate().before(today))
                continue;
            if ("cancelled".equalsIgnoreCase(c.getStatus()))
                continue;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId() == null ? null : String.valueOf(c.getId()));
            Calendar cal = Calendar.getInstance();
            cal.setTime(c.getCourseDate());
            int y = cal.get(Calendar.YEAR);
            int mo = cal.get(Calendar.MONTH) + 1;
            int d = cal.get(Calendar.DAY_OF_MONTH);
            m.put("course_date", String.format("%04d-%02d-%02d", y, mo, d));
            m.put("start_time", c.getStartTime() == null ? null : c.getStartTime().toString());
            m.put("end_time", c.getEndTime() == null ? null : c.getEndTime().toString());
            m.put("teacher", c.getTeacher());
            m.put("dance_type", c.getDanceType());
            m.put("capacity", c.getCapacity());
            m.put("current_participants", c.getCurrentParticipants());
            m.put("status", c.getStatus());

            long reservedCount = reservationMapper
                    .selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getCourseId, c.getId())
                            .eq(Reservation::getStatus, "reserved"));
            m.put("reserved_count", reservedCount);
            list.add(m);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("courses", list);
        return Result.buildSuccess(resp);
    }

    // GET /api/admin/courses/day?date=YYYY-MM-DD
    @GetMapping("/admin/courses/day")
    public Result<?> dayCourses(@RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date == null)
            return Result.buildFailure(400, "日期不能为空");
        Date startDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date today = new Date();

        List<com.example.mentor.dao.entity.Course> courses = courseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.mentor.dao.entity.Course>()
                        .ge(com.example.mentor.dao.entity.Course::getCourseDate, startDate)
                        .lt(com.example.mentor.dao.entity.Course::getCourseDate, endDate)
                        .orderByAsc(com.example.mentor.dao.entity.Course::getStartTime));

        List<Map<String, Object>> list = new ArrayList<>();
        for (com.example.mentor.dao.entity.Course c : courses) {
            if (c == null)
                continue;
            if (c.getCourseDate() == null)
                continue;
            if (c.getCourseDate().before(today))
                continue;
            if ("cancelled".equalsIgnoreCase(c.getStatus()))
                continue;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId() == null ? null : String.valueOf(c.getId()));
            Calendar cal = Calendar.getInstance();
            cal.setTime(c.getCourseDate());
            int y = cal.get(Calendar.YEAR);
            int mo = cal.get(Calendar.MONTH) + 1;
            int d = cal.get(Calendar.DAY_OF_MONTH);
            m.put("course_date", String.format("%04d-%02d-%02d", y, mo, d));
            m.put("start_time", c.getStartTime() == null ? null : c.getStartTime().toString());
            m.put("end_time", c.getEndTime() == null ? null : c.getEndTime().toString());
            m.put("teacher", c.getTeacher());
            m.put("dance_type", c.getDanceType());
            m.put("capacity", c.getCapacity());
            m.put("current_participants", c.getCurrentParticipants());
            m.put("status", c.getStatus());

            long reservedCount = reservationMapper
                    .selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getCourseId, c.getId())
                            .eq(Reservation::getStatus, "reserved"));
            m.put("reserved_count", reservedCount);
            list.add(m);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("courses", list);
        return Result.buildSuccess(resp);
    }

    // GET
    // /api/admin/courses/{courseId}/reservations?checkin_status=all|checked|not_checked
    @GetMapping("/admin/courses/{courseId}/reservations")
    public Result<?> reservationList(@PathVariable("courseId") Long courseId,
            @RequestParam(value = "checkin_status", defaultValue = "all") String checkinStatus) {
        if (courseId == null)
            return Result.buildFailure(400, "课程ID不能为空");

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Reservation> qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        qw.eq(Reservation::getCourseId, courseId).eq(Reservation::getStatus, "reserved");
        if ("checked".equalsIgnoreCase(checkinStatus))
            qw.eq(Reservation::getCheckinStatus, "checked");
        else if ("not_checked".equalsIgnoreCase(checkinStatus) || "unchecked".equalsIgnoreCase(checkinStatus))
            qw.eq(Reservation::getCheckinStatus, "not_checked");

        List<Reservation> reservations = reservationMapper.selectList(qw.orderByAsc(Reservation::getCreatedAt));

        List<Long> userIds = reservations.stream().map(Reservation::getUserId).filter(Objects::nonNull).distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User u : users) {
                if (u != null)
                    userMap.put(u.getId(), u);
            }
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Reservation r : reservations) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("reservation_id", r.getId() == null ? null : String.valueOf(r.getId()));
            m.put("user_id", r.getUserId() == null ? null : String.valueOf(r.getUserId()));
            User u = userMap.get(r.getUserId());
            if (u != null) {
                m.put("real_name", u.getRealName());
                m.put("phone", u.getPhone());
                m.put("real_name_image", u.getRealNameImage());
            } else {
                m.put("real_name", null);
                m.put("phone", null);
                m.put("real_name_image", null);
            }
            m.put("status", r.getStatus());
            m.put("checkin_status", r.getCheckinStatus());
            m.put("created_at", r.getCreatedAt());
            items.add(m);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("data", items);
        return Result.buildSuccess(resp);
    }

    // POST /api/admin/reservations/{id}/checkin 管理员进行签到
    @PostMapping("/admin/reservations/{id}/checkin")
    public Result<?> checkin(@PathVariable("id") Long id) {
        if (id == null)
            return Result.buildFailure(400, "reservation id required");
        Reservation r = reservationMapper.selectById(id);
        if (r == null)
            return Result.buildFailure(404, "预约不存在");
        if ("checked".equalsIgnoreCase(r.getCheckinStatus()))
            return Result.buildSuccess("OK");
        r.setCheckinStatus("checked");
        reservationMapper.updateById(r);
        return Result.buildSuccess("OK");
    }

    // POST /api/admin/reservations/{id}/uncheckin 取消签到
    @PostMapping("/admin/reservations/{id}/uncheckin")
    public Result<?> uncheckin(@PathVariable("id") Long id) {
        if (id == null)
            return Result.buildFailure(400, "reservation id required");
        Reservation r = reservationMapper.selectById(id);
        if (r == null)
            return Result.buildFailure(404, "预约不存在");
        if (!"checked".equalsIgnoreCase(r.getCheckinStatus()))
            return Result.buildSuccess("OK");
        r.setCheckinStatus("not_checked");
        reservationMapper.updateById(r);
        return Result.buildSuccess("OK");
    }

}
