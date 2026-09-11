package com.xiuxian.controller;

import com.xiuxian.model.dto.GenerateResponse;
import com.xiuxian.model.dto.SpiritRootResult;
import com.xiuxian.model.dto.TestSubmitRequest;
import com.xiuxian.service.SpiritRootService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 测灵根：注册成功后由前端引导用户作答 5 道题，根据正确率判定灵根并入库
 */
@RestController
@RequestMapping("/api/spirit-root")
public class SpiritRootController {

    private final SpiritRootService spiritRootService;

    public SpiritRootController(SpiritRootService spiritRootService) {
        this.spiritRootService = spiritRootService;
    }

    /** 出题：固定 5 题；difficulty 可选（入门/进阶/精通），默认入门 */
    @PostMapping("/start")
    public GenerateResponse start(@RequestParam(value = "difficulty", required = false) String difficulty,
                                  @RequestAttribute("userId") Long userId) {
        return spiritRootService.start(userId, difficulty);
    }

    /** 提交答案：一次性给 5 个，判定并返回所得灵根 */
    @PostMapping("/submit")
    public SpiritRootResult submit(@Valid @RequestBody TestSubmitRequest req,
                                   @RequestAttribute("userId") Long userId) {
        return spiritRootService.submit(userId, req.answers());
    }
}