// 스티커(폴라로이드/메모): 렌더 · 추가/삭제 · 선택 핸들 · 편집 패널
import {cfg, dom, state, STICKER, el, on, editingSticker, backgroundOf, runsText, notify} from './state.js';
import {currentView, applyView, unitPx} from './map.js';
import {wrapRuns, lineHeight, addLines, addInput, focusEditor} from './editor.js';
import {optimizeImage} from './optimize.js';

// 그림자 필터·인화지 그라데이션·레이어·측정용 텍스트를 만들고 스티커를 그린다
export function initStickers() {
    const svg = state.svg;
    svg.querySelector('defs').insertAdjacentHTML('beforeend',
        '<filter id="pm-shadow" x="-20%" y="-20%" width="140%" height="150%">' +
        '<feDropShadow dx="0.4" dy="1.4" stdDeviation="1.3" flood-color="#1E293B" flood-opacity="0.28"/>' +
        '</filter>' +
        '<linearGradient id="pm-paper" x1="0" y1="0" x2="0" y2="1">' +
        '<stop offset="0" stop-color="#FFFFFF" stop-opacity="0.35"/>' +
        '<stop offset="1" stop-color="#1E293B" stop-opacity="0.06"/>' +
        '</linearGradient>');

    state.stickerLayer = el('g');
    state.stickerLayer.classList.add('pm-stickers');
    svg.appendChild(state.stickerLayer);

    state.selectionLayer = el('g');
    state.selectionLayer.classList.add('pm-selection');
    svg.appendChild(state.selectionLayer);

    state.measureText = el('text');
    state.measureText.setAttribute('visibility', 'hidden');
    svg.appendChild(state.measureText);

    state.stickers.forEach(function (s, i) { renderSticker(s, i); });


    // 지도 화면이 바뀌면 핸들/패널 위치 갱신, 시·도 전환 시 편집 종료, 글자 입력 시 메모지 높이 갱신
    on('view', function () {
        renderSelection();
        positionStickerPanel();
    });
    on('zoom', closeStickerPanel);
    on('text', resizeEditing);
}

// 폴라로이드: 파일 선택창 열기
export function pickStickerPhoto() {
    dom.stickerFile.value = '';
    dom.stickerFile.click();
}

// 메모 추가
export function addMemo() {
    addSticker('MEMO', null);
}

// 스티커 g 요소
function stickerEl(index) {
    return state.stickerLayer.querySelector('[data-index="' + index + '"]');
}

// 위치·회전·크기 transform (중심 기준)
export function stickerTransform(s) {
    const size = stickerSize(s, 1);
    return 'translate(' + s.x + ', ' + s.y + ') rotate(' + s.rotation + ') scale(' + s.size + ') translate(' + (-size.w / 2) + ', ' + (-size.h / 2) + ')';
}

// 스티커 폭/높이 (scale 배율 적용)
function stickerSize(s, scale) {
    const k = scale === undefined ? s.size : scale;
    return {
        w: (s.type === 'MEMO' ? STICKER.MEMO_W : STICKER.W) * k,
        h: (s.type === 'MEMO' ? (s.height || STICKER.MEMO_MIN_H) : STICKER.H) * k
    };
}

// 메모지 높이 (글자 양에 맞춰)
function memoHeight(s) {
    const lines = wrapRuns(s.runs, STICKER.MEMO_W - STICKER.MEMO_PAD * 2);
    const textHeight = lines.reduce(function (sum, line) { return sum + lineHeight(line); }, 0);
    return Math.max(STICKER.MEMO_MIN_H, STICKER.MEMO_PAD * 2 + textHeight);
}

// rect 생성 도우미
function rect(attrs) {
    const r = el('rect');
    Object.keys(attrs).forEach(function (k) { r.setAttribute(k, attrs[k]); });
    return r;
}

// 메모지 (종이 + 테이프, 배경 없음이면 투명 클릭 영역만)
function renderMemo(g, s) {
    const lines = wrapRuns(s.runs, STICKER.MEMO_W - STICKER.MEMO_PAD * 2);
    s.height = memoHeight(s);
    const bg = backgroundOf(s);

    if (bg === 'none') {
        g.appendChild(rect({width: STICKER.MEMO_W, height: s.height, fill: 'transparent'}));
    } else {
        g.appendChild(rect({width: STICKER.MEMO_W, height: s.height, fill: bg, filter: 'url(#pm-shadow)'}));
        // 위쪽 마스킹테이프 한 조각
        g.appendChild(rect({
            x: STICKER.MEMO_W / 2 - 11, y: -3, width: 22, height: 6,
            transform: 'rotate(-4 ' + (STICKER.MEMO_W / 2) + ' 0)',
            fill: 'rgba(255,255,255,.55)', stroke: 'rgba(30,41,59,.08)', 'stroke-width': 0.3
        }));
    }

    if (s.editing) {
        addInput(g, s, STICKER.MEMO_PAD, STICKER.MEMO_PAD,
            STICKER.MEMO_W - STICKER.MEMO_PAD * 2, s.height - STICKER.MEMO_PAD * 2, 'left');
    } else {
        addLines(g, lines, STICKER.MEMO_PAD, STICKER.MEMO_PAD, 'start');
    }
}

// 폴라로이드 (프레임 + 그라데이션 + 사진 + 안쪽 그림자 + 캡션)
function renderPolaroid(g, s) {
    g.appendChild(rect({width: STICKER.W, height: STICKER.H, rx: 1.2, fill: backgroundOf(s), filter: 'url(#pm-shadow)'}));
    g.appendChild(rect({width: STICKER.W, height: STICKER.H, rx: 1.2, fill: 'url(#pm-paper)'}));

    const img = el('image');
    img.setAttribute('x', STICKER.PAD); img.setAttribute('y', STICKER.PAD);
    img.setAttribute('width', STICKER.PHOTO); img.setAttribute('height', STICKER.PHOTO);
    img.setAttribute('preserveAspectRatio', 'xMidYMid slice');
    img.setAttribute('href', s.url);
    g.appendChild(img);

    g.appendChild(rect({
        x: STICKER.PAD + 0.3, y: STICKER.PAD + 0.3, width: STICKER.PHOTO - 0.6, height: STICKER.PHOTO - 0.6,
        fill: 'none', stroke: 'rgba(30,41,59,.18)', 'stroke-width': 0.6
    }));

    const top = STICKER.PAD + STICKER.PHOTO + 3;
    if (s.editing) {
        addInput(g, s, STICKER.PAD, top, STICKER.W - STICKER.PAD * 2, STICKER.H - STICKER.PHOTO - STICKER.PAD * 2 - 3, 'center');
    } else {
        const lines = wrapRuns(s.runs, STICKER.W - STICKER.PAD * 2).slice(0, 2);
        addLines(g, lines, STICKER.W / 2, top, 'middle');
    }
}

// 스티커 하나를 (다시) 그린다
export function renderSticker(s, index) {
    let g = stickerEl(index);
    if (!g) {
        g = el('g');
        g.classList.add('pm-sticker');
        g.dataset.index = index;
        state.stickerLayer.appendChild(g);
    }
    g.innerHTML = '';
    if (s.type === 'MEMO') renderMemo(g, s);
    else renderPolaroid(g, s);
    g.setAttribute('transform', stickerTransform(s));
}

// 전부 다시 그린다 (순서 바뀔 때)
export function renderAllStickers() {
    state.stickerLayer.innerHTML = '';
    state.stickers.forEach(function (s, i) { renderSticker(s, i); });
}

// transform만 갱신 (드래그/핸들 중)
export function updateStickerTransform(s) {
    const g = stickerEl(state.stickers.indexOf(s));
    if (g) g.setAttribute('transform', stickerTransform(s));
}

// 화면 중앙에 새 스티커를 만들고 바로 편집
function addSticker(type, file) {
    const s = {
        id: null,
        type: type,
        url: file ? URL.createObjectURL(file) : null,
        file: file || null,
        runs: [],
        x: state.view.center.x,
        y: state.view.center.y,
        rotation: (Math.random() * 12 - 6).toFixed(1) * 1,
        size: parseFloat((currentView().width / state.fullBox.width).toFixed(2))
    };
    state.stickers.push(s);
    renderSticker(s, state.stickers.length - 1);
    openStickerPanel(state.stickers.length - 1);
}

// 스티커 삭제
function removeSticker(index) {
    const s = state.stickers[index];
    if (s.file) URL.revokeObjectURL(s.url);
    state.stickers.splice(index, 1);
    renderAllStickers();
}

// 입력하는 동안 메모지 높이만 다시 계산 (편집기는 그대로 둠)
export function resizeEditing(s) {
    if (s.type !== 'MEMO') return;
    const g = stickerEl(state.stickers.indexOf(s));
    if (!g) return;

    s.height = memoHeight(s);
    const paper = g.querySelector('rect');
    if (paper) paper.setAttribute('height', s.height);
    const fo = g.querySelector('foreignObject');
    if (fo) fo.setAttribute('height', s.height - STICKER.MEMO_PAD * 2);
    g.setAttribute('transform', stickerTransform(s));
    renderSelection();
    positionStickerPanel();
}

// 스티커가 너무 작으면 편집할 수 있게 지도를 당겨준다
function zoomForEditing(s) {
    const rect = dom.map.getBoundingClientRect();
    const h = stickerSize(s).h;
    const onScreen = h / currentView().height * rect.height;
    if (onScreen < 140) {
        state.view.zoom = Math.min(4, parseFloat((state.view.zoom * (200 / onScreen)).toFixed(2)));
    }
    state.view.center.x = s.x;
    state.view.center.y = s.y;
    applyView();
}

// ── 선택 핸들 ────────────────────────────────────────

// 선택 테두리 + 모서리 크기 핸들 + 회전 핸들 (화면 크기 고정)
export function renderSelection() {
    const layer = state.selectionLayer;
    if (!layer) return;
    layer.innerHTML = '';
    const s = editingSticker();
    if (!s || !s.editing) return;

    const u = unitPx();
    const size = stickerSize(s);
    const w = size.w, h = size.h;
    const g = el('g');
    g.setAttribute('transform', 'translate(' + s.x + ', ' + s.y + ') rotate(' + s.rotation + ')');

    g.appendChild(rect({
        x: -w / 2 - 2 * u, y: -h / 2 - 2 * u, width: w + 4 * u, height: h + 4 * u,
        fill: 'none', stroke: '#1E6FF4', 'stroke-width': 1.5 * u,
        'stroke-dasharray': (4 * u) + ' ' + (3 * u), 'pointer-events': 'none'
    }));

    const stem = el('line');
    stem.setAttribute('x1', 0); stem.setAttribute('y1', -h / 2 - 2 * u);
    stem.setAttribute('x2', 0); stem.setAttribute('y2', -h / 2 - 18 * u);
    stem.setAttribute('stroke', '#1E6FF4'); stem.setAttribute('stroke-width', 1.2 * u);
    stem.setAttribute('pointer-events', 'none');
    g.appendChild(stem);

    const knob = function (cx, cy, r, handle, cursor) {
        const c = el('circle');
        c.setAttribute('cx', cx); c.setAttribute('cy', cy); c.setAttribute('r', r);
        c.setAttribute('fill', '#FFFFFF');
        c.setAttribute('stroke', '#1E6FF4'); c.setAttribute('stroke-width', 1.5 * u);
        c.dataset.handle = handle;
        c.style.cursor = cursor;
        g.appendChild(c);
    };
    knob(0, -h / 2 - 24 * u, 6 * u, 'rotate', 'grab');
    [[-1, -1], [1, -1], [-1, 1], [1, 1]].forEach(function (c) {
        knob(c[0] * (w / 2 + 2 * u), c[1] * (h / 2 + 2 * u), 5 * u, 'resize', c[0] * c[1] < 0 ? 'nesw-resize' : 'nwse-resize');
    });

    layer.appendChild(g);
}

// ── 편집 패널 ────────────────────────────────────────

// 패널 닫기 (편집 종료, 빈 메모는 삭제)
export function closeStickerPanel() {
    state.panelEl = null;
    const s = editingSticker();
    state.panelIndex = -1;
    state.editEl = null;
    if (state.selectionLayer) state.selectionLayer.innerHTML = '';
    if (s && s.editing) {
        s.editing = false;
        const index = state.stickers.indexOf(s);
        if (s.type === 'MEMO' && !runsText(s.runs).trim()) removeSticker(index);
        else renderSticker(s, index);
    }
    notify();
}

// 패널을 스티커 위(공간 없으면 아래)에 붙인다
export function positionStickerPanel() {
    const panel = state.panelEl;
    if (!panel || state.panelIndex < 0) return;
    const g = stickerEl(state.panelIndex);
    if (!g) return;
    const stageBox = dom.map.parentElement.getBoundingClientRect();
    const box = g.getBoundingClientRect();
    const w = panel.offsetWidth;
    const h = panel.offsetHeight;

    let left = box.left - stageBox.left + box.width / 2 - w / 2;
    left = Math.min(Math.max(8, left), stageBox.width - w - 8);

    // 회전 핸들 자리를 비워두고 그 위에
    let top = box.top - stageBox.top - h - 42;
    let below = false;
    if (top < 8) {
        top = box.bottom - stageBox.top + 14;
        below = true;
    }
    top = Math.min(Math.max(8, top), stageBox.height - h - 8);

    panel.classList.toggle('below', below);
    panel.style.left = left + 'px';
    panel.style.top = top + 'px';
}


// 스티커 편집 시작: 패널 + 핸들 + 직접 입력
export function openStickerPanel(index) {
    const s = state.stickers[index];
    if (!s || !cfg.editable) return;
    closeStickerPanel();

    state.panelIndex = index;
    s.editing = true;
    renderSticker(s, index);
    zoomForEditing(s);
    renderSelection();
    notify();
    focusEditor(true);
}

// 상단 바의 폴라로이드/메모 버튼 연결
export function bindStickerButtons() {
    dom.stickerFile.addEventListener('change', async function () {
        const file = dom.stickerFile.files[0];
        if (file) addSticker('POLAROID', await optimizeImage(file));
    });
}

// 편집 중 스티커 배경색 변경
export function setStickerBackground(color) {
    const s = editingSticker();
    if (!s) return;
    s.bg = color;
    renderSticker(s, state.panelIndex);
    renderSelection();
    focusEditor(true);
    notify();
}

// 맨 앞으로
export function bringToFront() {
    const s = editingSticker();
    if (!s) return;
    s.editing = false;
    state.stickers.push(state.stickers.splice(state.panelIndex, 1)[0]);
    renderAllStickers();
    openStickerPanel(state.stickers.length - 1);
}

// 삭제 (확인 후)
export function deleteEditingSticker() {
    const s = editingSticker();
    if (!s) return;
    const index = state.panelIndex;
    s.editing = false;
    closeStickerPanel();
    confirmModal({ title: (s.type === 'MEMO' ? '메모' : '폴라로이드') + ' 삭제', message: '지도에서 떼어낼까요?', confirmText: '삭제', danger: true })
        .then(function (ok) { if (ok) removeSticker(index); });
}
