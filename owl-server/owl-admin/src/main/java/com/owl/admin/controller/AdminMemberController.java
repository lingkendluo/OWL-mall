package com.owl.admin.controller;

import com.owl.admin.aop.AdminAction;
import com.owl.admin.dto.MemberStatusRequest;
import com.owl.admin.service.AdminMemberService;
import com.owl.admin.vo.AdminMemberVO;
import com.owl.common.api.PageResult;
import com.owl.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-会员")
@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @Operation(summary = "会员列表")
    @GetMapping
    public Result<PageResult<AdminMemberVO>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return Result.ok(adminMemberService.list(keyword, status,
                Math.max(1, page), Math.min(100, Math.max(1, size))));
    }

    @Operation(summary = "启用/禁用会员")
    @AdminAction("MEMBER_STATUS")
    @PatchMapping("/{id}/status")
    public Result<AdminMemberVO> changeStatus(@PathVariable String id,
                                             @Valid @RequestBody MemberStatusRequest request) {
        return Result.ok(adminMemberService.changeStatus(id, request.getStatus()));
    }
}
