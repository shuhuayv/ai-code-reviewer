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
@Schema(description = "仓库响应")
public class RepoResponse {

    @Schema(description = "仓库ID", example = "1")
    private Long id;

    @Schema(description = "仓库名称", example = "ai-code-reviewer")
    private String name;

    @Schema(description = "仓库URL", example = "https://github.com/example/ai-code-reviewer.git")
    private String url;

    @Schema(description = "默认分支", example = "main")
    private String branch;

    @Schema(description = "仓库描述", example = "AI代码评审平台")
    private String description;

    @Schema(description = "编程语言", example = "Java")
    private String language;

    @Schema(description = "仓库状态", example = "ACTIVE")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}