package com.example.mentor.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String openid;

    private String nickname;

    @TableField("real_name")
    private String realName;

    private String phone;

    private String avatar;

    // ENUM('active','disabled')，用 String 映射
    private String status;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}