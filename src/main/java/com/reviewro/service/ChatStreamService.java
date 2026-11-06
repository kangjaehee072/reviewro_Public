package com.reviewro.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// 톡방·그룹 채팅의 SSE 연결을 채널별로 들고 있다가 새 메시지를 같은 채널의 모든 연결에 밀어준다.
// 연결은 서버 메모리에만 있어서 서버를 여러 대 띄우면 같은 서버에 붙은 사람끼리만 실시간으로 보인다
@Service
@Log4j2
@RequiredArgsConstructor
public class ChatStreamService {

    // 톡방에서 마지막 한 명이 나가 빈 방이 됐을 때 알린다 (ChatRoomService 가 받아서 방을 지운다).
    // 그룹 채팅은 이 이벤트를 내지 않는다 (방처럼 비었다고 없어지면 안 되므로)
    public record ChatRoomEmptiedEvent(Long roomId) {}

    private final ApplicationEventPublisher events;

    // 브라우저가 끊긴 연결을 알아서 다시 잇는다. 너무 길면 죽은 연결이 오래 남는다
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    // 채널(방·그룹) -> (연결 -> 회원 번호)
    private final Map<String, Map<SseEmitter, Long>> rooms = new ConcurrentHashMap<>();

    // 연결 번호 -> 연결 (브라우저가 나간다고 알려 올 때 찾는다)
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();

    private record Connection(String channel, SseEmitter emitter, Long memberId) {}

    public static String roomChannel(Long chatRoomId) {
        return "room:" + chatRoomId;
    }

    public static String groupChannel(Long groupId) {
        return "group:" + groupId;
    }

    public SseEmitter subscribe(String channel, Long memberId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        String connId = UUID.randomUUID().toString();
        connections.put(connId, new Connection(channel, emitter, memberId));
        // 넣기·빼기를 compute 안에서 해야 빈 방을 지우는 순간 들어온 연결이 사라지지 않는다
        rooms.compute(channel, (k, room) -> {
            Map<SseEmitter, Long> target = room == null ? new ConcurrentHashMap<>() : room;
            target.put(emitter, memberId);
            return target;
        });

        Runnable remove = () -> {
            connections.remove(connId);
            removeEmitter(channel, emitter);
            broadcastOnline(channel);
        };
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        // 첫 이벤트를 바로 보내야 브라우저가 연결됐다고 안다. 나갈 때 쓸 연결 번호도 같이 준다
        send(channel, emitter, SseEmitter.event().name("hello").data(connId));
        broadcastOnline(channel);
        return emitter;
    }

    // 브라우저가 페이지를 떠나거나 '나가기'를 누를 때 부른다.
    // 서버는 끊긴 연결을 다음에 보낼 때까지 모르므로, 기다리지 않고 바로 빼서 접속 인원을 맞춘다
    public void close(String connId, Long memberId) {
        Connection conn = connections.get(connId);
        if (conn == null || !conn.memberId().equals(memberId)) {
            return;
        }
        connections.remove(connId);
        removeEmitter(conn.channel(), conn.emitter());
        broadcastOnline(conn.channel());
        try {
            conn.emitter().complete();
        } catch (IllegalStateException ignored) {
            // 이미 끝난 연결
        }
    }

    // 서버 쪽에서 연결을 끊는다 (그룹을 나갔거나 그룹이 지워졌을 때).
    // 이걸 안 하면 이미 열려 있던 연결로 시간 제한(30분)까지 그룹 채팅이 계속 간다.
    // memberId 가 null 이면 그 채널의 모든 연결을 끊는다
    public void disconnect(String channel, Long memberId) {
        Map<SseEmitter, Long> room = rooms.get(channel);
        if (room == null) {
            return;
        }
        room.forEach((emitter, owner) -> {
            if (memberId != null && !memberId.equals(owner)) {
                return;
            }
            removeEmitter(channel, emitter);
            connections.values().removeIf(conn -> conn.emitter() == emitter);
            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
                // 이미 끝난 연결
            }
        });
        broadcastOnline(channel);
    }

    // 메시지가 아닌 알림 (예: 그룹 구성원·공유 계획이 바뀌었으니 다시 그려라)
    public void broadcastEvent(String channel, String name) {
        Map<SseEmitter, Long> room = rooms.get(channel);
        if (room == null) {
            return;
        }
        for (SseEmitter emitter : room.keySet()) {
            send(channel, emitter, SseEmitter.event().name(name).data(""));
        }
    }

    public void broadcastMessage(String channel, Object message) {
        Map<SseEmitter, Long> room = rooms.get(channel);
        if (room == null) {
            return;
        }
        for (SseEmitter emitter : room.keySet()) {
            send(channel, emitter, SseEmitter.event().name("message").data(message, MediaType.APPLICATION_JSON));
        }
    }

    // 같은 회원이 탭을 여러 개 열어도 한 명으로 센다
    public int onlineCount(String channel) {
        Map<SseEmitter, Long> room = rooms.get(channel);
        return room == null ? 0 : (int) room.values().stream().distinct().count();
    }

    // 프록시가 조용한 연결을 끊지 않도록 주기적으로 주석 한 줄을 보낸다
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        rooms.forEach((channel, room) -> {
            for (SseEmitter emitter : room.keySet()) {
                send(channel, emitter, SseEmitter.event().comment("ping"));
            }
        });
    }

    private void broadcastOnline(String channel) {
        Map<SseEmitter, Long> room = rooms.get(channel);
        if (room == null) {
            return;
        }
        int count = onlineCount(channel);
        for (SseEmitter emitter : room.keySet()) {
            send(channel, emitter, SseEmitter.event().name("online").data(count));
        }
    }

    // 보내다 실패한 연결은 끊긴 것이니 지운다
    private void send(String channel, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            removeEmitter(channel, emitter);
            emitter.completeWithError(e);
        }
    }

    private void removeEmitter(String channel, SseEmitter emitter) {
        boolean[] emptied = {false};
        rooms.computeIfPresent(channel, (k, room) -> {
            room.remove(emitter);
            if (room.isEmpty()) {
                emptied[0] = true;
                return null;
            }
            return room;
        });
        // 톡방이 완전히 비면 바로 방을 지운다 (그룹 채팅은 두고 톡방만). compute 밖에서 알려 맵 잠금을 오래 잡지 않는다
        if (emptied[0] && channel.startsWith("room:")) {
            events.publishEvent(new ChatRoomEmptiedEvent(Long.valueOf(channel.substring("room:".length()))));
        }
    }
}
