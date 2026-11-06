package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
// 내 저장 목록(member 로 거르고 최근 순)을 정렬 없이 읽고, 같은 장소를 두 번 저장하지 못하게 한다
@Table (name = "bookmark",
        indexes = @Index(name = "idx_bookmark_member_created", columnList = "member_id, created_at"),
        uniqueConstraints = @UniqueConstraint(name = "uk_bookmark_member_location",
                columnNames = {"member_id", "location_id"}))
@NoArgsConstructor
public class LocationBookMarkEntity extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookmarkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private LocationEntity location;


    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "member_id")
    private MemberEntity member;


}
