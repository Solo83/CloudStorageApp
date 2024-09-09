package com.solo83.dto;

import lombok.Builder;


@Builder
public record BreadCrumbDTO (String simpleName, String urlEncodedPath,boolean isDirectory, boolean isActive,boolean isSubfolder) {
}
