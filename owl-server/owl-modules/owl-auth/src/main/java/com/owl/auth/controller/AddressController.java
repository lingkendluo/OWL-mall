package com.owl.auth.controller;

import com.owl.auth.dto.AddressRequest;
import com.owl.auth.service.AddressService;
import com.owl.auth.vo.AddressVO;
import com.owl.common.api.Result;
import com.owl.common.context.UserContext;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "个人中心-地址")
@RestController
@RequestMapping("/m/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "地址列表")
    @GetMapping
    public Result<List<AddressVO>> list() {
        return Result.ok(addressService.list(UserContext.requireUserId()));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public Result<AddressVO> create(@Valid @RequestBody AddressRequest request) {
        return Result.ok(addressService.create(UserContext.requireUserId(), request));
    }

    @Operation(summary = "修改地址")
    @PutMapping("/{id}")
    public Result<AddressVO> update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return Result.ok(addressService.update(UserContext.requireUserId(), id, request));
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(UserContext.requireUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/{id}/default")
    public Result<AddressVO> setDefault(@PathVariable Long id) {
        return Result.ok(addressService.setDefault(UserContext.requireUserId(), id));
    }
}
