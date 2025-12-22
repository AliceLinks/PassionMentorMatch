package com.example.mentor.controller;

import com.example.mentor.service.ImageService;
import com.example.mentor.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping("/upload")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = imageService.upload(file);
        // 用 Result 返回，前端按 code===200 判断
        return Result.buildSuccess(url);
    }
}
