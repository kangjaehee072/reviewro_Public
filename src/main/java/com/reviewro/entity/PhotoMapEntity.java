package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "photo_map")
@Getter
@Setter
@NoArgsConstructor
public class PhotoMapEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long photoMapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Column(nullable = false, length = 50)
    private String photoMapTitle;

    @Column(length = 200)
    private String photoMapDescription;

    @OneToMany(mappedBy = "photoMap", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("photoMapImageRegionCode")
    private List<PhotoMapImageEntity> photoMapImages = new ArrayList<>();

    @OneToMany(mappedBy = "photoMap", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("photoMapStickerOrder")
    private List<PhotoMapStickerEntity> photoMapStickers = new ArrayList<>();
}
