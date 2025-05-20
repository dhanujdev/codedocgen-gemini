package com.codedocgen.service;

import org.eclipse.jgit.api.errors.GitAPIException;
import java.io.File;
import java.io.IOException;

public interface GitService {
    File cloneRepository(String repoUrl, String localPath) throws GitAPIException, IOException;
    void deleteRepository(File repoDir) throws IOException;
} 