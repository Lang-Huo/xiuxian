package com.xiuxian.service;

import com.xiuxian.mapper.QuestionMapper;
import com.xiuxian.mapper.SpiritRootMapper;
import com.xiuxian.mapper.UserMapper;
import com.xiuxian.model.dto.GenerateResponse;
import com.xiuxian.model.dto.SpiritRootResult;
import com.xiuxian.model.dto.SpiritRootView;
import com.xiuxian.model.dto.TestSubmitRequest;
import com.xiuxian.model.entity.Question;
import com.xiuxian.model.entity.SpiritRoot;
import com.xiuxian.model.entity.User;
import com.xiuxian.util.AnswerUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 测灵根服务：
 *   启动时把 spirit_roots 全表加载到 SpiritRootPolicy 缓存（避免每次按 id 查 DB）
 *   start   按所选难度复用出题引擎，固定生成 5 题；主题关键字带前缀以便从"修炼记录"里排除
 *   submit 一次性提交 5 题答案，按正确率判定灵根并把 spirit_root_code 写回 users
 */
@Service
public class SpiritRootService {

    private final QuestionService questionService;
    private final QuestionMapper questionMapper;
    private final UserMapper userMapper;
    private final SpiritRootMapper spiritRootMapper;

    public SpiritRootService(QuestionService questionService,
                             QuestionMapper questionMapper,
                             UserMapper userMapper,
                             SpiritRootMapper spiritRootMapper) {
        this.questionService = questionService;
        this.questionMapper = questionMapper;
        this.userMapper = userMapper;
        this.spiritRootMapper = spiritRootMapper;
    }

    /** 启动时把灵根图鉴灌入 Policy 缓存；之后业务全程走内存 */
    @PostConstruct
    public void warmUp() {
        try {
            SpiritRootPolicy.loadFromDatabase(spiritRootMapper.findAll());
        } catch (Exception e) {
            // 启动期建表未完成等情况，不阻塞应用；缓存为空时业务会用 1.0 兜底
            System.err.println("[SpiritRoot] 缓存灵根图鉴失败：" + e.getMessage());
        }
    }

    /** 生成测灵根题目（固定 5 题） */
    public GenerateResponse start(Long userId, String difficulty) {
        String diff = (difficulty == null || difficulty.isBlank()) ? "入门" : difficulty;
        return questionService.generate(
                userId,
                SpiritRootPolicy.TEST_TOPIC_PREFIX + diff,
                diff,
                SpiritRootPolicy.TEST_COUNT);
    }

    /** 提交答案，判定灵根并把 spirit_root_code 写回 users */
    @Transactional
    public SpiritRootResult submit(Long userId, List<TestSubmitRequest.AnswerItem> answers) {
        int total = answers == null ? 0 : answers.size();
        int correct = 0;
        if (answers != null) {
            for (TestSubmitRequest.AnswerItem a : answers) {
                Question q = questionMapper.selectById(a.questionId());
                if (AnswerUtil.isCorrect(q, a.userAnswer())) {
                    correct++;
                }
            }
        }
        SpiritRootView root = SpiritRootPolicy.judge(correct, total);
        if (root == null) {
            throw new IllegalStateException("灵根图鉴尚未加载，请稍后再试");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }
        User patch = new User();
        patch.setId(userId);
        patch.setSpiritRootCode(root.code());
        patch.setSpiritRoot(root.name());        // 同步写旧字段，方便排查
        userMapper.updateById(patch);

        double accuracy = total == 0 ? 0.0 : (double) correct / total;
        return new SpiritRootResult(root, correct, total, accuracy);
    }
}
