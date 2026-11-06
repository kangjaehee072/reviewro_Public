import { usePhotoMap } from './core/store.js';
import { cfg } from './core/state.js';
import { zoomTo, setMergeMode, applyMerge } from './core/map.js';
import { pickStickerPhoto, addMemo } from './core/stickers.js';

export default function TopBar() {
    const s = usePhotoMap();
    const province = s.activeProvince;
    const count = s.selected.size;

    return (
        <>
            {province && (
                <button type="button" className="pm-iconbtn" title="전국으로" onClick={() => zoomTo(null)}>←</button>
            )}
            <strong>{province ? province.dataset.name : '전국'}</strong>

            {province && cfg.editable && (
                <button type="button" className="btn btn-sm btn-line" onClick={() => setMergeMode(!s.mergeMode)}>
                    {s.mergeMode ? '취소' : '구 합치기'}
                </button>
            )}
            {s.mergeMode && (
                <button type="button" className="btn btn-sm btn-primary" disabled={count < 2} onClick={applyMerge}>
                    합치기{count ? ` (${count})` : ''}
                </button>
            )}

            {cfg.editable && s.svg && (
                <div className="pm-bar-right">
                    <button type="button" className="btn btn-sm btn-tint" onClick={pickStickerPhoto}>＋ 폴라로이드</button>
                    <button type="button" className="btn btn-sm btn-tint" onClick={addMemo}>＋ 메모</button>
                </div>
            )}
        </>
    );
}
