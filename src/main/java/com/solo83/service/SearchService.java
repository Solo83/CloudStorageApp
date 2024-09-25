package com.solo83.service;

import com.solo83.dto.ItemDto;
import com.solo83.utils.FileUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class SearchService {

    private final MinioService minioService;
    private final PathService pathService;

    public List<ItemDto> searchObjectsByName(String prefix, String query) {
        return minioService.searchFilesByName(prefix, query).stream()
                .filter(item -> !item.objectName().endsWith("/"))
                .map(item -> {
                    String objectName = item.objectName();
                    String simpleName = objectName.substring(objectName.lastIndexOf("/") + 1);
                    String pathWithoutUserFolder = objectName.substring(objectName.indexOf("/"));
                    String pathToItem = pathWithoutUserFolder.substring(0, pathWithoutUserFolder.length() - simpleName.length());
                    return ItemDto.builder()
                            .simpleName(simpleName)
                            .urlEncodedPath(pathService.encodePath(pathToItem))
                            .size(FileUtils.readableFileSize(item.size()))
                            .build();
                })
                .collect(Collectors.toList());
    }
}
