package com.reviewro.security;

import com.reviewro.entity.RememberMeTokenEntity;
import com.reviewro.repository.RememberMeTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

// 자동 로그인 토큰을 DB(remember_me_token)에 둔다. Spring Security 가 이 네 가지만 부른다
@Component
@RequiredArgsConstructor
@Transactional
public class RememberMeTokenStore implements PersistentTokenRepository {

    private final RememberMeTokenRepository repository;

    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        RememberMeTokenEntity entity = new RememberMeTokenEntity();
        entity.setSeries(token.getSeries());
        entity.setUsername(token.getUsername());
        entity.setToken(token.getTokenValue());
        entity.setLastUsed(toLocal(token.getDate()));
        repository.save(entity);
    }

    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        repository.findById(series).ifPresent(entity -> {
            entity.setToken(tokenValue);
            entity.setLastUsed(toLocal(lastUsed));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        return repository.findById(seriesId)
                .map(e -> new PersistentRememberMeToken(e.getUsername(), e.getSeries(), e.getToken(),
                        Date.from(e.getLastUsed().atZone(ZoneId.systemDefault()).toInstant())))
                .orElse(null);
    }

    // Spring Security 는 쿠키 도용이 의심될 때 이걸 불러 그 회원의 모든 기기를 끊는다
    @Override
    public void removeUserTokens(String username) {
        repository.deleteAllOfUser(username);
    }

    // 이 기기 하나만 (로그아웃할 때)
    public void removeSeries(String series) {
        repository.deleteById(series);
    }

    private static LocalDateTime toLocal(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
