package com.reviewro.repository;

import com.reviewro.entity.MemberEntity;
import com.reviewro.entity.MemberProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findByMemberUserId(String memberUserId);

    // 소셜 로그인: 제공자 + 제공자 쪽 회원 번호로 찾는다
    Optional<MemberEntity> findByMemberProviderAndMemberProviderId(MemberProvider memberProvider, String memberProviderId);

    // 그룹 초대할 때 닉네임으로 회원을 찾는다
    Optional<MemberEntity> findByMemberNickname(String memberNickname);

    // 그룹 초대 검색: 닉네임 일부가 맞거나 아이디가 정확히 같고, 아직 그룹에 없는(초대 대기 포함) 회원.
    // 아이디를 앞부분으로 찾게 하면 한 글자씩 늘려 가며 남의 로그인 아이디 전체를 알아낼 수 있어서 완전 일치만 받는다.
    // nickname 의 % _ ! 는 서비스에서 ! 로 이스케이프해서 넘긴다
    @Query("SELECT m FROM MemberEntity m"
            + " WHERE (m.memberUserId = :userId"
            + "     OR m.memberNickname LIKE CONCAT('%', :nickname, '%') ESCAPE '!')"
            + " AND m.memberId NOT IN (SELECT gm.member.memberId FROM TravelGroupMemberEntity gm"
            + "                        WHERE gm.group.groupId = :groupId)"
            + " ORDER BY m.memberNickname")
    List<MemberEntity> searchForInvite(@Param("nickname") String nickname, @Param("userId") String userId,
                                       @Param("groupId") Long groupId, Pageable pageable);

    // 회원관리 목록. 아이디·닉네임·이메일 중 하나만 걸려도 나온다.
    // keyword 가 비면 조건을 통과시켜 전체가 나온다
    @Query("""
           select m from MemberEntity m
            where :keyword = ''
               or m.memberUserId   like concat('%', :keyword, '%')
               or m.memberNickname like concat('%', :keyword, '%')
               or m.memberEmail    like concat('%', :keyword, '%')
           """)
    Page<MemberEntity> search(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByMemberUserId(String memberUserId);

    boolean existsByMemberNickname(String memberNickname);

    boolean existsByMemberEmail(String memberEmail);

    boolean existsByMemberNicknameAndMemberIdNot(String memberNickname, Long memberId);

    boolean existsByMemberEmailAndMemberIdNot(String memberEmail, Long memberId);
}
