import {useState} from "react";
import {REGIONS} from "./core/regions.js";
import Icon from "./core/icons.jsx";

const RADII = [3, 5, 10];

export default function LocationSearchBar({ cond, onChange, onMyLocation, locationMode, setLocationMode, page, more, onPageChange }) {

    const [keyword, setKeyword] = useState(cond.keyword);
    const set = (path) => onChange({...cond, ...path})

    return (
        <form className="loc-search" onSubmit={(e) => {
            e.preventDefault();
            // 검색 버튼을 누를 때는 항상 1페이지부터 다시 검색
            set({keyword: keyword.trim(), page:1});
        }}>
            {/* 검색어 (아이콘 + 입력 + 버튼 한 줄) */}
            <div className="loc-searchbox">
                <Icon name="search" size={16} />
                <input value={keyword} onChange={(e) => setKeyword(e.target.value)}
                       placeholder="장소명·주소 검색 (예: 종로구)" />
                <button type="submit" className="loc-searchbox-btn">검색</button>
            </div>

            {/* 위치 지정 방식 (세그먼트) */}
            <div className="loc-seg" role="radiogroup" aria-label="위치 지정 방식">
                <button type="button" role="radio" aria-checked={locationMode === 'auto'}
                        className={locationMode === 'auto' ? 'on' : ''}
                        onClick={() => {
                            setLocationMode("auto");
                            onMyLocation();
                        }}>
                    <Icon name="locate" size={14} />내 위치
                </button>
                <button type="button" role="radio" aria-checked={locationMode === 'manual'}
                        className={locationMode === 'manual' ? 'on' : ''}
                        onClick={() => setLocationMode("manual")}>
                    <Icon name="pointer" size={14} />직접 지정
                </button>
            </div>

            {locationMode === 'auto' ? (
                <button type="button" className="loc-linkbtn" onClick={onMyLocation}>
                    <Icon name="locate" size={13} />현재 위치 새로고침
                </button>
            ) : (
                <p className="loc-hint">지도를 클릭하면 그 위치를 기준으로 찾아요.</p>
            )}

            {/* 반경 + 지역 (칩 한 줄) */}
            <div className="loc-chiprow">
                <span className="loc-chiprow-label">반경</span>
                {RADII.map((r) => (
                    <button key={r} type="button"
                            className={'loc-chip' + ((cond.radius || 5) === r ? ' on' : '')}
                            onClick={() => set({ radius: r })}>
                        {r}km
                    </button>
                ))}
                {locationMode === 'manual' && (
                    <select className="loc-select" value={cond.address}
                            onChange={(e) => set({address : e.target.value})}>
                        <option value="">시/도</option>
                        {REGIONS.map((r) => <option key={r.key} value={r.key}>{r.label}</option>)}
                    </select>
                )}
            </div>

            {/* 페이지 이동 */}
            {(page > 1 || more) && (
                <div className="loc-pager">
                    <button type="button" onClick={() => onPageChange(page - 1)} disabled={cond.page <= 1}
                            aria-label="이전 페이지">
                        <Icon name="chevronLeft" size={14} />
                    </button>
                    <span>{cond.page || 1} 페이지</span>
                    <button type="button" onClick={() => onPageChange(page + 1)} disabled={!more}
                            aria-label="다음 페이지">
                        <Icon name="chevronRight" size={14} />
                    </button>
                </div>
            )}
        </form>
    );
}
