import { useState } from 'react';
import AiChat from '../common/AiChat.jsx';

// 지도 우측 하단 말풍선 → 지도 오른쪽에 네로 채팅창.
// 채팅창이 열리면 필터 칸 위로 덮는다 (location.css 의 z-index)
// context: 지금 왼쪽 패널 데이터(JSON 문자열). 바뀌면 다음 질문에 다시 실어 보낸다
const NERO_IDLE = '/img/char/nero-idle.png';
const NERO_THINKING = '/img/char/nero-thinking.jpg';

// 생각 중 그림은 처음 바뀌는 순간 깜빡이지 않게 미리 받아 둔다
new Image().src = NERO_THINKING;

export default function PlanAdvisor({ context }) {
    const [open, setOpen] = useState(false);
    const [thinking, setThinking] = useState(false);   // 답변을 기다리는 중

    return (
        <>
            {/* 닫아도 지우지 않고 숨기기만 한다. 지우면 대화가 날아간다 */}
            <section className="advisor-panel" hidden={!open}>
                {/* 평소엔 IDLE, 질문을 보내고 답이 오기 전까지는 THINKING. 파일이 없으면 숨긴다 */}
                <img className="advisor-illust" src={thinking ? NERO_THINKING : NERO_IDLE}
                     alt={thinking ? '네로가 생각하는 중' : '네로'}
                     onLoad={(e) => { e.currentTarget.style.display = ''; }}
                     onError={(e) => { e.currentTarget.style.display = 'none'; }} />

                <AiChat inline showHeader={false} showChips={false}
                        persona="PLANNER" icon="🧭" name="네로"
                        context={context}
                        onBusyChange={setThinking}
                        hint="일정이나 장소 목록을 보면서 휴무일, 동선, 시간 배분을 봐 드릴게요. 무엇이 궁금하세요?"
                        placeholder="예: 이 일정 동선 괜찮아?" />
            </section>

            <button type="button" className={'advisor-fab' + (open ? ' on' : '')}
                    onClick={() => setOpen(!open)}
                    aria-label={open ? '네로 닫기' : '네로에게 물어보기'}>
                {open ? '✕' : '💬'}
            </button>
        </>
    );
}
