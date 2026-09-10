package com.xiuxian.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "xiuxian.ai")
public class AiProperties {
    /** 是否启用真实大模型（false 时使用 Mock 出题） */
    private boolean enabled = false;
    private String provider = "openai";
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey = "";
    private String model = "gpt-4o-mini";
    private int timeoutSeconds = 60;
}
