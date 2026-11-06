import { useEffect, useRef } from 'react';
import { usePhotoMap } from './core/store.js';
import { state, notify, nameOf, pathsOf } from './core/state.js';
import { pickPhoto, removePhoto, splitGroup } from './core/map.js';

export default function RegionModal() {
    const s = usePhotoMap();
    const ref = useRef(null);
    const key = s.regionModalKey;

    // key 가 생기면 모달 열기
    useEffect(() => {
        if (key && !ref.current.open) ref.current.showModal();
    }, [key]);

    if (!key) return null;

    const r = s.regions.get(key);
    const group = s.groups.get(key);
    const province = pathsOf(key)[0]?.closest('.pm-province');
    const label = (province ? province.dataset.name + ' ' : '') + nameOf(key);

    // 닫히면 상태도 비움
    const onClose = () => { state.regionModalKey = null; notify(); };
    // 모달 닫고 나서 동작
    const after = (fn) => () => { ref.current.close(); fn(); };

    const askRemove = () => window.confirmModal({
        title: '사진 제거', message: label + ' 사진을 제거하시겠습니까?', confirmText: '제거', danger: true
    }).then((ok) => ok && removePhoto(key));

    const askSplit = () => window.confirmModal({
        title: '구 분리',
        message: label + '\n합친 구를 다시 나눌까요?' + (r ? '\n넣은 사진은 제거됩니다.' : ''),
        confirmText: '분리', danger: !!r
    }).then((ok) => ok && splitGroup(key));

    return (
        <dialog ref={ref} className="confirm-modal pm-photo-modal" onClose={onClose}
                onClick={(e) => e.target === ref.current && ref.current.close()}>
            <div className="body">
                <div className="title">{label}</div>
                {r && <img src={r.url} alt="" />}
            </div>
            <div className="actions">
                {group && <button type="button" className="btn btn-line" onClick={after(askSplit)}>분리</button>}
                <button type="button" className="btn btn-line" onClick={after(() => pickPhoto(key, nameOf(key)))}>
                    {r ? '수정' : '사진 넣기'}
                </button>
                {r && <button type="button" className="btn btn-danger" onClick={after(askRemove)}>제거</button>}
            </div>
        </dialog>
    );
}
