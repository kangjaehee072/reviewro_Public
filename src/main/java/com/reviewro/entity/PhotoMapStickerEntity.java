package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "photo_map_sticker")
@Getter
@Setter
@NoArgsConstructor
public class PhotoMapStickerEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long photoMapStickerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_map_id", nullable = false)
    private PhotoMapEntity photoMap;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private PhotoMapStickerType photoMapStickerType = PhotoMapStickerType.POLAROID;

    private String photoMapStickerImagePath;

    @Column(length = 200)
    private String photoMapStickerText;

    @Column(length = 8000)
    private String photoMapStickerTextStyle;

    @Column(length = 20)
    private String photoMapStickerBackground;

    @Column(nullable = false)
    private Double photoMapStickerX = 0.0;

    @Column(nullable = false)
    private Double photoMapStickerY = 0.0;

    @Column(nullable = false)
    private Double photoMapStickerRotation = 0.0;

    @Column(nullable = false)
    private Double photoMapStickerSize = 1.0;

    @Column(nullable = false)
    private Integer photoMapStickerOrder = 0;
}
