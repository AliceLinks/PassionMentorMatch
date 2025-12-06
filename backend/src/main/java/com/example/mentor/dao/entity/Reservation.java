package com.example.mentor.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("reservations")
public class Reservation {
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("course_id")
    private Long courseId;

    private String status;

    @TableField("checkin_status")
    private String checkinStatus;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;
}
