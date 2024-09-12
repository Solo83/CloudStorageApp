package com.solo83.service;

import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
public class PathService {

    public String appendUserRootFolder(String path, String userRootFolder) {
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        if (!userRootFolder.endsWith("/")) {
            userRootFolder = userRootFolder + "/";
        }
        if (decodedPath.startsWith("/")) {
            decodedPath = decodedPath.substring(1);
        }
        return userRootFolder + decodedPath;
    }

    public String appendNewName(String oldPath,String newName) {
        if (oldPath == null || newName == null) {
            throw new IllegalArgumentException("Arguments must not be null");
        }

        if (oldPath.endsWith("/")) {
            newName+="/";
            oldPath=oldPath.substring(0,oldPath.lastIndexOf('/'));
            return oldPath.substring(0,oldPath.lastIndexOf('/') + 1)+newName;
        }
        return oldPath.substring(0,oldPath.lastIndexOf('/') + 1)+newName;
    }

}
