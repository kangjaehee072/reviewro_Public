// AI 답변의 간단한 마크다운만 그린다 (굵게, 제목, 목록).
// HTML 로 끼워 넣지 않고 React 요소로 만들어서 답변에 태그가 섞여 와도 안전하다.
// 줄바꿈은 .chat-msg 의 white-space: pre-wrap 이 처리한다
// 장소가 새로 시작하는 줄 — 그 앞에 틈을 준다.
// 장소 이름만 **굵게** 나오므로 그걸로 가린다. 번호 목록도 같이 본다.
// 아래 딸린 주소·설명에는 굵은 표시가 없어서 안 걸린다
const LEAD = /^\s*\d{1,2}\.\s|\*\*/;

// 모델이 화살표 같은 기호를 LaTeX 로 쓸 때가 있다 ($\rightarrow$). 화면엔 그냥 기호로 보여준다
const LATEX = {
    rightarrow: '→', to: '→', leftarrow: '←', Rightarrow: '⇒', Leftarrow: '⇐',
    leftrightarrow: '↔', uparrow: '↑', downarrow: '↓',
    times: '×', cdot: '·', approx: '≈', sim: '~', le: '≤', ge: '≥', neq: '≠', pm: '±'
};

function stripLatex(text) {
    return text
        .replace(/\\(rightarrow|to|leftarrow|Rightarrow|Leftarrow|leftrightarrow|uparrow|downarrow|times|cdot|approx|sim|le|ge|neq|pm)(?![a-zA-Z])/g,
                 (_, name) => LATEX[name])
        .replace(/\$([^$\n]{1,40})\$/g, '$1');   // 남은 $ … $ 껍데기만 벗긴다 (금액 "$10" 같은 홑 $ 는 안 건드림)
}

export default function ChatText({ text }) {
    const lines = stripLatex(text).split('\n');
    return lines.map((line, i) => (
        <span key={i} className={i > 0 && LEAD.test(line) ? 'ct-lead' : undefined}>
            {renderLine(line)}
            {i < lines.length - 1 && '\n'}
        </span>
    ));
}


// "### 제목" → 굵은 줄, "- 항목" / "* 항목" → "• 항목"
function renderLine(line) {
    const heading = line.match(/^\s*#{1,6}\s+(.*)$/);
    if (heading) return <strong>{renderInline(heading[1])}</strong>;
    return renderInline(line.replace(/^(\s*)[-*]\s+/, '$1• '));
}

// **굵게** → <strong>. 짝이 안 맞아 남은 * 표시는 지운다
function renderInline(text) {
    return text.split(/\*\*(.+?)\*\*/g).map((part, i) =>
        i % 2 === 1 ? <strong key={i}>{part}</strong> : part.replace(/\*+/g, ''));
}
