import LocationSaved from "./LocationSaved.jsx";
import React, { useEffect, useState } from "react";
import AddSchedule from "./AddSchedule.jsx";
import Icon from "./core/icons.jsx";
import { fetchLocation } from "../../api/locations.js";
import { isLoggedIn, goLogin } from "./core/auth.js";
import PhotoViewer from "../common/PhotoViewer.jsx";

// 장소 상세 (관광지 찾기·저장한 장소의 왼쪽 패널)
export default function LocationInfo({ selectedLocation, memberId, travelPlansList, onScheduleAdded, onClose }) {
    const place = selectedLocation || {};
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);
    const [images, setImages] = useState(place.thumbnail ? [place.thumbnail] : []);
    const [current, setCurrent] = useState(0);
    const [viewer, setViewer] = useState(-1);      // -1 이면 사진 크게 보기가 닫힌 상태
    const [descOpen, setDescOpen] = useState(false);
    const category = place.locationCategory || (place.categorname || []).join(', ');

    // 목록에는 대표 사진 한 장만 오므로, 상세를 열면 사진 전체를 따로 받는다
    useEffect(() => {
        let alive = true;
        setCurrent(0);
        setViewer(-1);
        setDescOpen(false);
        setImages(place.thumbnail ? [place.thumbnail] : []);
        if (!place.locationId) return;
        fetchLocation(place.locationId)
            .then((full) => { if (alive && full.images?.length) setImages(full.images); })
            .catch(() => {});
        return () => { alive = false; };
    }, [place.locationId]);

    // 깨진 사진은 목록에서 빼고 첫 장으로
    const dropImage = (index) => {
        setImages((list) => list.filter((_, i) => i !== index));
        setCurrent(0);
    };

    const hours = place.locationOpentime
        ? place.locationOpentime.slice(0, 5) + (place.locationClosetime ? ' ~ ' + place.locationClosetime.slice(0, 5) : '')
        : null;
    const longDesc = (place.locationDescription || '').length > 120;

    return (
        <div className="loc-detail">
            {/* 위쪽 줄: 목록으로 · 저장 */}
            <div className="loc-detail-bar">
                <button type="button" className="loc-back" onClick={onClose}>
                    <Icon name="back" size={15} />목록
                </button>
                <LocationSaved key={place.locationId} locationId={place.locationId} />
            </div>

            {/* 사진: 누르면 크게, 여러 장이면 아래 썸네일로 고른다 */}
            {images.length > 0 && (
                <div className="loc-photos">
                    <button type="button" className="loc-photo-main" onClick={() => setViewer(current)}
                            aria-label="사진 크게 보기">
                        <img src={images[current]} alt={place.locationName} onError={() => dropImage(current)} />
                        {images.length > 1 && <span className="loc-photo-count">{current + 1} / {images.length}</span>}
                        <span className="loc-photo-zoom"><Icon name="search" size={13} /></span>
                    </button>
                    {images.length > 1 && (
                        <div className="loc-photo-thumbs">
                            {images.slice(0, 5).map((src, i) => (
                                <button key={src} type="button" className={i === current ? 'on' : ''}
                                        onClick={() => setCurrent(i)} aria-label={`${i + 1}번째 사진`}>
                                    <img src={src} alt="" onError={() => dropImage(i)} />
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            )}

            {/* 제목 · 카테고리 · 평점 */}
            <div className="loc-detail-title">
                <div className="loc-detail-tags">
                    {category && <span className="loc-badge">{category}</span>}
                    <span className="loc-detail-rating">
                        <Icon name="star" size={12} fill="currentColor" />
                        {place.locationReviewAvg ? place.locationReviewAvg.toFixed(1) : '평점 없음'}
                    </span>
                </div>
                <h3>{place.locationName}</h3>
                {place.locationAddress && <p className="loc-detail-addr">{place.locationAddress}</p>}
            </div>

            {/* 빠른 버튼 */}
            <div className="loc-quick">
                {place.locationLat && place.locationLng && (
                    <a className="loc-quick-btn" target="_blank" rel="noreferrer"
                       href={`https://map.kakao.com/link/to/${encodeURIComponent(place.locationName)},${place.locationLat},${place.locationLng}`}>
                        <Icon name="route" size={15} />길찾기
                    </a>
                )}
                {place.locationNumber && (
                    <a className="loc-quick-btn" href={'tel:' + place.locationNumber}>
                        <Icon name="phone" size={15} />전화
                    </a>
                )}
                {place.locationURL && (
                    <a className="loc-quick-btn" href={place.locationURL} target="_blank" rel="noreferrer">
                        <Icon name="link" size={15} />홈페이지
                    </a>
                )}
            </div>

            {/* 운영 정보 */}
            {(hours || place.locationDayoff || place.locationNumber) && (
                <dl className="loc-facts">
                    {hours && (<><dt><Icon name="clock" size={13} />운영</dt><dd>{hours}</dd></>)}
                    {place.locationDayoff && (<><dt><Icon name="calendar" size={13} />휴무</dt><dd>{place.locationDayoff}</dd></>)}
                    {place.locationNumber && (<><dt><Icon name="phone" size={13} />전화</dt><dd>{place.locationNumber}</dd></>)}
                </dl>
            )}

            {/* 설명 (길면 접어 두고 더 보기) */}
            {place.locationDescription && (
                <div className="loc-detail-desc">
                    <p className={longDesc && !descOpen ? 'clamped' : ''}>{place.locationDescription}</p>
                    {longDesc && (
                        <button type="button" onClick={() => setDescOpen((v) => !v)}>
                            {descOpen ? '접기' : '더 보기'}
                        </button>
                    )}
                </div>
            )}

            <div className="loc-detail-actions">
                <button
                    type="button"
                    className="btn btn-primary"
                    onClick={() => (isLoggedIn() ? setIsAddModalOpen(true) : goLogin())}
                >
                    <Icon name="plus" size={15} />일정에 추가하기
                </button>
            </div>

            {viewer >= 0 && (
                <PhotoViewer images={images} index={viewer} title={place.locationName}
                             onChange={setViewer} onClose={() => setViewer(-1)} />
            )}

            {/* 스케줄에 추가하기 서브 모달 */}
            {isAddModalOpen && (
                <AddSchedule
                    place={place}
                    travelPlansList={travelPlansList}
                    onClose={() => setIsAddModalOpen(false)}
                    onScheduleAdded={onScheduleAdded}
                />
            )}
        </div>
    );
}
