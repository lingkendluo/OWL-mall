package com.owl.infra.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.owl.infra.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek（OpenAI 兼容协议）客户端。用 Boot 3.2 自带的 RestClient，不引第三方 SDK。
 * API Key 只从配置读（配置只从环境变量 DEEPSEEK_API_KEY 读），绝不返回给前端。
 */
@Slf4j
@Component
public class DeepSeekClient implements AiClient {

    private final AiProperties props;
    private final RestClient restClient;

    public DeepSeekClient(AiProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofMillis(props.getTimeoutMs()));
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public AiReply chat(List<AiMessage> messages) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new AiUnavailableException("未配置 DEEPSEEK_API_KEY");
        }

        List<Map<String, String>> payload = new ArrayList<>();
        for (AiMessage message : messages) {
            payload.add(Map.of("role", message.role(), "content", message.content()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getModel());
        body.put("messages", payload);
        body.put("temperature", props.getTemperature());
        body.put("max_tokens", props.getMaxTokens());
        body.put("stream", false);

        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new AiUnavailableException("模型返回空响应");
            }
            JsonNode choices = response.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new AiUnavailableException("模型返回缺少 choices");
            }
            String content = choices.get(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new AiUnavailableException("模型返回内容为空");
            }
            JsonNode usage = response.path("usage");
            return new AiReply(
                    content.trim(),
                    response.path("model").asText(props.getModel()),
                    usage.path("prompt_tokens").asInt(0),
                    usage.path("completion_tokens").asInt(0));
        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("调用 DeepSeek 失败: {}", e.toString());
            throw new AiUnavailableException("调用模型失败", e);
        }
    }
}
