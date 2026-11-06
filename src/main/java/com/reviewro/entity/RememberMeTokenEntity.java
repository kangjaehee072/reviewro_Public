package com.reviewro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 자동 로그인 토큰. 기기(브라우저)마다 한 줄씩 생긴다 (series = 기기 번호, token = 쓸 때마다 바뀌는 값).
// 쿠키를 훔쳐 쓰면 원래 기기와 token 이 어긋나서 들킨다 (Spring Security 영구 토큰 방식)
@Entity
@Table(name = "remember_me_token", indexes = @Index(name = "idx_remember_me_user", columnList = "username"))
@Getter
@Setter
@NoArgsConstructor
public class RememberMeTokenEntity {

    @Id
    @Column(length = 64)
    private String series;

    // 회원 아이디 (memberUserId)
    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 64)
    private String token;

    @Column(nullable = false)
    private LocalDateTime lastUsed;
}
