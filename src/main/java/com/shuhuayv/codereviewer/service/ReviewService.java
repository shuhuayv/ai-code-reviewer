package com.shuhuayv.codereviewer.service;

import com.shuhuayv.codereviewer.dto.*;
import com.shuhuayv.codereviewer.entity.*;
import com.shuhuayv.codereviewer.exception.BusinessException;
import com.shuhuayv.codereviewer.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final RepoInfoMapper repoInfoMapper;
    private final ReviewTaskMapper reviewTaskMapper;
    private final ReviewIssueMapper reviewIssueMapper;
    private final ReviewReportMapper reviewReportMapper;
    private final CodeReviewAnalysisService codeReviewAnalysisService;

    @Transactional
    public ReviewTaskResponse createMockReview(CreateReviewTaskRequest request) {
        // 1. 校验 repoId 是否存在
        RepoInfo repo = repoInfoMapper.selectById(request.getRepoId());
        if (repo == null) {
            throw new BusinessException(404, "仓库不存在: " + request.getRepoId());
        }

        // 2. 创建 review_task
        ReviewTask task = new ReviewTask();
        task.setRepoId(request.getRepoId());
        task.setCommitId(request.getCommitId() != null ? request.getCommitId() : "mock-commit");
        task.setBranch(request.getBranch() != null ? request.getBranch() : repo.getBranch());
        task.setStatus("COMPLETED");
        task.setIssueCount(0);
        reviewTaskMapper.insert(task);

        // 3. 生成评审问题：优先基于真实扫描代码，否则 fallback
        List<ReviewIssue> issues = codeReviewAnalysisService.analyze(request.getRepoId());
        if (issues.isEmpty()) {
            log.info("repoId={} 没有已扫描的代码文件，使用 fallback Mock 数据", request.getRepoId());
            issues = generateFallbackIssues(task.getId());
        } else {
            log.info("repoId={} 基于已扫描代码分析，共发现 {} 个问题", request.getRepoId(), issues.size());
            for (ReviewIssue issue : issues) {
                issue.setTaskId(task.getId());
            }
        }

        for (ReviewIssue issue : issues) {
            reviewIssueMapper.insert(issue);
        }

        // 更新问题数量
        task.setIssueCount(issues.size());
        reviewTaskMapper.updateById(task);

        // 4. 生成 review_report（含 Markdown）
        ReviewReport report = generateReport(task.getId(), repo, request.getRepoId(), issues);
        reviewReportMapper.insert(report);

        // 5. 返回响应
        return ReviewTaskResponse.builder()
                .taskId(task.getId())
                .repoId(task.getRepoId())
                .status(task.getStatus())
                .issueCount(task.getIssueCount())
                .summary(report.getSummary())
                .createdAt(task.getCreatedAt())
                .build();
    }

    public ReviewTaskDetailResponse getTaskDetail(Long taskId) {
        ReviewTask task = reviewTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "评审任务不存在: " + taskId);
        }
        return ReviewTaskDetailResponse.builder()
                .id(task.getId())
                .repoId(task.getRepoId())
                .commitId(task.getCommitId())
                .branch(task.getBranch())
                .status(task.getStatus())
                .issueCount(task.getIssueCount())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    public List<ReviewIssueResponse> getTaskIssues(Long taskId) {
        ReviewTask task = reviewTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "评审任务不存在: " + taskId);
        }
        return reviewIssueMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewIssue>()
                                .eq(ReviewIssue::getTaskId, taskId))
                .stream()
                .map(issue -> ReviewIssueResponse.builder()
                        .id(issue.getId())
                        .taskId(issue.getTaskId())
                        .filePath(issue.getFilePath())
                        .lineNumber(issue.getLineNumber())
                        .severity(issue.getSeverity())
                        .category(issue.getCategory())
                        .title(issue.getTitle())
                        .description(issue.getDescription())
                        .suggestion(issue.getSuggestion())
                        .createdAt(issue.getCreatedAt())
                        .build())
                .toList();
    }

    public ReviewReportResponse getTaskReport(Long taskId) {
        ReviewTask task = reviewTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "评审任务不存在: " + taskId);
        }
        ReviewReport report = reviewReportMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewReport>()
                        .eq(ReviewReport::getTaskId, taskId));
        if (report == null) {
            throw new BusinessException(404, "评审报告不存在: " + taskId);
        }
        return ReviewReportResponse.builder()
                .id(report.getId())
                .taskId(report.getTaskId())
                .summary(report.getSummary())
                .overallAssessment(report.getOverallAssessment())
                .markdownContent(report.getMarkdownContent())
                .createdAt(report.getCreatedAt())
                .build();
    }

    // ==================== fallback Mock ====================

    private List<ReviewIssue> generateFallbackIssues(Long taskId) {
        ReviewIssue issue1 = new ReviewIssue();
        issue1.setTaskId(taskId);
        issue1.setFilePath("src/main/java/com/example/UserService.java");
        issue1.setLineNumber(45);
        issue1.setSeverity("SUGGESTION");
        issue1.setCategory("NULL_CHECK");
        issue1.setTitle("建议对返回值进行空值检查");
        issue1.setDescription("user.getName() 方法可能返回 null，调用方未做空值判断，可能导致 NullPointerException。");
        issue1.setSuggestion("建议使用 Optional.ofNullable(user.getName()).orElse(\"未知用户\") 进行空值处理。");

        ReviewIssue issue2 = new ReviewIssue();
        issue2.setTaskId(taskId);
        issue2.setFilePath("src/main/java/com/example/OrderController.java");
        issue2.setLineNumber(78);
        issue2.setSeverity("WARNING");
        issue2.setCategory("EXCEPTION_HANDLING");
        issue2.setTitle("建议使用全局异常处理器统一处理业务异常");
        issue2.setDescription("Controller 中直接使用 try-catch 捕获异常并返回错误信息，建议统一由全局异常处理器处理。");
        issue2.setSuggestion("移除 Controller 中的 try-catch，将异常抛出由 GlobalExceptionHandler 统一处理。");

        ReviewIssue issue3 = new ReviewIssue();
        issue3.setTaskId(taskId);
        issue3.setFilePath("src/main/java/com/example/OrderRepository.java");
        issue3.setLineNumber(23);
        issue3.setSeverity("SUGGESTION");
        issue3.setCategory("PERFORMANCE");
        issue3.setTitle("建议为高频查询字段添加数据库索引");
        issue3.setDescription("findByUserIdAndStatus 方法对应的查询条件未建立复合索引，数据量大时可能影响查询性能。");
        issue3.setSuggestion("建议在 user_id 和 status 字段上建立复合索引：CREATE INDEX idx_user_status ON orders(user_id, status)。");

        return List.of(issue1, issue2, issue3);
    }

    // ==================== 报告生成 ====================

    private ReviewReport generateReport(Long taskId, RepoInfo repo, Long repoId, List<ReviewIssue> issues) {
        long warningCount = issues.stream().filter(i -> "WARNING".equals(i.getSeverity())).count();
        long suggestionCount = issues.stream().filter(i -> "SUGGESTION".equals(i.getSeverity())).count();

        String summary = String.format("本次代码评审共发现 %d 个问题，其中警告 %d 个、建议 %d 个。",
                issues.size(), warningCount, suggestionCount);
        String assessment = "代码整体质量良好，结构清晰，符合团队编码规范。建议根据评审意见进行优化后合并。";

        String markdown = buildMarkdownReport(repo, repoId, issues, warningCount, suggestionCount);

        ReviewReport report = new ReviewReport();
        report.setTaskId(taskId);
        report.setSummary(summary);
        report.setOverallAssessment(assessment);
        report.setMarkdownContent(markdown);
        return report;
    }

    private String buildMarkdownReport(RepoInfo repo, Long repoId, List<ReviewIssue> issues,
                                        long warningCount, long suggestionCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("# AI Code Review Report\n\n");

        sb.append("## 1. Repository Summary\n");
        sb.append("- **Repo ID**: ").append(repoId).append("\n");
        sb.append("- **Name**: ").append(repo.getName()).append("\n");
        sb.append("- **URL**: ").append(repo.getUrl()).append("\n");
        sb.append("- **Branch**: ").append(repo.getBranch()).append("\n");
        sb.append("- **Review Scope**: FULL_REPO\n");
        sb.append("- **Issue Count**: ").append(issues.size()).append("\n");
        sb.append("- **Warnings**: ").append(warningCount).append("\n");
        sb.append("- **Suggestions**: ").append(suggestionCount).append("\n\n");

        sb.append("## 2. Overall Assessment\n");
        sb.append("代码整体质量良好，结构清晰。基于规则检测发现 ").append(issues.size())
                .append(" 个问题，建议在合并前处理 WARNING 级别问题，SUGGESTION 级别可根据实际情况采纳。\n\n");

        sb.append("## 3. Issues\n\n");
        sb.append("| # | 文件 | 风险等级 | 问题类型 | 问题描述 | 修改建议 |\n");
        sb.append("|---|------|----------|----------|----------|----------|\n");
        for (int i = 0; i < issues.size(); i++) {
            ReviewIssue issue = issues.get(i);
            String severity = "WARNING".equals(issue.getSeverity()) ? "⚠️ 警告" : "💡 建议";
            sb.append("| ").append(i + 1)
                    .append(" | ").append(issue.getFilePath())
                    .append(" | ").append(severity)
                    .append(" | ").append(issue.getCategory())
                    .append(" | ").append(issue.getDescription())
                    .append(" | ").append(issue.getSuggestion())
                    .append(" |\n");
        }

        sb.append("\n## 4. Recommendations\n");
        sb.append("1. 完善单元测试覆盖率，确保核心业务逻辑有充分的测试保护\n");
        sb.append("2. 统一异常处理机制，所有业务异常通过 GlobalExceptionHandler 统一返回\n");
        sb.append("3. 加强日志记录，关键操作和异常情况应记录详细日志\n");
        sb.append("4. 将敏感配置信息迁移到环境变量，避免硬编码\n");
        sb.append("5. 定期重构代码，保持代码简洁和可维护性\n");

        return sb.toString();
    }
}