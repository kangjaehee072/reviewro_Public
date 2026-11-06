// 서식 있는 글자: 조각(run) 측정·줄바꿈·SVG 렌더 · 스티커 위 직접 입력 · 서식 명령
import {STICKER, FONTS, TEXT_COLOR, TEXT_COLORS, state, el, emit, editingSticker, runsText} from './state.js';

// 조각의 서식 서명 (같은 서명끼리 합치기/캐시 키)
export function runStyle(r) {
    return (r.b ? 'b' : '') + (r.i ? 'i' : '') + (r.f || '') + (r.z || 1);
}

const charWidths = new Map();

// 글자 하나의 폭 (서식별로 측정, 캐시)
function charWidth(ch, r) {
    const key = runStyle(r) + ch;
    if (charWidths.has(key)) return charWidths.get(key);
    const m = state.measureText;
    m.setAttribute('font-size', STICKER.FONT * (r.z || 1));
    m.setAttribute('font-family', r.f === 'sans' ? FONTS.sans : FONTS.pen);
    m.setAttribute('font-weight', r.b ? 700 : 400);
    m.setAttribute('font-style', r.i ? 'italic' : 'normal');
    m.textContent = ch;
    const w = m.getComputedTextLength();
    charWidths.set(key, w);
    return w;
}

// 폭에 맞춰 줄바꿈 (한글은 공백이 없어도 글자 단위로)
export function wrapRuns(runs, maxWidth) {
    const lines = [];
    let line = [];
    let width = 0;

    function push() {
        lines.push(line);
        line = [];
        width = 0;
    }

    runs.forEach(function (r) {
        for (const ch of r.t) {
            if (ch === '\n') {
                push();
                continue;
            }
            const w = charWidth(ch, r);
            if (line.length && width + w > maxWidth) push();
            const last = line[line.length - 1];
            if (last && last.run === r) last.t += ch;
            else line.push({t: ch, run: r});
            width += w;
        }
    });
    push();
    return lines;
}

// 한 줄의 높이 (가장 큰 글자 기준)
export function lineHeight(line) {
    return STICKER.LINE * line.reduce(function (max, seg) {
        return Math.max(max, seg.run.z || 1);
    }, 1);
}

// 줄들을 SVG <text>로 그린다. top = 글상자 위쪽, 베이스라인은 줄 높이에 비례
export function addLines(g, lines, x, top, anchor) {
    const text = el('text');
    text.setAttribute('font-family', STICKER.FONT_FAMILY);
    text.setAttribute('text-anchor', anchor);
    let y = top;

    lines.forEach(function (line) {
        const lh = lineHeight(line);
        const baseline = y + lh * (STICKER.FONT / STICKER.LINE);
        const segs = line.length ? line : [{t: ' ', run: {}}];
        segs.forEach(function (seg, i) {
            const r = seg.run;
            const tspan = el('tspan');
            if (i === 0) {
                tspan.setAttribute('x', x);
                tspan.setAttribute('y', baseline);
            }
            tspan.setAttribute('font-size', STICKER.FONT * (r.z || 1));
            if (r.f === 'sans') tspan.setAttribute('font-family', FONTS.sans);
            tspan.setAttribute('fill', r.c || TEXT_COLOR);
            if (r.b) tspan.setAttribute('font-weight', 700);
            if (r.i) tspan.setAttribute('font-style', 'italic');
            const deco = (r.u ? 'underline ' : '') + (r.s ? 'line-through' : '');
            if (deco.trim()) tspan.setAttribute('text-decoration', deco.trim());
            tspan.textContent = seg.t;
            text.appendChild(tspan);
        });
        y += lh;
    });

    g.appendChild(text);
}

// ── HTML ↔ 조각 변환 ──────────────────────────────────

function escapeHtml(text) {
    return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

// 조각 배열 → 편집기 HTML
export function runsToHtml(runs, base) {
    if (!runs.length) return '';
    return runs.map(function (r) {
        // 색·크기도 저장된 JSON 에서 오므로 style 속성을 깨지 못하게 형식을 확인한다
        const color = /^(#[0-9a-fA-F]{3,8}|rgba?\([\d\s.,%]+\))$/.test(r.c || '') ? r.c : TEXT_COLOR;
        const style = ['color:' + color, 'font-size:' + (base * (Number(r.z) || 1)) + 'px'];
        if (r.b) style.push('font-weight:700');
        if (r.i) style.push('font-style:italic');
        if (r.f === 'sans') style.push('font-family:' + FONTS.sans);
        const deco = (r.u ? 'underline ' : '') + (r.s ? 'line-through' : '');
        if (deco.trim()) style.push('text-decoration:' + deco.trim());
        return '<span style="' + style.join(';') + '">' + escapeHtml(r.t).replace(/\n/g, '<br>') + '</span>';
    }).join('');
}

// rgb(...) → #RRGGBB
function toHex(color) {
    const m = /rgba?\((\d+),\s*(\d+),\s*(\d+)/.exec(color || '');
    if (!m) return TEXT_COLOR;
    return '#' + [m[1], m[2], m[3]].map(function (n) {
        return ('0' + parseInt(n, 10).toString(16)).slice(-2);
    }).join('').toUpperCase();
}

// 밑줄/취소선은 상속되지 않아 조상까지 훑는다
function decorationOf(node, root) {
    let u = false, s = false;
    for (let e = node; e && e !== root.parentElement; e = e.parentElement) {
        const d = getComputedStyle(e).textDecorationLine || '';
        if (d.includes('underline')) u = true;
        if (d.includes('line-through')) s = true;
    }
    return {u: u, s: s};
}

// 편집기 DOM → 조각 배열 (계산된 스타일 기준이라 중첩 태그도 그대로 읽힘)
export function readRuns(editor) {
    const runs = [];
    const base = parseFloat(getComputedStyle(editor).fontSize) || STICKER.FONT;
    let started = false;

    function add(run) {
        const last = runs[runs.length - 1];
        if (last && runStyle(last) === runStyle(run) && last.c === run.c
            && !!last.u === !!run.u && !!last.s === !!run.s) {
            last.t += run.t;
        } else {
            runs.push(run);
        }
    }

    function newline() {
        if (!runs.length) runs.push({t: '\n', c: TEXT_COLOR, z: 1});
        else runs[runs.length - 1].t += '\n';
    }

    function walk(node) {
        node.childNodes.forEach(function (child) {
            if (child.nodeType === 3) {
                if (!child.nodeValue) return;
                const cs = getComputedStyle(child.parentElement);
                const deco = decorationOf(child.parentElement, editor);
                add({
                    t: child.nodeValue,
                    f: /Nanum Pen/i.test(cs.fontFamily) ? undefined : 'sans',
                    b: parseInt(cs.fontWeight, 10) >= 600,
                    i: cs.fontStyle === 'italic' || cs.fontStyle === 'oblique',
                    u: deco.u,
                    s: deco.s,
                    c: toHex(cs.color),
                    z: parseFloat((parseFloat(cs.fontSize) / base).toFixed(2)) || 1
                });
                started = true;
            } else if (child.nodeName === 'BR') {
                // 빈 블록의 <br>은 블록 줄바꿈과 중복
                if (child.parentNode.childNodes.length === 1 && child.parentNode !== editor) return;
                newline();
            } else if (child.nodeType === 1) {
                if (/^(DIV|P|LI)$/.test(child.nodeName) && started) newline();
                walk(child);
            }
        });
    }

    walk(editor);
    return runs.filter(function (r) { return r.t; });
}

// ── 스티커 위 직접 입력 ──────────────────────────────

// 스티커 안에 contenteditable(foreignObject)을 놓는다
export function addInput(g, s, x, y, width, height, align) {
    const fo = el('foreignObject');
    fo.setAttribute('x', x);
    fo.setAttribute('y', y);
    fo.setAttribute('width', width);
    fo.setAttribute('height', height);

    const div = document.createElement('div');
    div.className = 'pm-sticker-input';
    div.contentEditable = 'true';
    div.style.fontFamily = STICKER.FONT_FAMILY;
    div.style.fontSize = STICKER.FONT + 'px';
    div.style.lineHeight = (STICKER.LINE / STICKER.FONT);
    div.style.textAlign = align;
    div.style.color = TEXT_COLOR;
    div.innerHTML = runsToHtml(s.runs, STICKER.FONT);
    div.addEventListener('input', function () {
        stripFontTags(div);
        s.runs = readRuns(div);
        emit('text', s);
    });

    fo.appendChild(div);
    g.appendChild(fo);
    state.editEl = div;
}

// 편집기에 포커스 (atEnd면 커서를 끝으로)
export function focusEditor(atEnd) {
    const editEl = state.editEl;
    if (!editEl) return;
    editEl.focus();
    if (!atEnd) return;
    const range = document.createRange();
    range.selectNodeContents(editEl);
    range.collapse(false);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
}

// 선택이 없으면 전체를 선택 (서식을 전체에 적용). 자동 선택했으면 true
function ensureSelection() {
    const editEl = state.editEl;
    const sel = window.getSelection();
    if (sel.rangeCount && !sel.isCollapsed && editEl.contains(sel.anchorNode)) return false;
    const range = document.createRange();
    range.selectNodeContents(editEl);
    sel.removeAllRanges();
    sel.addRange(range);
    return true;
}

// Chrome이 타이핑 중 끼워 넣는 <font size>를 풀어서 크기가 튀지 않게
function stripFontTags(editor) {
    editor.querySelectorAll('font').forEach(function (font) {
        font.replaceWith.apply(font, Array.from(font.childNodes));
    });
}

// 편집기 내용을 조각으로 다시 읽고 스티커에 알림
function afterEdit() {
    const s = editingSticker();
    if (!s || !state.editEl) return;
    stripFontTags(state.editEl);
    s.runs = readRuns(state.editEl);
    emit('text', s);
}

// execCommand 서식 적용 (굵게/기울임/밑줄/취소선/색/글꼴). 자동 전체선택이었으면 커서를 끝으로
export function applyCommand(command, value) {
    if (!state.editEl) return;
    state.editEl.focus();
    const auto = ensureSelection();
    document.execCommand('styleWithCSS', false, true);
    document.execCommand(command, false, value === undefined ? null : value);
    afterEdit();
    if (auto) focusEditor(true);
}

// 글자 크기 한 단계 키우기/줄이기 (연속 클릭 가능하도록 선택을 유지)
export function stepFontSize(dir) {
    const editEl = state.editEl;
    if (!editEl) return;
    editEl.focus();
    const auto = ensureSelection();
    const sel = window.getSelection();
    const factor = dir > 0 ? 1.2 : 1 / 1.2;

    // 전체가 선택된 경우: Chrome의 fontSize 명령이 기존 크기를 지워버리므로 조각 값을 직접 바꿔 다시 그린다
    const s = editingSticker();
    if (s && (auto || sel.toString().trim() === editEl.textContent.trim())) {
        s.runs.forEach(function (r) {
            r.z = Math.min(2.4, Math.max(0.6, parseFloat(((r.z || 1) * factor).toFixed(2))));
        });
        editEl.innerHTML = runsToHtml(s.runs, STICKER.FONT);
        emit('text', s);
        focusEditor(true);
        return;
    }

    // 선택 시작 지점의 실제 글자 노드 기준으로 현재 크기를 읽는다
    const range = sel.getRangeAt(0);
    let node = range.startContainer;
    if (node.nodeType === 1) {
        node = node.childNodes[range.startOffset] || node;
        while (node && node.nodeType === 1 && node.firstChild) node = node.firstChild;
    }
    if (node && node.nodeType === 3) node = node.parentElement;
    const current = node && editEl.contains(node)
        ? parseFloat(getComputedStyle(node).fontSize) / STICKER.FONT : 1;
    const z = Math.min(2.4, Math.max(0.6, parseFloat((current * factor).toFixed(2))));

    // execCommand는 1~7 단계만 지원해서, 7로 표시한 뒤 실제 크기로 교체
    document.execCommand('styleWithCSS', false, false);
    document.execCommand('fontSize', false, '7');
    const spans = [];
    editEl.querySelectorAll('font[size="7"]').forEach(function (font) {
        const span = document.createElement('span');
        span.style.fontSize = (STICKER.FONT * z) + 'px';
        span.innerHTML = font.innerHTML;
        span.querySelectorAll('[style*="font-size"]').forEach(function (inner) { inner.style.fontSize = ''; });
        font.replaceWith(span);
        spans.push(span);
    });

    // 노드를 바꿔치기하면 선택이 풀리므로 새 span들을 다시 선택 (연속 클릭용)
    if (spans.length) {
        const r = document.createRange();
        r.setStartBefore(spans[0]);
        r.setEndAfter(spans[spans.length - 1]);
        sel.removeAllRanges();
        sel.addRange(r);
    }
    afterEdit();
}