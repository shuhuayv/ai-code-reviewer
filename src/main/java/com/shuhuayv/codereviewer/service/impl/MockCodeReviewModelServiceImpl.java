package com.shuhuayv.codereviewer.service.impl;

import com.shuhuayv.codereviewer.service.CodeReviewModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Mock 代码评审实现。
 * 当 ai.mock-enabled=true 时激活（默认），返回稳定的 Mock 评审文本。
 * 不依赖 CodeReviewAnalysisService，不查询数据库。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockCodeReviewModelServiceImpl implements CodeReviewModelService {

    @Override
    public String reviewCode(String prompt) {
        log.info("Mock 评审模式，prompt 长度={}", prompt != null ? prompt.length() : 0);
        return """
                [Mock AI] 以下为 Mock 规则驱动评审结果，真实 AI 评审请设置 AI_MOCK_ENABLED=false

                **文件**: src/main/java/com/example/UserService.java
                **严重级别**: SUGGESTION
                **问题类型**: NULL_CHECK
                **问题描述**: 对 user.getName() 的返回值未做空值检查，可能导致 NullPointerException。
                **修改建议**: 建议使用 Optional.ofNullable(user.getName()).orElse("未知用户") 进行空值处理。

                ---

                **文件**: src/main/java/com/example/OrderController.java
                **严重级别**: WARNING
                **问题类型**: EXCEPTION_HANDLING
                **问题描述**: Controller 中直接使用 try-catch 捕获异常并返回错误信息，建议统一由全局异常处理器处理。
                **修改建议**: 移除 Controller 中的 try-catch，将异常抛出由 GlobalExceptionHandler 统一处理。

                ---

                **文件**: src/main/java/com/example/OrderRepository.java
                **严重级别**: SUGGESTION
                **问题类型**: PERFORMANCE
                **问题描述**: findByUserIdAndStatus 方法对应的查询条件未建立复合索引，数据量大时可能影响查询性能。
                **修改建议**: 建议在 user_id 和 status 字段上建立复合索引：CREATE INDEX idx_user_status ON orders(user_id, status)。
                """;
    }
}