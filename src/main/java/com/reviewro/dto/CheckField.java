package com.reviewro.dto;

// 가입 폼에서 입력 중 중복 확인하는 칸.
// 이메일은 넣지 않는다: 로그인 없이 아무 이메일이나 가입 여부를 알아낼 수 있어서 (가입할 때 서버가 따로 막는다)
public enum CheckField {
    USER_ID, NICKNAME
}
