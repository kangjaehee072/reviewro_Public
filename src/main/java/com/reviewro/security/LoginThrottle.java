package com.reviewro.security;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// 로그인 시도 제한. 같은 접속 IP 에서 10초 안에 다섯 번째 시도가 오면 30초 동안 막는다.
// 아이디 기준이 아니라 IP 기준인 이유: 아이디로 막으면 남의 아이디를 계속 틀려서 그 사람 로그인을 막을 수 있다.
// 메모리에만 두므로 서버를 다시 켜면 초기화된다
@Component
public class LoginThrottle {

    // 접속 로그 맨 끝 칸에 찍힐 표시 (application.properties 의 accesslog.pattern)
    public static final String LOG_ATTRIBUTE = "reviewro.security";

    public enum Result { ALLOW, LOCKED_NOW, LOCKED }

    private static final long WINDOW_MS = 10_000;
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MS = 30_000;

    // 로그인 실패는 같은 IP 가 10분 안에 3번째 틀렸을 때 한 번만 로그에 남긴다
    private static final long FAIL_WINDOW_MS = 600_000;
    private static final int FAIL_MARK = 3;

    private final Map<String, Deque<Long>> attempts = new HashMap<>();
    private final Map<String, Long> lockedUntil = new HashMap<>();
    private final Map<String, long[]> failures = new HashMap<>();   // {실패 수, 마지막 실패 시각}

    // 이번 시도를 받아도 되면 ALLOW. 방금 막혔으면 LOCKED_NOW, 이미 막힌 상태면 LOCKED
    public synchronized Result check(String ip) {
        String key = key(ip);
        long now = System.currentTimeMillis();

        Long until = lockedUntil.get(key);
        if (until != null) {
            if (until > now) {
                return Result.LOCKED;
            }
            lockedUntil.remove(key);
        }

        Deque<Long> hits = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!hits.isEmpty() && now - hits.peekFirst() >= WINDOW_MS) {
            hits.pollFirst();
        }
        if (hits.size() + 1 >= MAX_ATTEMPTS) {
            attempts.remove(key);
            lockedUntil.put(key, now + LOCK_MS);
            return Result.LOCKED_NOW;
        }
        hits.addLast(now);
        return Result.ALLOW;
    }

    // 로그인 실패를 세고, 이번이 3번째 실패면 true
    public synchronized boolean failedThird(String ip) {
        String key = key(ip);
        long now = System.currentTimeMillis();
        long[] fail = failures.get(key);
        if (fail == null || now - fail[1] >= FAIL_WINDOW_MS) {
            fail = new long[2];
            failures.put(key, fail);
        }
        fail[0]++;
        fail[1] = now;
        return fail[0] == FAIL_MARK;
    }

    private static String key(String ip) {
        return ip == null ? "" : ip.trim().toLowerCase(Locale.ROOT);
    }
}
