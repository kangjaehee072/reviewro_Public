function csrf() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    return token && header ? { [header.content]: token.content } : {};
}

// 대화를 보내고 서버가 흘려주는 이벤트를 처리한다.
// on = { token, places, context } — 필요한 것만 넘기면 된다.
//   token    글자가 올 때마다 (지금까지 받은 전체 문장)
//   places   서버가 도구로 검색한 장소 목록 (칩·지도에 쓴다)
//   context  서버가 대화에 덧붙인 메시지들. 다음 턴에 그대로 되돌려줘야 프롬프트 캐시가 산다
// 돌려주는 값은 완성된 답변 문장.
export async function chat(persona, messages, on = {}) {
    const res = await fetch('/api/ai/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...csrf() },
        body: JSON.stringify({ persona, messages })
    });

    if (res.status === 401) {
        // 로그인하고 나면 보던 화면으로 돌아오게 한다
        window.location.href = '/login?next=' + encodeURIComponent(location.pathname + location.search + location.hash);
        return '';
    }
    if (res.status === 429) throw new Error('요청이 너무 잦아요. 잠시 후 다시 시도해 주세요.');
    if (!res.ok) throw new Error('AI 응답을 받지 못했습니다.');

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';     // 아직 덜 온 조각
    let full = '';       // 지금까지 받은 전체 문장

    while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // 빈 줄 하나가 이벤트 하나의 끝이다
        const blocks = buffer.split('\n\n');
        buffer = blocks.pop();     // 마지막 조각은 아직 안 끝났을 수 있다

        for (const block of blocks) {
            let name = '';
            let data = '';
            for (const line of block.split('\n')) {
                if (line.startsWith('event:')) name = line.slice(6).trim();
                if (line.startsWith('data:')) data += line.slice(5).trim();
            }
            if (!name) continue;

            if (name === 'token') {
                full += JSON.parse(data);          // "안녕" -> 안녕
                on.token?.(full);
            } else if (name === 'reset') {
                // 도구를 부르기 전에 흘린 앞머리는 버린다. 2라운드가 답을 새로 만든다
                full = '';
                on.token?.('');
            } else if (name === 'places') {
                on.places?.(safeParse(data));      // 검색 결과가 없으면 문장이 올 수도 있다
            } else if (name === 'context') {
                on.context?.(safeParse(data) ?? []);
            } else if (name === 'error') {
                throw new Error(data);
            }
        }
    }
    return full;
}

// 서버가 JSON 대신 안내 문장을 보낼 때도 있어서 터지지 않게 감싼다
function safeParse(text) {
    try {
        return JSON.parse(text);
    } catch {
        return null;
    }
}
