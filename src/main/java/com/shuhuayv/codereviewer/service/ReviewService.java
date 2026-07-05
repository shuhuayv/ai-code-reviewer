package com.shuhuayv.codereviewer.service;

import com.shuhuayv.codereviewer.dto.*;
import com.shuhuayv.codereviewer.entity.*;
import com.shuhuayv.codereviewer.exception.BusinessException;
import com.shuhuayv.codereviewer.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final RepoInfoMapper repoInfoMapper;
    private final ReviewTaskMapper reviewTaskMapper;
    private final ReviewIssueMapper reviewIssueMapper;
    private final ReviewReportMapper reviewReportMapper;

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
        task.setCommitId(request.getCommitId());
        task.setBranch(request.getBranch());
        task.setStatus("COMPLETED");
        task.setIssueCount(0);
        reviewTaskMapper.insert(task);

        // 3. 生成 Mock 评审问题
        List<ReviewIssue> issues = generateMockIssues(task.getId());
        for (ReviewIssue issue : issues) {
            reviewIssueMapper.insert(issue);
        }

        // 更新问题数量
        task.setIssueCount(issues.size());
        reviewTaskMapper.updateById(task);

        // 4. 生成 review_report
        ReviewReport report = generateMockReport(task.getId(), issues);
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
                .createdAt(report.getCreatedAt())
                .build();
    }

    private List<ReviewIssue> generateMockIssues(Long taskId) {
        ReviewIssue issue1 = new ReviewIssue();
        issue1.setTaskId(taskId);
        issue1.setFilePath("src/main/java/com/example/UserService.java");
        issue1.setLineNumber(45);
        issue1.setSeverity("SUGGESTION");
        issue1.setCategory("空值检查");
        issue1.setTitle("建议对返回值进行空值检查");
        issue1.setDescription("user.getName() 方法可能返回 null，调用方未做空值判断，可能导致 NullPointerException。");
        issue1.setSuggestion("建议使用 Optional.ofNullable(user.getName()).orElse(\"未知用户\") 进行空值处理。");

        ReviewIssue issue2 = new ReviewIssue();
        issue2.setTaskId(taskId);
        issue2.setFilePath("src/main/java/com/example/OrderController.java");
        issue2.setLineNumber(78);
        issue2.setSeverity("WARNING");
        issue2.setCategory("异常处理");
        issue2.setTitle("建议使用全局异常处理器统一处理业务异常");
        issue2.setDescription("Controller 中直接使用 try-catch 捕获异常并返回错误信息，建议统一由全局异常处理器处理。");
        issue2.setSuggestion("移除 Controller 中的 try-catch，将异常抛出由 GlobalExceptionHandler 统一处理。");

        ReviewIssue issue3 = new ReviewIssue();
        issue3.setTaskId(taskId);
        issue3.setFilePath("src/main/java/com/example/OrderRepository.java");
        issue3.setLineNumber(23);
        issue3.setSeverity("SUGGESTION");
        issue3.setCategory("性能优化");
        issue3.setTitle("建议为高频查询字段添加数据库索引");
        issue3.setDescription("findByUserIdAndStatus 方法对应的查询条件未建立复合索引，数据量大时可能影响查询性能。");
        issue3.setSuggestion("建议在 user_id 和 status 字段上建立复合索引：CREATE INDEX idx_user_status ON orders(user_id, status)。");

        return List.of(issue1, issue2, issue3);
    }

    private ReviewReport generateMockReport(Long taskId, List<ReviewIssue> issues) {
        long suggestionCount = issues.stream().filter(i -> "SUGGESTION".equals(i.getSeverity())).count();
        long warningCount = issues.stream().filter(i -> "WARNING".equals(i.getSeverity())).count();

        ReviewReport report = new ReviewReport();
        report.setTaskId(taskId);
        report.setSummary(String.format("本次代码评审共发现 %d 个问题，其中建议 %d 个、警告 %d 个。代码整体质量良好，建议在合并前处理以上问题。",
                issues.size(), suggestionCount, warningCount));
        report.setOverallAssessment("代码整体质量良好，结构清晰，符合团队编码规范。未发现严重问题，建议根据评审意见进行优化后合并。");
        return report;
    }
}