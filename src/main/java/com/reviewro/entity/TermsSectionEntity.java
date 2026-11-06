package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.*;

// 약관 한 덩어리 (이용약관, 개인정보 수집·이용 동의, 개인정보 처리방침 …).
// 슈퍼어드민이 관리자 메뉴 '이용약관 수정' 에서 고친다
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "terms_section")
public class TermsSectionEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long termsSectionId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 화면에 나오는 순서 (작을수록 위)
    @Column(nullable = false)
    private int sortOrder;

    // true 면 회원가입 때 '(필수) 동의' 체크박스로 나온다. false 면 약관 페이지에서 읽기만 한다
    @Column(nullable = false)
    private boolean requiredOnSignup;
}
