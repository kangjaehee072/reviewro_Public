package com.reviewro.service;

import com.reviewro.entity.SiteInfoEntity;
import com.reviewro.entity.TermsSectionEntity;
import com.reviewro.repository.SiteInfoRepository;
import com.reviewro.repository.TermsSectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

// 이용약관·개인정보 처리방침과 운영 정보(개인정보 보호책임자 등).
// 내용은 슈퍼어드민이 '이용약관 수정' 화면에서 바꾼다. 처음 띄울 때 비어 있으면 기본 문구를 넣어 둔다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Log4j2
public class TermsService {

    private static final int TITLE_MAX = 100;
    private static final int CONTENT_MAX = 60_000;

    private final TermsSectionRepository sectionRepository;
    private final SiteInfoRepository siteInfoRepository;

    public List<TermsSectionEntity> sections() {
        return sectionRepository.findAllByOrderBySortOrderAscTermsSectionIdAsc();
    }

    // 가입 화면에 '(필수) 동의' 로 나오는 것
    public List<TermsSectionEntity> requiredSections() {
        return sectionRepository.findByRequiredOnSignupTrueOrderBySortOrderAscTermsSectionIdAsc();
    }

    public SiteInfoEntity siteInfo() {
        return siteInfoRepository.findById(SiteInfoEntity.ID).orElseGet(SiteInfoEntity::new);
    }

    // 가입 때 필수 항목에 모두 체크했는지 (체크박스 값은 섹션 번호)
    public boolean agreedToAllRequired(Collection<Long> agreedIds) {
        Set<Long> agreed = agreedIds == null ? Set.of() : Set.copyOf(agreedIds);
        return requiredSections().stream().allMatch(s -> agreed.contains(s.getTermsSectionId()));
    }

    // ===== 슈퍼어드민 수정 =====

    @Transactional
    public void saveSection(Long id, String title, String content, int sortOrder, boolean requiredOnSignup) {
        String t = title == null ? "" : title.trim();
        String c = content == null ? "" : content.trim();
        if (t.isEmpty() || c.isEmpty()) {
            throw new IllegalArgumentException("제목과 내용을 모두 입력해 주세요.");
        }
        if (t.length() > TITLE_MAX || c.length() > CONTENT_MAX) {
            throw new IllegalArgumentException("제목은 " + TITLE_MAX + "자, 내용은 " + CONTENT_MAX + "자까지예요.");
        }
        TermsSectionEntity section = id == null
                ? new TermsSectionEntity()
                : sectionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("없는 항목입니다."));
        section.setTitle(t);
        section.setContent(c);
        section.setSortOrder(sortOrder);
        section.setRequiredOnSignup(requiredOnSignup);
        sectionRepository.save(section);
    }

    @Transactional
    public void deleteSection(Long id) {
        sectionRepository.deleteById(id);
    }

    @Transactional
    public void saveSiteInfo(SiteInfoEntity form) {
        SiteInfoEntity info = siteInfoRepository.findById(SiteInfoEntity.ID).orElseGet(SiteInfoEntity::new);
        info.setOperatorName(trimToNull(form.getOperatorName()));
        info.setOfficerName(trimToNull(form.getOfficerName()));
        info.setOfficerPosition(trimToNull(form.getOfficerPosition()));
        info.setOfficerPhone(trimToNull(form.getOfficerPhone()));
        info.setOfficerEmail(trimToNull(form.getOfficerEmail()));
        info.setEffectiveDate(form.getEffectiveDate());
        siteInfoRepository.save(info);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // ===== 처음 띄울 때 기본 문구 =====

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaults() {
        if (sectionRepository.count() == 0) {
            sectionRepository.save(TermsSectionEntity.builder()
                    .title("이용약관").content(DEFAULT_TERMS).sortOrder(1).requiredOnSignup(true).build());
            sectionRepository.save(TermsSectionEntity.builder()
                    .title("개인정보 수집·이용 동의").content(DEFAULT_COLLECTION).sortOrder(2).requiredOnSignup(true).build());
            sectionRepository.save(TermsSectionEntity.builder()
                    .title("개인정보 처리방침").content(DEFAULT_PRIVACY).sortOrder(3).requiredOnSignup(false).build());
            log.info("이용약관 기본 문구를 넣었습니다. 관리자메뉴 > 이용약관 수정에서 실제 운영 내용으로 바꿔 주세요.");
        }
        if (!siteInfoRepository.existsById(SiteInfoEntity.ID)) {
            SiteInfoEntity info = new SiteInfoEntity();
            info.setOperatorName("리뷰로");
            info.setEffectiveDate(LocalDate.now());
            siteInfoRepository.save(info);
        }
    }

    private static final String DEFAULT_TERMS = """
            제1조 (목적)
            이 약관은 리뷰로(이하 "서비스")가 제공하는 여행 정보·커뮤니티 서비스의 이용 조건과 절차, 회원과 서비스의 권리·의무 및 책임 사항을 정합니다.

            제2조 (정의)
            1. "회원"이란 이 약관에 동의하고 가입하여 서비스를 이용하는 사람을 말합니다.
            2. "게시물"이란 회원이 서비스에 올린 글, 사진, 댓글, 리뷰, 여행계획 등을 말합니다.

            제3조 (약관의 효력과 변경)
            1. 이 약관은 서비스 화면에 게시하여 효력이 생깁니다.
            2. 서비스는 관련 법령을 위반하지 않는 범위에서 약관을 바꿀 수 있으며, 바꿀 때에는 시행일 7일 전(회원에게 불리한 변경은 30일 전)부터 공지합니다.

            제4조 (회원가입)
            1. 가입 신청자가 이 약관과 개인정보 수집·이용에 동의하고 정해진 양식을 작성하면 가입이 완료됩니다.
            2. 타인의 정보를 도용하거나 거짓 정보를 적은 경우 가입을 거절하거나 취소할 수 있습니다.

            제5조 (회원 탈퇴와 자격 상실)
            1. 회원은 언제든지 마이페이지에서 탈퇴할 수 있으며, 서비스는 즉시 처리합니다.
            2. 탈퇴하면 회원의 개인정보는 지체 없이 파기합니다. 다만 회원이 쓴 게시물은 다른 회원의 이용을 위해 남으며, 작성자는 "탈퇴회원"으로 표시됩니다. 남기를 원하지 않는 게시물은 탈퇴 전에 직접 지워 주세요.
            3. 회원이 법령이나 이 약관을 위반한 경우 서비스는 이용을 제한하거나 자격을 박탈할 수 있습니다.

            제6조 (회원의 의무)
            회원은 다음 행위를 하여서는 안 됩니다.
            1. 타인의 정보 도용, 거짓 정보 등록
            2. 타인의 명예를 훼손하거나 불쾌감을 주는 게시물, 음란·폭력적인 게시물 등록
            3. 저작권 등 타인의 권리를 침해하는 행위
            4. 서비스의 운영을 방해하는 행위

            제7조 (게시물의 관리)
            1. 게시물의 저작권은 작성한 회원에게 있습니다.
            2. 서비스는 법령이나 이 약관에 어긋나는 게시물을 사전 통지 없이 지우거나 가릴 수 있습니다.

            제8조 (서비스의 변경과 중단)
            서비스는 운영상·기술상 필요에 따라 서비스의 전부 또는 일부를 바꾸거나 중단할 수 있으며, 이 경우 미리 공지합니다.

            제9조 (책임의 한계)
            서비스가 제공하는 장소 정보·AI 추천은 참고용이며, 실제 영업 여부·요금 등은 방문 전 직접 확인해야 합니다. 서비스는 회원 간 또는 회원과 제3자 사이의 거래·분쟁에 개입하지 않습니다.

            제10조 (분쟁 해결)
            이 약관에 관한 분쟁은 대한민국 법을 따르며, 관할 법원은 민사소송법에 따릅니다.
            """;

    private static final String DEFAULT_COLLECTION = """
            1. 수집하는 항목
            - 필수: 아이디, 비밀번호, 닉네임, 이메일
            - 선택: 프로필 사진, 연락처, 주소
            - 소셜 로그인 시: 제공자가 주는 회원 식별값, 닉네임, 프로필 사진, 이메일(제공에 동의한 경우)
            - 서비스 이용 중 자동으로 생기는 정보: 접속 기록, 쿠키, 접속 IP

            2. 수집·이용 목적
            회원 식별과 가입 의사 확인, 게시판·여행계획·그룹 등 서비스 제공, 부정 이용 방지, 문의 응대

            3. 보유·이용 기간
            회원 탈퇴 시까지 보유하며, 탈퇴하면 지체 없이 파기합니다.
            다만 관계 법령에 따라 보관해야 하는 정보는 그 기간 동안 보관합니다.
            - 서비스 접속 기록: 3개월 (통신비밀보호법)

            4. 동의를 거부할 권리
            위 개인정보 수집·이용에 동의하지 않을 수 있습니다. 다만 필수 항목에 동의하지 않으면 회원가입을 할 수 없습니다.
            """;

    private static final String DEFAULT_PRIVACY = """
            리뷰로(이하 "서비스")는 「개인정보 보호법」에 따라 이용자의 개인정보를 보호하고 관련 고충을 신속하게 처리하기 위해 다음과 같이 개인정보 처리방침을 둡니다.

            1. 처리하는 개인정보 항목과 목적
            '개인정보 수집·이용 동의' 에 적힌 항목을 회원 관리와 서비스 제공 목적으로만 처리합니다.

            2. 보유 및 파기
            회원 탈퇴 시 개인정보를 지체 없이 파기합니다. 전자 파일은 복구할 수 없는 방법으로 지우며, 탈퇴 회원이 쓴 게시물은 작성자를 "탈퇴회원"으로 바꾸어 남깁니다.

            3. 제3자 제공
            이용자의 동의나 법령의 근거 없이 개인정보를 제3자에게 제공하지 않습니다.

            4. 처리 위탁
            서비스 운영을 위해 다음 외부 서비스를 이용합니다.
            - 카카오(지도 표시, 카카오 로그인), 네이버·구글(소셜 로그인)

            5. 이용자의 권리
            이용자는 언제든지 마이페이지에서 자신의 개인정보를 조회·수정하거나 회원 탈퇴(동의 철회)를 할 수 있습니다.

            6. 안전성 확보 조치
            비밀번호는 복호화할 수 없도록 암호화해 저장하며, 개인정보에 접근할 수 있는 사람을 최소한으로 제한합니다.

            7. 개인정보 보호책임자
            개인정보 처리와 관련한 문의·불만·피해 구제는 아래 개인정보 보호책임자에게 연락해 주세요.
            """;
}
