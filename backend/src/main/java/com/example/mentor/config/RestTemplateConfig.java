package com.example.mentor.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class RestTemplateConfig {

    private final WeChatProperties weChatProperties;

    public RestTemplateConfig(WeChatProperties weChatProperties) {
        this.weChatProperties = weChatProperties;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        boolean insecure = weChatProperties.isInsecure() || Boolean.parseBoolean(System.getenv().getOrDefault("WECHAT_SSL_INSECURE", "false"));
        if (!insecure) {
            return builder
                    .setConnectTimeout(Duration.ofSeconds(5))
                    .setReadTimeout(Duration.ofSeconds(5))
                    .build();
        }
        // Insecure mode: trust all certs and disable hostname verification (仅用于应急排障)
        try {
            X509TrustManager trustAll = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{trustAll}, new SecureRandom());

            HttpClient httpClient = HttpClient.newBuilder()
                    .sslContext(sc)
                    .sslParameters(disableHostnameVerification())
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
            RestTemplate rt = new RestTemplate(factory);
            return rt;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            // 回退到默认安全配置
            return builder
                    .setConnectTimeout(Duration.ofSeconds(5))
                    .setReadTimeout(Duration.ofSeconds(5))
                    .build();
        }
    }

    private static SSLParameters disableHostnameVerification() {
        SSLParameters params = new SSLParameters();
        // 通过置空关闭基于主机名的校验
        params.setEndpointIdentificationAlgorithm(null);
        return params;
    }
}
