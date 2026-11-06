import { useState } from 'react';
import { REGIONS } from '../location/core/regions.js';

// 지역은 꼭 고르게 한다 (전체 조회 시 마커가 너무 많아 지도가 멈춘다)
export default function AdminLocationSearch({ cond, more, onChange }) {
    const [keyword, setKeyword] = useState(cond.keyword);
    const set = (patch) => onChange({ ...cond, ...patch });

    return (
        <form className="admin-search" onSubmit={(e) => {
            e.preventDefault();
            set({ keyword: keyword.trim(), page: 1 });
        }}>
            <div className="admin-search-row">
                <select value={cond.address} onChange={(e) => set({ address: e.target.value, page: 1 })}>
                    {REGIONS.map((r) => <option key={r.key} value={r.key}>{r.label}</option>)}
                </select>
                <input value={keyword} onChange={(e) => setKeyword(e.target.value)}
                       placeholder="장소명·주소" />
                <button className="btn btn-sm btn-primary">검색</button>
            </div>

            <div className="pagination-bar">
                <button type="button" className="btn btn-sm btn-line"
                        disabled={cond.page <= 1}
                        onClick={() => set({ page: cond.page - 1 })}>&lt; 이전</button>
                <span className="hint">{cond.page} 페이지</span>
                <button type="button" className="btn btn-sm btn-line"
                        disabled={!more}
                        onClick={() => set({ page: cond.page + 1 })}>다음 &gt;</button>
            </div>
        </form>
    );
}
