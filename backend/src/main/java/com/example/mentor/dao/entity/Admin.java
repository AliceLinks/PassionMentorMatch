package com.example.mentor.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("admins")
public class Admin {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @TableField("phone")
    private String phone;   

    @TableField("password_hash")
    private String passwordHash;

    private String role; // admin

    private String status; // active, disabled

    @TableField("last_login")
    private Date lastLogin;

    @TableField("created_at")
    private Date createdAt;
}
