package com.reviewro.dto;

import com.reviewro.entity.PhotoMapStickerType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class PhotoMapStickerDTO {

    private Long photoMapStickerId;
    private PhotoMapStickerType photoMapStickerType;
    private MultipartFile photoMapStickerImageFile;
    private String photoMapStickerImagePath;
    private String photoMapStickerText;
    private String photoMapStickerTextStyle;
    private String photoMapStickerBackground;
    private Double photoMapStickerX;
    private Double photoMapStickerY;
    private Double photoMapStickerRotation;
    private Double photoMapStickerSize;
    private Integer photoMapStickerOrder;
}
