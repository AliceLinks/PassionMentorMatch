package com.example.mentor.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("cards")
public class Card {
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("card_number")
    private String cardNumber;

    @TableField("card_type")
    private String cardType;

    @TableField("face_image_url")
    private String faceImageUrl;

    private String status;

    @TableField("start_date")
    private Date startDate;

    @TableField("end_date")
    private Date endDate;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;
}
