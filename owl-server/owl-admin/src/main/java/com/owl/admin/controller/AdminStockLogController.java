package com.owl.admin.controller;

import com.owl.admin.service.StockLogService;
import com.owl.admin.vo.StockLogVO;
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

@Tag(name = "后台-库存流水")
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminStockLogController {

    private final StockLogService stockLogService;

    @Operation(summary = "某个商品的库存流水，按时间倒序")
    @GetMapping("/{code}/stock-logs")
    public Result<PageResult<StockLogVO>> list(@PathVariable String code,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return Result.ok(stockLogService.listByProduct(code,
                Math.max(1, page), Math.min(100, Math.max(1, size))));
    }
}
