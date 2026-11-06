// 추천받은 장소 목록. 카드를 누르면 지도가 그 곳으로 간다.
// onDetail 을 주면 오른쪽에 상세 아이콘이 붙고, 그걸 눌러야 모달이 열린다
export default function PlaceCards({ places, picked, onPick, onDetail }) {
    if (places.length === 0) {
        return <p className="hint">어울리는 장소를 아직 찾지 못했어요. 채팅에서 가고 싶은 지역을 말해 보세요.</p>;
    }

    return (
        <div className="pick-cards">
            {places.map((p) => (
                <div key={p.locationId}
                     className={'pick-card' + (picked?.locationId === p.locationId ? ' on' : '')}>
                    <button type="button" className="pick-main" onClick={() => onPick(p)}>
                        {/* 썸네일이 없는 장소도 많다. 없으면 자리도 잡지 않는다 */}
                        {p.images?.[0] && (
                            <img className="pick-thumb" src={p.images[0]} alt=""
                                 onError={(e) => { e.currentTarget.style.display = 'none'; }} />
                        )}

                        <span className="pick-body">
                            <strong>{p.locationName}</strong>
                            <span>{p.locationAddress}</span>
                            {/* location_element 컬럼에 쉼표로 이어진 해시태그가 들어 있다 */}
                            {p.locationElement && (
                                <span className="pick-tags">
                                    {p.locationElement.split(',').map((t) => '#' + t.trim()).join(' ')}
                                </span>
                            )}
                        </span>
                    </button>

                    {onDetail && (
                        <button type="button" className="pick-detail"
                                onClick={() => onDetail(p)}
                                title="상세 정보"
                                aria-label={p.locationName + ' 상세 정보'}>
                            ℹ
                        </button>
                    )}
                </div>
            ))}
        </div>
    );
}
