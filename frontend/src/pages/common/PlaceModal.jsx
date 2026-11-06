import { useEffect, useState } from 'react';
import LocationSaved from '../location/LocationSaved.jsx';
import PhotoViewer from './PhotoViewer.jsx';

// 장소 상세. 목록에서 카드를 누르면 이 모달이 뜬다.
// 안에서 사진을 또 누르면 사진만 크게 보는 화면으로 한 겹 더 들어간다
export default function PlaceModal({ place, onClose }) {
    const [viewer, setViewer] = useState(-1);   // -1 이면 사진 뷰어가 닫힌 상태
    const images = place?.images ?? [];

    // Esc 로 닫는다. 사진 뷰어가 열려 있으면 뷰어가 키(Esc ← →)를 맡는다
    useEffect(() => {
        if (viewer >= 0) return;
        const onKey = (e) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [viewer, onClose]);

    if (!place) return null;

    return (
        <div className="pm-back" onClick={onClose}>
            {/* 안쪽을 눌렀을 때 닫히지 않게 막는다 */}
            <div className="pm-box" onClick={(e) => e.stopPropagation()}>
                <button type="button" className="pm-close" onClick={onClose} aria-label="닫기">✕</button>

                {images.length > 0 && (
                    <img className="pm-cover" src={images[0]} alt=""
                         onClick={() => setViewer(0)}
                         onError={(e) => { e.currentTarget.style.display = 'none'; }} />
                )}

                <div className="pm-body">
                    <span className="badge badge-tint">{place.locationCategory || '여행지'}</span>
                    <h3 className="pm-name">{place.locationName}</h3>

                    <p className="pm-addr">
                        {place.locationAddress}
                        {place.locationReviewAvg > 0 && (
                            <span className="pm-rate">★ {place.locationReviewAvg.toFixed(1)}</span>
                        )}
                    </p>

                    {place.locationDescription && <p className="pm-desc">{place.locationDescription}</p>}

                    <dl className="pm-meta">
                        {place.locationOpentime && (
                            <>
                                <dt>운영</dt>
                                <dd>{place.locationOpentime} ~ {place.locationClosetime}</dd>
                            </>
                        )}
                        {place.locationDayoff && (
                            <>
                                <dt>휴무</dt>
                                <dd>{place.locationDayoff}</dd>
                            </>
                        )}
                        {place.locationNumber && (
                            <>
                                <dt>전화</dt>
                                <dd>{place.locationNumber}</dd>
                            </>
                        )}
                    </dl>

                    <div className="pm-actions">
                        <a className="btn btn-line" target="_blank" rel="noreferrer"
                           href={`https://map.kakao.com/link/to/${encodeURIComponent(place.locationName)},${place.locationLat},${place.locationLng}`}>
                            카카오맵 길찾기
                        </a>
                        {/* 장소가 바뀌면 버튼도 새로 만들어야 북마크 상태가 안 섞인다 */}
                        <LocationSaved key={place.locationId} locationId={place.locationId} />
                    </div>
                </div>
            </div>

            {/* 사진만 크게 보는 화면 */}
            {viewer >= 0 && (
                <PhotoViewer images={images} index={viewer} title={place.locationName}
                             onChange={setViewer} onClose={() => setViewer(-1)} />
            )}
        </div>
    );
}
