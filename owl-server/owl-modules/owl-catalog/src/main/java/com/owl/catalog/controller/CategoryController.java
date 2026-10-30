package com.owl.catalog.controller;

import com.owl.catalog.service.CategoryService;
import com.owl.catalog.service.ProductService;
import com.owl.catalog.vo.CategoryVO;
import com.owl.catalog.vo.HomeVO;
import com.owl.catalog.vo.ProductVO;
import com.owl.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商品-类目")
@RestController
@RequestMapping("/c")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    @Operation(summary = "首页聚合")
    @GetMapping("/home")
    public Result<HomeVO> home() {
        return Result.ok(productService.home());
    }

    @Operation(summary = "类目列表")
    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.ok(categoryService.listAll());
    }

    @Operation(summary = "类目详情")
    @GetMapping("/categories/{code}")
    public Result<CategoryVO> category(@PathVariable String code,
                                       @RequestParam(required = false) String sub) {
        return Result.ok(categoryService.detail(code, sub));
    }

    @Operation(summary = "推荐位")
    @GetMapping("/featured")
    public Result<List<ProductVO>> featured(@RequestParam(defaultValue = "12") int limit) {
        return Result.ok(productService.featured(limit));
    }
}
