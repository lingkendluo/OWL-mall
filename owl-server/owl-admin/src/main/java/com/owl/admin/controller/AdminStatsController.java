package com.owl.admin.controller;

import com.owl.admin.service.StatsService;
import com.owl.admin.vo.StatsVO;
import com.owl.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-统计")
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    @Operation(summary = "概览")
    @GetMapping("/overview")
    public Result<StatsVO> overview() {
        return Result.ok(statsService.overview());
    }
}
