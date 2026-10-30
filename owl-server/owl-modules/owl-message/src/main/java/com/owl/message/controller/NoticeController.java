package com.owl.message.controller;

import com.owl.common.api.Result;
import com.owl.message.service.NoticeService;
import com.owl.message.vo.NoticeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端公告。挂在 /c 下，走 WebConfig 的免鉴权白名单，未登录的访客也能看到首页横幅。
 */
@Tag(name = "公告")
@RestController
@RequestMapping("/c/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "已发布公告列表")
    @GetMapping
    public Result<List<NoticeVO>> list(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(noticeService.listPublished(limit));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/{id}")
    public Result<NoticeVO> detail(@PathVariable String id) {
        return Result.ok(noticeService.detail(id));
    }
}
