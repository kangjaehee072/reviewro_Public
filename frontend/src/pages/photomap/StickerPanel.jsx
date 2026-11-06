import { useLayoutEffect, useRef } from 'react';
import { usePhotoMap } from './core/store.js';
import { state, BACKGROUNDS, TEXT_COLORS, TEXT_COLOR, FONTS, backgroundOf } from './core/state.js';
import { applyCommand, stepFontSize } from './core/editor.js';
import { positionStickerPanel, closeStickerPanel, setStickerBackground, bringToFront, deleteEditingSticker } from './core/stickers.js';

// 버튼 누를 때 스티커 글자 선택이 풀리지 않게
const keep = (e) => e.preventDefault();

const ICONS = {
    front: <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round"><path d="M8 2.5 14 6 8 9.5 2 6z" /><path d="M2 10l6 3.5 6-3.5" /></svg>,
    trash: <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"><path d="M3 4.5h10M6.5 4.5V3h3v1.5M4.5 4.5l.7 8.5h5.6l.7-8.5" /></svg>,
    check: <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 8.5l3.2 3L13 4.5" /></svg>
};

const FORMATS = [
    ['bold', '굵게', <b>B</b>],
    ['italic', '기울임', <i>I</i>],
    ['underline', '밑줄', <u>U</u>],
    ['strikeThrough', '취소선', <s>S</s>]
];

// 색 동그라미
function Swatch({ color, on, onPick }) {
    const none = color === 'none';
    return (
        <button type="button"
                className={'swatch' + (none ? ' none' : '') + (on ? ' on' : '')}
                title={none ? '배경 없음' : color}
                style={none ? undefined : { background: color }}
                onMouseDown={keep}
                onClick={onPick} />
    );
}

export default function StickerPanel() {
    const s = usePhotoMap();
    const ref = useRef(null);
    const sticker = s.stickers[s.panelIndex];

    // 그려진 뒤 코어에 패널 요소를 알려주고 위치 잡기
    useLayoutEffect(() => {
        state.panelEl = ref.current;
        if (ref.current) positionStickerPanel();
    });

    if (!sticker || !sticker.editing) return null;
    const bg = backgroundOf(sticker);

    return (
        <div className="pm-popover" ref={ref}>
            <div className="pop-row">
                {FORMATS.map(([cmd, title, label]) => (
                    <button key={cmd} type="button" title={title} onMouseDown={keep} onClick={() => applyCommand(cmd)}>
                        {label}
                    </button>
                ))}
                <span className="sep" />
                <button type="button" data-font="-" title="글자 작게" onMouseDown={keep} onClick={() => stepFontSize(-1)}>A<small>−</small></button>
                <button type="button" data-font="+" title="글자 크게" onMouseDown={keep} onClick={() => stepFontSize(1)}>A<small>+</small></button>
                <span className="sep" />
                <select title="글꼴" onChange={(e) => applyCommand('fontName', FONTS[e.target.value])}>
                    <option value="pen">손글씨</option>
                    <option value="sans">기본</option>
                </select>
            </div>

            <div className="pop-row">
                <span>글자</span>
                <div className="swatches">
                    {TEXT_COLORS.map((c) => (
                        <Swatch key={c} color={c} onPick={() => applyCommand('foreColor', c)} />
                    ))}
                </div>
                <label className="swatch custom" title="다른 색">+
                    <input type="color" defaultValue={TEXT_COLOR} onInput={(e) => applyCommand('foreColor', e.target.value)} />
                </label>
            </div>

            <div className="pop-row">
                <span>배경</span>
                <div className="swatches">
                    {BACKGROUNDS[sticker.type].map((c) => (
                        <Swatch key={c} color={c} on={c === bg} onPick={() => setStickerBackground(c)} />
                    ))}
                </div>
                <span className="sep" />
                <button type="button" className="icon" title="맨 앞으로" onClick={bringToFront}>{ICONS.front}</button>
                <button type="button" className="icon danger" title="삭제" onClick={deleteEditingSticker}>{ICONS.trash}</button>
                <button type="button" className="icon ok" title="완료" onClick={closeStickerPanel}>{ICONS.check}</button>
            </div>
        </div>
    );
}
