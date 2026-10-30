package com.owl.message.controller;

import com.owl.common.api.PageResult;
import com.owl.common.api.Result;
import com.owl.common.context.UserContext;
import com.owl.message.service.MessageService;
import com.owl.message.vo.MessageVO;
import com.owl.message.vo.UnreadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "消息")
@RestController
@RequestMapping("/m/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "消息列表")
    @GetMapping
    public Result<PageResult<MessageVO>> list(@RequestParam(defaultValue = "all") String kind,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return Result.ok(messageService.list(UserContext.requireUserId(), kind, Math.max(1, page), Math.min(100, size)));
    }

    @Operation(summary = "未读数")
    @GetMapping("/unread")
    public Result<UnreadVO> unread() {
        return Result.ok(messageService.unread(UserContext.requireUserId()));
    }

    @Operation(summary = "标记已读")
    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        messageService.markRead(UserContext.requireUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "全部已读")
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        messageService.markAllRead(UserContext.requireUserId());
        return Result.ok();
    }

    @Operation(summary = "删除消息")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageService.delete(UserContext.requireUserId(), id);
        return Result.ok();
    }
}
