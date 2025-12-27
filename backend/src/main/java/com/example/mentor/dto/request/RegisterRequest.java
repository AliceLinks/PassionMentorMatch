
package com.example.mentor.dto.request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String phone;
    private String password;
    private String realName;
    private String avatar;
}