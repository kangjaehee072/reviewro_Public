// 지역 톡방·그룹 채팅 공용 SSE 채팅.
// 화면에 #chatLog, #chatForm, #chatInput, #onlineCount, #chatStatus 가 있어야 한다.
// base 아래 /stream 으로 받고, /messages 로 보낸다. closeBase + 연결번호로 연결을 끊는다.
// 페이지가 다른 이벤트(예: refresh)를 더 받거나 나가기 버튼을 달 수 있게 {source, leave} 를 돌려준다
function initSseChat(base, myId, closeBase) {
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    const log = document.getElementById('chatLog');
    const form = document.getElementById('chatForm');
    const input = document.getElementById('chatInput');
    const online = document.getElementById('onlineCount');
    const status = document.getElementById('chatStatus');

    log.scrollTop = log.scrollHeight;

    function pad(n) {
        return String(n).padStart(2, '0');
    }

    // 메시지는 사용자가 쓴 글이라 textContent 로만 넣는다
    function appendMessage(msg) {
        document.getElementById('chatEmpty')?.remove();
        const atBottom = log.scrollHeight - log.scrollTop - log.clientHeight < 60;

        const item = document.createElement('div');
        item.className = 'chat-item' + (msg.memberId === myId ? ' mine' : '');

        const meta = document.createElement('div');
        meta.className = 'chat-meta';
        const name = document.createElement('strong');
        name.textContent = msg.chatRoomMember;
        const time = document.createElement('time');
        const d = new Date(msg.chatRoomSentAt);
        time.textContent = pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
        meta.append(name, time);

        const body = document.createElement('div');
        body.className = 'chat-msg chat-msg-other';
        body.textContent = msg.chatRoomContent;

        item.append(meta, body);
        log.appendChild(item);
        if (atBottom || msg.memberId === myId) {
            log.scrollTop = log.scrollHeight;
        }
    }

    // 실시간 수신. 끊기면 EventSource 가 알아서 다시 붙는다
    const source = new EventSource(base + '/stream');
    source.addEventListener('open', () => {
        status.textContent = '';
        status.classList.remove('off');
    });
    source.addEventListener('message', e => appendMessage(JSON.parse(e.data)));
    source.addEventListener('online', e => {
        online.textContent = e.data + '명 접속 중';
    });
    source.addEventListener('error', () => {
        if (source.readyState === EventSource.CLOSED) return;
        status.textContent = '연결이 끊겼어요. 다시 연결하는 중…';
        status.classList.add('off');
    });

    // 서버가 준 연결 번호. 나갈 때 이 번호로 끊어 달라고 해야 접속 인원이 바로 줄어든다
    let connId = null;
    source.addEventListener('hello', e => {
        connId = e.data;
    });

    function leave() {
        source.close();
        if (!connId) return;
        // 페이지가 닫히는 중에도 보내지도록 sendBeacon 을 쓴다 (헤더를 못 넣어서 CSRF 토큰은 본문에)
        navigator.sendBeacon(closeBase + encodeURIComponent(connId),
            new URLSearchParams({_csrf: csrfToken}));
        connId = null;
    }

    window.addEventListener('pagehide', leave);

    // 보내기. 내 메시지도 SSE 로 돌아와서 그때 그린다
    form.addEventListener('submit', async e => {
        e.preventDefault();
        const text = input.value.trim();
        if (!text) return;
        input.value = '';
        input.focus();

        const res = await fetch(base + '/messages', {
            method: 'POST',
            headers: {'Content-Type': 'application/json', [csrfHeader]: csrfToken},
            body: JSON.stringify({chatRoomContent: text})
        });
        if (!res.ok) {
            const body = await res.json().catch(() => ({}));
            alert(body.message || '메시지를 보내지 못했어요.');
            input.value = text;
        }
    });

    // 뒤로가기로 돌아오면(bfcache) 연결이 끊긴 채라 새로 불러온다
    window.addEventListener('pageshow', e => {
        if (e.persisted) location.reload();
    });

    return {source, leave};
}
