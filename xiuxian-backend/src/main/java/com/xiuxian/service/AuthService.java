package com.xiuxian.service;

import com.xiuxian.config.JwtUtil;
import com.xiuxian.exception.AuthException;
import com.xiuxian.mapper.UserMapper;
import com.xiuxian.model.dto.AuthResponse;
import com.xiuxian.model.dto.AvatarView;
import com.xiuxian.model.dto.UserStateView;
import com.xiuxian.model.entity.User;
import com.xiuxian.util.PhoneUtil;
import com.xiuxian.util.UserNoUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证与账户：注册、登录、当前用户状态
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 注册：username 为登录账号（唯一），nickname 为展示昵称（可重复，留空则默认"道友"）
     */
    @Transactional
    public AuthResponse register(String username, String nickname, String password) {
        if (userMapper.selectByUsername(username.trim()) != null) {
            throw new AuthException("用户名已被占用，请另择");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setNickname(nickname == null || nickname.isBlank() ? "道友" : nickname.trim());
        user.setPassword(encoder.encode(password));
        user.setUserNo(allocateUserNo());
        user.setAvatar(AvatarPolicy.AUTO_CODE);   // 默认头像 = 昵称首字
        userMapper.insert(user);
        return build(user);
    }

    /**
     * 分配仙途编号：随机 6 位（首位非 0），撞号则重摇；数据库另有唯一索引兜底。
     */
    private String allocateUserNo() {
        for (int i = 0; i < UserNoUtil.MAX_RETRY; i++) {
            String no = UserNoUtil.random();
            if (userMapper.selectByUserNo(no) == null) {
                return no;
            }
        }
        throw new IllegalStateException("仙途编号分配失败，请稍后重试");
    }

    /** 登录：按手机号校验 */
    public AuthResponse login(String username, String password) {
        PhoneUtil.requireValid(username);
        User user = userMapper.selectByUsername(username.trim());
        if (user == null || user.getPassword() == null || !encoder.matches(password, user.getPassword())) {
            throw new AuthException("手机号或密码错误");
        }
        ensureUserNo(user);
        return build(user);
    }

    /** 根据令牌中的 userId 取当前用户状态 */
    public UserStateView currentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new AuthException("用户不存在");
        ensureUserNo(user);
        return toView(user);
    }

    /** 更换头像：编码非法直接抛 400 */
    public AvatarView setAvatar(Long userId, String code) {
        if (!AvatarPolicy.isValid(code)) {
            throw new IllegalArgumentException("头像不存在：" + code);
        }
        User user = userMapper.selectById(userId);
        if (user == null) throw new AuthException("用户不存在");
        AvatarView avatar = AvatarPolicy.of(code, user.getNickname());
        User patch = new User();
        patch.setId(userId);
        patch.setAvatar(avatar.code());
        userMapper.updateById(patch);
        return avatar;
    }

    /**
     * 老账号补发仙途编号：历史数据在加列之前注册，编号为空；
     * 首次登录/查询时懒迁移，补一个未被占用的编号并回写。
     */
    private void ensureUserNo(User user) {
        if (UserNoUtil.isValid(user.getUserNo())) {
            return;
        }
        String no = allocateUserNo();
        User patch = new User();
        patch.setId(user.getId());
        patch.setUserNo(no);
        userMapper.updateById(patch);
        user.setUserNo(no);
    }

    private AuthResponse build(User user) {
        return new AuthResponse(jwtUtil.generate(user.getId()), toView(user));
    }

    private UserStateView toView(User u) {
        var realm = RealmPolicy.compute(u.getExp());
        double accuracy = u.getAnswerCount() == 0 ? 0.0
                : (double) u.getCorrectCount() / u.getAnswerCount();
        return new UserStateView(
                u.getId(), u.getUserNo(), u.getNickname(), u.getRealm(), u.getLayer(),
                u.getExp(), u.getHp(), u.getMaxHp(), realm.progress(),
                u.getAnswerCount(), u.getCorrectCount(), accuracy, u.getHp() > 0
        );
    }
}
