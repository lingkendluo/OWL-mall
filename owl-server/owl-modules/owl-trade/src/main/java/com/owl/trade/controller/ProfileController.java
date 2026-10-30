package com.owl.trade.controller;

import com.owl.common.api.Result;
import com.owl.common.context.UserContext;
import com.owl.trade.service.ProfileService;
import com.owl.trade.vo.ProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "个人中心")
@RestController
@RequestMapping("/m/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "个人中心汇总")
    @GetMapping
    public Result<ProfileVO> profile() {
        return Result.ok(profileService.profile(UserContext.requireUserId()));
    }
}
