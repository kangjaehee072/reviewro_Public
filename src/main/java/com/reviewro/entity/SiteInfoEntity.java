package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// 약관·개인정보 처리방침에 함께 보여 줄 운영 정보. 한 줄만 쓴다 (id = 1)
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "site_info")
public class SiteInfoEntity extends BaseTimeEntity {

    public static final long ID = 1L;

    @Id
    private Long siteInfoId = ID;

    @Column(length = 100)
    private String operatorName;          // 운영자(상호)

    @Column(length = 50)
    private String officerName;           // 개인정보 보호책임자 이름

    @Column(length = 50)
    private String officerPosition;       // 직책

    @Column(length = 30)
    private String officerPhone;          // 연락처 (비우면 화면에서 숨긴다)

    @Column(length = 100)
    private String officerEmail;          // 이메일 (비우면 화면에서 숨긴다)

    private LocalDate effectiveDate;      // 약관 시행일
}
