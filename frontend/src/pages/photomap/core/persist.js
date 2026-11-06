// 포토맵 저장/삭제
import {cfg, dom, state, csrfHeaders, showError, runsText, backgroundOf, notify} from './state.js';

// 구 사진 + 스티커를 FormData로
function buildFormData(title) {
    const fd = new FormData();
    fd.append('photoMapTitle', title);
    fd.append('photoMapDescription', dom.descInput.value.trim());

    let i = 0;
    state.regions.forEach(function (r) {
        const p = 'photoMapImages[' + i + '].';
        fd.append(p + 'photoMapImageRegionCode', r.code);
        fd.append(p + 'photoMapImageRegionName', r.name);
        fd.append(p + 'photoMapImageOffsetX', r.offsetX);
        fd.append(p + 'photoMapImageOffsetY', r.offsetY);
        fd.append(p + 'photoMapImageSize', r.scale);
        if (r.file) fd.append(p + 'photoMapImageFile', r.file);
        i++;
    });

    // 빈 메모는 저장하지 않는다
    state.stickers.filter(function (s) {
        return s.type !== 'MEMO' || runsText(s.runs).trim();
    }).forEach(function (s, j) {
        const p = 'photoMapStickers[' + j + '].';
        if (s.id) fd.append(p + 'photoMapStickerId', s.id);
        fd.append(p + 'photoMapStickerType', s.type);
        fd.append(p + 'photoMapStickerText', runsText(s.runs).slice(0, 200));
        fd.append(p + 'photoMapStickerTextStyle', s.runs.length ? JSON.stringify(s.runs) : '');
        fd.append(p + 'photoMapStickerBackground', backgroundOf(s));
        fd.append(p + 'photoMapStickerX', s.x);
        fd.append(p + 'photoMapStickerY', s.y);
        fd.append(p + 'photoMapStickerRotation', s.rotation);
        fd.append(p + 'photoMapStickerSize', s.size);
        fd.append(p + 'photoMapStickerOrder', j);
        if (s.file) fd.append(p + 'photoMapStickerImageFile', s.file);
    });
    return fd;
}

// 서버 업로드 한도 (application.properties 와 같게). 넘으면 서버가 답 없이 연결을 끊어서
// 화면에는 "Failed to fetch" 만 뜬다. 보내기 전에 여기서 먼저 걸러 어느 사진인지 알려 준다
const MAX_FILE_MB = 20;
const MAX_TOTAL_MB = 2048;
const MB = 1024 * 1024;

function sizeProblem() {
    let total = 0;
    const big = [];
    state.regions.forEach(function (r) {
        if (!r.file) return;
        total += r.file.size;
        if (r.file.size > MAX_FILE_MB * MB) big.push(r.name || '지역 사진');
    });
    state.stickers.forEach(function (s) {
        if (!s.file) return;
        total += s.file.size;
        if (s.file.size > MAX_FILE_MB * MB) big.push('폴라로이드 사진');
    });
    if (big.length) {
        return '사진 한 장은 ' + MAX_FILE_MB + 'MB까지 올릴 수 있어요. 너무 큰 사진: ' + big.join(', ');
    }
    if (total > MAX_TOTAL_MB * MB) {
        return '한 번에 올리는 사진이 모두 ' + Math.round(total / MB) + 'MB예요. '
            + (MAX_TOTAL_MB / 1024) + 'GB 이하로 나눠서 저장해 주세요.';
    }
    return null;
}

// 저장 (새로 만들면 POST, 수정이면 PATCH) 후 상세로 이동
export function savePhotoMap() {
    const title = dom.titleInput.value.trim();
    if (!title) {
        showError('제목을 입력해 주세요.');
        return;
    }
    const problem = sizeProblem();
    if (problem) {
        showError(problem);
        return;
    }
    state.saving = true;
    notify();
    dom.error.hidden = true;
    fetch(cfg.id ? '/contents/photomap/' + cfg.id : '/contents/photomap', {
        method: cfg.id ? 'PATCH' : 'POST',
        headers: csrfHeaders(),
        body: buildFormData(title)
    })
        .then(async function (res) {
            if (res.status === 401) {
                location.href = '/login';
                return null;
            }
            if (!res.ok) throw new Error(await res.text() || '저장에 실패했습니다.');
            return res.text();
        })
        .then(function (id) {
            if (id) location.href = '/contents/photomap/' + id;
        })
        .catch(function (e) {
            // 서버가 아무 답도 못 준 경우 (연결 끊김·용량 초과 등). 브라우저 원문 대신 할 일을 알려 준다
            showError(e instanceof TypeError
                ? '서버에 보내지 못했어요. 인터넷 연결이나 사진 용량을 확인하고 다시 저장해 주세요.'
                : e.message);
            state.saving = false;
            notify();
        });
}

// 포토맵 삭제 (확인 후, 사진이 있으면 한 번 더)
export function removePhotoMap() {
    const photoCount = state.regions.size
        + state.stickers.filter(function (s) { return s.type === 'POLAROID' && (s.url || s.file); }).length;

    confirmModal({
        title: '포토맵 삭제',
        message: '이 포토맵을 삭제할까요? 첨부한 사진도 함께 삭제됩니다.',
        confirmText: '삭제',
        danger: true
    }).then(function (ok) {
        if (!ok || photoCount === 0) return ok;
        return confirmModal({
            title: '정말 삭제할까요?',
            message: '사진 ' + photoCount + '장이 함께 지워지고 되돌릴 수 없어요.',
            confirmText: '영구 삭제',
            danger: true
        });
    }).then(function (ok) {
        if (!ok) return;
        fetch('/contents/photomap/' + cfg.id, {method: 'DELETE', headers: csrfHeaders()})
            .then(async function (res) {
                if (!res.ok) throw new Error(await res.text() || '삭제에 실패했습니다.');
                location.href = '/contents/photomap';
            })
            .catch(function (e) { showError(e.message); });
    });
}