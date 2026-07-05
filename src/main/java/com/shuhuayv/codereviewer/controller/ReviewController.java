package com.shuhuayv.codereviewer.controller;

import com.shuhuayv.codereviewer.common.ApiResponse;
import com.shuhuayv.codereviewer.dto.*;
import com.shuhuayv.codereviewer.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "评审任务管理", description = "Mock 代码评审任务的创建与查询接口")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/tasks")
    @Operation(summary = "创建评审任务", description = "创建 Mock 评审任务，生成模拟评审结果（问题 + 报告）")
    public ApiResponse<ReviewTaskResponse> create(@Valid @RequestBody CreateReviewTaskRequest request) {
        return ApiResponse.success(reviewService.createMockReview(request));
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "评审任务详情", description = "获取评审任务基本信息")
    public ApiResponse<ReviewTaskDetailResponse> getTaskDetail(
            @Parameter(description = "任务ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(reviewService.getTaskDetail(id));
    }

    @GetMapping("/tasks/{id}/issues")
    @Operation(summary = "评审问题列表", description = "获取评审任务发现的所有问题")
    public ApiResponse<List<ReviewIssueResponse>> getTaskIssues(
            @Parameter(description = "任务ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(reviewService.getTaskIssues(id));
    }

    @GetMapping("/tasks/{id}/report")
    @Operation(summary = "评审报告", description = "获取评审任务的评审报告，返回 JSON 格式，包含 markdownContent 字段")
    public ApiResponse<ReviewReportResponse> getTaskReport(
            @Parameter(description = "任务ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(reviewService.getTaskReport(id));
    }

    @GetMapping(value = "/tasks/{id}/report/markdown", produces = MediaType.TEXT_MARKDOWN_VALUE)
    @Operation(summary = "导出 Markdown 报告", description = "直接返回 markdownContent 文本，可用于导出 .md 文件")
    public String getTaskReportMarkdown(
            @Parameter(description = "任务ID", example = "1") @PathVariable Long id) {
        return reviewService.getTaskReportMarkdown(id);
    }
}