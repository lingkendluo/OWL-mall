package com.owl.trade.controller;

import com.owl.common.api.Result;
import com.owl.common.context.UserContext;
import com.owl.trade.dto.CartAddRequest;
import com.owl.trade.dto.CartCheckRequest;
import com.owl.trade.dto.CartMergeRequest;
import com.owl.trade.dto.CartUpdateRequest;
import com.owl.trade.service.CartService;
import com.owl.trade.vo.CartMergeVO;
import com.owl.trade.vo.CartVO;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 购物车。除了清空，所有写操作都返回整车，前端不用再补一次 GET。
 */
@Tag(name = "购物车")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "查询购物车")
    @GetMapping
    public Result<CartVO> get() {
        return Result.ok(cartService.get(UserContext.requireUserId()));
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear(UserContext.requireUserId());
        return Result.ok();
    }

    @Operation(summary = "加入购物车")
    @PostMapping("/items")
    public Result<CartVO> add(@Valid @RequestBody CartAddRequest request) {
        return Result.ok(cartService.add(UserContext.requireUserId(), request));
    }

    @Operation(summary = "修改数量 / 勾选")
    @PatchMapping("/items/{productId}")
    public Result<CartVO> update(@PathVariable String productId,
                                 @Valid @RequestBody CartUpdateRequest request) {
        return Result.ok(cartService.updateLine(UserContext.requireUserId(), productId, request));
    }

    @Operation(summary = "删除购物车行")
    @DeleteMapping("/items/{productId}")
    public Result<CartVO> remove(@PathVariable String productId) {
        return Result.ok(cartService.remove(UserContext.requireUserId(), productId));
    }

    @Operation(summary = "批量勾选")
    @PutMapping("/checked")
    public Result<CartVO> check(@Valid @RequestBody CartCheckRequest request) {
        return Result.ok(cartService.check(UserContext.requireUserId(), request));
    }

    @Operation(summary = "删除已勾选")
    @DeleteMapping("/checked")
    public Result<CartVO> removeChecked() {
        return Result.ok(cartService.removeChecked(UserContext.requireUserId()));
    }

    @Operation(summary = "合并本地购物车")
    @PostMapping("/merge")
    public Result<CartMergeVO> merge(@Valid @RequestBody CartMergeRequest request) {
        return Result.ok(cartService.merge(UserContext.requireUserId(), request));
    }
}
