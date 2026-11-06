import { useEffect, useState } from 'react';
import AdminPlacePanel from './AdminPlacePanel.jsx';
import {searchLocations, createLocation, updateLocation, deleteLocation} from '../../api/locations.js';
import { REGIONS } from '../location/core/regions.js';

// 지도에 한 번에 올리는 마커 수 상한 (서버 페이지 크기와 같음)
const MAX_MARKERS = 10;

// 주소창 #place 같은 값으로 시작 메뉴 결정 (새로고침해도 유지됨)
function menuFromHash() {
    return window.location.hash.slice(1) || 'place';
}

export default function AdminLocationApp() {
    const [cond, setCond] = useState({ address: REGIONS[0].key, keyword: '', page: 1 });
    const [locations, setLocations] = useState([]);
    const [more, setMore] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // 조건이 바뀔 때마다 그 페이지만 불러오기. 이전 요청은 취소
    useEffect(() => {
        const ctrl = new AbortController();
        setLoading(true);
        setError('');
        searchLocations(cond, ctrl.signal)
            .then((res) => {
                setLocations(res.locations.slice(0, MAX_MARKERS));
                setMore(res.more);
            })
            .catch((e) => { if (e.name !== 'AbortError') setError(e.message); })
            .finally(() => { if (!ctrl.signal.aborted) setLoading(false); });
        return () => ctrl.abort();
    }, [cond]);

    // // 메뉴 바꾸기 + 주소창 # 갱신 (페이지 이동은 안 함)
    // const changeMenu = (key) => {
    //     setMenu(key);
    //     window.history.replaceState(null, '', '#' + key);
    // };

    // 장소 추가: 서버 저장 -> 성공하면 지금 목록 맨 앞에 추가 -> 지도/목록 동시 갱신
    const addLocation = async (form) => {
        const id = await createLocation(form);
        if (id === null) return;                                    // 로그인 페이지로 감
        setLocations((prev) => [{ ...form, locationId: id }, ...prev].slice(0, MAX_MARKERS));
    };

    //수정
    const editLocation = async (id, form) => {
        if (!(await updateLocation(id, form))) return;
        setLocations((prev) => prev.map((l) => (l.locationId === id ? {...l,...form} : l)));
    };


    //삭제
    const removeLocation = async (id) => {
        if (!(await deleteLocation(id))) return;
        setLocations((prev) => prev.filter((l) => l.locationId !== id));
    };

    const counts = { place: locations.length };   // 메뉴 옆 숫자

    return (
        <>
            {/*<Sidebar active={menu} counts={counts} onChange={changeMenu} /> */}
            {/*{menu === 'place' ? (*/}
                <AdminPlacePanel locations={locations}
                                 loading={loading}
                                 error={error}
                                 cond={cond}
                                 more={more}
                                 onCondChange={setCond}
                                 onAdd={addLocation}
                                 onEdit={editLocation}
                                 onRemove={removeLocation} />
                {/*) : (<CommingSoon menu={menu} />)}*/}
        </>
    );





}
