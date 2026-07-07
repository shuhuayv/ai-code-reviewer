package com.shuhuayv.codereviewer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuhuayv.codereviewer.dto.AiReviewAnalysisResult;
import com.shuhuayv.codereviewer.entity.CodeFile;
import com.shuhuayv.codereviewer.entity.ReviewIssue;
import com.shuhuayv.codereviewer.mapper.CodeFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码评审分析服务。
 * 支持两种模式：
 * - Mock 模式：使用内置规则检测常见问题（analyze 方法）
 * - AI 模式：调用 CodeReviewModelService 进行 AI 评审（analyzeWithAi 方法）
 */
@Slf4j
@Service
public class CodeReviewAnalysisService {

    private final CodeFileMapper codeFileMapper;
    private final CodeReviewModelService codeReviewModelService;
    private final boolean mockEnabled;

    private static final String AI_PROVIDER;
    private static final String AI_MODEL;

    static {
        AI_PROVIDER = System.getenv().getOrDefault("AI_PROVIDER", "mock");
        AI_MODEL = System.getenv().getOrDefault("AI_MODEL", "glm-4.7-flash");
    }

    public CodeReviewAnalysisService(CodeFileMapper codeFileMapper,
                                      CodeReviewModelService codeReviewModelService,
                                      @Value("${ai.mock-enabled:true}") boolean mockEnabled) {
        this.codeFileMapper = codeFileMapper;
        this.codeReviewModelService = codeReviewModelService;
        this.mockEnabled = mockEnabled;
    }

    private static final int MAX_ANALYZE_FILES = 20; // Mock 模式用
    private static final int MAX_AI_FILES = 3;       // AI 模式最多 3 个文件
    private static final int MAX_FILE_CONTENT_CHARS = 800; // 每个文件最多截取字符数
    private static final int MAX_PROMPT_TOTAL_CHARS = 5000; // prompt 总长度上限

    private static final Set<String> VALID_SEVERITIES = Set.of("ERROR", "WARNING", "SUGGESTION");
    private static final int MAX_FILE_PATH_LENGTH = 500;
    private static final int MAX_CATEGORY_LENGTH = 50;
    private static final int MAX_TITLE_LENGTH = 500;
    private static final String DEFAULT_FILE_PATH = "AI_RAW_REVIEW.md";

    private static final Set<String> SKIP_EXTENSIONS = Set.of("md", "xml", "yaml", "yml");
    private static final Set<String> PRIORITY_PACKAGES = Set.of(
            "controller", "service", "config", "exception", "entity", "dto", "mapper", "common");

    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(password|secret|token|api[_-]?key|apiKey)\\s*[:=]\\s*\"[^\"]+\"|" +
            "(password|secret|token|api[_-]?key|apiKey)\\s*[:=]\\s*'[^']+'",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TODO_PATTERN = Pattern.compile(
            "(TODO|FIXME|HACK|XXX)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRANSACTIONAL_PATTERN = Pattern.compile(
            "@Transactional", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "log\\.(info|warn|error|debug)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_PATTERN = Pattern.compile(
            "@Valid|@Validated", Pattern.CASE_INSENSITIVE);

    /**
     * 根据 repoId 分析已扫描的代码文件，生成评审问题列表。
     */
    public List<ReviewIssue> analyze(Long repoId) {
        // 1. 查询该仓库下所有代码文件
        List<CodeFile> allFiles = codeFileMapper.selectList(
                new LambdaQueryWrapper<CodeFile>()
                        .eq(CodeFile::getRepoId, repoId));

        if (allFiles.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 筛选要分析的文件
        List<CodeFile> toAnalyze = selectFilesToAnalyze(allFiles);
        log.info("repoId={} 扫描文件总数={}, 选择分析文件数={}", repoId, allFiles.size(), toAnalyze.size());

        // 3. 逐文件分析
        List<ReviewIssue> issues = new ArrayList<>();
        for (CodeFile file : toAnalyze) {
            analyzeFile(file, issues);
        }

        log.info("repoId={} 分析完成，共发现 {} 个问题", repoId, issues.size());
        return issues;
    }

    /**
     * 使用 AI 模型评审代码文件。
     * 构造评审 prompt，调用 CodeReviewModelService，解析响应为 ReviewIssue 列表。
     */
    public AiReviewAnalysisResult analyzeWithAi(Long repoId) {
        List<CodeFile> allFiles = codeFileMapper.selectList(
                new LambdaQueryWrapper<CodeFile>()
                        .eq(CodeFile::getRepoId, repoId));

        if (allFiles.isEmpty()) {
            log.info("repoId={} 没有已扫描的代码文件", repoId);
            return AiReviewAnalysisResult.builder()
                    .rawReviewText("")
                    .issues(Collections.emptyList())
                    .structuredParsed(false)
                    .provider(AI_PROVIDER)
                    .model(AI_MODEL)
                    .build();
        }

        List<CodeFile> toAnalyze = selectFilesForAiPrompt(allFiles);
        log.info("repoId={} AI 评审模式，provider={}, model={}, 文件数={}",
                repoId, AI_PROVIDER, AI_MODEL, toAnalyze.size());

        Set<String> validFilePaths = allFiles.stream()
                .map(CodeFile::getFilePath)
                .collect(java.util.stream.Collectors.toSet());

        String prompt = buildAiReviewPrompt(toAnalyze);
        log.info("AI prompt 长度={}", prompt.length());
        String aiResponse = codeReviewModelService.reviewCode(prompt);
        log.info("真实 AI 原始评审返回长度={}", aiResponse != null ? aiResponse.length() : 0);
        List<ReviewIssue> issues = parseReviewResponse(aiResponse, validFilePaths);
        boolean structuredParsed = issues != null && !issues.isEmpty();

        log.info("repoId={} AI 评审完成，原始响应长度={}, 解析出 {} 个结构化问题",
                repoId, aiResponse != null ? aiResponse.length() : 0, issues.size());
        return AiReviewAnalysisResult.builder()
                .rawReviewText(aiResponse)
                .issues(issues)
                .structuredParsed(structuredParsed)
                .provider(AI_PROVIDER)
                .model(AI_MODEL)
                .build();
    }

    /**
     * 判断当前是否为 Mock 模式。
     */
    public boolean isMockEnabled() {
        return mockEnabled;
    }

    /**
     * 构造 AI 代码评审 prompt（精简版，控制总长度）。
     */
    private String buildAiReviewPrompt(List<CodeFile> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("请不要输出推理过程，不要长篇背景分析，只输出最终评审结果。\n");
        sb.append("总输出不超过 800 个中文字符，最多输出 3 条问题。\n");
        sb.append("如果没有明显问题，也必须输出：未发现明显高风险问题，代码整体可维护性良好。\n");
        sb.append("禁止输出空内容。\n\n");
        sb.append("请对以下 Java 代码文件进行代码评审，只输出最重要的问题。\n");
        sb.append("注意：以下代码片段可能因长度限制被截断，请不要把片段末尾不完整（如 import 不完整、类体不完整、方法体不完整）当作语法错误。");
        sb.append("只评审已经完整展示出来的代码逻辑，不要因为提示词中的截断行为生成 SYNTAX 问题。\n\n");

        int totalChars = sb.length();
        int included = 0;
        for (int i = 0; i < files.size(); i++) {
            CodeFile file = files.get(i);
            String content = file.getContent() != null ? file.getContent() : "";
            if (content.length() > MAX_FILE_CONTENT_CHARS) {
                content = content.substring(0, MAX_FILE_CONTENT_CHARS) + "\n// ... 内容已截断";
            }
            String fileBlock = "### 文件 " + (i + 1) + ": " + file.getFilePath() + "\n```java\n" + content + "\n```\n\n";
            if (totalChars + fileBlock.length() > MAX_PROMPT_TOTAL_CHARS) {
                break;
            }
            sb.append(fileBlock);
            totalChars += fileBlock.length();
            included++;
        }

        sb.append("请按以下格式输出每个问题（每个问题之间用 --- 分隔）：\n\n");
        sb.append("**文件**: 文件路径\n");
        sb.append("**严重级别**: ERROR / WARNING / SUGGESTION\n");
        sb.append("**问题类型**: 如 SECURITY, PERFORMANCE, MAINTAINABILITY 等\n");
        sb.append("**问题描述**: 具体描述\n");
        sb.append("**修改建议**: 具体的修改建议\n\n");
        sb.append("如果未发现明显问题，请返回一段简短总结，不要返回空内容。\n");

        log.info("AI prompt 构建完成，包含 {} 个文件，总长度={}", included, sb.length());
        return sb.toString();
    }

    /**
     * 构造代码评审 prompt（Mock 模式用，保留原逻辑）。
     */
    private String buildReviewPrompt(List<CodeFile> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("请对以下代码文件进行评审，从安全性、健壮性、可维护性、性能和工程规范角度分析。\n\n");

        int maxContentLen = 2000;
        for (int i = 0; i < files.size(); i++) {
            CodeFile file = files.get(i);
            String content = file.getContent() != null ? file.getContent() : "";
            if (content.length() > maxContentLen) {
                content = content.substring(0, maxContentLen) + "\n... (内容已截断)";
            }
            sb.append("### 文件 ").append(i + 1).append("\n");
            sb.append("路径: ").append(file.getFilePath()).append("\n");
            sb.append("语言: ").append(file.getLanguage()).append("\n");
            sb.append("代码:\n```\n").append(content).append("\n```\n\n");
        }

        sb.append("请按以下格式输出每个问题（每个问题之间用 --- 分隔）：\n\n");
        sb.append("**文件**: 文件路径\n");
        sb.append("**严重级别**: WARNING 或 SUGGESTION\n");
        sb.append("**问题类型**: 如 SECURITY, PERFORMANCE, MAINTAINABILITY, EXCEPTION_HANDLING, NULL_CHECK 等\n");
        sb.append("**问题描述**: 具体描述问题\n");
        sb.append("**修改建议**: 具体的修改建议\n");

        return sb.toString();
    }

    /**
     * 解析模型评审响应文本为 ReviewIssue 列表，并对每个 issue 做字段清洗。
     */
    private List<ReviewIssue> parseReviewResponse(String response, Set<String> validFilePaths) {
        List<ReviewIssue> issues = new ArrayList<>();
        if (response == null || response.isBlank()) {
            return issues;
        }

        // 按 --- 分隔每个问题
        String[] blocks = response.split("\\n---\\n|\\n---\\r?\\n");
        for (String block : blocks) {
            ReviewIssue issue = parseIssueBlock(block.trim());
            if (issue != null) {
                issues.add(sanitizeAiIssue(issue, validFilePaths));
            }
        }

        // 如果分隔失败，尝试用正则匹配
        if (issues.isEmpty()) {
            Pattern issuePattern = Pattern.compile(
                    "\\*\\*文件\\*\\*\\s*[:：]\\s*(.+?)\\n" +
                    "\\*\\*严重级别\\*\\*\\s*[:：]\\s*(WARNING|SUGGESTION|ERROR)\\n" +
                    "\\*\\*问题类型\\*\\*\\s*[:：]\\s*(.+?)\\n" +
                    "\\*\\*问题描述\\*\\*\\s*[:：]\\s*(.+?)\\n" +
                    "\\*\\*修改建议\\*\\*\\s*[:：]\\s*(.+?)(?=\\n\\n|\\n---|\\n\\*\\*文件|$)",
                    Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher m = issuePattern.matcher(response);
            while (m.find()) {
                ReviewIssue issue = new ReviewIssue();
                issue.setFilePath(m.group(1).trim());
                issue.setLineNumber(0);
                issue.setSeverity(m.group(2).trim().toUpperCase());
                issue.setCategory(m.group(3).trim());
                String desc = m.group(4).trim();
                issue.setTitle(desc.length() > 80 ? desc.substring(0, 80) + "..." : desc);
                issue.setDescription(desc);
                issue.setSuggestion(m.group(5).trim());
                issues.add(sanitizeAiIssue(issue, validFilePaths));
            }
        }

        return issues;
    }

    /**
     * 解析单个问题块。
     */
    private ReviewIssue parseIssueBlock(String block) {
        if (block.isBlank()) return null;

        String filePath = extractField(block, "文件");
        String severity = extractField(block, "严重级别");
        String category = extractField(block, "问题类型");
        String description = extractField(block, "问题描述");
        String suggestion = extractField(block, "修改建议");

        if (filePath == null || severity == null) return null;

        ReviewIssue issue = new ReviewIssue();
        issue.setFilePath(filePath);
        issue.setLineNumber(0);
        issue.setSeverity(severity.toUpperCase());
        issue.setCategory(category != null ? category : "UNKNOWN");
        issue.setTitle(description != null && description.length() > 80
                ? description.substring(0, 80) + "..." : description);
        issue.setDescription(description != null ? description : "");
        issue.setSuggestion(suggestion != null ? suggestion : "");
        return issue;
    }

    /**
     * 从文本块中提取指定字段的值。
     */
    private String extractField(String block, String fieldName) {
        Pattern p = Pattern.compile(
                "\\*\\*" + Pattern.quote(fieldName) + "\\*\\*\\s*[:：]\\s*(.+?)(\\n\\*\\*|$)",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(block);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    /**
     * 对 AI 解析出的 ReviewIssue 做字段清洗和长度保护，防止数据库插入失败。
     * filePath 为空/过长/非路径文本时回退到默认值；
     * severity 只允许 ERROR/WARNING/SUGGESTION；
     * category/title 超长截断；lineNumber 为空时用 0。
     */
    private ReviewIssue sanitizeAiIssue(ReviewIssue issue, Set<String> validFilePaths) {
        // filePath 安全处理
        String fp = issue.getFilePath();
        if (fp == null || fp.isBlank() || fp.contains("\n") || fp.contains("\r")) {
            issue.setFilePath(DEFAULT_FILE_PATH);
        } else {
            fp = fp.trim();
            if (fp.length() > MAX_FILE_PATH_LENGTH) {
                fp = fp.substring(0, MAX_FILE_PATH_LENGTH);
            }
            if (validFilePaths != null && !validFilePaths.isEmpty() && !validFilePaths.contains(fp)) {
                issue.setFilePath(DEFAULT_FILE_PATH);
            } else {
                issue.setFilePath(fp);
            }
        }

        // severity 只允许 ERROR/WARNING/SUGGESTION
        String sev = issue.getSeverity();
        if (sev == null || !VALID_SEVERITIES.contains(sev.toUpperCase())) {
            issue.setSeverity("SUGGESTION");
        } else {
            issue.setSeverity(sev.toUpperCase());
        }

        // category 截断
        String cat = issue.getCategory();
        if (cat != null && cat.length() > MAX_CATEGORY_LENGTH) {
            issue.setCategory(cat.substring(0, MAX_CATEGORY_LENGTH));
        }

        // title 截断
        String t = issue.getTitle();
        if (t != null && t.length() > MAX_TITLE_LENGTH) {
            issue.setTitle(t.substring(0, MAX_TITLE_LENGTH));
        }

        // lineNumber 为空时用 0
        if (issue.getLineNumber() == null) {
            issue.setLineNumber(0);
        }

        return issue;
    }

    /**
     * 筛选要分析的文件：优先 Java 核心包，跳过非代码文件，限制数量。
     */
    private List<CodeFile> selectFilesToAnalyze(List<CodeFile> allFiles) {
        // 1. 过滤：只保留代码文件，排除 md/xml/yaml/yml
        List<CodeFile> codeFiles = allFiles.stream()
                .filter(f -> isCodeFile(f.getFilePath()))
                .toList();

        // 2. 优先选择 Java 核心包文件
        List<CodeFile> priority = codeFiles.stream()
                .filter(f -> "Java".equalsIgnoreCase(f.getLanguage()) && isPriorityFile(f.getFilePath()))
                .toList();

        List<CodeFile> otherJava = codeFiles.stream()
                .filter(f -> "Java".equalsIgnoreCase(f.getLanguage()) && !isPriorityFile(f.getFilePath()))
                .toList();

        List<CodeFile> otherCode = codeFiles.stream()
                .filter(f -> !"Java".equalsIgnoreCase(f.getLanguage()))
                .toList();

        // 3. 合并：优先级文件 + 其他 Java + 其他代码，限制总数
        List<CodeFile> result = new ArrayList<>();
        result.addAll(priority);
        result.addAll(otherJava);
        result.addAll(otherCode);

        if (result.size() > MAX_ANALYZE_FILES) {
            return result.subList(0, MAX_ANALYZE_FILES);
        }
        return result;
    }

    /**
     * 筛选 AI 评审要分析的文件（最多 8 个，优先 src/main 下的 Java 核心文件）。
     */
    private List<CodeFile> selectFilesForAiPrompt(List<CodeFile> allFiles) {
        // 1. 过滤：只保留 Java 代码文件
        List<CodeFile> javaFiles = allFiles.stream()
                .filter(f -> "Java".equalsIgnoreCase(f.getLanguage()))
                .toList();

        // 2. 优先 src/main 下的核心包文件
        List<CodeFile> srcMain = javaFiles.stream()
                .filter(f -> f.getFilePath().contains("src/main/") && isPriorityFile(f.getFilePath()))
                .toList();

        List<CodeFile> srcMainOther = javaFiles.stream()
                .filter(f -> f.getFilePath().contains("src/main/") && !isPriorityFile(f.getFilePath()))
                .toList();

        List<CodeFile> rest = javaFiles.stream()
                .filter(f -> !f.getFilePath().contains("src/main/"))
                .toList();

        // 3. 合并：核心包 + 其他 src/main + 其余，最多 8 个
        List<CodeFile> result = new ArrayList<>();
        result.addAll(srcMain);
        result.addAll(srcMainOther);
        result.addAll(rest);

        if (result.size() > MAX_AI_FILES) {
            return result.subList(0, MAX_AI_FILES);
        }
        return result;
    }

    /**
     * 判断文件是否为 Java 核心包中的文件。
     */
    private boolean isPriorityFile(String filePath) {
        String lower = filePath.toLowerCase();
        for (String pkg : PRIORITY_PACKAGES) {
            if (lower.contains("/" + pkg + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对单个文件执行所有规则检测。
     */
    private void analyzeFile(CodeFile file, List<ReviewIssue> issues) {
        String content = file.getContent();
        if (content == null || content.isBlank()) {
            return;
        }
        String filePath = file.getFilePath();
        String language = file.getLanguage();

        // 只对 Java 文件做深度分析，其他语言只做基础检测
        if (!"Java".equals(language) && !"java".equals(language)) {
            checkTodoFixme(filePath, content, issues);
            checkSecret(filePath, content, issues);
            return;
        }

        // Java 文件：完整规则检测
        checkTodoFixme(filePath, content, issues);
        checkSecret(filePath, content, issues);
        checkParamValidation(filePath, content, issues);
        checkTransactional(filePath, content, issues);
        checkExceptionHandling(filePath, content, issues);
        checkLogging(filePath, content, issues);
        checkComplexity(file, issues);
    }

    // ==================== 规则检测方法 ====================

    private void checkParamValidation(String filePath, String content, List<ReviewIssue> issues) {
        if (!filePath.contains("/controller/")) return;

        boolean hasRequestMapping = content.contains("@PostMapping") || content.contains("@GetMapping")
                || content.contains("@PutMapping") || content.contains("@DeleteMapping")
                || content.contains("@RequestMapping");

        if (!hasRequestMapping) return;

        boolean hasValid = VALID_PATTERN.matcher(content).find();
        if (!hasValid) {
            issues.add(buildIssue(filePath, 0, "WARNING", "PARAM_VALIDATION",
                    "Controller 入参校验建议",
                    "Controller 方法参数未使用 @Valid 或 @Validated 注解，可能导致非法数据进入业务逻辑。",
                    "建议在请求参数上添加 @Valid 注解，并在 DTO 中配置 JSR-303 校验规则（如 @NotBlank、@NotNull 等）。"));
        }
    }

    private void checkTransactional(String filePath, String content, List<ReviewIssue> issues) {
        if (!filePath.contains("/service/")) return;

        boolean hasTransactional = TRANSACTIONAL_PATTERN.matcher(content).find();

        int insertCount = countOccurrences(content, "Mapper.insert");
        int updateCount = countOccurrences(content, "Mapper.update");
        int deleteCount = countOccurrences(content, "Mapper.delete");
        int multiDbOps = insertCount + updateCount + deleteCount;

        if (multiDbOps >= 2 && !hasTransactional) {
            issues.add(buildIssue(filePath, 0, "WARNING", "TRANSACTION",
                    "Service 方法缺少事务控制",
                    "当前 Service 包含 " + multiDbOps + " 处数据库写操作，但类或方法上未添加 @Transactional 注解，可能导致数据不一致。",
                    "建议在涉及多表写操作的方法上添加 @Transactional 注解，确保数据操作的原子性。"));
        }
    }

    private void checkExceptionHandling(String filePath, String content, List<ReviewIssue> issues) {
        boolean hasRuntimeException = content.contains("throw new RuntimeException")
                || content.contains("throw new Exception");

        if (hasRuntimeException) {
            issues.add(buildIssue(filePath, 0, "SUGGESTION", "EXCEPTION_HANDLING",
                    "建议使用统一异常处理机制",
                    "代码中使用了 RuntimeException 或 Exception 直接抛出，建议使用自定义业务异常配合全局异常处理器统一处理。",
                    "建议定义 BusinessException 等自定义异常，在需要抛出异常时使用，由 GlobalExceptionHandler 统一拦截并返回友好提示。"));
        }

        // 检查是否有 catch 块吞噬异常
        if (content.contains("catch") && content.contains("e.printStackTrace()")) {
            issues.add(buildIssue(filePath, 0, "SUGGESTION", "EXCEPTION_HANDLING",
                    "避免使用 printStackTrace 处理异常",
                    "代码中使用 e.printStackTrace() 处理异常，在生产环境中不会记录到日志系统。",
                    "建议使用 log.error() 记录异常信息，或抛出异常由全局异常处理器统一处理。"));
        }
    }

    private void checkLogging(String filePath, String content, List<ReviewIssue> issues) {
        if (!filePath.contains("/service/") && !filePath.contains("/controller/")) return;

        boolean hasLog = LOG_PATTERN.matcher(content).find();
        boolean hasLogAnnotation = content.contains("@Slf4j");

        if (!hasLog && !hasLogAnnotation) {
            issues.add(buildIssue(filePath, 0, "SUGGESTION", "LOGGING",
                    "建议增加日志记录",
                    "当前 Service/Controller 类中未发现日志记录语句，建议添加关键操作日志以便问题排查。",
                    "建议添加 @Slf4j 注解，并在关键方法中使用 log.info/log.warn/log.error 记录操作日志。"));
        }
    }

    private void checkSecret(String filePath, String content, List<ReviewIssue> issues) {
        if (SECRET_PATTERN.matcher(content).find()) {
            issues.add(buildIssue(filePath, 0, "WARNING", "SECURITY",
                    "配置文件中可能存在硬编码的敏感信息",
                    "检测到配置或代码中可能包含明文密码、密钥或 Token，存在安全风险。",
                    "建议将敏感配置替换为环境变量占位符（如 ${DB_PASSWORD}），并将实际值存放在 .env 文件或环境变量中。"));
        }
    }

    private void checkTodoFixme(String filePath, String content, List<ReviewIssue> issues) {
        if (TODO_PATTERN.matcher(content).find()) {
            issues.add(buildIssue(filePath, 0, "SUGGESTION", "TODO",
                    "代码中存在待办标记",
                    "检测到 TODO/FIXME/HACK/XXX 标记，表示此处代码可能需要后续处理或完善。",
                    "建议在合并前处理所有 TODO/FIXME 标记，或创建对应的 Issue 进行跟踪。"));
        }
    }

    private void checkComplexity(CodeFile file, List<ReviewIssue> issues) {
        if (file.getLineCount() != null && file.getLineCount() > 150) {
            issues.add(buildIssue(file.getFilePath(), 0, "SUGGESTION", "MAINTAINABILITY",
                    "文件代码行数较多，建议拆分",
                    "文件 " + file.getFilePath() + " 共 " + file.getLineCount() + " 行，超过 150 行阈值，建议按职责拆分为多个文件以提高可维护性。",
                    "建议将较大文件拆分为多个职责单一的类，遵循单一职责原则。"));
        }
    }

    // ==================== 工具方法 ====================

    private ReviewIssue buildIssue(String filePath, int lineNumber, String severity,
                                   String category, String title, String description, String suggestion) {
        ReviewIssue issue = new ReviewIssue();
        issue.setFilePath(filePath);
        issue.setLineNumber(lineNumber);
        issue.setSeverity(severity);
        issue.setCategory(category);
        issue.setTitle(title);
        issue.setDescription(description);
        issue.setSuggestion(suggestion);
        return issue;
    }

    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) {
            count++;
            idx += keyword.length();
        }
        return count;
    }

    private static String findExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1).toLowerCase() : null;
    }

    private static boolean isCodeFile(String filePath) {
        String[] parts = filePath.split("/");
        if (parts.length == 0) return false;
        String ext = findExtension(parts[parts.length - 1]);
        return ext != null && !SKIP_EXTENSIONS.contains(ext);
    }
}