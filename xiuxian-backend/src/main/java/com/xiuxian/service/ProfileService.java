package com.xiuxian.service;

import com.xiuxian.mapper.QuestionMapper;
import com.xiuxian.mapper.TopicMapper;
import com.xiuxian.mapper.UserMapper;
import com.xiuxian.model.dto.ProfileView;
import com.xiuxian.model.entity.Topic;
import com.xiuxian.model.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 个人详情：聚合用户基础信息、修仙进度、答题战绩与修炼统计
 */
@Service
public class ProfileService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int RECENT_LIMIT = 5;

    /** 境界 -> 称号 */
    private static final Map<String, String> TITLES = Map.of(
            "凡人", "初入凡尘",
            "练气", "初窥门径",
            "筑基", "登堂入室",
            "金丹", "丹成九转",
            "元婴", "道基稳固",
            "化神", "神游太虚",
            "炼虚", "炼虚合道",
            "合体", "天人合一",
            "大乘", "大乘可期"
    );

    private final UserMapper userMapper;
    private final TopicMapper topicMapper;
    private final QuestionMapper questionMapper;

    public ProfileService(UserMapper userMapper, TopicMapper topicMapper, QuestionMapper questionMapper) {
        this.userMapper = userMapper;
        this.topicMapper = topicMapper;
        this.questionMapper = questionMapper;
    }

    /** 查询当前用户的详细信息 */
    public ProfileView profile(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }

        var info = RealmPolicy.compute(u.getExp());
        double accuracy = u.getAnswerCount() == 0 ? 0.0
                : (double) u.getCorrectCount() / u.getAnswerCount();

        long topicCount = topicMapper.countByUserId(userId);
        long questionCount = questionMapper.countByUserId(userId);

        List<ProfileView.TopicBrief> recent = topicMapper
                .findRecentByUserId(userId, RECENT_LIMIT)
                .stream()
                .map(this::toBrief)
                .toList();

        LocalDateTime joined = u.getCreatedAt();
        LocalDate joinDay = joined == null ? LocalDate.now() : joined.toLocalDate();
        int days = (int) ChronoUnit.DAYS.between(joinDay, LocalDate.now()) + 1;

        return new ProfileView(
                u.getId(),
                u.getUserNo() == null ? "" : u.getUserNo(),
                maskPhone(u.getUsername()),
                u.getNickname(),
                AvatarPolicy.of(u.getAvatar(), u.getNickname()),
                TITLES.getOrDefault(info.realm(), "修行中人"),
                info.realm(),
                info.layer(),
                u.getExp(),
                info.isMax() ? 0 : Math.max(0, info.nextThreshold() - u.getExp()),
                RealmPolicy.nextRealmName(info.realm()),
                info.progress(),
                info.isMax(),
                u.getHp(),
                u.getMaxHp(),
                u.getAnswerCount(),
                u.getCorrectCount(),
                accuracy,
                (int) topicCount,
                (int) questionCount,
                joinDay.format(DATE),
                Math.max(1, days),
                recent
        );
    }

    private ProfileView.TopicBrief toBrief(Topic t) {
        return new ProfileView.TopicBrief(
                t.getId(),
                t.getKeyword(),
                t.getDifficulty(),
                t.getCount(),
                t.getCreatedAt() == null ? "" : t.getCreatedAt().format(DATETIME)
        );
    }

    /** 手机号脱敏：13812345678 -> 138****5678 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone == null ? "" : phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
