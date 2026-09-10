package com.xiuxian.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiuxian.exception.AuthException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 轻量 JWT 工具：手写 HS256（header.payload.signature），不引入额外依赖。
 * 仅用于 MVP 无状态鉴权；生产环境建议改用标准库（jjwt）并保管好密钥。
 */
@Component
public class JwtUtil {

    private final JwtProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    public JwtUtil(JwtProperties props) {
        this.props = props;
    }

    public String generate(Long userId) {
        long exp = System.currentTimeMillis() + props.getExpirationHours() * 3_600_000L;
        String header = b64url(HEADER.getBytes(StandardCharsets.UTF_8));
        String payload = b64url(("{\"uid\":" + userId + ",\"exp\":" + exp + "}").getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        String sig = b64url(hmac(signingInput.getBytes(StandardCharsets.UTF_8)));
        return signingInput + "." + sig;
    }

    public Long verify(String token) {
        if (token == null || token.isBlank()) throw new AuthException("未登录");
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new AuthException("令牌格式错误");
        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = hmac(signingInput.getBytes(StandardCharsets.UTF_8));
        byte[] actual = b64urlDecode(parts[2]);
        if (!MessageDigest.isEqual(expected, actual)) throw new AuthException("令牌无效");
        try {
            JsonNode node = mapper.readTree(new String(b64urlDecode(parts[1]), StandardCharsets.UTF_8));
            long exp = node.get("exp").asLong();
            if (System.currentTimeMillis() > exp) throw new AuthException("令牌已过期");
            return node.get("uid").asLong();
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException("令牌解析失败");
        }
    }

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(props.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 初始化失败", e);
        }
    }

    private String b64url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] b64urlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }
}
