package com.shuhuayv.codereviewer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuhuayv.codereviewer.entity.CodeFile;
import com.shuhuayv.codereviewer.entity.ReviewIssue;
import com.shuhuayv.codereviewer.mapper.CodeFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 基于已扫描代码文件内容进行规则驱动的代码评审分析。
 * 不依赖真实 AI API，使用内置规则检测常见问题。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeReviewAnalysisService {

    private final CodeFileMapper codeFileMapper;

    private static final int MAX_ANALYZE_FILES = 20;

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