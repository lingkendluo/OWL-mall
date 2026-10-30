package com.owl.infra.ai;

import java.util.List;

/**
 * 大模型客户端抽象。以后换模型只换实现，Service 不动。
 */
public interface AiClient {

    AiReply chat(List<AiMessage> messages);

    record AiMessage(String role, String content) {
    }

    record AiReply(String content, String model, int promptTokens, int completionTokens) {
    }

    /** 调用失败（超时 / 未配 Key / 上游报错）统一抛这个，上层决定降级还是报错 */
    class AiUnavailableException extends RuntimeException {
        public AiUnavailableException(String message) {
            super(message);
        }

        public AiUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
