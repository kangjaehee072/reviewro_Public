package com.reviewro.service;

import com.reviewro.config.ImageFiles;
import com.reviewro.dto.CheckField;
import com.reviewro.dto.MemberDTO;
import com.reviewro.entity.MemberEntity;
import com.reviewro.entity.MemberRole;
import com.reviewro.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    @Value("${app.home}")
    String appHome;

    @Value("${file.profile.dir}")
    Path profileDir;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 가입 폼 입력 중 중복 확인 (true 면 이미 사용 중)
    public boolean isDuplicate(CheckField field, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return switch (field) {
            case USER_ID -> memberRepository.existsByMemberUserId(value);
            case NICKNAME -> memberRepository.existsByMemberNickname(value);
        };
    }

    // 회원가입 (중복 검사 후 비밀번호 암호화, 새 회원 id 반환)
    @Transactional
    public Long signup(MemberDTO dto) {
        if (memberRepository.existsByMemberUserId(dto.getMemberUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (memberRepository.existsByMemberNickname(dto.getMemberNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        if (memberRepository.existsByMemberEmail(dto.getMemberEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 폼 값을 통째로 복사하면 memberRole 같은 필드까지 클라이언트가 정할 수 있다. 필요한 것만 옮긴다
        MemberEntity member = new MemberEntity();
        member.setMemberUserId(dto.getMemberUserId());
        member.setMemberNickname(dto.getMemberNickname());
        member.setMemberEmail(dto.getMemberEmail());
        member.setMemberPhone(dto.getMemberPhone());
        member.setMemberAddress(dto.getMemberAddress());
        member.setMemberRole(MemberRole.USER);
        // 컨트롤러가 필수 약관 동의를 확인한 뒤에만 여기까지 온다
        member.setMemberTermsAgreedAt(LocalDateTime.now());
        member.setMemberPassword(passwordEncoder.encode(dto.getMemberPassword()));
        member.setMemberProfileImagePath(saveProfileImage(dto.getMemberProfileImage()));

        return memberRepository.save(member).getMemberId();
    }

    // 프로필 수정 (중복 검사는 자신 제외, 비밀번호·사진은 새 값이 있을 때만 변경)
    @Transactional
    public MemberEntity updateProfile(Long memberId, MemberDTO dto) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (memberRepository.existsByMemberNicknameAndMemberIdNot(dto.getMemberNickname(), memberId)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        if (memberRepository.existsByMemberEmailAndMemberIdNot(dto.getMemberEmail(), memberId)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        member.setMemberNickname(dto.getMemberNickname());
        member.setMemberEmail(dto.getMemberEmail());
        member.setMemberPhone(dto.getMemberPhone());
        member.setMemberAddress(dto.getMemberAddress());

        MultipartFile image = dto.getMemberProfileImage();
        if (image != null && !image.isEmpty()) {
            deleteProfileImage(member.getMemberProfileImagePath());
            member.setMemberProfileImagePath(saveProfileImage(image));
        }

        String newPassword = dto.getMemberNewPassword();
        if (newPassword != null && !newPassword.isBlank()) {
            if (dto.getMemberPassword() == null
                    || !passwordEncoder.matches(dto.getMemberPassword(), member.getMemberPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
            }
            if (newPassword.length() < 8) {
                throw new IllegalArgumentException("새 비밀번호는 8자 이상이어야 합니다.");
            }
            member.setMemberPassword(passwordEncoder.encode(newPassword));
        }

        return member;
    }

    // 프로필 사진 교체 (회원당 1장이라 기존 파일을 지운다)
    @Transactional
    public void changeProfileImage(Long memberId, MultipartFile file) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        deleteProfileImage(member.getMemberProfileImagePath());
        member.setMemberProfileImagePath(saveProfileImage(file));
    }

    // 프로필 사진 저장 (이름 겹치면 _(1) 증분, 파일이 없으면 null)
    private String saveProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            Path target = profileDir.resolve(ImageFiles.newName(file));
            Files.createDirectories(profileDir);
            Files.copy(file.getInputStream(), target);
            String path = profileDir.toString().replace("\\", "/").substring(appHome.length());
            return path + "/" + target.getFileName();
        } catch (IOException e) {
            throw new UncheckedIOException("프로필 이미지 저장에 실패했습니다.", e);
        }
    }

    // 기존 프로필 사진 삭제 (경로가 null 이면 아무것도 안 함). 회원 탈퇴 때도 쓴다
    public void deleteProfileImage(String profileImagePath) {
        // 소셜 가입 회원은 제공자 쪽 사진 주소(http...)를 쓰고 있어 지울 파일이 없다
        if (profileImagePath == null || profileImagePath.startsWith("http")) {
            return;
        }
        try {
            String fileName = profileImagePath.substring(profileImagePath.lastIndexOf('/') + 1);
            Files.deleteIfExists(profileDir.resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException("기존 프로필 이미지 삭제에 실패했습니다.", e);
        }
    }
}
