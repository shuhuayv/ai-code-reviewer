package com.shuhuayv.codereviewer.service;

import com.shuhuayv.codereviewer.dto.CodeScanResponse;

public interface CodeScanService {

    CodeScanResponse scanRepo(Long repoId);
}