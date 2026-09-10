package com.xiuxian.controller;

import com.xiuxian.model.dto.BagActionRequest;
import com.xiuxian.model.dto.BagActionResult;
import com.xiuxian.model.dto.BagView;
import com.xiuxian.service.BagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 背包（储物）接口：容量随境界提升，装备储物法宝可再加格数
 */
@RestController
@RequestMapping("/api/bag")
public class BagController {

    private final BagService bagService;

    public BagController(BagService bagService) {
        this.bagService = bagService;
    }

    /** 查看背包（需登录） */
    @GetMapping
    public BagView bag(@RequestAttribute("userId") Long userId) {
        return bagService.bag(userId);
    }

    /** 装备（需登录） */
    @PostMapping("/equip")
    public BagActionResult equip(@Valid @RequestBody BagActionRequest req,
                                 @RequestAttribute("userId") Long userId) {
        return bagService.equip(userId, req.itemId());
    }

    /** 卸下（需登录） */
    @PostMapping("/unequip")
    public BagActionResult unequip(@Valid @RequestBody BagActionRequest req,
                                   @RequestAttribute("userId") Long userId) {
        return bagService.unequip(userId, req.itemId());
    }

    /** 使用/服用（需登录） */
    @PostMapping("/use")
    public BagActionResult use(@Valid @RequestBody BagActionRequest req,
                               @RequestAttribute("userId") Long userId) {
        return bagService.use(userId, req.itemId());
    }

    /** 丢弃（需登录），可传 count 指定数量，默认 1 */
    @PostMapping("/discard")
    public BagActionResult discard(@Valid @RequestBody BagActionRequest req,
                                   @RequestAttribute("userId") Long userId) {
        return bagService.discard(userId, req.itemId(), req.count());
    }
}
