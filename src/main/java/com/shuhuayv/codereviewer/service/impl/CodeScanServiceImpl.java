package com.shuhuayv.codereviewer.service.impl;

import com.shuhuayv.codereviewer.dto.CodeScanResponse;
import com.shuhuayv.codereviewer.entity.CodeFile;
import com.shuhuayv.codereviewer.entity.RepoInfo;
import com.shuhuayv.codereviewer.exception.BusinessException;
import com.shuhuayv.codereviewer.mapper.CodeFileMapper;
import com.shuhuayv.codereviewer.mapper.RepoInfoMapper;
import com.shuhuayv.codereviewer.service.CodeScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeScanServiceImpl implements CodeScanService {

    private final RepoInfoMapper repoInfoMapper;
    private final CodeFileMapper codeFileMapper;

    @Value("${app.repo.max-file-size:200000}")
    private int maxFileSize;

    private static final Set<String> SKIP_DIRS = Set.of(
            ".git", "target", "node_modules", "dist", "build", ".idea", ".vscode"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".java", ".py", ".js", ".ts", ".vue", ".go", ".md", ".yml", ".yaml", ".xml"
    );

    private static final Map<String, String> EXT_LANGUAGE_MAP = new HashMap<>();

    static {
        EXT_LANGUAGE_MAP.put(".java", "Java");
        EXT_LANGUAGE_MAP.put(".py", "Python");
        EXT_LANGUAGE_MAP.put(".js", "JavaScript");
        EXT_LANGUAGE_MAP.put(".ts", "TypeScript");
        EXT_LANGUAGE_MAP.put(".vue", "Vue");
        EXT_LANGUAGE_MAP.put(".go", "Go");
        EXT_LANGUAGE_MAP.put(".md", "Markdown");
        EXT_LANGUAGE_MAP.put(".yml", "YAML");
        EXT_LANGUAGE_MAP.put(".yaml", "YAML");
        EXT_LANGUAGE_MAP.put(".xml", "XML");
    }

    @Override
    @Transactional
    public CodeScanResponse scanRepo(Long repoId) {
        long startTime = System.currentTimeMillis();

        RepoInfo repo = repoInfoMapper.selectById(repoId);
        if (repo == null) {
            throw new BusinessException(404, "仓库不存在: " + repoId);
        }

        if (repo.getLocalPath() == null || repo.getLocalPath().isEmpty()) {
            throw new BusinessException("仓库尚未克隆，请先执行 clone 操作");
        }

        File localDir = new File(repo.getLocalPath());
        if (!localDir.exists() || !localDir.isDirectory()) {
            throw new BusinessException("本地仓库目录不存在: " + repo.getLocalPath());
        }

        // 删除旧记录
        codeFileMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CodeFile>()
                .eq(CodeFile::getRepoId, repoId));

        List<CodeFile> codeFiles = new ArrayList<>();
        int skippedCount = 0;
        int totalLineCount = 0;
        Map<String, Integer> languageCount = new HashMap<>();

        Path basePath = Path.of(repo.getLocalPath());
        try (Stream<Path> fileStream = Files.walk(basePath)) {
            List<Path> files = fileStream
                    .filter(Files::isRegularFile)
                    .filter(this::shouldScan)
                    .toList();

            for (Path filePath : files) {
                long fileSize = Files.size(filePath);
                if (fileSize > maxFileSize) {
                    skippedCount++;
                    log.debug("跳过文件(超过大小限制): {}", filePath);
                    continue;
                }

                String relativePath = basePath.relativize(filePath).toString();
                String fileName = filePath.getFileName().toString();
                String extension = getExtension(fileName);
                String language = EXT_LANGUAGE_MAP.getOrDefault(extension, "Unknown");

                String content = Files.readString(filePath, StandardCharsets.UTF_8);
                int lineCount = content.split("\n").length;
                String contentHash = sha256(content);

                CodeFile codeFile = new CodeFile();
                codeFile.setRepoId(repoId);
                codeFile.setFilePath(relativePath);
                codeFile.setFileName(fileName);
                codeFile.setLanguage(language);
                codeFile.setContent(content);
                codeFile.setCharCount(content.length());
                codeFile.setLineCount(lineCount);
                codeFile.setContentHash(contentHash);
                codeFiles.add(codeFile);

                totalLineCount += lineCount;
                languageCount.merge(language, 1, Integer::sum);
            }
        } catch (IOException e) {
            throw new BusinessException("文件扫描失败: " + e.getMessage());
        }

        // 批量写入
        for (CodeFile cf : codeFiles) {
            codeFileMapper.insert(cf);
        }

        long costMs = System.currentTimeMillis() - startTime;
        log.info("扫描完成: repoId={}, 文件数={}, 跳过={}, 总行数={}, 耗时={}ms",
                repoId, codeFiles.size(), skippedCount, totalLineCount, costMs);

        return CodeScanResponse.builder()
                .repoId(repoId)
                .scannedFileCount(codeFiles.size())
                .skippedFileCount(skippedCount)
                .totalLineCount(totalLineCount)
                .languages(languageCount)
                .costMs(costMs)
                .build();
    }

    private boolean shouldScan(Path filePath) {
        // 检查是否在跳过目录中
        for (Path part : filePath) {
            if (SKIP_DIRS.contains(part.toString())) {
                return false;
            }
        }
        String fileName = filePath.getFileName().toString();
        String extension = getExtension(fileName);
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex).toLowerCase();
        }
        return "";
    }

    private String sha256(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}