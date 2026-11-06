package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "free_board",
indexes = {
@Index(name = "idx_free_board_member", columnList = "member_id")
    })
@NoArgsConstructor
public class FreeBoardEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long freeBoardId;

    private String freeBoardTitle;

    private String freeBoardName;

    private LocalDate freeBoardDate;

    // @Lob 만 두면 Hibernate 7 + MariaDB 에서 tinytext(255바이트)로 잡혀서,
    // 시작할 때마다 긴 글이 있는 컬럼을 줄이려다 실패한다. 본문 5000자가 들어가게 TEXT 로 못 박는다
    @Column(columnDefinition = "TEXT", nullable = false)
    private String freeBoardContent;

    private Integer freeBoardShareCount=0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberEntity member;

    // 2. HashTagEntity와 다대일(N:1) 연관관계 매핑 (FK 설정)
    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "hash_tag_id")
    //private HashTagEntity hashTag;
}