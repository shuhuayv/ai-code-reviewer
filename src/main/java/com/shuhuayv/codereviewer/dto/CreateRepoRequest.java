package com.shuhuayv.codereviewer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建仓库请求")
public class CreateRepoRequest {

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 200)
    @Schema(description = "仓库名称", example = "ai-code-reviewer", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "仓库URL不能为空")
    @Size(max = 500)
    @Schema(description = "仓库URL", example = "https://github.com/example/ai-code-reviewer.git", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;

    @NotBlank(message = "默认分支不能为空")
    @Size(max = 100)
    @Schema(description = "默认分支", example = "main", requiredMode = Schema.RequiredMode.REQUIRED)
    private String branch;

    @Size(max = 1000)
    @Schema(description = "仓库描述", example = "AI代码评审平台")
    private String description;

    @Size(max = 50)
    @Schema(description = "编程语言", example = "Java")
    private String language;
}