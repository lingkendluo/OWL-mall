package com.owl.admin.controller;

import com.owl.admin.aop.AdminAction;
import com.owl.common.api.Result;
import com.owl.common.context.AdminContext;
import com.owl.message.dto.NoticeUpsertRequest;
import com.owl.message.service.NoticeAdminService;
import com.owl.message.vo.NoticeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-公告")
@RestController
@RequestMapping("/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeAdminService noticeAdminService;

    @Operation(summary = "公告列表，含草稿")
    @GetMapping
    public Result<List<NoticeVO>> list(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Integer status) {
        return Result.ok(noticeAdminService.list(keyword, status));
    }

    @Operation(summary = "新建公告")
    @AdminAction("NOTICE_CREATE")
    @PostMapping
    public Result<NoticeVO> create(@Valid @RequestBody NoticeUpsertRequest request) {
        return Result.ok(noticeAdminService.create(request, AdminContext.requireAdminId()));
    }

    @Operation(summary = "修改公告")
    @AdminAction("NOTICE_UPDATE")
    @PutMapping("/{id}")
    public Result<NoticeVO> update(@PathVariable String id,
                                   @Valid @RequestBody NoticeUpsertRequest request) {
        return Result.ok(noticeAdminService.update(id, request, AdminContext.requireAdminId()));
    }

    @Operation(summary = "发布公告")
    @AdminAction("NOTICE_PUBLISH")
    @PostMapping("/{id}/publish")
    public Result<NoticeVO> publish(@PathVariable String id) {
        return Result.ok(noticeAdminService.publish(id, AdminContext.requireAdminId()));
    }

    @Operation(summary = "下线公告")
    @AdminAction("NOTICE_OFFLINE")
    @PostMapping("/{id}/offline")
    public Result<NoticeVO> offline(@PathVariable String id) {
        return Result.ok(noticeAdminService.offline(id, AdminContext.requireAdminId()));
    }

    @Operation(summary = "删除公告")
    @AdminAction("NOTICE_DELETE")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        noticeAdminService.delete(id);
        return Result.ok();
    }
}
