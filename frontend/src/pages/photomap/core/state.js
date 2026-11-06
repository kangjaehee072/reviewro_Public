// 포토맵 공용: 설정 · DOM · 상수 · 상태 · 모듈 간 이벤트

export const cfg = window.PM_CONFIG || {};
export const SVG_NS = 'http://www.w3.org/2000/svg';

// 스티커 치수 (지도 좌표 단위) — 폴라로이드 88×107mm 비율
export const STICKER = {
    W: 90, H: 109, PAD: 4.5, PHOTO: 81, FONT: 8, LINE: 9.5,
    MEMO_W: 90, MEMO_MIN_H: 24, MEMO_PAD: 6,
    FONT_FAMILY: '"Nanum Pen Script", "Pretendard Variable", Pretendard, cursive'
};

// 글꼴: 손글씨(기본) / 홈페이지 기본 서체
export const FONTS = {
    pen: STICKER.FONT_FAMILY,
    sans: '"Pretendard Variable", Pretendard, sans-serif'
};

export const TEXT_COLOR = '#1E293B';
export const TEXT_COLORS = ['#1E293B', '#FFFFFF', '#1E6FF4', '#E5484D', '#FFB800', '#10B981'];
export const BACKGROUNDS = {
    POLAROID: ['#FFFFFF', '#FFF4D6', '#EEF4FF', '#E3F7EF', '#FDECEC', '#1E293B'],
    MEMO: ['#FFF3B0', '#FFFFFF', '#FFF4D6', '#EEF4FF', '#E3F7EF', 'none']
};

const byId = function (id) { return document.getElementById(id); };

export const dom = {
    map: byId('pmMap'),
    zoomIn: byId('pmZoomIn'),
    zoomOut: byId('pmZoomOut'),
    tooltip: byId('pmTooltip'),
    error: byId('pmError'),
    file: byId('pmFile'),
    stickerFile: byId('pmStickerFile'),
    exportBtn: byId('pmExport'),
    preview: byId('pmPreview'),
    previewDialog: byId('pmPreviewDialog'),
    previewImg: byId('pmPreviewImg'),
    previewClose: byId('pmPreviewClose'),
    preset: byId('pmPreset'),
    titleInput: byId('photoMapTitle'),
    descInput: byId('photoMapDescription')
};

// 화면 전체가 공유하는 상태 (한 곳에서만 바꾼다)
export const state = {
    svg: null,
    fullBox: null,
    mode: 'nation',
    activeProvince: null,
    pendingCode: null,
    mergeMode: false,
    groups: new Map(),      // 합쳐진 구: key "34050,34080"
    regions: new Map(),     // 구 사진: key = 구 코드 또는 그룹 key
    stickers: [],
    stickerLayer: null,
    selectionLayer: null,
    measureText: null,
    editEl: null,           // 편집 중인 contenteditable
    panelEl: null,
    panelIndex: -1,
    selected: new Set(),
    view: {base: null, zoom: 1, center: null},
    regionModalKey: null,   // 열려 있는 구 모달의 key
    saving: false           // 저장 중 (Actions 버튼 비활성용)
};

// 서식 JSON → 조각 배열. 없으면 평문 한 조각
export function parseRuns(json, text) {
    try {
        const runs = JSON.parse(json);
        if (Array.isArray(runs) && runs.length) return runs;
    } catch (e) {
        // 스타일이 없으면 평문으로
    }
    return text ? [{t: text, c: TEXT_COLOR, z: 1}] : [];
}

// 조각 배열 → 평문
export function runsText(runs) {
    return runs.map(function (r) { return r.t; }).join('');
}

// 서버에서 받은 사진/그룹/스티커를 상태로 적재
export function loadConfig() {
    (cfg.photoMapImages || []).forEach(function (i) {
        state.regions.set(i.photoMapImageRegionCode, {
            code: i.photoMapImageRegionCode,
            name: i.photoMapImageRegionName,
            url: i.photoMapImagePath,
            file: null,
            offsetX: i.photoMapImageOffsetX ?? 0.0,
            offsetY: i.photoMapImageOffsetY ?? 0.0,
            scale: i.photoMapImageSize || 1.0
        });
        if (i.photoMapImageRegionCode.includes(',')) {
            state.groups.set(i.photoMapImageRegionCode, {
                key: i.photoMapImageRegionCode,
                codes: i.photoMapImageRegionCode.split(','),
                name: i.photoMapImageRegionName
            });
        }
    });

    state.stickers = (cfg.photoMapStickers || []).map(function (s) {
        return {
            id: s.photoMapStickerId,
            type: s.photoMapStickerType || 'POLAROID',
            url: s.photoMapStickerImagePath,
            file: null,
            runs: parseRuns(s.photoMapStickerTextStyle, s.photoMapStickerText),
            bg: s.photoMapStickerBackground || null,
            x: s.photoMapStickerX ?? 0,
            y: s.photoMapStickerY ?? 0,
            rotation: s.photoMapStickerRotation ?? 0,
            size: s.photoMapStickerSize || 1
        };
    });
}

// 모듈 간 알림 (map → stickers 방향의 순환 참조를 끊는 용도)
const bus = new EventTarget();

export function emit(name, detail) {
    bus.dispatchEvent(new CustomEvent(name, {detail: detail}));
}

export function on(name, handler) {
    bus.addEventListener(name, function (e) { handler(e.detail); });
}

// SVG 요소 생성
export function el(tag) {
    return document.createElementNS(SVG_NS, tag);
}

// CSRF 헤더
export function csrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    if (!token || !header) return {};
    const headers = {};
    headers[header.content] = token.content;
    return headers;
}

// 상단 에러 알림
export function showError(message) {
    dom.error.textContent = message;
    dom.error.hidden = false;
    window.scrollTo({top: 0, behavior: 'smooth'});
}

// 그룹 key → 구 코드 목록
export function codesOf(key) {
    return key.split(',');
}

// 구 코드 → 속한 그룹 key (없으면 자기 코드)
export function keyOf(code) {
    for (const g of state.groups.values()) {
        if (g.codes.includes(code)) return g.key;
    }
    return code;
}

// key → 표시 이름
export function nameOf(key) {
    const g = state.groups.get(key);
    if (g) return g.name;
    const path = state.svg.querySelector('#pm-r-' + key);
    return path ? path.dataset.name : key;
}

// key → 지도 path 요소들
export function pathsOf(key) {
    return codesOf(key).map(function (code) {
        return state.svg.querySelector('#pm-r-' + code);
    }).filter(Boolean);
}

// 지금 편집 중인 스티커
export function editingSticker() {
    return state.stickers[state.panelIndex];
}

// 스티커 배경색 (없으면 타입 기본색)
export function backgroundOf(s) {
    return s.bg || BACKGROUNDS[s.type][0];
}

// 리액트 화면 갱신용: 코어가 상태를 바꾼 뒤 notify()
let version = 0;
const listeners = new Set();

export function notify() {
    version++;
    listeners.forEach(function (l) { l(); });
}

export function subscribe(listener) {
    listeners.add(listener);
    return function () { listeners.delete(listener); };
}

export function getVersion() {
    return version;
}
