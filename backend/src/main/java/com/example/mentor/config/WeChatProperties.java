package com.example.mentor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WeChatProperties {
    private String appid;
    private String secret;
    // 仅应在网络代理或证书异常的应急场景下打开，默认 false
    private boolean insecure = false;
}
