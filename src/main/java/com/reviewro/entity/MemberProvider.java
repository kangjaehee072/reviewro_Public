package com.reviewro.entity;

// 어디로 가입했는지. 예전 회원은 값이 비어 있고 LOCAL 로 본다
public enum MemberProvider {
    LOCAL,      // 아이디·비밀번호 가입
    KAKAO,
    GOOGLE,
    NAVER
}
