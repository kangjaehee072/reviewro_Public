import { useState } from 'react';
import MapView from '../common/MapView.jsx';
import AdminLocationForm from './AdminLocationForm.jsx';
import LocationList from '../common/LocationList.jsx';
import AdminLocationDetail from "./AdminLocationDetail.jsx";
import AdminLocationSearch from './AdminLocationSearch.jsx';
import { REGIONS } from '../location/core/regions.js';

export default function AdminPlacePanel({ locations, loading, error, cond, more, onCondChange, onAdd ,onEdit, onRemove }) {

    const [draft, setDraft] = useState(null);       // 지도 클릭 좌표 {lat, lng}
    const [selected, setSelected] = useState(null); // 지도를 옮길 대상
    const [tab, setTab] = useState('list');         // 'list' 장소 목록 | 'add' 새 장소 등록
    //목록/마커에서 고른 "실제 장소"인지 (현재 위치 이동용 selected는 id가 없음)
    const place = selected?.locationId
        ? locations.find((l) => l.locationId === selected.locationId)
        : null;

    // 폼이 준 값 + 지도에서 찍은 좌표를 합쳐서 위로 올림
    const save = async (form) => {
        await onAdd({
            ...form,
            locationLng: draft.lng,
            locationLat: draft.lat
        });
        setDraft(null);
        setTab('list');
    };

    // 지도를 찍으면 등록 탭으로, 목록/마커를 고르면 목록 탭으로
    const clickMap = (point) => {
        setDraft(point);
        setTab('add');
    };

    const pick = (loc) => {
        setSelected(loc);
        setTab('list');
    };

    // 현재 위치로 지도 이동 (selected 를 재활용)
    const goMyPlace = () => {
        navigator.geolocation.getCurrentPosition((p) =>
            setSelected({
                locationLat: p.coords.latitude,
                locationLng: p.coords.longitude
            }));
    };

    // 지역이 바뀌면 지도도 그 지역으로 이동
    const changeCond = (next) => {
        if (next.address !== cond.address) {
            const r = REGIONS.find((x) => x.key === next.address);
            if (r) setSelected({ locationLat: r.lat, locationLng: r.lng });
        }
        onCondChange(next);
    };

    return (
        <section className="map-box">
            <MapView locations={locations} draft={draft} focus={selected}
                     onMapClick={clickMap} onMarkerClick={pick} />

            <div className="map-foot">
                <span className="hint">지도를 클릭하면 위치가 찍혀요</span>
                <button type="button" className="btn btn-sm btn-tint"
                        onClick={goMyPlace}>현재 위치</button>
            </div>

            <div className="card list-form-module">
                <div className="card-body">
                    <div className="tabs">
                        <button type="button" className={'tab' + (tab === 'list' ? ' on' : '')}
                                onClick={() => setTab('list')}>장소 목록</button>
                        <button type="button" className={'tab' + (tab === 'add' ? ' on' : '')}
                                onClick={() => setTab('add')}>새 장소 등록</button>
                    </div>

                    {tab === 'add'
                        ? <AdminLocationForm draft={draft} onSave={save} />
                        : place
                            ? <AdminLocationDetail key={place.locationId} place={place}
                                                   onEdit={onEdit}
                                                   onRemove={async (id) => { await onRemove(id); setSelected(null); }}
                                                   onClose={() => setSelected(null)} />
                            : <>
                                <AdminLocationSearch cond={cond} more={more} onChange={changeCond} />
                                <LocationList variant="card" locations={locations} loading={loading} error={error}
                                              selectedId={selected?.locationId} onSelect={pick} />
                              </>}
                </div>
            </div>

        </section>
    );
}