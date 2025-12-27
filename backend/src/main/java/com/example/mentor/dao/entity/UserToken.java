package com.example.mentor.dao.entity;

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
import java.util.Date;

@Data
@lombok.EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_token")
@TableName("user_token")
public class UserToken extends BaseDO {
    @TableId(type = IdType.AUTO)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String token;

    @TableField("expired_at")
    private Date expiredAt;
}