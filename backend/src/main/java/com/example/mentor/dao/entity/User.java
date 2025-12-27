package com.example.mentor.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@lombok.EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
@TableName("users")
public class User extends BaseDO {
    @TableId(type = IdType.AUTO)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String openid;

    private String passwordHash; // 密码MD5加密存储

    @TableField("real_name")
    private String realName;

    private String phone;

    private String avatar;

    // ENUM('active','disabled')，用 String 映射
    private String status;

    @TableField("real_name_image")
    private String realNameImage;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}