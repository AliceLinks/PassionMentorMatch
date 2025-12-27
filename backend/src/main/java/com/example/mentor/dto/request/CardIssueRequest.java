package com.example.mentor.dto.request;

import java.util.Date;

/**
 * 管理员发放导师卡时的请求体实体类。
 * 用于接收前端传来的发卡参数。
 */
public class CardIssueRequest {
    private String phone;
    private String cardType;
    private Date startDate;
    private String realNameImage;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getRealNameImage() {
        return realNameImage;
    }

    public void setRealNameImage(String realNameImage) {
        this.realNameImage = realNameImage;
    }
}