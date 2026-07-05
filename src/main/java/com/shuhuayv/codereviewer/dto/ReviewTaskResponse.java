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
@Schema(description = "创建评审任务响应")
public class ReviewTaskResponse {

    @Schema(description = "任务ID", example = "1")
    private Long taskId;

    @Schema(description = "仓库ID", example = "1")
    private Long repoId;

    @Schema(description = "任务状态", example = "COMPLETED")
    private String status;

    @Schema(description = "问题数量", example = "3")
    private Integer issueCount;

    @Schema(description = "评审摘要")
    private String summary;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}