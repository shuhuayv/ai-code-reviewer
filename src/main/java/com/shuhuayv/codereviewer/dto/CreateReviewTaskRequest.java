package com.shuhuayv.codereviewer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建评审任务请求")
public class CreateReviewTaskRequest {

    @NotNull(message = "仓库ID不能为空")
    @Schema(description = "仓库ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long repoId;

    @NotBlank(message = "Commit ID不能为空")
    @Schema(description = "提交ID", example = "abc123def456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String commitId;

    @NotBlank(message = "分支不能为空")
    @Schema(description = "评审分支", example = "feature/new-feature", requiredMode = Schema.RequiredMode.REQUIRED)
    private String branch;
}