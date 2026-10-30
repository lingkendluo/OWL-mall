package com.owl.assistant.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owl.assistant.dto.AssistContextDTO;
import com.owl.assistant.dto.ChatMessageDTO;
import com.owl.assistant.dto.ChatRequest;
import com.owl.assistant.entity.AiUsageLog;
import com.owl.assistant.mapper.AiUsageLogMapper;
import com.owl.assistant.service.AssistantService;
import com.owl.assistant.vo.ChatResponseVO;
import com.owl.assistant.vo.UsageVO;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.infra.ai.AiClient;
import com.owl.infra.config.AiProperties;
import com.owl.infra.redis.KVStore;
import com.owl.infra.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    /**
     * 服务端固定 system prompt。前端也会传一条 system，一律丢弃：
     * 前端内容可被篡改，采信它等于把提示词控制权交给浏览器。
     */
    private static final String SYSTEM_PROMPT = """
            你是 OWL 商城的购物分析助手，风格像一位懂行的店员：简洁、有分寸，不推销。

            规则：
            1. 优先使用【当前购物车】里的真实数据回答，涉及金额必须与给定数字一致，不要自己编。
            2. 用户问凑单时，先算差额，再给 1-2 个具体建议，说明加了之后合计多少。
            3. 用户问推荐、库存、活动，而上下文里没有数据时，直说"这个我查不到"，不要编造商品或价格。
            4. 只聊购物相关（商品、价格、运费、配送、售后、订单）。其它话题礼貌拒绝并拉回购物。
            5. 回复用纯文本，可以用「·」列举，不要用 Markdown 标题、表格、代码块。
            6. 不超过 200 字。""";

    /** 模型超时/报错时的兜底，前端体验不能中断 */
    private static final String FALLBACK_REPLY =
            "抱歉，我这边算得有点慢。你可以先看购物车页的合计，或者再问我一次。";

    private static final int USER_LIMIT_PER_HOUR = 20;
    private static final int GUEST_LIMIT_PER_HOUR = 5;
    /** 只带最近 10 条，越长的历史越贵且不提升效果 */
    private static final int MAX_HISTORY = 10;

    private final AiClient aiClient;
    private final AiProperties aiProperties;
    private final AiUsageLogMapper usageLogMapper;
    private final KVStore kv;
    private final ObjectMapper objectMapper;

    @Override
    public ChatResponseVO chat(Long userId, String ip, ChatRequest request) {
        limit(userId, ip);

        List<AiClient.AiMessage> messages = new ArrayList<>();
        messages.add(new AiClient.AiMessage("system", SYSTEM_PROMPT));
        messages.addAll(historyOf(request.getMessages()));
        String context = contextText(request.getContext());
        if (context != null) {
            messages.add(new AiClient.AiMessage("user", context));
        }
        if (messages.size() == 1) {
            // 既没历史也没上下文，等于空对话，直接判定参数问题
            throw new BizException(ErrorCode.PARAM_ERROR, "说点什么吧");
        }

        String model = aiProperties.getModel();
        long start = System.currentTimeMillis();
        try {
            AiClient.AiReply reply = aiClient.chat(messages);
            long latency = System.currentTimeMillis() - start;
            writeLog(userId, model, reply.promptTokens(), reply.completionTokens(), latency, true);
            return build(reply.content(), model, reply.promptTokens(), reply.completionTokens(), false);
        } catch (AiClient.AiUnavailableException e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("AI 不可用，走降级文案 userId={} latency={}ms reason={}", userId, latency, e.getMessage());
            writeLog(userId, model, 0, 0, latency, false);
            return build(FALLBACK_REPLY, model, 0, 0, true);
        }
    }

    /** 固定窗口限流：窗口从第一次调用开始算 1 小时 */
    private void limit(Long userId, String ip) {
        if (userId != null) {
            long used = kv.increment(RedisKey.aiLimitUser(userId), Duration.ofHours(1));
            if (used > USER_LIMIT_PER_HOUR) {
                throw new BizException(ErrorCode.AI_LIMIT,
                        "这个小时已经聊了 " + USER_LIMIT_PER_HOUR + " 次，歇一会儿再来？");
            }
            return;
        }
        long used = kv.increment(RedisKey.aiLimitIp(ip == null ? "unknown" : ip), Duration.ofHours(1));
        if (used > GUEST_LIMIT_PER_HOUR) {
            throw new BizException(ErrorCode.AI_LIMIT, "登录后可以聊得更多");
        }
    }

    /** 丢掉 system（防注入）与非 user/assistant 角色，只保留最后 10 条 */
    private List<AiClient.AiMessage> historyOf(List<ChatMessageDTO> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<AiClient.AiMessage> kept = new ArrayList<>();
        for (ChatMessageDTO message : messages) {
            if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            String role = message.getRole();
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            kept.add(new AiClient.AiMessage(role, message.getContent()));
        }
        return kept.size() <= MAX_HISTORY ? kept : kept.subList(kept.size() - MAX_HISTORY, kept.size());
    }

    /** 购物车快照序列化成一条 user 消息，跟文档约定的前缀保持一致 */
    private String contextText(AssistContextDTO context) {
        if (context == null) {
            return null;
        }
        try {
            return "【当前购物车】" + objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            log.warn("购物车上下文序列化失败", e);
            return null;
        }
    }

    private ChatResponseVO build(String reply, String model, int promptTokens, int completionTokens, boolean degraded) {
        UsageVO usage = new UsageVO();
        usage.setPromptTokens(promptTokens);
        usage.setCompletionTokens(completionTokens);
        usage.setTotalTokens(promptTokens + completionTokens);

        ChatResponseVO vo = new ChatResponseVO();
        vo.setReply(reply);
        vo.setModel(model);
        vo.setUsage(usage);
        vo.setDegraded(degraded);
        return vo;
    }

    /** 成功失败都写，用于对账和排查慢请求 */
    private void writeLog(Long userId, String model, int promptTokens, int completionTokens,
                          long latency, boolean success) {
        try {
            AiUsageLog usageLog = new AiUsageLog();
            usageLog.setUserId(userId);
            usageLog.setModel(model);
            usageLog.setPromptTokens(promptTokens);
            usageLog.setCompletionTokens(completionTokens);
            usageLog.setLatencyMs((int) latency);
            usageLog.setSuccess(success ? 1 : 0);
            usageLog.setCreatedAt(LocalDateTime.now());
            usageLogMapper.insert(usageLog);
        } catch (Exception e) {
            // 记账失败不能影响对话
            log.warn("AI 用量写入失败", e);
        }
    }
}
