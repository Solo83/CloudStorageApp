package com.solo83.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BreadCrumbDTO {
    private String simpleName;
    private String urlEncodedPath;
    private boolean isDirectory = false;
}
