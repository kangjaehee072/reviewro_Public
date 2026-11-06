package com.reviewro.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PhotoMapDTO {

    private Long photoMapId;
    private String photoMapTitle;
    private String photoMapDescription;
    private List<PhotoMapImageDTO> photoMapImages = new ArrayList<>();
    private List<PhotoMapStickerDTO> photoMapStickers = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
