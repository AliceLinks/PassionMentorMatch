package com.example.mentor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    private int code;
    private String message;
    private T data;

    @JsonProperty("error_code")
    private String errorCode;

    public static <T> Result<T> buildSuccess(T data) {
        return new Result<>(200, "success", data, null);
    }

    public static <T> Result<T> buildFailure(int code, String message) {
        return new Result<>(code, message, null, String.valueOf(code));
    }

    public static <T> Result<T> buildFailure(int code, String errorCode, String message) {
        return new Result<>(code, message, null, errorCode);
    }
}
