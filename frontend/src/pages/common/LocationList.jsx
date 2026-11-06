// 장소 목록. variant="card" 는 관광지 찾기·저장한 장소의 카드형, 기본은 어드민의 한 줄 목록
export default function LocationList({ locations, loading, error, selectedId, onSelect, variant, emptyText }) {
    if (loading) {
        return variant === 'card'
            ? <ul className="loc-places">{[0, 1, 2, 3].map((i) => <li key={i} className="loc-place skeleton" />)}</ul>
            : <p className="hint">불러오는 중...</p>;
    }
    if (error) return <div className="notice notice-error">{error}</div>;
    if (!locations.length) {
        return <p className={variant === 'card' ? 'loc-empty' : 'hint'}>{emptyText || '등록된 장소가 없어요'}</p>;
    }

    if (variant === 'card') {
        return (
            <ul className="loc-places">
                {locations.map((loc) => {
                    const category = loc.locationCategory || (loc.categorname || []).join(', ');
                    return (
                        <li key={loc.locationId}
                            className={'loc-place' + (loc.locationId === selectedId ? ' on' : '')}
                            onClick={() => onSelect(loc)}>
                            <span className="loc-place-thumb" aria-hidden="true">
                                {/* 대표 사진이 없거나 못 불러오면 이름 첫 글자 */}
                                {loc.thumbnail
                                    ? <img src={loc.thumbnail} alt="" loading="lazy"
                                           onError={(e) => { e.currentTarget.replaceWith((loc.locationName || '?').slice(0, 1)); }} />
                                    : (loc.locationName || '?').slice(0, 1)}
                            </span>
                            <span className="loc-place-body">
                                <strong>{loc.locationName}</strong>
                                <span className="loc-place-sub">
                                    {[category, loc.locationAddress].filter(Boolean).join(' · ') || '정보 없음'}
                                </span>
                            </span>
                            {loc.locationReviewAvg > 0 && (
                                <span className="loc-place-rating">★ {Number(loc.locationReviewAvg).toFixed(1)}</span>
                            )}
                        </li>
                    );
                })}
            </ul>
        );
    }

    return (
        <ul className="place-list">
            {locations.map((loc) => (
                <li key={loc.locationId}
                    className={loc.locationId === selectedId ? 'on' : ''}
                    onClick={() => onSelect(loc)}>
                    <strong>{loc.locationName}</strong>
                    <span>{loc.locationCategory}</span>
                </li>
            ))}
        </ul>
    );
}
