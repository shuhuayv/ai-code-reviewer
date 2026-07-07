package com.shuhuayv.codereviewer.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuhuayv.codereviewer.exception.BusinessException;
import com.shuhuayv.codereviewer.service.CodeReviewModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible Chat Completions API 评审实现。
 * 当 ai.mock-enabled=false 时激活。
 * 支持智谱 AI、阿里百炼、DeepSeek、火山方舟等兼容 OpenAI 接口的 provider。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.mock-enabled", havingValue = "false")
public class OpenAiCompatibleCodeReviewModelServiceImpl implements CodeReviewModelService {

    private static final String SAFE_EMPTY_REVIEW_JSON = "{\"summary\":\"AI 未返回最终结构化评审结果，未发现可保存的高风险问题。\",\"issues\":[]}";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int maxTokens;

    public OpenAiCompatibleCodeReviewModelServiceImpl(
            ObjectMapper objectMapper,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.api-base-url:https://open.bigmodel.cn/api/paas/v4}") String baseUrl,
            @Value("${ai.model:glm-4.7-flash}") String model,
            @Value("${ai.temperature:0.2}") double temperature,
            @Value("${ai.max-tokens:4096}") int maxTokens,
            @Value("${ai.timeout-seconds:30}") int timeoutSeconds) {

        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;

        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(cleanBaseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    @Override
    public String reviewCode(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(400, "真实 AI 模式需要配置 ZHIPU_API_KEY 或 AI_API_KEY 环境变量");
        }

        log.info("真实 AI 评审模式，model={}, prompt 长度={}", model, prompt != null ? prompt.length() : 0);

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            log.info("AI API 请求 max_tokens={}", maxTokens);

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> systemMsg = new LinkedHashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是一个严谨的 Java 代码评审助手。请从安全性、健壮性、可维护性、性能和工程规范角度评审代码。输出应结构化、具体、可落地，不要编造不存在的代码。");
            messages.add(systemMsg);

            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);

            requestBody.put("messages", messages);

            String response = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.value() >= 400, (req, resp) -> {
                        byte[] body = resp.getBody().readAllBytes();
                        String errorBody = new String(body);
                        log.error("AI API 调用失败，HTTP {}, 响应: {}", resp.getStatusCode().value(), errorBody);
                        throw new BusinessException(502, "AI API 调用失败: HTTP " + resp.getStatusCode().value());
                    })
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            log.info("AI API 原始响应长度={}", response.length());

            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new BusinessException(502, "AI API 响应格式异常: 缺少 choices");
            }
            JsonNode firstChoice = choices.get(0);
            log.info("AI API finish_reason={}", firstChoice.path("finish_reason").asText(""));

            JsonNode messageNode = firstChoice.get("message");
            if (messageNode == null) {
                throw new BusinessException(502, "AI API 响应格式异常: 缺少 message");
            }
            JsonNode content = messageNode.get("content");
            log.info("AI API message.content nodeType={}", content != null ? content.getNodeType() : null);

            String reviewText = extractContentText(content, messageNode);
            log.info("AI API 最终解析文本长度={}", reviewText != null ? reviewText.length() : 0);

            if (reviewText == null || reviewText.isBlank()) {
                String finishReason = firstChoice.path("finish_reason").asText("");
                if ("length".equals(finishReason)) {
                    throw new BusinessException(502,
                            "AI API 输出被截断且 content/reasoning_content 均为空，请进一步缩短 prompt 或提高 max_tokens");
                }
                throw new BusinessException(502, "AI API 响应内容为空");
            }
            return reviewText;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 AI API 异常: {}", e.getMessage());
            throw new BusinessException(502, "AI API 调用失败: " + e.getMessage());
        }
    }

    /**
     * 从 API 响应中健壮地提取 message.content 文本。
     * 支持 content 为字符串、数组或对象类型。
     */
    private String extractContentText(JsonNode content, JsonNode messageNode) {
        if (content == null) {
            log.info("message.content 为 null，尝试 fallback message.reasoning_content");
            return extractReasoningContent(messageNode);
        }

        if (content.isTextual()) {
            String text = content.asText();
            log.info("AI API content 文本长度={}", text.length());
            if (!text.isBlank()) {
                return normalizeReviewJsonText(text);
            }
            // content 是 STRING 但为空，尝试 reasoning_content
            log.info("content 为 STRING 但为空，尝试 fallback message.reasoning_content");
            return extractReasoningContent(messageNode);
        }

        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : content) {
                if (item.isTextual()) {
                    sb.append(item.asText());
                } else if (item.isObject()) {
                    JsonNode text = item.get("text");
                    if (text != null && text.isTextual()) {
                        sb.append(text.asText());
                    } else {
                        JsonNode contentField = item.get("content");
                        if (contentField != null && contentField.isTextual()) {
                            sb.append(contentField.asText());
                        }
                    }
                }
            }
            String result = sb.toString();
            log.info("AI API content 数组拼接长度={}", result.length());
            if (!result.isBlank()) {
                return result;
            }
        }

        if (content.isObject()) {
            JsonNode text = content.get("text");
            if (text != null && text.isTextual()) {
                return text.asText();
            }
            JsonNode contentField = content.get("content");
            if (contentField != null && contentField.isTextual()) {
                return contentField.asText();
            }
        }

        // final fallback: reasoning_content
        log.info("message.content 解析为空，尝试 fallback message.reasoning_content");
        return extractReasoningContent(messageNode);
    }

    /**
     * 从 message.reasoning_content 中提取文本。
     * reasoning_content 包含模型推理过程，不应直接展示。
     * 优先尝试从中提取 JSON 评审结果；提取不到则返回安全空结果 JSON。
     */
    private String extractReasoningContent(JsonNode messageNode) {
        JsonNode reasoning = messageNode.get("reasoning_content");
        if (reasoning != null && reasoning.isTextual()) {
            String text = reasoning.asText();
            log.info("reasoning_content 非空，尝试提取最终 JSON，长度={}", text.length());
            if (text.isBlank()) {
                return null;
            }
            String json = extractJsonObject(text);
            if (json != null) {
                log.info("从 reasoning_content 提取 JSON 成功");
                return normalizeReviewJsonText(json);
            }
            log.info("reasoning_content 未提取到 JSON，返回安全空结果 JSON");
            return SAFE_EMPTY_REVIEW_JSON;
        }
        log.info("AI API reasoning_content 不可用");
        return null;
    }

    /**
     * 从文本中提取合法的 JSON 评审结果。
     * 支持直接 JSON、```json 包裹、转义 JSON（\" → "）、
     * 以及混有解释文字的 JSON。
     * 用括号计数提取多个 JSON object 候选，返回最后一个合法候选。
     */
    private String extractJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        List<String> rawTexts = new ArrayList<>();
        rawTexts.add(text);

        // 如果有转义 JSON，先反转义
        if (text.contains("\\\"")) {
            rawTexts.add(text.replace("\\\"", "\""));
        }

        String lastValid = null;
        for (String raw : rawTexts) {
            String candidate = extractLastValidJson(raw);
            if (candidate != null) {
                lastValid = candidate;
            }
        }
        return lastValid;
    }

    /**
     * 从单段文本中提取所有 JSON object 候选，返回最后一个合法者。
     * 用括号计数：遇到 { 时计数+1，遇到 } 时计数-1，
     * 计数归零时即为一个完整 JSON object。
     */
    private String extractLastValidJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        // 先尝试去掉 ```json 代码块
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        String lastValid = null;
        int depth = 0;
        int start = -1;

        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '{' && (i == 0 || cleaned.charAt(i - 1) != '\\')) {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}' && (i == 0 || cleaned.charAt(i - 1) != '\\')) {
                depth--;
                if (depth == 0 && start >= 0) {
                    String candidate = cleaned.substring(start, i + 1);
                    String normalized = tryNormalizeReviewJson(candidate);
                    if (normalized != null) {
                        lastValid = normalized;
                    }
                    start = -1;
                }
            }
        }

        return lastValid;
    }

    /**
     * 规范化评审 JSON 文本。
     * 如果文本是转义 JSON，通过 readTree + writeValueAsString 返回规范 JSON。
     */
    private String normalizeReviewJsonText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String normalized = tryNormalizeReviewJson(text.trim());
        if (normalized != null) {
            return normalized;
        }

        if (text.contains("\\\"")) {
            normalized = tryNormalizeReviewJson(text.replace("\\\"", "\"").trim());
            if (normalized != null) {
                return normalized;
            }
        }

        return text;
    }

    private String tryNormalizeReviewJson(String candidate) {
        try {
            JsonNode root = objectMapper.readTree(candidate);
            if (root.isObject() && root.has("summary") && root.has("issues") && root.get("issues").isArray()) {
                return objectMapper.writeValueAsString(root);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 验证字符串是否为合法 JSON 且包含 "summary" 和 "issues" 字段。
     */
    private boolean isValidReviewJson(String candidate) {
        try {
            JsonNode root = objectMapper.readTree(candidate);
            return root.isObject() && root.has("summary") && root.has("issues") && root.get("issues").isArray();
        } catch (Exception e) {
            return false;
        }
    }
}