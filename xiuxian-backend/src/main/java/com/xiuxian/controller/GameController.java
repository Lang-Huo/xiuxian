package com.xiuxian.controller;

import com.xiuxian.model.dto.*;
import com.xiuxian.model.entity.User;
import com.xiuxian.service.GameService;
import com.xiuxian.service.ProfileService;
import com.xiuxian.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GameController {

    private final QuestionService questionService;
    private final GameService gameService;
    private final ProfileService profileService;

    public GameController(QuestionService questionService, GameService gameService, ProfileService profileService) {
        this.questionService = questionService;
        this.gameService = gameService;
        this.profileService = profileService;
    }

    /** 输入想学的知识 -> 生成题目（需登录） */
    @PostMapping("/topics/generate")
    public GenerateResponse generate(@Valid @RequestBody GenerateRequest req,
                                     @RequestAttribute("userId") Long userId) {
        return questionService.generate(userId, req.topic(), req.difficulty(), req.count() == null ? 5 : req.count());
    }

    /** 提交答案 -> 结算（经验/血量/境界 + 解析）（需登录） */
    @PostMapping("/answer")
    public AnswerResponse answer(@Valid @RequestBody AnswerRequest req,
                                 @RequestAttribute("userId") Long userId) {
        return gameService.answer(userId, req.questionId(), req.userAnswer());
    }

    /** 当前登录修仙者状态（需登录） */
    @PostMapping("/users/me")
    public UserStateView me(@RequestAttribute("userId") Long userId) {
        User user = gameService.getUserById(userId);
        return toView(user);
    }

    /** 查询用户状态（需登录） */
    @GetMapping("/users/{id}")
    public UserStateView user(@PathVariable Long id) {
        User user = gameService.getUserById(id);
        return toView(user);
    }

    /** 当前登录修仙者的详细信息（需登录）：进度、战绩、修炼统计、最近修炼记录 */
    @GetMapping("/users/profile")
    public ProfileView profile(@RequestAttribute("userId") Long userId) {
        return profileService.profile(userId);
    }

    private UserStateView toView(User u) {
        var realm = com.xiuxian.service.RealmPolicy.compute(u.getExp());
        double accuracy = u.getAnswerCount() == 0 ? 0.0
                : (double) u.getCorrectCount() / u.getAnswerCount();
        return new UserStateView(
                u.getId(), u.getUserNo(), u.getNickname(), u.getRealm(), u.getLayer(),
                u.getExp(), u.getHp(), u.getMaxHp(), realm.progress(),
                u.getAnswerCount(), u.getCorrectCount(), accuracy, u.getHp() > 0
        );
    }
}
