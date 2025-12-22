package com.example.mentor.service.impl;


import com.example.mentor.exception.BizException;
import com.example.mentor.service.ImageService;
import com.example.mentor.config.OssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
// ImageService 接口的实现类被标记为 Spring 的 bean
public class ImageServicelmpl implements ImageService {

    @Autowired
    private OssUtil ossUtil;

    @Override
    public String upload(MultipartFile file) {
        try {
            return ossUtil.upload(file.getOriginalFilename(), file.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException(500, "500", "图片上传失败");
        }
    }

}
