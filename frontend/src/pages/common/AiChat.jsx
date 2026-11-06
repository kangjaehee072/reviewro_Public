import { useEffect, useRef, useState } from 'react';
import { chat } from '../../api/ai.js';
import ChatText from './ChatText.jsx';

// 서버가 30개까지만 받는다 (AiController.MAX_MESSAGES).
// 한 번 물어보면 user + assistant(도구호출) + tool + assistant 로 최대 4개가 쌓인다
const MAX_MESSAGES = 28;

// 지금 보고 있는 장소를 AI 에게 알려줄 때 쓰는 필드만 추림
const trim = (p) => ({
    locationId: p.locationId,
    locationName: p.locationName,
    locationAddress: p.locationAddress,
    locationDescription: p.locationDescription,
    locationReviewAvg: p.locationReviewAvg
});

// 길어진 대화를 자른다. 자르고 나면 프롬프트 캐시가 한 번 깨지지만,
// 넘겨서 400 을 맞는 것보다는 낫다. 도구 결과만 남고 질문이 잘리면
// 모델이 혼란해지므로 맨 앞이 user 가 될 때까지 더 버린다
const fit = (history) => {
    if (history.length <= MAX_MESSAGES) return history;
    let cut = history.slice(-MAX_MESSAGES);
    while (cut.length > 0 && cut[0].role !== 'user') cut = cut.slice(1);
    return cut;
};

// 랜덤(파트라슈) 기본값. 사주 페이지는 props 로 페르소나와 문구를 바꿔 쓴다
//   starter     열자마자 자동으로 보낼 첫 메시지 {content, display} (display: null 이면 화면에 안 보임)
//   defaultOpen 처음부터 열어둘지
//   onPlaces    AI 가 찾은 장소 목록 (지도 마커용)
//   inline      말풍선 버튼 없이 화면 안에 항상 펼쳐 둔다 (사주 페이지)
//   showHeader  위쪽 "아이콘 + 이름" 줄. 옆에 일러스트가 있으면 중복이라 끈다
//   showChips   채팅 아래 장소 칩. 화면 다른 쪽에 장소 목록이 있으면 중복이라 끈다
//   initialMessages 이미 오간 대화 (화면엔 숨기고 다음 질문의 맥락으로만 쓴다)
//   context     지금 화면에 보이는 데이터(문자열). 바뀌었을 때만 다음 질문 앞에 실어 보낸다 (장소 페이지 네로)
//   onBusyChange 답변을 기다리는 중인지 알려준다 (일러스트를 생각 중 그림으로 바꿀 때)
export default function AiChat({
    place, onPick, onPlaces, context,
    persona = 'HOTPLACE',
    icon = '🐶',
    name = '파트라슈',
    hint = '"부산에 조용한 카페 없나?" 처럼 물어보세요. 지역을 알려주시면 제가 찾아봐요.',
    placeholder = '어떤 곳을 찾고 있어요?',
    starter,
    defaultOpen = false,
    inline = false,
    showHeader = true,
    showChips = true,
    initialMessages = [],
    onBusyChange
}) {
    const [open, setOpen] = useState(defaultOpen || inline);
    const [messages, setMessages] = useState(initialMessages);   // {role, content, tool_calls?, tool_name?, display?}
    const [places, setPlaces] = useState([]);       // 서버가 검색해 내려준 장소 (칩)
    const [busy, setBusy] = useState(false);
    useEffect(() => { onBusyChange?.(busy); }, [busy]);
    const [error, setError] = useState('');
    const [input, setInput] = useState('');
    const bottom = useRef(null);
    const injected = useRef(null);                  // 어느 장소를 이미 알려줬는지
    const injectedContext = useRef(null);           // 어느 화면 데이터를 이미 알려줬는지

    // history 를 인자로 받는다. state 를 읽으면 한 박자 늦은 값이 잡힌다.
    // 대화에 덧붙이기만 하는 것이 중요하다 — 앞이 그대로여야 Ollama 프롬프트 캐시가 산다
    const send = async (history) => {
        setBusy(true);
        setError('');

        // 칩은 늘 "가장 최근 검색 결과" 여야 한다.
        // 안 비우면 AI 가 되묻기만 한 턴에서 지난 검색 결과가 그대로 남아,
        // 서울 칩을 띄운 채 부산 얘기를 하는 상태가 된다
        setPlaces([]);

        const appended = [];                        // 서버가 대화에 붙인 도구 메시지
        try {
            // display 같은 화면 전용 필드는 서버로 보내지 않는다
            const payload = history.map(({ display, ...m }) => m);

            // 글자를 흘려 보여주지 않고 완성된 답만 띄운다 (중간에 도구 호출 JSON·덜 닫힌 ** 가 보이지 않게)
            const full = await chat(persona, payload, {
                places: (list) => {
                    const found = Array.isArray(list) ? list : [];
                    setPlaces(found);
                    onPlaces?.(found);
                },
                // 다음 턴에 그대로 되돌려줘야 캐시가 살아난다. 화면에는 숨긴다
                context: (added) => appended.push(...added.map((m) => ({ ...m, display: null })))
            });
            if (!full) return;                      // 401 → 로그인 페이지로 이동함

            setMessages([...history, ...appended, { role: 'assistant', content: full }]);
        } catch (e) {
            setError(e.message);
        } finally {
            setBusy(false);
        }
    };

    // 첫 메시지 자동 전송 (사주 페이지). 랜덤은 starter 가 없어서 사용자가 보낼 때만 돈다
    useEffect(() => {
        if (open && starter && messages.length === 0 && !busy) {
            const first = [{ role: 'user', ...starter }];
            setMessages(first);
            send(first);
        }
    }, [open]);

    const submit = (e) => {
        e.preventDefault();
        const text = input.trim();
        if (!text || busy) return;

        // 뽑힌 장소가 바뀌었으면, 이번 발화에 그 장소 정보를 같이 실어 보낸다
        // (화면에는 사용자가 쓴 문장만 보이고, 서버로는 장소 JSON 이 앞에 붙는다)
        let msg = { role: 'user', content: text };
        if (place && injected.current !== place.locationId) {
            injected.current = place.locationId;
            msg = {
                role: 'user',
                content: '지금 사용자가 보고 있는 장소야.\n' + JSON.stringify(trim(place))
                       + '\n\n' + text,
                display: text
            };
        }
        // 화면 데이터도 같은 방식. 그대로면 다시 안 보낸다 (대화가 불어나지 않게).
        // 다만 대화가 길어 앞을 잘라낼 땐 처음 실어 보낸 데이터도 같이 잘리므로 다시 싣는다
        if (messages.length + 1 > MAX_MESSAGES) injectedContext.current = null;
        if (context && injectedContext.current !== context) {
            injectedContext.current = context;
            msg = {
                role: 'user',
                content: '[현재 화면 데이터]\n' + context + '\n\n' + msg.content,
                display: text
            };
        }

        const next = fit([...messages, msg]);
        setMessages(next);
        setInput('');
        send(next);
    };

    // 칩 클릭 → 지도 이동. 도구 결과에는 좌표가 없으니 눌렀을 때만 가져온다
    const openPlace = async (locationId) => {
        const res = await fetch('/api/locations/' + locationId);
        if (res.ok) onPick(await res.json());
    };

    // 새 글이 오면 맨 아래로 (닫혀 있으면 ref 가 없으므로 ?. 로 막는다)
    useEffect(() => {
        bottom.current?.scrollIntoView({ block: 'end' });
    }, [messages, busy, open]);

    // 화면에 보일 메시지만 (display 가 null 이면 숨은 메시지)
    const visible = messages.filter((m) => m.display !== null);

    return (
        <>
            {/* 우측 하단 말풍선 버튼 (inline 이면 항상 펼쳐 두므로 없음) */}
            {!inline && (
                <button type="button" className={'fab' + (open ? ' on' : '')}
                        onClick={() => setOpen(!open)}
                        aria-label={open ? name + ' 닫기' : name + '에게 물어보기'}>
                    {open ? '✕' : '💬'}
                    {!open && busy && <span className="fab-dot" />}
                </button>
            )}

            {/* 토글되는 채팅 모달 */}
            {open && (
                <section className={inline ? 'chat-inline' : 'float-panel'}>
                    {showHeader && (
                        <header className="card-head" style={{ display: "flex", alignItems: "center", gap: 10 }}>
                            <span className="chat-dog">{icon}</span>
                            <strong>{name}</strong>
                        </header>
                    )}

                    <div className="chat-log">
                        {visible.length === 0 && !busy && <p className="hint">{hint}</p>}

                        {visible.map((m, i) => (
                            <div key={i} className={'chat-msg chat-msg-' + (m.role === 'user' ? 'mine' : 'other')}>
                                {m.role === 'user' ? (m.display ?? m.content) : <ChatText text={m.content} />}
                            </div>
                        ))}

                        {busy && (
                            <div className="chat-msg chat-msg-other chat-typing">
                                <span /><span /><span />
                            </div>
                        )}
                        <div ref={bottom} />
                    </div>

                    {showChips && places.length > 0 && (
                        <div className="chip-group" style={{ padding: 12 }}>
                            {places.map((p) => (
                                <button key={p.locationId} type="button" className="chip"
                                        onClick={() => openPlace(p.locationId)}>
                                    📍 {p.name}
                                </button>
                            ))}
                        </div>
                    )}

                    {error && <p className="notice notice-error">{error}</p>}

                    <form className="chat-form" onSubmit={submit}>
                        <input value={input} onChange={(e) => setInput(e.target.value)}
                               placeholder={placeholder} disabled={busy} />
                        <button type="submit" disabled={busy || !input.trim()}>보내기</button>
                    </form>
                </section>
            )}
        </>
    );
}
