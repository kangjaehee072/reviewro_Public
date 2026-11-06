import { useEffect } from 'react';
import { createPortal } from 'react-dom';

// 사진 크게 보기 (AI 지도 장소 모달·장소 상세 공용). 모양은 style.css 의 .pm-viewer
// ← → 로 넘기고 Esc 나 바깥을 누르면 닫힌다
export default function PhotoViewer({ images, index, title, onChange, onClose }) {
    const count = images.length;
    const go = (step) => onChange((index + step + count) % count);

    useEffect(() => {
        const onKey = (e) => {
            if (e.key === 'Escape') onClose();
            if (count > 1 && e.key === 'ArrowLeft') go(-1);
            if (count > 1 && e.key === 'ArrowRight') go(1);
        };
        window.addEventListener('keydown', onKey);
        // 뒤 페이지가 같이 스크롤되지 않게
        const prevOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        return () => {
            window.removeEventListener('keydown', onKey);
            document.body.style.overflow = prevOverflow;
        };
    }, [index, count]);

    if (!images[index]) return null;

    // 지도·패널 안쪽 overflow 에 잘리지 않게 body 바로 아래에 그린다
    return createPortal(
        // 포털이어도 React 클릭은 부모 컴포넌트로 올라간다. 뒤의 장소 모달까지 닫히지 않게 끊는다
        <div className="pm-viewer" onClick={(e) => { e.stopPropagation(); onClose(); }}
             role="dialog" aria-label="사진 크게 보기">
            <div className="pm-viewer-top" onClick={(e) => e.stopPropagation()}>
                <span className="pm-viewer-title">{title}</span>
                {count > 1 && <span className="pm-count">{index + 1} / {count}</span>}
                <button type="button" className="pm-viewer-close" onClick={onClose} aria-label="닫기">✕</button>
            </div>

            <img className="pm-viewer-img" key={images[index]} src={images[index]} alt=""
                 onClick={(e) => e.stopPropagation()} />

            {count > 1 && (
                <>
                    <button type="button" className="pm-nav pm-prev" aria-label="이전 사진"
                            onClick={(e) => { e.stopPropagation(); go(-1); }}>‹</button>
                    <button type="button" className="pm-nav pm-next" aria-label="다음 사진"
                            onClick={(e) => { e.stopPropagation(); go(1); }}>›</button>

                    <div className="pm-strip" onClick={(e) => e.stopPropagation()}>
                        {images.map((src, i) => (
                            <button key={src} type="button" className={i === index ? 'on' : ''}
                                    onClick={() => onChange(i)} aria-label={`${i + 1}번째 사진`}>
                                <img src={src} alt="" />
                            </button>
                        ))}
                    </div>
                </>
            )}
        </div>,
        document.body
    );
}
