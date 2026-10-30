package com.owl.admin.controller;

import com.owl.admin.service.AdminLogService;
import com.owl.admin.vo.AdminLogVO;
import com.owl.common.api.PageResult;
import com.owl.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-操作日志")
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService adminLogService;

    @Operation(summary = "操作日志，按 id 倒序")
    @GetMapping
    public Result<PageResult<AdminLogVO>> page(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String action,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return Result.ok(adminLogService.page(keyword, action,
                Math.max(1, page), Math.min(100, Math.max(1, size))));
    }
}
