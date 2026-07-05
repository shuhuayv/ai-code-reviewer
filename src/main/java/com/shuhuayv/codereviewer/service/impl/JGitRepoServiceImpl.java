package com.shuhuayv.codereviewer.service.impl;

import com.shuhuayv.codereviewer.dto.RepoCloneResponse;
import com.shuhuayv.codereviewer.entity.RepoInfo;
import com.shuhuayv.codereviewer.exception.BusinessException;
import com.shuhuayv.codereviewer.mapper.RepoInfoMapper;
import com.shuhuayv.codereviewer.service.GitRepoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class JGitRepoServiceImpl implements GitRepoService {

    private final RepoInfoMapper repoInfoMapper;

    @Value("${app.repo.base-dir:repos}")
    private String baseDir;

    @Override
    public RepoCloneResponse cloneRepo(Long repoId) {
        long startTime = System.currentTimeMillis();

        RepoInfo repo = repoInfoMapper.selectById(repoId);
        if (repo == null) {
            throw new BusinessException(404, "仓库不存在: " + repoId);
        }

        String localPath = Paths.get(baseDir, repoId.toString()).toString();
        File localDir = new File(localPath);

        // 如果已存在则删除重新克隆
        if (localDir.exists()) {
            deleteDirectory(localDir);
            log.info("已删除旧目录: {}", localPath);
        }

        try {
            log.info("开始克隆仓库: {} -> {} (分支: {})", repo.getUrl(), localPath, repo.getBranch());
            Git.cloneRepository()
                    .setURI(repo.getUrl())
                    .setDirectory(localDir)
                    .setBranch(repo.getBranch())
                    .call();
            log.info("仓库克隆成功: {}", localPath);

            repo.setLocalPath(localPath);
            repo.setStatus("CLONED");
            repo.setRemark("clone success");
            repoInfoMapper.updateById(repo);

            long costMs = System.currentTimeMillis() - startTime;
            return RepoCloneResponse.builder()
                    .repoId(repoId)
                    .repoUrl(repo.getUrl())
                    .branch(repo.getBranch())
                    .localPath(localPath)
                    .status("CLONED")
                    .message("clone success")
                    .costMs(costMs)
                    .build();

        } catch (GitAPIException e) {
            log.error("仓库克隆失败: {}", e.getMessage(), e);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 200) {
                errorMsg = errorMsg.substring(0, 200);
            }

            repo.setStatus("FAILED");
            repo.setRemark(errorMsg);
            repoInfoMapper.updateById(repo);

            long costMs = System.currentTimeMillis() - startTime;
            return RepoCloneResponse.builder()
                    .repoId(repoId)
                    .repoUrl(repo.getUrl())
                    .branch(repo.getBranch())
                    .localPath(null)
                    .status("FAILED")
                    .message(errorMsg)
                    .costMs(costMs)
                    .build();
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
}