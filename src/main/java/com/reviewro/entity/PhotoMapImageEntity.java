package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "photo_map_image")
@Getter
@Setter
@NoArgsConstructor
public class PhotoMapImageEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long photoMapImageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_map_id", nullable = false)
    private PhotoMapEntity photoMap;

    @Column(name = "photo_map_image_region_code", nullable = false, length = 100)
    private String photoMapImageRegionCode;

    @Column(nullable = false, length = 100)
    private String photoMapImageRegionName;

    @Column(nullable = false)
    private String photoMapImagePath;

    @Column
    private Double photoMapImageOffsetX = 0.0;

    @Column
    private Double photoMapImageOffsetY = 0.0;

    @Column
    private Double photoMapImageSize = 1.0;
}
