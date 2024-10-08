package com.solo83.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class PathService {

    private final static String DIRECTORY_PREFIX = "/";

    public String appendUserRootFolder(String path, String userRootFolder) {
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        if (!userRootFolder.endsWith(DIRECTORY_PREFIX)) {
            userRootFolder = userRootFolder + "/";
        }
        if (decodedPath.startsWith(DIRECTORY_PREFIX)) {
            decodedPath = decodedPath.substring(1);
        }
        return userRootFolder + decodedPath;
    }

    public String updatePathWithNewName(String oldPath, String newName) {
        if (oldPath == null || newName == null) {
            throw new IllegalArgumentException("Arguments must not be null");
        }

        if (oldPath.isEmpty() || newName.isEmpty()) {
            throw new IllegalArgumentException("Arguments must not be empty");
        }

        if (oldPath.endsWith(DIRECTORY_PREFIX)) {
            newName += DIRECTORY_PREFIX;
            oldPath = oldPath.substring(0, oldPath.lastIndexOf(DIRECTORY_PREFIX));
            return oldPath.substring(0, oldPath.lastIndexOf(DIRECTORY_PREFIX) + 1) + newName;
        }
        return oldPath.substring(0, oldPath.lastIndexOf(DIRECTORY_PREFIX) + 1) + newName;
    }

    public String getPathToNewFolder(String oldPath, String newFolderName) {
        if (oldPath == null || newFolderName == null) {
            throw new IllegalArgumentException("Arguments must not be null");
        }

        if (oldPath.isEmpty() || newFolderName.isEmpty()) {
            throw new IllegalArgumentException("Arguments must not be empty");
        }

        if (oldPath.endsWith(DIRECTORY_PREFIX)) {
            newFolderName += DIRECTORY_PREFIX;

        }
        return oldPath + newFolderName;
    }

    public String encodePath(String path) {
        try {
            return URLEncoder.encode(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error encoding path", e);
            return path;
        }
    }

}
