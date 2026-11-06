package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name="balance_game")
public class BalanceGameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long balanceGameId;

    private String balanceGameTitle;

    private String balanceGameOptionA;

    private String balanceGameOptionB;

    private Long  balanceGameCountA = 0L;

    private Long  balanceGameCountB = 0L;

    private Boolean balanceGameStatus = true;

    // 자유게시판/로케이션처럼 작성자(Member) 연동이 필요하다면 추가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberEntity member;


}
