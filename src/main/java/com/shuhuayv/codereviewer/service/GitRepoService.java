package com.shuhuayv.codereviewer.service;

import com.shuhuayv.codereviewer.dto.RepoCloneResponse;

public interface GitRepoService {

    RepoCloneResponse cloneRepo(Long repoId);
}