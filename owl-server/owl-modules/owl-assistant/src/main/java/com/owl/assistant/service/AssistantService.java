package com.owl.assistant.service;

import com.owl.assistant.dto.ChatRequest;
import com.owl.assistant.vo.ChatResponseVO;

public interface AssistantService {

    /**
     * 对话。模型不可用时不抛异常，返回降级文案。
     *
     * @param userId 登录用户 id，匿名传 null
     * @param ip     匿名请求的来源 IP，用于限流
     */
    ChatResponseVO chat(Long userId, String ip, ChatRequest request);
}
