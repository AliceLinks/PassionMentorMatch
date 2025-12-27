package com.example.mentor.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;
import java.util.Date;
import java.sql.Time;

@Data
@Entity
@Table(name = "courses")
@TableName("courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TableField("course_date")
    private Date courseDate;

    @TableField("start_time")
    private Time startTime;

    @TableField("end_time")
    private Time endTime;

    private String teacher;

    @TableField("dance_type")
    private String danceType;

    private Integer capacity;

    @TableField("current_participants")
    private Integer currentParticipants;

    private String status;

    @TableField(value = "week_number", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer weekNumber;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;
}
