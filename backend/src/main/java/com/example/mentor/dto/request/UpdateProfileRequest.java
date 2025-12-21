package com.example.mentor.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String realName;
    private String phone;
    private String avatar;
    private String nickname; 
}
