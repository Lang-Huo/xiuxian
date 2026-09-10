package com.xiuxian.controller;

import com.xiuxian.model.dto.AuthResponse;
import com.xiuxian.model.dto.LoginRequest;
import com.xiuxian.model.dto.RegisterRequest;
import com.xiuxian.model.dto.UserStateView;
import com.xiuxian.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 注册：创建修仙者并返回令牌 */
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req.username(), req.nickname(), req.password());
    }

    /** 登录：校验凭证并返回令牌 */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.username(), req.password());
    }

    /** 当前登录用户状态（需令牌） */
    @PostMapping("/me")
    public UserStateView me(@RequestAttribute("userId") Long userId) {
        return authService.currentUser(userId);
    }
}
