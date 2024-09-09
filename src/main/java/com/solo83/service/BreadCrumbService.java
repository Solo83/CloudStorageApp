package com.solo83.service;


import com.solo83.dto.BreadCrumbDTO;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class BreadCrumbService {

    private final MinioService minioService;

    public List<BreadCrumbDTO> getBreadCrumbsChain(String path) {
        List<BreadCrumbDTO> breadCrumbDTOList = new ArrayList<>();
        Iterable<Result<Item>> objects;
        try {
            objects = minioService.getObjects(path);
        } catch (Exception e) {
            log.error("Error fetching objects from Minio", e);
            return breadCrumbDTOList; // return empty list on error
        }

        for (Result<Item> result : objects) {
            String objectName = "";
            try {
                objectName = result.get().objectName();
            } catch (Exception e) {
                log.error("Error while getting objectName", e);
            }

            String pathWithoutUserFolder = objectName.substring(path.indexOf("/")); // remove user folder from path
            String objectNameWithoutPath = objectName.substring(path.length()); // remove path from objectName

            if (objectNameWithoutPath.isEmpty()) {
                breadCrumbDTOList.addAll(createPathItemsBreadCrumb(pathWithoutUserFolder));
            } else {
                breadCrumbDTOList.addAll(createSubItemsBreadcrumb(objectNameWithoutPath, pathWithoutUserFolder));
            }
        }

        log.info("Number of items: {}", breadCrumbDTOList.size());
        return breadCrumbDTOList;
    }

    private List<BreadCrumbDTO> createPathItemsBreadCrumb(String pathWithoutUserFolder) {
        List<BreadCrumbDTO> breadCrumbDTOList = new ArrayList<>();
        String[] pathArray = pathWithoutUserFolder.split("/");

        if (pathArray.length == 0) {
            breadCrumbDTOList.add(createRootBreadCrumb());
            return breadCrumbDTOList;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < pathArray.length; i++) {
            String segment = pathArray[i];
            BreadCrumbDTO breadCrumbFromPath = BreadCrumbDTO.builder()
                    .simpleName(segment.isEmpty() ? "/" : segment)
                    .urlEncodedPath(encodePath(stringBuilder.append(segment).append("/").toString()))
                    .isDirectory(true)
                    .isActive(i == pathArray.length - 1)
                    .build();
            breadCrumbDTOList.add(breadCrumbFromPath);
        }
        return breadCrumbDTOList;
    }

    private List<BreadCrumbDTO> createSubItemsBreadcrumb(String objectNameWithoutPath, String pathWithoutUserFolder) {
        List<BreadCrumbDTO> breadCrumbDTOList = new ArrayList<>();
        String[] split = objectNameWithoutPath.split("/");

        if (split.length == 1) { // single item in current path
            BreadCrumbDTO breadCrumbDTO = BreadCrumbDTO.builder()
                    .simpleName(split[split.length - 1])
                    .urlEncodedPath(encodePath(pathWithoutUserFolder))
                    .isDirectory(objectNameWithoutPath.endsWith("/"))
                    .isSubfolder(objectNameWithoutPath.endsWith("/"))
                    .build();
            breadCrumbDTOList.add(breadCrumbDTO);
        }
        return breadCrumbDTOList;
    }

    private String encodePath(String path) {
        try {
            return URLEncoder.encode(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error encoding path", e);
            return path; // return original path on encoding error
        }
    }

    private BreadCrumbDTO createRootBreadCrumb() {
        return BreadCrumbDTO.builder()
                .simpleName("/")
                .urlEncodedPath("/")
                .isActive(true)
                .isDirectory(true)
                .build();
    }
}
