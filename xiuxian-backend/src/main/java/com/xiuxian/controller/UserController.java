package com.xiuxian.controller;

import com.xiuxian.mapper.UserMapper;
import com.xiuxian.model.dto.AvatarRequest;
import com.xiuxian.model.dto.AvatarView;
import com.xiuxian.model.entity.User;
import com.xiuxian.service.AuthService;
import com.xiuxian.service.AvatarPolicy;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户资料：内置头像库与头像设置
 */
@RestController
@RequestMapping("/api")
public class UserController {

    private final AuthService authService;
    private final UserMapper userMapper;

    public UserController(AuthService authService, UserMapper userMapper) {
        this.authService = authService;
        this.userMapper = userMapper;
    }

    /**
     * 头像列表（需登录）：首项为「本名」——头像字即当前昵称第一个字，其后为 12 款预设
     */
    @GetMapping("/avatars")
    public List<AvatarView> avatars(@RequestAttribute("userId") Long userId) {
        User user = userMapper.selectById(userId);
        return AvatarPolicy.all(user == null ? "" : user.getNickname());
    }

    /** 设置头像（需登录），code 传 AUTO 表示用昵称首字 */
    @PostMapping("/users/avatar")
    public AvatarView setAvatar(@Valid @RequestBody AvatarRequest req,
                                @RequestAttribute("userId") Long userId) {
        return authService.setAvatar(userId, req.code());
    }
}
