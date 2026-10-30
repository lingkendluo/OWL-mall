package com.owl.catalog.controller;

import com.owl.catalog.service.ProductService;
import com.owl.catalog.vo.ProductDetailBundleVO;
import com.owl.catalog.vo.ProductVO;
import com.owl.common.api.PageResult;
import com.owl.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商品")
@RestController
@RequestMapping("/c/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品列表 / 搜索")
    @GetMapping
    public Result<PageResult<ProductVO>> search(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String sub,
                                                @RequestParam(defaultValue = "default") String sort,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return Result.ok(productService.search(keyword, category, sub, sort,
                Math.max(1, page), Math.min(100, Math.max(1, size))));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{code}")
    public Result<ProductDetailBundleVO> detail(@PathVariable String code) {
        return Result.ok(productService.detail(code));
    }
}
