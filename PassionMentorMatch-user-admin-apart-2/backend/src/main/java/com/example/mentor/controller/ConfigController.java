package com.example.mentor.controller;

import com.example.mentor.dto.Result;
import com.example.mentor.service.SimpleConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ConfigController {
    @Autowired
    private SimpleConfigService simpleConfigService;

    // 前台获取路线图和课程介绍
    @GetMapping("/config")
    public Result<Map<String, String>> getConfig() {
        Map<String, String> map = new HashMap<>();
        map.put("roadmapImg", simpleConfigService.getValue("roadmap_img"));
        map.put("courseIntro", simpleConfigService.getValue("course_intro"));
        return Result.success(map);
    }

    // 管理员获取配置
    @GetMapping("/admin/config")
    public Result<Map<String, String>> getAdminConfig() {
        Map<String, String> map = new HashMap<>();
        map.put("roadmapImg", simpleConfigService.getValue("roadmap_img"));
        map.put("courseIntro", simpleConfigService.getValue("course_intro"));
        return Result.success(map);
    }


    // 管理员上传路线图图片（本地存储版）
    @PostMapping("/admin/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        System.out.println("[上传图片] 接收到上传请求");
        if (file.isEmpty()) {
            System.out.println("[上传图片] 文件为空");
            return Result.fail("文件为空");
        }
        try {
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String uploadDir = new File("./upload/").getAbsolutePath();
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                System.out.println("[上传图片] 创建upload目录: " + uploadDir + " 结果: " + created);
            } else {
                System.out.println("[上传图片] upload目录已存在: " + uploadDir);
            }
            File dest = new File(dir, filename);
            System.out.println("[上传图片] 保存文件路径: " + dest.getAbsolutePath());
            file.transferTo(dest);

            // 生成图片访问URL（假设静态资源已映射 /upload/**）
            String url = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/upload/" + filename;
            System.out.println("[上传图片] 图片访问URL: " + url);
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            return Result.success(data);
        } catch (IOException e) {
            System.out.println("[上传图片] 上传失败: " + e.getMessage());
            e.printStackTrace();
            return Result.fail("上传失败: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[上传图片] 未知异常: " + e.getMessage());
            e.printStackTrace();
            return Result.fail("上传失败: " + e.getMessage());
        }
    }

    // 管理员保存路线图图片URL
    @PostMapping("/admin/config/roadmap")
    public Result<?> setRoadmapImg(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        boolean ok = simpleConfigService.setValue("roadmap_img", url);
        return ok ? Result.success() : Result.fail("保存失败");
    }

    // 管理员设置课程介绍链接
    @PostMapping("/admin/config/course-intro")
    public Result<?> setCourseIntro(@RequestBody Map<String, String> body) {
        String link = body.get("courseIntro");
        boolean ok = simpleConfigService.setValue("course_intro", link);
        return ok ? Result.success() : Result.fail("保存失败");
    }
}