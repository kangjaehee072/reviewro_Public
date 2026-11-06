package com.reviewro.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class PhotoMapImageDTO {

    private String photoMapImageRegionCode;
    private String photoMapImageRegionName;
    private MultipartFile photoMapImageFile;
    private String photoMapImagePath;
    private Double photoMapImageOffsetX;
    private Double photoMapImageOffsetY;
    private Double photoMapImageSize;
}
