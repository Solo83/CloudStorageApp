package com.solo83.service;


import com.solo83.dto.BreadCrumbDTO;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
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

    @SneakyThrows
    public List<BreadCrumbDTO> getBreadCrumbsChain(String path) {
        List<BreadCrumbDTO> breadCrumbDTOList = new ArrayList<>();
        Iterable<Result<Item>> objects = minioService.getObjects(path);

        for (Result<Item> result : objects) {
            String pathToObject = result.get().objectName().substring(path.indexOf("/"));
            String objectName = result.get().objectName().substring(path.length());

            if (objectName.isEmpty()) {
                String[] pathArray = pathToObject.split("/");
                if (pathArray.length > 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String s : pathArray) {
                        BreadCrumbDTO breadCrumbFromPath = BreadCrumbDTO.builder()
                                .simpleName(s.isEmpty() ? "root" : s)
                                .urlEncodedPath(URLEncoder.encode(stringBuilder.append(s).append("/").toString(), StandardCharsets.UTF_8))
                                .isDirectory(true)
                                .build();
                        //breadCrumbFromPath.setUrlEncodedPath(stringBuilder.append(s).append("/").toString());
                        breadCrumbDTOList.add(breadCrumbFromPath);
                    }
                }
                if (!breadCrumbDTOList.isEmpty()) {
                    BreadCrumbDTO activeBreadCrumb = breadCrumbDTOList.get(breadCrumbDTOList.size() - 1);
                    activeBreadCrumb.setActive(true);
                }
                continue;
            }
            String[] split = objectName.split("/");
            if (split.length == 1) { // subs in current path
                BreadCrumbDTO breadCrumbDTO = BreadCrumbDTO.builder()
                        .simpleName(split[split.length - 1])
                        .urlEncodedPath(URLEncoder.encode(pathToObject, StandardCharsets.UTF_8))
                        .isDirectory(objectName.endsWith("/"))
                        .isSubfolder(true)
                        .build();
                //breadCrumbDTO.setUrlEncodedPath(pathToObject);
                breadCrumbDTOList.add(breadCrumbDTO);
            }
        }
        log.info("Number of breadcrumbs: {}", breadCrumbDTOList.size());
        return breadCrumbDTOList;
    }
}
