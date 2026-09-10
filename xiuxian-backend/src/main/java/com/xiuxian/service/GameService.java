package com.xiuxian.service;

import com.xiuxian.mapper.QuestionMapper;
import com.xiuxian.mapper.TreasureMapper;
import com.xiuxian.mapper.UserMapper;
import com.xiuxian.model.dto.AnswerResponse;
import com.xiuxian.model.dto.TreasureDrop;
import com.xiuxian.model.entity.Question;
import com.xiuxian.model.entity.Treasure;
import com.xiuxian.model.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 游戏化结算：答题 -> 经验/血量/境界变化 + 宝物掉落
 * 经验倍率 = 难度系数 × (1 + 已装备宝物的经验加成%)
 */
@Service
public class GameService {

    private static final int BASE_EXP = 10;
    private static final int WRONG_HP_COST_BASE = 8;
    private static final int CORRECT_HP_HEAL = 3;   // 答对小幅回血，避免卡死

    /**
     * 答对后的基础掉落概率（计划书：常见 5%~15%）
     */
    private static final double DROP_RATE_CORRECT = 1; //0.12

    /**
     * 答错不掉落宝物；若日后想恢复"答错也有机缘"，把这里改成正数即可（如 0.04）
     */
    private static final double DROP_RATE_WRONG = 0.0;

    /**
     * 难度加成后的掉落概率上限，避免精通题把背包刷爆
     */
    private static final double DROP_RATE_MAX = 0.35;

    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final TreasureMapper treasureMapper;
    private final BagService bagService;

    public GameService(UserMapper userMapper, QuestionMapper questionMapper,
                       TreasureMapper treasureMapper, BagService bagService) {
        this.userMapper = userMapper;
        this.questionMapper = questionMapper;
        this.treasureMapper = treasureMapper;
        this.bagService = bagService;
    }

    /**
     * 按 id 取用户，不存在则抛异常（登录后由令牌保证存在）
     */
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }
        return user;
    }

    @Transactional
    public AnswerResponse answer(Long userId, Long questionId, String userAnswer) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }
        Question q = questionMapper.selectById(questionId);
        if (q == null) {
            throw new IllegalArgumentException("题目不存在: " + questionId);
        }

        boolean correct = isCorrect(q, userAnswer);
        double coeff = difficultyCoeff(q.getDifficulty());
        double equipBonus = 1 + bagService.expBonusPercent(userId) / 100.0;

        int expGain = (int) Math.round(BASE_EXP * coeff * (correct ? 1.0 : 0.3) * equipBonus);
        int hpDelta = correct ? CORRECT_HP_HEAL
                : -(int) Math.round(WRONG_HP_COST_BASE * coeff);

        int newHp = Math.max(0, Math.min(user.getMaxHp(), user.getHp() + hpDelta));
        int newExp = user.getExp() + expGain;

        String oldRealm = user.getRealm();
        var realmInfo = RealmPolicy.compute(newExp);

        boolean leveledUp = !realmInfo.realm().equals(oldRealm);

        user.setExp(newExp);
        user.setHp(newHp);
        user.setRealm(realmInfo.realm());
        user.setLayer(realmInfo.layer());
        user.setAnswerCount(user.getAnswerCount() + 1);
        if (correct) user.setCorrectCount(user.getCorrectCount() + 1);
        userMapper.updateById(user);

        // 宝物掉落：仅答对才有机会，难度越高越容易出宝物（DROP_RATE_WRONG 为 0，答错不掉）
        TreasureDrop drop = null;
        boolean bagFull = false;
        double dropRate = Math.min(DROP_RATE_MAX,
                (correct ? DROP_RATE_CORRECT : DROP_RATE_WRONG) * difficultyDropCoeff(q.getDifficulty()));
        Treasure picked = rollTreasure(dropRate);
        if (picked != null) {
            if (bagService.grant(userId, picked, 1)) {
                drop = new TreasureDrop(picked.getName(), picked.getRarity(), picked.getDescription());
            } else {
                bagFull = true;
            }
        }

        return new AnswerResponse(
                correct,
                q.getAnswer(),
                q.getExplanation(),
                q.getKnowledgePoint(),
                q.getSourceUrl(),
                expGain,
                hpDelta,
                user.getHp(),
                user.getMaxHp(),
                user.getExp(),
                user.getRealm(),
                user.getLayer(),
                realmInfo.progress(),
                leveledUp,
                user.getHp() > 0,
                drop,
                bagFull
        );
    }

    /**
     * 难度对掉落概率的影响：越难的题，机缘越厚
     */
    private double difficultyDropCoeff(String difficulty) {
        return switch (difficulty) {
            case "进阶" -> 1.3;
            case "精通" -> 1.6;
            default -> 1.0;   // 入门
        };
    }

    /**
     * 按概率决定是否掉落，命中后按 drop_weight 权重抽取一件宝物。
     *
     * @return 抽中的宝物；未命中或无可掉落图鉴时返回 null
     */
    private Treasure rollTreasure(double rate) {
        if (rate <= 0 || ThreadLocalRandom.current().nextDouble() >= rate) {
            return null;
        }
        List<Treasure> pool = treasureMapper.findDroppable();
        if (pool.isEmpty()) {
            return null;
        }
        int total = pool.stream().mapToInt(Treasure::getDropWeight).sum();
        if (total <= 0) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (Treasure t : pool) {
            acc += t.getDropWeight();
            if (roll < acc) {
                return t;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private boolean isCorrect(Question q, String userAnswer) {
        if (userAnswer == null) return false;
        String ua = userAnswer.trim();
        if ("JUDGE".equals(q.getType())) {
            return Boolean.parseBoolean(ua) == Boolean.parseBoolean(q.getAnswer());
        }
        return ua.equalsIgnoreCase(q.getAnswer().trim());
    }

    private double difficultyCoeff(String difficulty) {
        return switch (difficulty) {
            case "进阶" -> 1.5;
            case "精通" -> 2.0;
            default -> 1.0;   // 入门
        };
    }
}
