import { useEffect, useRef, useState } from 'react';
import { usePhotoMap } from './core/store.js';
import { cfg, pathsOf } from './core/state.js';
import { zoomTo, removePhoto } from './core/map.js';

export default function PhotoList() {
    const s = usePhotoMap();
    const [zoom, setZoom] = useState(null);   // 크게 볼 사진 { url, label }
    const dialogRef = useRef(null);

    useEffect(() => {
        if (zoom) dialogRef.current?.showModal();
    }, [zoom]);

    if (!s.svg) return null;   // 지도 로드 전

    const province = s.activeProvince;
    const items = [...s.regions.values()]
        .filter((r) => !province || r.code.startsWith(province.dataset.code));

    const infoOf = (r) => {
        const p = pathsOf(r.code)[0]?.closest('.pm-province');
        return { province: p, label: (p ? p.dataset.name + ' ' : '') + r.name };
    };

    const openZoom = (e, r, label) => {
        e.stopPropagation();
        setZoom({ url: r.url, label });
    };

    const askRemove = (e, r, label) => {
        e.stopPropagation();
        window.confirmModal({ title: '사진 삭제', message: label + ' 사진을 삭제하시겠습니까?', confirmText: '삭제', danger: true })
            .then((ok) => ok && removePhoto(r.code));
    };

    return (
        <>
            <div className="card-head">사진 목록 <span className="count">{items.length}</span></div>
            {items.length === 0 ? (
                <div className="pm-side-empty">
                    {province ? province.dataset.name + '에 넣은 사진이 없어요' : '아직 넣은 사진이 없어요'}
                </div>
            ) : (
                <ul className="pm-list">
                    {items.map((r) => {
                        const { province: p, label } = infoOf(r);
                        return (
                            <li key={r.code} onClick={() => p && zoomTo(p)}>
                                <img src={r.url} alt={label + ' 사진 크게 보기'} title="크게 보기"
                                     onClick={(e) => openZoom(e, r, label)} />
                                <div className="info"><div className="name">{label}</div></div>
                                {cfg.editable && (
                                    <button type="button" className="remove" aria-label="삭제"
                                            onClick={(e) => askRemove(e, r, label)}>×</button>
                                )}
                            </li>
                        );
                    })}
                </ul>
            )}
            <dialog ref={dialogRef} className="pm-preview" onClose={() => setZoom(null)}
                    onClick={(e) => e.target === e.currentTarget && dialogRef.current.close()}>
                <button type="button" className="pm-preview-close" aria-label="닫기"
                        onClick={() => dialogRef.current.close()}>&times;</button>
                {zoom && <img src={zoom.url} alt={zoom.label} />}
            </dialog>
        </>
    );
}
