// PNG 저장/미리보기: SVG 복제 → 이미지/폰트 인라인 → 캔버스
import {cfg, dom, state, el, showError, runsText} from './state.js';
import {closeStickerPanel} from './stickers.js';

const PEN_CSS_URL = 'https://fonts.googleapis.com/css2?family=Nanum+Pen+Script&display=swap';
const SANS_CSS_URL = 'https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css';

// URL → data URL (외부 리소스를 SVG 안에 넣기 위해)
function toDataUrl(url) {
    return fetch(url)
        .then(function (res) { return res.blob(); })
        .then(function (blob) {
            return new Promise(function (resolve) {
                const reader = new FileReader();
                reader.onload = function () { resolve(reader.result); };
                reader.readAsDataURL(blob);
            });
        });
}

// unicode-range 안에 쓰인 글자가 하나라도 있는지
function rangeHits(rangeText, codes) {
    return rangeText.split(',').some(function (part) {
        part = part.trim().replace(/^U\+/i, '');
        let lo, hi;
        if (part.includes('?')) {
            lo = parseInt(part.replace(/\?/g, '0'), 16);
            hi = parseInt(part.replace(/\?/g, 'F'), 16);
        } else {
            const bounds = part.split('-');
            lo = parseInt(bounds[0], 16);
            hi = bounds[1] ? parseInt(bounds[1], 16) : lo;
        }
        for (const c of codes) if (c >= lo && c <= hi) return true;
        return false;
    });
}

// 폰트 CSS에서 쓰인 글자가 포함된 @font-face 조각만 골라 base64로 인라인
async function fontFaceCss(cssUrl, codes) {
    if (!codes.size) return '';
    try {
        const css = await fetch(cssUrl).then(function (r) { return r.text(); });
        const blocks = css.match(/@font-face\s*\{[^}]*\}/g) || [];
        const out = [];
        for (const block of blocks) {
            const url = /url\(\s*['"]?([^'")]+)['"]?\s*\)/.exec(block);
            const range = /unicode-range:\s*([^;}]+)/.exec(block);
            if (!url || (range && !rangeHits(range[1], codes))) continue;
            const absolute = new URL(url[1], cssUrl).href;
            out.push(block.replace(url[0], 'url(' + await toDataUrl(absolute) + ')'));
        }
        return out.join('\n');
    } catch (e) {
        return '';
    }
}

// 손글씨/기본 글꼴별로 쓰인 글자를 모아 필요한 폰트만 인라인
async function embeddedFontCss() {
    const pen = new Set();
    const sans = new Set();
    state.stickers.forEach(function (s) {
        s.runs.forEach(function (r) {
            const target = r.f === 'sans' ? sans : pen;
            Array.from(r.t).forEach(function (ch) { target.add(ch.codePointAt(0)); });
        });
    });
    return await fontFaceCss(PEN_CSS_URL, pen) + '\n' + await fontFaceCss(SANS_CSS_URL, sans);
}

// 현재 화면을 선택한 해상도의 PNG Blob으로
async function renderPng() {
    const size = dom.preset.value.split('x').map(Number);
    const width = size[0];
    const height = size[1];

    const clone = state.svg.cloneNode(true);
    const images = Array.from(clone.querySelectorAll('image'));
    await Promise.all(images.map(async function (img) {
        img.setAttribute('href', await toDataUrl(img.getAttribute('href')));
    }));

    // 페이지 CSS는 안 따라오므로 지도 스타일을 인라인 (선 굵기는 화면 픽셀 기준으로 환산)
    const vb = state.svg.viewBox.baseVal;
    const unit = Math.max(vb.width / width, vb.height / height);
    const style = el('style');
    style.textContent =
        await embeddedFontCss() +
        '#pm-provinces{filter:drop-shadow(0 ' + (6 * unit) + 'px ' + (10 * unit) + 'px rgba(30,41,59,.16))}' +
        '.pm-region{fill:#E9EDF3;stroke:#fff;stroke-width:' + (1.5 * unit) + 'px}' +
        '.pm-region.has-photo{fill:none}' +
        '.pm-region.merged{stroke:none}' +
        '.pm-outline{fill:none;stroke:#9AA7B8;stroke-width:' + (2 * unit) + 'px}' +
        'svg[data-mode="province"] .pm-province:not(.active){opacity:.25}';
    clone.insertAdjacentElement('afterbegin', style);
    clone.setAttribute('width', width);
    clone.setAttribute('height', height);

    const blob = new Blob([new XMLSerializer().serializeToString(clone)], {type: 'image/svg+xml;charset=utf-8'});
    const url = URL.createObjectURL(blob);
    const image = new Image();
    await new Promise(function (resolve, reject) {
        image.onload = resolve;
        image.onerror = reject;
        image.src = url;
    });

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#E4F0FA';
    ctx.fillRect(0, 0, width, height);
    ctx.drawImage(image, 0, 0, width, height);
    URL.revokeObjectURL(url);

    return new Promise(function (resolve) {
        canvas.toBlob(function (png) {
            resolve({blob: png, width: width, height: height});
        }, 'image/png');
    });
}

// 버튼을 잠그고 PNG를 만든 뒤 handler에 넘긴다
function withPng(button, handler) {
    if (!state.svg) return;
    closeStickerPanel();
    button.disabled = true;
    renderPng()
        .then(handler)
        .catch(function () { showError('PNG 생성에 실패했습니다.'); })
        .finally(function () { button.disabled = false; });
}

// 저장 파일명
function pngName(width, height) {
    const name = (dom.titleInput.value || cfg.title || '포토맵').trim() || '포토맵';
    return name + '_' + width + 'x' + height + '.png';
}

// 다운로드/미리보기 버튼 연결
export function bindExport() {
    if (dom.exportBtn) {
        dom.exportBtn.addEventListener('click', function () {
            withPng(dom.exportBtn, function (result) {
                const a = document.createElement('a');
                a.href = URL.createObjectURL(result.blob);
                a.download = pngName(result.width, result.height);
                a.click();
                setTimeout(function () { URL.revokeObjectURL(a.href); }, 1000);
            });
        });
    }

    if (dom.preview) {
        dom.preview.addEventListener('click', function () {
            withPng(dom.preview, function (result) {
                if (dom.previewImg.src) URL.revokeObjectURL(dom.previewImg.src);
                dom.previewImg.src = URL.createObjectURL(result.blob);
                dom.previewDialog.showModal();
            });
        });
        dom.previewClose.addEventListener('click', function () { dom.previewDialog.close(); });
        dom.previewDialog.addEventListener('click', function (e) {
            if (e.target === dom.previewDialog) dom.previewDialog.close();
        });
    }
}
