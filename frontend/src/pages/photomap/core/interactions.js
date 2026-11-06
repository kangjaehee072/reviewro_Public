// 포인터(마우스·터치)/휠 통합 처리: 시·도 확대 · 사진 드래그/확대 · 스티커 드래그/핸들 · 지도 이동/확대
import {cfg, dom, state, keyOf, nameOf, editingSticker} from './state.js';
import {svgPoint, unitPx, zoomTo, zoomBy, panTo, applyTransform, openRegionModal, pickPhoto, toggleSelect, setBusy} from './map.js';
import {updateStickerTransform, renderSelection, resizeEditing, positionStickerPanel, openStickerPanel, closeStickerPanel} from './stickers.js';
import {focusEditor} from './editor.js';

// 드래그 한 번의 상태
const drag = {
    down: false,
    moved: false,
    screenX: 0, screenY: 0,          // mousedown 화면 좌표
    startX: 0, startY: 0,            // 대상 원점 보정값
    panX: 0, panY: 0,                // 팬 시작 시 화면 중심
    region: null, img: null,         // 구 사진 드래그
    sticker: null,                   // 스티커 드래그
    inputEl: null,                   // 편집 중인 글자칸에서 시작한 드래그면 그 칸
    handle: null, handleStart: null, // 회전/크기 핸들
    path: null                       // 클릭 대상 구
};

// 지도 좌표 기준 사진 요소
function photoImg(key) {
    return state.svg.querySelector('.pm-photos [data-code="' + key + '"]');
}

// 핸들 드래그 → 회전/크기
function dragHandle(pt) {
    const s = editingSticker();
    if (!s) return;
    if (drag.handle === 'rotate') {
        const angle = Math.atan2(pt.y - s.y, pt.x - s.x) * 180 / Math.PI;
        let rot = drag.handleStart.rotation + (angle - drag.handleStart.angle);
        rot = ((rot + 180) % 360 + 360) % 360 - 180;
        if (Math.abs(rot) < 3) rot = 0;
        s.rotation = Math.round(rot * 10) / 10;
    } else {
        const dist = Math.hypot(pt.x - s.x, pt.y - s.y);
        s.size = Math.min(4, Math.max(0.2, parseFloat((drag.handleStart.size * dist / drag.handleStart.dist).toFixed(2))));
    }
    updateStickerTransform(s);
    renderSelection();
    positionStickerPanel();
}

// 툴팁 갱신용 캐시
let tooltipCode = null;
let tooltipRect = null;

// pointermove: 드래그 중이면 대상 이동, 아니면 툴팁
function onMove(e) {
    if (!e.isPrimary) return;
    if (drag.down) {
        if (!drag.moved) {
            if (Math.hypot(e.clientX - drag.screenX, e.clientY - drag.screenY) <= 4) return;
            drag.moved = true;
            setBusy(true);
            // 글자칸에서 끌기 시작했으면 이제부터는 글자 선택이 아니라 스티커 이동
            if (drag.inputEl) {
                drag.inputEl.style.userSelect = 'none';
                drag.inputEl.blur();
                window.getSelection()?.removeAllRanges();
                state.svg.setPointerCapture(e.pointerId);
            }
        }

        if (drag.handle) {
            dragHandle(svgPoint(e));
        } else if (drag.sticker) {
            const pt = svgPoint(e);
            drag.sticker.x = pt.x - drag.startX;
            drag.sticker.y = pt.y - drag.startY;
            updateStickerTransform(drag.sticker);
            renderSelection();
        } else if (drag.region && drag.img) {
            const pt = svgPoint(e);
            drag.region.offsetX = pt.x - drag.startX;
            drag.region.offsetY = pt.y - drag.startY;
            applyTransform(drag.img, drag.region);
        } else {
            // 빈 곳 드래그 → 지도 이동
            const unit = unitPx();
            panTo(drag.panX - (e.clientX - drag.screenX) * unit, drag.panY - (e.clientY - drag.screenY) * unit);
        }
        return;
    }

    // 툴팁은 마우스일 때만 (터치는 hover 가 없다)
    if (e.pointerType !== 'mouse') return;
    const path = e.target.closest('.pm-region');
    if (!path) {
        dom.tooltip.hidden = true;
        tooltipCode = null;
        return;
    }
    // 같은 구 위에서는 글자를 다시 쓰지 않는다 (마우스를 움직일 때마다 레이아웃이 다시 잡히는 것을 막는다)
    if (path.dataset.code !== tooltipCode) {
        tooltipCode = path.dataset.code;
        const province = path.closest('.pm-province');
        dom.tooltip.textContent = state.mode === 'nation'
            ? province.dataset.name
            : province.dataset.name + ' ' + nameOf(keyOf(path.dataset.code));
        dom.tooltip.hidden = false;
    }
    if (!tooltipRect) tooltipRect = dom.map.getBoundingClientRect();
    dom.tooltip.style.transform =
        'translate(' + (e.clientX - tooltipRect.left + 14) + 'px, ' + (e.clientY - tooltipRect.top + 14) + 'px)';
}

// pointerdown: 무엇을 잡았는지 정한다 (핸들 > 스티커 > 구 사진 > 빈 곳)
function onDown(e) {
    if (!e.isPrimary || e.button !== 0) return;

    // 편집 중인 글자칸 위: 그냥 누르면 글자 입력(커서 놓기), 끌면 스티커 이동.
    // 메모는 거의 전부가 글자칸이라 여기서 끌기를 막으면 편집 중엔 옮길 수가 없다.
    // 기본 동작(커서 놓기)을 살리려고 preventDefault 와 포인터 캡처는 실제로 끌기 시작할 때 한다 (onMove)
    const inputEl = e.target.closest('.pm-sticker-input');
    if (inputEl) {
        const stickerEl = inputEl.closest('.pm-sticker');
        if (!stickerEl || !cfg.editable) return;
        drag.down = true;
        drag.moved = false;
        drag.screenX = e.clientX;
        drag.screenY = e.clientY;
        drag.path = null;
        drag.inputEl = inputEl;
        drag.sticker = state.stickers[parseInt(stickerEl.dataset.index, 10)];
        const pt = svgPoint(e);
        drag.startX = pt.x - drag.sticker.x;
        drag.startY = pt.y - drag.sticker.y;
        return;
    }

    e.preventDefault();
    // 손가락이 지도 밖으로 나가도 move/up 을 계속 받는다
    state.svg.setPointerCapture(e.pointerId);

    drag.down = true;
    drag.moved = false;
    drag.screenX = e.clientX;
    drag.screenY = e.clientY;
    drag.panX = state.view.center.x;
    drag.panY = state.view.center.y;
    drag.path = e.target.closest('.pm-region');

    const handleEl = e.target.closest('[data-handle]');
    const editing = editingSticker();
    if (handleEl && editing) {
        const pt = svgPoint(e);
        drag.handle = handleEl.dataset.handle;
        drag.handleStart = {
            size: editing.size,
            rotation: editing.rotation,
            dist: Math.hypot(pt.x - editing.x, pt.y - editing.y) || 1,
            angle: Math.atan2(pt.y - editing.y, pt.x - editing.x) * 180 / Math.PI
        };
        drag.moved = true;
        drag.path = null;
        return;
    }

    const stickerEl = e.target.closest('.pm-sticker');
    if (!stickerEl) closeStickerPanel();
    if (stickerEl && cfg.editable) {
        drag.sticker = state.stickers[parseInt(stickerEl.dataset.index, 10)];
        const pt = svgPoint(e);
        drag.startX = pt.x - drag.sticker.x;
        drag.startY = pt.y - drag.sticker.y;
        drag.path = null;
        return;
    }
    if (!drag.path) return;

    const key = keyOf(drag.path.dataset.code);
    const img = photoImg(key);
    if (img && state.mode === 'province' && !state.mergeMode) {
        drag.img = img;
        drag.region = state.regions.get(key);
        const pt = svgPoint(e);
        drag.startX = pt.x - drag.region.offsetX;
        drag.startY = pt.y - drag.region.offsetY;
        img.style.cursor = 'grabbing';
    }
}

// 구 클릭: 전국이면 확대, 시·도면 사진 넣기/모달/합치기 선택
function clickRegion(path) {
    if (state.mode === 'nation') {
        zoomTo(path.closest('.pm-province'));
        return;
    }
    if (!cfg.editable) return;
    const key = keyOf(path.dataset.code);
    if (state.mergeMode) toggleSelect(path);
    else if (state.regions.has(key) || state.groups.has(key)) openRegionModal(key);
    else pickPhoto(key, nameOf(key));
}

// pointerup/cancel: 드래그 마무리, 움직이지 않았으면 클릭으로 처리
function onUp() {
    if (!drag.down) return;

    setBusy(false);
    state.svg.style.cursor = '';
    if (drag.img) drag.img.style.cursor = 'grab';

    if (drag.handle) {
        const s = editingSticker();
        if (s) resizeEditing(s);
        focusEditor(false);
    } else if (drag.sticker) {
        if (drag.moved) positionStickerPanel();
        // 글자칸을 그냥 누른 건 글자 편집이다. 이미 열린 패널을 다시 열지 않는다
        else if (!drag.inputEl) openStickerPanel(state.stickers.indexOf(drag.sticker));
    }
    if (drag.inputEl) drag.inputEl.style.userSelect = '';

    if (!drag.moved && drag.path) clickRegion(drag.path);

    drag.down = false;
    drag.moved = false;
    drag.region = null;
    drag.img = null;
    drag.sticker = null;
    drag.inputEl = null;
    drag.handle = null;
    drag.handleStart = null;
    drag.path = null;
}

// wheel: 스티커 위는 회전, 사진 위는 사진 확대, 그 외는 지도 확대
function onWheel(e) {
    e.preventDefault();
    const factor = e.deltaY < 0 ? 1.08 : 0.92;

    const stickerEl = e.target.closest('.pm-sticker');
    if (stickerEl && cfg.editable) {
        const s = state.stickers[parseInt(stickerEl.dataset.index, 10)];
        s.rotation = Math.min(Math.max(-60, s.rotation + (e.deltaY < 0 ? -3 : 3)), 60);
        updateStickerTransform(s);
        renderSelection();
        return;
    }

    const path = e.target.closest('.pm-region');
    const key = path ? keyOf(path.dataset.code) : null;
    const r = key ? state.regions.get(key) : null;
    const img = key ? photoImg(key) : null;
    if (state.mode === 'province' && !state.mergeMode && r && img) {
        r.scale = parseFloat(Math.min(Math.max(0.2, r.scale * factor), 5.0).toFixed(2));
        applyTransform(img, r);
        return;
    }

    zoomBy(factor, svgPoint(e));
}

// SVG에 이벤트 연결
export function bindInteractions() {
    const svg = state.svg;
    // 터치 시 브라우저 기본 동작(페이지 스크롤·확대)을 끄고 직접 처리
    svg.style.touchAction = 'none';
    svg.addEventListener('pointermove', onMove);
    svg.addEventListener('pointerleave', function () { dom.tooltip.hidden = true; });
    svg.addEventListener('pointerdown', onDown);
    window.addEventListener('pointerup', onUp);
    window.addEventListener('pointercancel', onUp);
    svg.addEventListener('wheel', onWheel, {passive: false});
}
