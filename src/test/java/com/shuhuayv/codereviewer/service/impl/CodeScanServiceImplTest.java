package com.shuhuayv.codereviewer.service.impl;

import com.shuhuayv.codereviewer.dto.CodeScanResponse;
import com.shuhuayv.codereviewer.entity.CodeFile;
import com.shuhuayv.codereviewer.entity.RepoInfo;
import com.shuhuayv.codereviewer.mapper.CodeFileMapper;
import com.shuhuayv.codereviewer.mapper.RepoInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CodeScanServiceImpl 单元测试：在临时目录上验证扫描（仅读磁盘 + Mock Mapper，不连数据库）。
 * 覆盖：扩展名识别、语言映射、跳过 .git 目录、超大文件跳过、contentHash 计算、repoId 绑定。
 */
class CodeScanServiceImplTest {

    private final RepoInfoMapper repoInfoMapper = mock(RepoInfoMapper.class);
    private final CodeFileMapper codeFileMapper = mock(CodeFileMapper.class);

    private CodeScanServiceImpl service() {
        return new CodeScanServiceImpl(repoInfoMapper, codeFileMapper);
    }

    @Test
    void shouldScanJavaAndMarkdownSkipGitAndOversized(@TempDir Path repoDir) throws Exception {
        Files.writeString(repoDir.resolve("Demo.java"), "public class Demo {}\n");
        Files.writeString(repoDir.resolve("readme.md"), "# readme\n");
        Files.createDirectories(repoDir.resolve(".git"));
        Files.writeString(repoDir.resolve(".git/config"), "[core]\n");
        // 构造一个超过默认上限（200000 字符）的大文件
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 300_000; i++) big.append("a");
        Files.writeString(repoDir.resolve("Big.java"), big.toString());

        RepoInfo repo = new RepoInfo();
        repo.setId(1L);
        repo.setLocalPath(repoDir.toString());
        when(repoInfoMapper.selectById(1L)).thenReturn(repo);
        when(codeFileMapper.insert(any(CodeFile.class))).thenReturn(1);

        // 直接实例化未走 Spring，@Value maxFileSize 不会被注入（默认 0 会导致所有文件被判定为超限）。
        // 通过反射设为 1000，使 Demo.java / readme.md 被扫描、Big.java（300000 字符）被跳过。
        CodeScanServiceImpl svc = service();
        Field maxFileSizeField = CodeScanServiceImpl.class.getDeclaredField("maxFileSize");
        maxFileSizeField.setAccessible(true);
        maxFileSizeField.set(svc, 1000);

        CodeScanResponse resp = svc.scanRepo(1L);

        // Demo.java + readme.md 被扫描；Big.java 超大被跳过；.git/config 被跳过目录忽略
        assertThat(resp.getScannedFileCount()).isEqualTo(2);
        assertThat(resp.getSkippedFileCount()).isEqualTo(1);

        ArgumentCaptor<CodeFile> captor = ArgumentCaptor.forClass(CodeFile.class);
        verify(codeFileMapper, times(2)).insert(captor.capture());
        List<CodeFile> inserted = captor.getAllValues();

        assertThat(inserted).anyMatch(f -> "Java".equals(f.getLanguage()) && "Demo.java".equals(f.getFilePath()));
        assertThat(inserted).anyMatch(f -> "Markdown".equals(f.getLanguage()) && "readme.md".equals(f.getFilePath()));
        // 每个入库文件都绑定 repoId 并计算 SHA-256（64 位十六进制）
        assertThat(inserted).allMatch(f ->
                f.getRepoId().equals(1L)
                        && f.getContentHash() != null
                        && f.getContentHash().length() == 64);
    }
}
