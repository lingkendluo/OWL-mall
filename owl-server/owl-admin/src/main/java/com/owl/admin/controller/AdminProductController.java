package com.owl.admin.controller;

import com.owl.admin.aop.AdminAction;
import com.owl.catalog.dto.AdminProductRequest;
import com.owl.catalog.dto.ProductStatusRequest;
import com.owl.catalog.dto.StockAdjustRequest;
import com.owl.catalog.service.ProductAdminService;
import com.owl.catalog.vo.AdminProductVO;
import com.owl.catalog.vo.CategoryStatVO;
import com.owl.common.api.PageResult;
import com.owl.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-商品")
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductAdminService productAdminService;

    @Operation(summary = "商品列表")
    @GetMapping
    public Result<PageResult<AdminProductVO>> list(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) String category,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return Result.ok(productAdminService.list(keyword, status, category,
                Math.max(1, page), Math.min(100, Math.max(1, size))));
    }

    @Operation(summary = "大类索引统计")
    @GetMapping("/stats")
    public Result<List<CategoryStatVO>> stats() {
        return Result.ok(productAdminService.categoryStats());
    }

    @Operation(summary = "新增商品")
    @AdminAction("PRODUCT_CREATE")
    @PostMapping
    public Result<AdminProductVO> create(@Valid @RequestBody AdminProductRequest request) {
        return Result.ok(productAdminService.create(request));
    }

    @Operation(summary = "修改商品")
    @AdminAction("PRODUCT_UPDATE")
    @PutMapping("/{code}")
    public Result<AdminProductVO> update(@PathVariable String code,
                                        @Valid @RequestBody AdminProductRequest request) {
        return Result.ok(productAdminService.update(code, request));
    }

    @Operation(summary = "删除商品")
    @AdminAction("PRODUCT_DELETE")
    @DeleteMapping("/{code}")
    public Result<Void> delete(@PathVariable String code) {
        productAdminService.delete(code);
        return Result.ok();
    }

    @Operation(summary = "上下架")
    @AdminAction("PRODUCT_STATUS")
    @PatchMapping("/{code}/status")
    public Result<AdminProductVO> changeStatus(@PathVariable String code,
                                              @Valid @RequestBody ProductStatusRequest request) {
        return Result.ok(productAdminService.changeStatus(code, request.getStatus()));
    }

    @Operation(summary = "库存调整")
    @AdminAction("PRODUCT_STOCK")
    @PatchMapping("/{code}/stock")
    public Result<AdminProductVO> adjustStock(@PathVariable String code,
                                              @Valid @RequestBody StockAdjustRequest request) {
        return Result.ok(productAdminService.adjustStock(code, request.getDelta(), request.getReason()));
    }
}
