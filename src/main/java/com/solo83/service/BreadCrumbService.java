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
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class BreadCrumbService {

    private final MinioService minioService;

    @SneakyThrows
    public List<BreadCrumbDTO> getBreadCrumb(String path,String userRootFolder) {

        if (!userRootFolder.endsWith("/")) {
            userRootFolder = userRootFolder + "/";
        }

        BreadCrumbDTO rootBreadCrumbDTO = new BreadCrumbDTO();
        rootBreadCrumbDTO.setDirectory(true);
        rootBreadCrumbDTO.setSimpleName("Home");
        rootBreadCrumbDTO.setUrlEncodedPath(URLEncoder.encode("/", StandardCharsets.UTF_8));

        List<BreadCrumbDTO> breadCrumbDTOList = new LinkedList<>();
        Iterable<Result<Item>> objects = minioService.getObjects(path);
        breadCrumbDTOList.add(rootBreadCrumbDTO);

        for (Result<Item> result : objects) {

            String objectName = result.get().objectName();

            if (objectName.equals(userRootFolder)){
                continue;
            }

            // Удаляем префикс userRootFolder из objectName
            if (objectName.startsWith(userRootFolder)) {
                objectName = objectName.substring(userRootFolder.length());
            }

            BreadCrumbDTO breadCrumbDTO = new BreadCrumbDTO();
            if (objectName.endsWith("/")) {
                breadCrumbDTO.setDirectory(true);
            }

            String[] pathArray = objectName.split("/");
            breadCrumbDTO.setSimpleName(pathArray[pathArray.length - 1]);

            objectName = URLEncoder.encode(objectName, StandardCharsets.UTF_8);
            breadCrumbDTO.setUrlEncodedPath(objectName);
            breadCrumbDTOList.add(breadCrumbDTO);
            log.info(breadCrumbDTO.toString());
        }
        return breadCrumbDTOList;
    }
}
