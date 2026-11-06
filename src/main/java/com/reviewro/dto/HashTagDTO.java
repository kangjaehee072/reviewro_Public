package com.reviewro.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
public class HashTagDTO {

    private Long hashTagId;
    private String hashTagName;
    private String hashTagCategory;
    private Integer hashTagUseCnt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
