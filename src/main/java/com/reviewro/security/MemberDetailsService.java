package com.reviewro.security;

import com.reviewro.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String memberUserId) throws UsernameNotFoundException {
        return memberRepository.findByMemberUserId(memberUserId)
                .map(MemberDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 아이디입니다: " + memberUserId));
    }
}
