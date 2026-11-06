package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "saju")
@Entity
public class SajuEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sajuId;

    private String sajuName;
    private String sajuGender;
    private String sajuBirthDate;
    private String sajuBirthTime;
    private Boolean sajuLunar;

//    private String sajuYearPillar;
//    private String sajuMonthPillar;
//    private String sajuDayPillar;
//    private String sajuTimePillar;
//    private String sajuMainElement;
//    private String sajuWeakElement;
//    private Integer sajuWoodCnt;
//    private Integer sajuFireCnt;
//    private Integer sajuEarthCnt;
//    private Integer sajuMetalCnt;
//    private Integer sajuWaterCnt;

    //@JoinColumn
    //@ManyToOne (fetch = FetchType.LAZY)
    //private Member member;
}
