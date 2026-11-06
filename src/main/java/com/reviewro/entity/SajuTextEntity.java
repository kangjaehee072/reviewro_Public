package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 사주 풀이 문구 (일간·일주·십신·오행). 데이터는 공용 DB 에 직접 넣어 두었다
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "saju_text")
public class SajuTextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sajuTextId;

    // DAY_MASTER / DAY_PILLAR / TEN_GOD / ELEMENT_MANY / ELEMENT_LACK / ELEMENT_TRIP
    private String sajuTextType;

    // 비견
    private String sajuTextCode;

    private String sajuTextTitle;

    @Column(columnDefinition = "TEXT")
    private String sajuTextContent;
}
