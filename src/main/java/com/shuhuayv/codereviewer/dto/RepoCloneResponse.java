package com.shuhuayv.codereviewer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "仓库克隆响应")
public class RepoCloneResponse {

    @Schema(description = "仓库ID", example = "1")
    private Long repoId;

    @Schema(description = "仓库URL", example = "https://github.com/example/repo.git")
    private String repoUrl;

    @Schema(description = "克隆分支", example = "main")
    private String branch;

    @Schema(description = "本地路径", example = "repos/1")
    private String localPath;

    @Schema(description = "克隆状态", example = "CLONED")
    private String status;

    @Schema(description = "状态描述", example = "clone success")
    private String message;

    @Schema(description = "耗时（毫秒）", example = "3500")
    private long costMs;
}