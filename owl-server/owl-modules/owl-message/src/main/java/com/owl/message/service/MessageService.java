package com.owl.message.service;

import com.owl.common.api.PageResult;
import com.owl.message.enums.MessageKind;
import com.owl.message.enums.MessageTemplate;
import com.owl.message.vo.MessageVO;
import com.owl.message.vo.UnreadVO;

public interface MessageService {

    /** 其它模块发消息的统一入口 */
    void send(Long userId, MessageKind kind, String title, String body, String bizNo);

    void sendTemplate(Long userId, MessageKind kind, MessageTemplate template, String bizNo, Object... args);

    PageResult<MessageVO> list(Long userId, String kind, int page, int size);

    UnreadVO unread(Long userId);

    void markRead(Long userId, Long messageId);

    void markAllRead(Long userId);

    void delete(Long userId, Long messageId);

    /** 清理游客账号时连带删除 */
    void deleteAllOfUser(Long userId);
}
