package com.owl.admin.controller;

import com.owl.admin.aop.AdminAction;
import com.owl.catalog.dto.CategoryUpsertRequest;
import com.owl.catalog.service.CategoryAdminService;
import com.owl.catalog.vo.CategoryVO;
import com.owl.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-类目")
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryAdminService categoryAdminService;

    @Operation(summary = "类目列表")
    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryAdminService.list());
    }

    @Operation(summary = "新增类目")
    @AdminAction("CATEGORY_CREATE")
    @PostMapping
    public Result<CategoryVO> create(@Valid @RequestBody CategoryUpsertRequest request) {
        return Result.ok(categoryAdminService.create(request));
    }

    @Operation(summary = "修改类目")
    @AdminAction("CATEGORY_UPDATE")
    @PutMapping("/{code}")
    public Result<CategoryVO> update(@PathVariable String code,
                                     @Valid @RequestBody CategoryUpsertRequest request) {
        return Result.ok(categoryAdminService.update(code, request));
    }
}
