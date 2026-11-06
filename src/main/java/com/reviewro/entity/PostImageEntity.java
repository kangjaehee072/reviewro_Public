package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "post_image",
       indexes = @Index(name = "idx_post_image_target",
       columnList = "target_type, target_id, post_image_id"))
public class PostImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postImageId;

    // 어느 게시판의
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20, nullable = false,
            columnDefinition = "varchar(20)")
    private TargetType targetType;

    // 몇 번 글인지
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    // 화면에서 바로 쓸 수 있는 경로. 예: /data/post/REVIEW/3/bada.jpg
    @Column(nullable = false, length = 255)
    private String imagePath;

}
