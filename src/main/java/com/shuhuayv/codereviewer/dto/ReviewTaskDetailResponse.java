package com.shuhuayv.codereviewer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "评审任务详情")
public class ReviewTaskDetailResponse {

    @Schema(description = "任务ID", example = "1")
    private Long id;

    @Schema(description = "仓库ID", example = "1")
    private Long repoId;

    @Schema(description = "提交ID", example = "abc123def456")
    private String commitId;

    @Schema(description = "评审分支", example = "feature/new-feature")
    private String branch;

    @Schema(description = "任务状态", example = "COMPLETED")
    private String status;

    @Schema(description = "问题数量", example = "3")
    private Integer issueCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}