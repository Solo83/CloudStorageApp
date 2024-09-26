package com.solo83.service;

import com.solo83.dto.ItemDto;
import com.solo83.exception.MinioServiceException;
import com.solo83.utils.FileUtils;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class BreadCrumbService {

    private final MinioService minioService;
    private final PathService pathService;

    public List<ItemDto> getBreadCrumbsChain(String path) {

        Iterable<Result<Item>> objects = minioService.getObjects(path,false);
        String pathWithoutUserFolder = path.substring(path.indexOf("/"));
        List<ItemDto> itemDtoList = new ArrayList<>(createPathItemsBreadCrumb(pathWithoutUserFolder));

        for (Result<Item> result : objects) {
            String objectName;
            String objectSize;
            try {
                objectName = result.get().objectName();
                objectSize = FileUtils.readableFileSize(result.get().size());
            } catch (Exception e) {
                log.error("Error while getting objectName", e);
                throw new MinioServiceException("Error while getting objectName " + result, e);
            }

            String objectNameWithoutUserFolder = objectName.substring(path.indexOf("/"));
            String objectNameWithoutPath = objectName.substring(path.length());
            if (!objectNameWithoutPath.isEmpty()){
                itemDtoList.addAll(createSubItems(objectNameWithoutPath, objectNameWithoutUserFolder,objectSize));
            }
        }
        return itemDtoList;
    }

    private List<ItemDto> createPathItemsBreadCrumb(String pathWithoutUserFolder) {
        List<ItemDto> itemDtoList = new ArrayList<>();
        String[] pathArray = pathWithoutUserFolder.split("/");

        if (pathArray.length == 0) {
            itemDtoList.add(createRootBreadCrumb());
            return itemDtoList;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < pathArray.length; i++) {
            String segment = pathArray[i];
            ItemDto breadCrumbFromPath = ItemDto.builder()
                    .simpleName(segment.isEmpty() ? "/" : segment)
                    .urlEncodedPath(pathService.encodePath(stringBuilder.append(segment).append("/").toString()))
                    .isDirectory(true)
                    .isActive(i == pathArray.length - 1)
                    .build();
            itemDtoList.add(breadCrumbFromPath);
        }
        return itemDtoList;
    }

    private List<ItemDto> createSubItems(String objectNameWithoutPath, String pathWithoutUserFolder, String size) {
        List<ItemDto> itemDtoList = new ArrayList<>();
        String[] split = objectNameWithoutPath.split("/");

        if (split.length == 1) { // single item in current path
            ItemDto itemDTO = ItemDto.builder()
                    .simpleName(split[split.length - 1])
                    .urlEncodedPath(pathService.encodePath(pathWithoutUserFolder))
                    .isDirectory(objectNameWithoutPath.endsWith("/"))
                    .isSubfolder(objectNameWithoutPath.endsWith("/"))
                    .size(size)
                    .build();
            itemDtoList.add(itemDTO);
        }
        return itemDtoList;
    }

    private ItemDto createRootBreadCrumb() {
        return ItemDto.builder()
                .simpleName("/")
                .urlEncodedPath("/")
                .isActive(true)
                .isDirectory(true)
                .build();
    }

}
