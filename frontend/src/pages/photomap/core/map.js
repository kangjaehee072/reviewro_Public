// 지도: SVG 로드 · 화면(확대/이동) · 구 사진 · 구 합치기 · 사진 목록
import {cfg, dom, state, el, emit, keyOf, nameOf, pathsOf, notify} from './state.js';
import {optimizeImage} from './optimize.js';

// 지도 SVG를 불러와 상태를 초기화하고 사진을 그린다
export function loadMap() {
    return fetch('/img/korea-map.svg')
        .then(function (res) { return res.text(); })
        .then(function (text) {
            dom.map.innerHTML = text;
            const svg = dom.map.querySelector('svg');
            state.svg = svg;
            svg.dataset.mode = 'nation';
            svg.insertAdjacentElement('afterbegin', el('defs'));
            const vb = svg.viewBox.baseVal;
            state.fullBox = {x: vb.x, y: vb.y, width: vb.width, height: vb.height};
            setViewBase(state.fullBox);

            state.groups.forEach(function (g) {
                pathsOf(g.key).forEach(function (p) { p.classList.add('merged'); });
            });
            state.regions.forEach(renderPhoto);
            notify();
        });
}

// getScreenCTM · getBoundingClientRect는 호출할 때마다 브라우저가 레이아웃을 다시 계산한다.
// 마우스를 움직일 때마다 부르면 느려지므로 화면이 바뀔 때만 다시 구한다.
let ctmCache = null;
let rectCache = null;

function invalidateCache() {
    ctmCache = null;
    rectCache = null;
}

window.addEventListener('resize', invalidateCache);
window.addEventListener('scroll', invalidateCache, {passive: true});

// viewBox 변경은 항상 이 함수로 (캐시를 같이 비운다)
function writeViewBox(x, y, w, h) {
    state.svg.setAttribute('viewBox', x + ' ' + y + ' ' + w + ' ' + h);
    ctmCache = null;
}

// 지도를 움직이는 동안에는 그림자·전환 효과를 끈다 (저사양에서 체감 차이가 크다)
let busyTimer = null;

export function setBusy(on, autoOffMs) {
    clearTimeout(busyTimer);
    state.svg.classList.toggle('pm-busy', on);
    if (on && autoOffMs) {
        busyTimer = setTimeout(function () { state.svg.classList.remove('pm-busy'); }, autoOffMs);
    }
}

// 화면 좌표 → 지도 좌표
export function svgPoint(e) {
    if (!ctmCache) ctmCache = state.svg.getScreenCTM().inverse();
    const p = state.svg.createSVGPoint();
    p.x = e.clientX;
    p.y = e.clientY;
    return p.matrixTransform(ctmCache);
}

// 화면 1px이 지도 좌표로 몇 단위인지
export function unitPx() {
    if (!rectCache) rectCache = dom.map.getBoundingClientRect();
    const vb = state.svg.viewBox.baseVal;
    return Math.max(vb.width / rectCache.width, vb.height / rectCache.height);
}

// ── 화면(뷰) ──────────────────────────────────────────

// 기준 화면을 바꾸고 확대/중심을 초기화
export function setViewBase(box) {
    state.view.base = box;
    state.view.zoom = 1;
    state.view.center = {x: box.x + box.width / 2, y: box.y + box.height / 2};
}

// 현재 확대/중심으로 보이는 영역 (기준 화면 밖으로 못 나가게 보정)
export function currentView() {
    const v = state.view;
    const w = v.base.width / v.zoom;
    const h = v.base.height / v.zoom;
    v.center.x = Math.min(Math.max(v.base.x + w / 2, v.center.x), v.base.x + v.base.width - w / 2);
    v.center.y = Math.min(Math.max(v.base.y + h / 2, v.center.y), v.base.y + v.base.height - h / 2);
    return {x: v.center.x - w / 2, y: v.center.y - h / 2, width: w, height: h};
}

// 계산된 영역을 viewBox에 반영하고 다른 모듈에 알림
// mousemove는 1초에 수백 번 올 수 있으므로 화면 갱신은 프레임당 한 번으로 모은다
let viewPending = false;

export function applyView() {
    if (viewPending) return;
    viewPending = true;
    requestAnimationFrame(function () {
        viewPending = false;
        const v = currentView();
        writeViewBox(v.x, v.y, v.width, v.height);
        emit('view');
    });
}

// 확대 배율 변경 (point가 있으면 그 지점을 고정한 채 확대)
export function zoomBy(factor, point) {
    const v = state.view;
    const prev = v.zoom;
    v.zoom = parseFloat(Math.min(Math.max(1, v.zoom * factor), 4).toFixed(2));
    setBusy(true, 180);
    if (point) {
        v.center.x = point.x + (v.center.x - point.x) * (prev / v.zoom);
        v.center.y = point.y + (v.center.y - point.y) * (prev / v.zoom);
    }
    applyView();
}

// 화면 중심 이동 (드래그 팬)
export function panTo(x, y) {
    state.view.center.x = x;
    state.view.center.y = y;
    state.svg.style.cursor = 'grabbing';
    applyView();
}

// viewBox를 부드럽게 이동
function animateViewBox(to) {
    const svg = state.svg;
    const vb = svg.viewBox.baseVal;
    const from = {x: vb.x, y: vb.y, width: vb.width, height: vb.height};
    const start = performance.now();
    const duration = 350;
    setBusy(true);

    function frame(now) {
        let t = Math.min(1, (now - start) / duration);
        t = 1 - Math.pow(1 - t, 3);
        const x = from.x + (to.x - from.x) * t;
        const y = from.y + (to.y - from.y) * t;
        const w = from.width + (to.width - from.width) * t;
        const h = from.height + (to.height - from.height) * t;
        writeViewBox(x, y, w, h);
        if (t < 1) requestAnimationFrame(frame);
        else setBusy(false);
    }
    requestAnimationFrame(frame);
}

// 시·도 확대 (null이면 전국으로)
export function zoomTo(province) {
    if (state.activeProvince) state.activeProvince.classList.remove('active');
    state.activeProvince = province;

    setMergeMode(false);
    emit('zoom');

    if (!province) {
        state.mode = 'nation';
        state.svg.dataset.mode = 'nation';
        setViewBase(state.fullBox);
        animateViewBox(state.fullBox);
        notify();
        return;
    }

    state.mode = 'province';
    state.svg.dataset.mode = 'province';
    province.classList.add('active');

    const box = province.getBBox();
    const pad = Math.max(box.width, box.height) * 0.08;
    const target = {
        x: box.x - pad, y: box.y - pad,
        width: box.width + pad * 2, height: box.height + pad * 2
    };
    setViewBase(target);
    animateViewBox(target);
    notify();
}

// ── 구 사진 ──────────────────────────────────────────

// image에 기록해 둔 구 bbox
function boxOf(img) {
    return {
        x: parseFloat(img.dataset.boxX),
        y: parseFloat(img.dataset.boxY),
        w: parseFloat(img.dataset.boxW),
        h: parseFloat(img.dataset.boxH)
    };
}

// 이동/확대 값을 image transform에 반영
export function applyTransform(img, r) {
    const box = boxOf(img);
    const cx = box.x + box.w / 2;
    const cy = box.y + box.h / 2;
    img.setAttribute('transform',
        'translate(' + r.offsetX + ', ' + r.offsetY + ') translate(' + cx + ', ' + cy + ') scale(' + r.scale + ') translate(' + (-cx) + ', ' + (-cy) + ')');
}

// 사진 원본 비율로 구 영역을 덮는 크기 계산
function fitPhoto(img, r) {
    const box = boxOf(img);
    const probe = new Image();
    probe.onload = function () {
        if (img.getAttribute('href') !== r.url) return;
        const s = Math.max(box.w / probe.naturalWidth, box.h / probe.naturalHeight);
        const w = probe.naturalWidth * s;
        const h = probe.naturalHeight * s;
        img.setAttribute('x', box.x + box.w / 2 - w / 2);
        img.setAttribute('y', box.y + box.h / 2 - h / 2);
        img.setAttribute('width', w);
        img.setAttribute('height', h);
        img.dataset.ready = '1';
        applyTransform(img, r);
    };
    probe.src = r.url;
}

// 여러 path의 bbox 합집합
function unionBox(paths) {
    return paths.reduce(function (acc, p) {
        const b = p.getBBox();
        if (!acc) return {x: b.x, y: b.y, width: b.width, height: b.height};
        const x = Math.min(acc.x, b.x);
        const y = Math.min(acc.y, b.y);
        return {
            x: x, y: y,
            width: Math.max(acc.x + acc.width, b.x + b.width) - x,
            height: Math.max(acc.y + acc.height, b.y + b.height) - y
        };
    }, null);
}

// 구(또는 합쳐진 구) 모양으로 클리핑한 사진을 그린다
export function renderPhoto(r) {
    const svg = state.svg;
    const paths = pathsOf(r.code);
    if (!paths.length) return;
    const province = paths[0].closest('.pm-province');
    let photos = province.querySelector('.pm-photos');
    if (!photos) {
        photos = el('g');
        photos.classList.add('pm-photos');
        province.insertAdjacentElement('afterbegin', photos);
    }
    let img = photos.querySelector('[data-code="' + r.code + '"]');

    if (!img) {
        const clipId = 'pm-clip-' + r.code.replace(/,/g, '-');
        if (!svg.querySelector('#' + clipId)) {
            const clip = el('clipPath');
            clip.id = clipId;
            paths.forEach(function (p) {
                const use = el('use');
                use.setAttribute('href', '#' + p.id);
                clip.appendChild(use);
            });
            svg.querySelector('defs').appendChild(clip);
        }

        const box = unionBox(paths);
        const clipGroup = el('g');
        clipGroup.setAttribute('clip-path', 'url(#' + clipId + ')');
        photos.appendChild(clipGroup);

        img = el('image');
        img.dataset.code = r.code;
        img.dataset.boxX = box.x;
        img.dataset.boxY = box.y;
        img.dataset.boxW = box.width;
        img.dataset.boxH = box.height;
        img.setAttribute('preserveAspectRatio', 'xMidYMid slice');
        img.setAttribute('x', box.x);
        img.setAttribute('y', box.y);
        img.setAttribute('width', box.width);
        img.setAttribute('height', box.height);
        img.style.pointerEvents = 'all';
        img.style.cursor = 'grab';
        clipGroup.appendChild(img);
    }

    if (img.getAttribute('href') !== r.url) {
        delete img.dataset.ready;
        img.setAttribute('href', r.url);
        fitPhoto(img, r);
    }
    paths.forEach(function (p) { p.classList.add('has-photo'); });
    applyTransform(img, r);
}

// 구 사진 제거
export function removePhoto(key) {
    const r = state.regions.get(key);
    if (!r) return;
    if (r.file) URL.revokeObjectURL(r.url);
    state.regions.delete(key);
    const img = state.svg.querySelector('.pm-photos [data-code="' + key + '"]');
    if (img) {
        const clipGroup = img.parentElement;
        if (clipGroup && clipGroup.tagName === 'g' && clipGroup.hasAttribute('clip-path')) clipGroup.remove();
        else img.remove();
    }
    pathsOf(key).forEach(function (p) { p.classList.remove('has-photo'); });
    notify();
}

// 파일 선택창 열기 (구에 사진 넣기)
export function pickPhoto(code, name) {
    state.pendingCode = code;
    dom.file.dataset.name = name;
    dom.file.value = '';
    dom.file.click();
}

// 선택한 파일을 구 사진으로 등록 (올리기 전에 FHD·WebP 로 줄인다)
async function onPhotoPicked() {
    const picked = dom.file.files[0];
    const code = state.pendingCode;
    if (!picked || !code) return;
    const file = await optimizeImage(picked);
    state.pendingCode = code;
    const existing = state.regions.get(state.pendingCode) || {
        code: state.pendingCode,
        name: dom.file.dataset.name
    };
    if (existing.file) URL.revokeObjectURL(existing.url);
    existing.file = file;
    existing.url = URL.createObjectURL(file);
    existing.offsetX = 0.0;
    existing.offsetY = 0.0;
    existing.scale = 1.0;
    state.regions.set(state.pendingCode, existing);
    renderPhoto(existing);
    notify();
    state.pendingCode = null;
}

// 구 모달 (사진 원본 + 수정/제거, 합쳐진 구면 분리)
export function openRegionModal(key) {
    state.regionModalKey = key;
    notify();
}

// ── 구 합치기 / 분리 ──────────────────────────────────

// 선택한 구들을 하나의 그룹으로 (기존 사진/그룹은 해제)
export function mergeGroup(codes) {
    codes.forEach(function (code) {
        const key = keyOf(code);
        if (state.groups.has(key)) splitGroup(key);
        else removePhoto(key);
    });
    const sorted = codes.slice().sort();
    const key = sorted.join(',');
    state.groups.set(key, {key: key, codes: sorted, name: sorted.map(nameOf).join(', ')});
    pathsOf(key).forEach(function (p) { p.classList.add('merged'); });
}

// 그룹 해제 (사진도 제거)
export function splitGroup(key) {
    removePhoto(key);
    pathsOf(key).forEach(function (p) { p.classList.remove('merged'); });
    state.groups.delete(key);
}

// 합치기 모드에서 선택된 구 코드들
function selectedCodes() {
    return [...state.selected];
}

// 합치기 모드 켜기/끄기
export function setMergeMode(on) {
    state.mergeMode = on;
    state.svg.classList.toggle('merge-mode', on);
    state.svg.querySelectorAll('.pm-region.selected').forEach(function (p) { p.classList.remove('selected'); });
    state.selected.clear();
    notify();
}

// 구 선택 토글 (이미 그룹이면 그룹째로)
export function toggleSelect(path) {
    const key = keyOf(path.dataset.code);
    const on = !path.classList.contains('selected');
    pathsOf(key).forEach(function (p) {
        p.classList.toggle('selected', on);
        if (on) {
            state.selected.add(p.dataset.code);
            // 선택 외곽선이 이웃 구에 가려지지 않도록 맨 위로
            p.parentNode.insertBefore(p, p.parentNode.querySelector('.pm-outline'));
        } else {
            state.selected.delete(p.dataset.code);
        }
    });
    notify();
}

// 선택한 구 합치기 실행 (사진이 있으면 확인)
export function applyMerge() {
    const codes = selectedCodes();
    if (codes.length < 2) return;
    const losing = codes.some(function (code) { return state.regions.has(keyOf(code)); });
    const proceed = losing
        ? confirmModal({title: '구 합치기', message: '선택한 구에 넣은 사진은 제거됩니다.\n합칠까요?', confirmText: '합치기', danger: true})
        : Promise.resolve(true);
    proceed.then(function (ok) {
        if (!ok) return;
        mergeGroup(codes);
        setMergeMode(false);
        notify();
    });
}

// ── 사진 목록 (우측) ──────────────────────────────────



// 상단 바 · 파일 입력 · 줌 버튼 연결
export function bindMapControls() {
    if (dom.file) dom.file.addEventListener('change', onPhotoPicked);
    if (dom.zoomIn) dom.zoomIn.addEventListener('click', function () { zoomBy(1.25); });
    if (dom.zoomOut) dom.zoomOut.addEventListener('click', function () { zoomBy(1 / 1.25); });
}
