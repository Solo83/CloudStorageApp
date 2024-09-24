package com.solo83.dto;

import lombok.Builder;


@Builder
public record ItemDto(String simpleName, String urlEncodedPath, boolean isDirectory, boolean isActive, boolean isSubfolder, String size) {
}
