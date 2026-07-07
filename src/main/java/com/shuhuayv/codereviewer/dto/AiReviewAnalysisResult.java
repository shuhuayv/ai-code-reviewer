package com.shuhuayv.codereviewer.dto;

import com.shuhuayv.codereviewer.entity.ReviewIssue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 评审分析结果，包含原始响应文本、解析出的结构化问题列表及元信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReviewAnalysisResult {

    /** AI 模型返回的原始评审内容 */
    private String rawReviewText;

    /** 解析出的结构化问题列表，可能为空 */
    private List<ReviewIssue> issues;

    /** 是否成功解析出结构化问题 */
    private boolean structuredParsed;

    /** AI 服务提供商 */
    private String provider;

    /** AI 模型名称 */
    private String model;
}