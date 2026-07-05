package com.shuhuayv.codereviewer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuhuayv.codereviewer.common.PageResult;
import com.shuhuayv.codereviewer.dto.CodeFileResponse;
import com.shuhuayv.codereviewer.dto.CreateRepoRequest;
import com.shuhuayv.codereviewer.dto.RepoResponse;
import com.shuhuayv.codereviewer.entity.CodeFile;
import com.shuhuayv.codereviewer.entity.RepoInfo;
import com.shuhuayv.codereviewer.exception.BusinessException;
import com.shuhuayv.codereviewer.mapper.CodeFileMapper;
import com.shuhuayv.codereviewer.mapper.RepoInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RepoService {

    private final RepoInfoMapper repoInfoMapper;
    private final CodeFileMapper codeFileMapper;

    @Transactional
    public RepoResponse create(CreateRepoRequest request) {
        RepoInfo repo = new RepoInfo();
        repo.setName(request.getName());
        repo.setUrl(request.getUrl());
        repo.setBranch(request.getBranch());
        repo.setDescription(request.getDescription());
        repo.setLanguage(request.getLanguage());
        repo.setStatus("ACTIVE");
        repoInfoMapper.insert(repo);
        return toResponse(repo);
    }

    public List<RepoResponse> listAll() {
        return repoInfoMapper.selectList(null).stream()
                .map(this::toResponse)
                .toList();
    }

    public PageResult<RepoResponse> page(int pageNum, int pageSize) {
        Page<RepoInfo> page = new Page<>(pageNum, pageSize);
        Page<RepoInfo> result = repoInfoMapper.selectPage(page,
                new LambdaQueryWrapper<RepoInfo>().orderByDesc(RepoInfo::getCreatedAt));
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(),
                result.getRecords().stream().map(this::toResponse).toList());
    }

    public RepoResponse getById(Long id) {
        RepoInfo repo = repoInfoMapper.selectById(id);
        if (repo == null) {
            throw new BusinessException("仓库不存在: " + id);
        }
        return toResponse(repo);
    }

    @Transactional
    public void delete(Long id) {
        RepoInfo repo = repoInfoMapper.selectById(id);
        if (repo == null) {
            throw new BusinessException("仓库不存在: " + id);
        }
        repoInfoMapper.deleteById(id);
    }

    public List<CodeFileResponse> listCodeFiles(Long repoId) {
        return codeFileMapper.selectList(
                new LambdaQueryWrapper<CodeFile>()
                        .eq(CodeFile::getRepoId, repoId)
                        .orderByAsc(CodeFile::getFilePath))
                .stream()
                .map(this::toCodeFileResponse)
                .toList();
    }

    public PageResult<CodeFileResponse> pageCodeFiles(Long repoId, int pageNum, int pageSize) {
        LambdaQueryWrapper<CodeFile> wrapper = new LambdaQueryWrapper<CodeFile>()
                .eq(CodeFile::getRepoId, repoId)
                .orderByAsc(CodeFile::getFilePath);

        long total = codeFileMapper.selectCount(wrapper);

        int offset = (pageNum - 1) * pageSize;
        wrapper.last("LIMIT " + pageSize + " OFFSET " + offset);

        List<CodeFileResponse> records = codeFileMapper.selectList(wrapper).stream()
                .map(this::toCodeFileResponse)
                .toList();

        return PageResult.of(total, pageNum, pageSize, records);
    }

    private RepoResponse toResponse(RepoInfo repo) {
        return RepoResponse.builder()
                .id(repo.getId())
                .name(repo.getName())
                .url(repo.getUrl())
                .branch(repo.getBranch())
                .description(repo.getDescription())
                .language(repo.getLanguage())
                .status(repo.getStatus())
                .createdAt(repo.getCreatedAt())
                .updatedAt(repo.getUpdatedAt())
                .build();
    }

    private CodeFileResponse toCodeFileResponse(CodeFile cf) {
        return CodeFileResponse.builder()
                .id(cf.getId())
                .repoId(cf.getRepoId())
                .filePath(cf.getFilePath())
                .language(cf.getLanguage())
                .lineCount(cf.getLineCount())
                .contentHash(cf.getContentHash())
                .createdAt(cf.getCreatedAt())
                .build();
    }
}