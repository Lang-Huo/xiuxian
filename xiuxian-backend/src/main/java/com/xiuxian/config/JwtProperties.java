package com.xiuxian.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    /** 签名密钥（生产请通过环境变量 XIUXIAN_JWT_SECRET 注入强随机串） */
    private String secret = "xiuxian-dev-secret-change-me";
    /** 令牌有效期（小时） */
    private long expirationHours = 168; // 默认 7 天
}
