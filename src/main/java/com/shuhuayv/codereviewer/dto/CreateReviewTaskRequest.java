package com.shuhuayv.codereviewer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建评审任务请求")
public class CreateReviewTaskRequest {

    @NotNull(message = "仓库ID不能为空")
    @Schema(description = "仓库ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long repoId;

    @Schema(description = "提交ID，为空时默认使用 mock-commit", example = "abc123def456")
    private String commitId;

    @Schema(description = "评审分支，为空时默认使用仓库默认分支", example = "feature/new-feature")
    private String branch;

    @Schema(description = "评审范围，默认 FULL_REPO", example = "FULL_REPO")
    private String reviewScope;
}