package com.example.mentor.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import jakarta.persistence.MappedSuperclass;

import java.time.LocalDateTime;

@Data
@MappedSuperclass
public class BaseDO {
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}