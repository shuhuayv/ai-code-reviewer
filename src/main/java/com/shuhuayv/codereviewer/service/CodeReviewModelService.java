package com.shuhuayv.codereviewer.service;

/**
 * 代码评审 AI 模型服务抽象接口。
 * Mock 和真实 OpenAI-compatible 两种实现通过 ai.mock-enabled 配置切换。
 * 只负责根据 prompt 调用模型，不负责查询数据库、扫描文件或保存评审结果。
 */
public interface CodeReviewModelService {

    /**
     * 根据评审 prompt 调用模型，返回评审结果文本。
     *
     * @param prompt 评审 prompt，包含文件路径、语言、代码内容及评审要求
     * @return 模型评审结果文本
     */
    String reviewCode(String prompt);
}