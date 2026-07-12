package com.shuhuayv.codereviewer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuhuayv.codereviewer.dto.AiReviewAnalysisResult;
import com.shuhuayv.codereviewer.entity.CodeFile;
import com.shuhuayv.codereviewer.entity.ReviewIssue;
import com.shuhuayv.codereviewer.mapper.CodeFileMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CodeReviewAnalysisService 单元测试（最强工程点：AI JSON/Markdown 解析 + 字段清洗）。
 * 使用 Mockito 模拟 CodeFileMapper 与 CodeReviewModelService，不加载 Spring / 不连数据库。
 */
class CodeReviewAnalysisServiceTest {

    private final CodeFileMapper codeFileMapper = mock(CodeFileMapper.class);
    private final CodeReviewModelService modelService = mock(CodeReviewModelService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private CodeReviewAnalysisService service(boolean mockEnabled) {
        return new CodeReviewAnalysisService(codeFileMapper, modelService, objectMapper, mockEnabled);
    }

    private CodeFile file(long id, String path, String language, String content) {
        CodeFile f = new CodeFile();
        f.setId(id);
        f.setRepoId(1L);
        f.setFilePath(path);
        f.setLanguage(language);
        f.setContent(content);
        return f;
    }

    @Test
    void shouldParseStructuredJsonIssuesAndKeepValidSeverity() {
        CodeFile f = file(1L, "src/main/java/com/demo/Service.java", "Java", "public class Service {}");
        when(codeFileMapper.selectList(any())).thenReturn(List.of(f));
        String aiJson = "{\"summary\":\"ok\",\"issues\":["
                + "{\"filePath\":\"src/main/java/com/demo/Service.java\",\"lineNumber\":10,"
                + "\"severity\":\"warning\",\"category\":\"MAINTAINABILITY\","
                + "\"title\":\"t\",\"description\":\"d\",\"suggestion\":\"s\"}]}";
        when(modelService.reviewCode(any())).thenReturn(aiJson);

        AiReviewAnalysisResult result = service(false).analyzeWithAi(1L);

        assertThat(result.isStructuredParsed()).isTrue();
        assertThat(result.getIssues()).hasSize(1);
        ReviewIssue issue = result.getIssues().get(0);
        assertThat(issue.getFilePath()).isEqualTo("src/main/java/com/demo/Service.java");
        assertThat(issue.getSeverity()).isEqualTo("WARNING");
    }

    @Test
    void shouldFallbackInvalidFilePathToDefault() {
        CodeFile f = file(1L, "src/main/java/com/demo/Service.java", "Java", "x");
        when(codeFileMapper.selectList(any())).thenReturn(List.of(f));
        // filePath 不在合法文件集合中 → 回退到 AI_RAW_REVIEW.md
        String aiJson = "{\"summary\":\"ok\",\"issues\":["
                + "{\"filePath\":\"some/unknown/path.java\",\"severity\":\"ERROR\","
                + "\"title\":\"t\",\"description\":\"d\",\"suggestion\":\"s\"}]}";
        when(modelService.reviewCode(any())).thenReturn(aiJson);

        AiReviewAnalysisResult result = service(false).analyzeWithAi(1L);

        assertThat(result.getIssues()).hasSize(1);
        assertThat(result.getIssues().get(0).getFilePath()).isEqualTo("AI_RAW_REVIEW.md");
        assertThat(result.getIssues().get(0).getSeverity()).isEqualTo("ERROR");
    }

    @Test
    void shouldNormalizeUnknownSeverityToSuggestion() {
        CodeFile f = file(1L, "src/main/java/com/demo/Service.java", "Java", "x");
        when(codeFileMapper.selectList(any())).thenReturn(List.of(f));
        String aiJson = "{\"summary\":\"ok\",\"issues\":["
                + "{\"filePath\":\"src/main/java/com/demo/Service.java\",\"severity\":\"WEIRD\","
                + "\"title\":\"t\",\"description\":\"d\",\"suggestion\":\"s\"}]}";
        when(modelService.reviewCode(any())).thenReturn(aiJson);

        AiReviewAnalysisResult result = service(false).analyzeWithAi(1L);

        assertThat(result.getIssues().get(0).getSeverity()).isEqualTo("SUGGESTION");
    }

    @Test
    void shouldFallbackToMarkdownParsingWhenNoJson() {
        CodeFile f = file(1L, "src/main/java/com/demo/Service.java", "Java", "x");
        when(codeFileMapper.selectList(any())).thenReturn(List.of(f));
        String markdown = "**文件**: src/main/java/com/demo/Service.java\n"
                + "**严重级别**: WARNING\n"
                + "**问题类型**: NULL_CHECK\n"
                + "**问题描述**: 可能为 null\n"
                + "**修改建议**: 加判空";
        when(modelService.reviewCode(any())).thenReturn(markdown);

        AiReviewAnalysisResult result = service(false).analyzeWithAi(1L);

        assertThat(result.getIssues()).hasSize(1);
        assertThat(result.getIssues().get(0).getFilePath()).isEqualTo("src/main/java/com/demo/Service.java");
        assertThat(result.getIssues().get(0).getSeverity()).isEqualTo("WARNING");
    }

    @Test
    void shouldReturnEmptyWhenNoFilesScanned() {
        when(codeFileMapper.selectList(any())).thenReturn(List.of());

        AiReviewAnalysisResult result = service(false).analyzeWithAi(1L);

        assertThat(result.getIssues()).isEmpty();
        assertThat(result.isStructuredParsed()).isFalse();
    }

    @Test
    void shouldDetectRuleIssuesInAnalyzeMode() {
        CodeFile f = file(1L, "src/main/java/com/demo/Controller.java", "Java",
                "import x; @RestController public class Controller {\n"
                        + "  @GetMapping public String a() {\n"
                        + "    // TODO fix this later\n"
                        + "    String config = \"password='admin123'\";\n"
                        + "    return config;\n"
                        + "  }\n}");
        when(codeFileMapper.selectList(any())).thenReturn(List.of(f));

        List<ReviewIssue> issues = service(true).analyze(1L);

        assertThat(issues).isNotEmpty();
        assertThat(issues).anyMatch(i -> "TODO".equals(i.getCategory()));
        assertThat(issues).anyMatch(i -> "SECURITY".equals(i.getCategory()));
    }
}
