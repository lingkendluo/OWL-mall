package com.owl.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.api.PageResult;
import com.owl.message.converter.MessageConverter;
import com.owl.message.entity.NMessage;
import com.owl.message.enums.MessageKind;
import com.owl.message.enums.MessageTemplate;
import com.owl.message.mapper.NMessageMapper;
import com.owl.message.service.MessageService;
import com.owl.message.vo.MessageVO;
import com.owl.message.vo.UnreadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final NMessageMapper messageMapper;

    @Override
    public void send(Long userId, MessageKind kind, String title, String body, String bizNo) {
        NMessage message = new NMessage();
        message.setUserId(userId);
        message.setKind(kind.getCode());
        message.setTitle(title);
        message.setBody(body);
        message.setBizNo(bizNo);
        message.setReadFlag(0);
        messageMapper.insert(message);
    }

    @Override
    public void sendTemplate(Long userId, MessageKind kind, MessageTemplate template, String bizNo, Object... args) {
        send(userId, kind, template.title(), template.body(args), bizNo);
    }

    @Override
    public PageResult<MessageVO> list(Long userId, String kind, int page, int size) {
        LambdaQueryWrapper<NMessage> wrapper = new LambdaQueryWrapper<NMessage>()
                .eq(NMessage::getUserId, userId)
                .orderByDesc(NMessage::getCreatedAt)
                .orderByDesc(NMessage::getId);
        if (kind != null && !kind.isBlank() && !"all".equalsIgnoreCase(kind)) {
            wrapper.eq(NMessage::getKind, MessageKind.of(kind).getCode());
        }
        Page<NMessage> result = messageMapper.selectPage(Page.of(page, size), wrapper);
        List<MessageVO> list = result.getRecords().stream().map(MessageConverter::toVO).toList();
        return PageResult.of(list, result.getTotal(), page, size);
    }

    @Override
    public UnreadVO unread(Long userId) {
        // 一条 GROUP BY 查完，不要按 kind 发四条 count
        List<Map<String, Object>> rows = messageMapper.selectMaps(new QueryWrapper<NMessage>()
                .select("kind", "COUNT(*) AS cnt")
                .eq("user_id", userId)
                .eq("read_flag", 0)
                .groupBy("kind"));

        UnreadVO vo = new UnreadVO();
        int total = 0;
        int order = 0;
        int promo = 0;
        int system = 0;
        for (Map<String, Object> row : rows) {
            String kind = Objects.toString(row.get("kind"), "");
            int count = Integer.parseInt(Objects.toString(row.get("cnt"), "0"));
            total += count;
            switch (kind) {
                case "order" -> order += count;
                case "promo" -> promo += count;
                case "system" -> system += count;
                default -> {
                }
            }
        }
        vo.setTotal(total);
        vo.setOrder(order);
        vo.setPromo(promo);
        vo.setSystem(system);
        return vo;
    }

    @Override
    public void markRead(Long userId, Long messageId) {
        NMessage message = requireOwn(userId, messageId);
        if (message.getReadFlag() != null && message.getReadFlag() == 1) {
            return;
        }
        messageMapper.update(null, new LambdaUpdateWrapper<NMessage>()
                .eq(NMessage::getId, messageId)
                .eq(NMessage::getUserId, userId)
                .set(NMessage::getReadFlag, 1));
    }

    @Override
    public void markAllRead(Long userId) {
        messageMapper.update(null, new LambdaUpdateWrapper<NMessage>()
                .eq(NMessage::getUserId, userId)
                .eq(NMessage::getReadFlag, 0)
                .set(NMessage::getReadFlag, 1));
    }

    @Override
    public void delete(Long userId, Long messageId) {
        requireOwn(userId, messageId);
        messageMapper.deleteById(messageId);
    }

    @Override
    public void deleteAllOfUser(Long userId) {
        messageMapper.delete(new LambdaQueryWrapper<NMessage>().eq(NMessage::getUserId, userId));
    }

    private NMessage requireOwn(Long userId, Long messageId) {
        NMessage message = messageMapper.selectOne(new LambdaQueryWrapper<NMessage>()
                .eq(NMessage::getId, messageId)
                .eq(NMessage::getUserId, userId));
        if (message == null) {
            throw new BizException(ErrorCode.MESSAGE_NOT_FOUND);
        }
        return message;
    }
}
