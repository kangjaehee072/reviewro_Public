import { useEffect, useMemo, useState } from 'react';
import Sidebar from "./Sidebar.jsx";
import CommingSoon from '../common/CommingSoon.jsx';
import MapView from '../common/MapView.jsx';
import LocationList from '../common/LocationList.jsx';
import LocationSearchBar from './LocationSearchBar.jsx';
import LocationInfo from './LocationInfo.jsx';
import LocationPlan from './LocationPlan.jsx';
import PlanAdvisor from './PlanAdvisor.jsx';
import GuideView from './GuideView.jsx';
import PlaceModal from '../common/PlaceModal.jsx';
import { dayColor } from './core/dayColors.js';
import { buildAdvisorContext } from './core/advisorContext.js';
import { REGIONS } from "./core/regions.js";
import { searchLocations, fetchLocations, fetchLocation, fetchTypes } from '../../api/locations.js';
import { fetchBookmarks } from "../../api/bookmark.js";
import AddSchedule from "./AddSchedule.jsx";
import Icon from "./core/icons.jsx";
import { isLoggedIn, goLogin, LOGIN_MENUS } from "./core/auth.js";

const DEFAULT_REGION = REGIONS[0] || { key: '', lat: 37.49146, lng: 126.72113 };
const INITIAL_COND = {
    address: DEFAULT_REGION.key,
    typeId : '',
    keyword: '',
    element: '',
    minRating: '',
    sort: 'latest',
    radius : 5,
    page: 1
};

const ELEMENTS = [
    { value: 'wood', label: '목(木)' }, { value: 'fire', label: '화(火)' },
    { value: 'earth', label: '토(土)' }, { value: 'metal', label: '금(金)' },
    { value: 'water', label: '수(水)' }
];

function menuFromHash(){
    return window.location.hash.slice(1) || 'place-list';
}

const loggedIn = isLoggedIn();

// 로그인해야 쓰는 메뉴로 가려 하면 로그인 화면으로 보내고 true 를 돌려준다
function needLogin(m) {
    if (!loggedIn && LOGIN_MENUS.includes(m)) {
        goLogin('/location#' + m);
        return true;
    }
    return false;
}

export default function LocationApp() {
    const [menu, setMenu] = useState(() => {
        const m = menuFromHash();
        return needLogin(m) ? 'place-list' : m;
    });

    // 이 페이지에 있는 채로 상단 메뉴(/location#plans 등)를 누르면 새로고침 없이 해시만 바뀐다
    useEffect(() => {
        const sync = () => {
            const m = menuFromHash();
            if (!needLogin(m)) setMenu(m);
        };
        window.addEventListener('hashchange', sync);
        return () => window.removeEventListener('hashchange', sync);
    }, []);
    const [cond, setCond] = useState(INITIAL_COND);
    const [result, setResult] = useState({locations: [], more: false});
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [selected, setSelected] = useState(null);
    const [types, setTypes] = useState([]);
    const [locationMode, setLocationMode] = useState('auto');
    const [focus, setFocus] = useState({
        locationLat: DEFAULT_REGION.lat,
        locationLng: DEFAULT_REGION.lng
    });
    const [isFilterOpen, setIsFilterOpen] = useState(false);
    const [bookmarks, setBookmarks] = useState([]);
    const [bookmarkLoading, setBookmarkLoading] = useState(false);
    const [bookmarkError, setBookmarkError] = useState('');
    const [isMapModalOpen, setIsMapModalOpen] = useState(false);
    const [travelList, setTravelList] = useState([]);
    const [planDetail, setPlanDetail] = useState(null);   // 여행 계획 짜기에서 고른 계획의 상세 일정
    const [view, setView] = useState('map');              // 오른쪽 칸: 'map' 지도 | 'guide' AI 가이드
    const [guideKey, setGuideKey] = useState(0);          // 계획을 저장하고 가이드로 넘어오면 올려서 다시 읽게 한다
    const [guideMounted, setGuideMounted] = useState(false); // 한 번 연 가이드는 지도로 돌아가도 남겨 둔다 (초안 보존)

    const switchView = (v) => {
        if (v === 'guide') setGuideMounted(true);
        setView(v);
    };

    // 계획 패널의 [AI 가이드]: 저장은 패널이 끝냈으니 가이드를 새로 읽고 전환만 한다
    const openGuide = () => {
        setGuideKey((k) => k + 1);
        switchView('guide');
    };

    // 여행 계획: 목록이나 번호 핀을 누르면 지도를 그 장소로 옮기고 상세 모달을 띄운다
    const [planPlace, setPlanPlace] = useState(null);
    const openPlanPlace = (item) => {
        const loc = item.location ?? item;   // 목록은 일정(schedule), 핀은 장소(location)를 넘긴다
        if (!loc?.locationId) return;
        switchView('map');
        setFocus(loc);
        // 모달은 사진까지 있는 전체 정보로 띄운다. 못 받아 오면 가진 것만으로 띄운다
        fetchLocation(loc.locationId).then(setPlanPlace).catch(() => setPlanPlace(loc));
    };

    // 가이드에서 장소를 누르면 지도로 돌아가 그 장소로 옮긴다
    const showOnMap = (place) => {
        switchView('map');
        if (place.lat != null && place.lng != null) {
            setFocus({ locationLat: place.lat, locationLng: place.lng });
        }
    };

    // 네로 AI 가 읽을 지금 화면 데이터 (왼쪽 패널에 보이는 것)
    const advisorContext = useMemo(() => buildAdvisorContext({
        menu, travelList, planDetail,
        locations: result.locations, selected, bookmarks, cond
    }), [menu, travelList, planDetail, result.locations, selected, bookmarks, cond]);

    useEffect(() => {
        fetchTypes()
            .then(setTypes)
            .catch((e) => console.error("타입 로딩 실패", e));
    }, []);

    const counts = {
        'place-list': result.locations.length,
        'bookmarks': bookmarks.length
    };

    // 조건이 바뀌면 장소 검색 실행
    useEffect(() => {
        setSelected(null);

        const hasSearchCondition =
            cond.address || (cond.lat && cond.lng) ||
            cond.keyword?.trim() || cond.typeId ||
            cond.element || cond.minRating;

        if (!hasSearchCondition) {
            setResult({locations: [], more: false});
            return;
        }

        const ctrl = new AbortController();
        setLoading(true);
        setError('');

        searchLocations(cond, ctrl.signal)
            .then(setResult)
            .catch((e) => {
                if (e.name !== 'AbortError') setError(e.message);
            })
            .finally(() => {
                if (!ctrl.signal.aborted) setLoading(false);
            });
        return () => ctrl.abort();
    }, [cond]);

    // 시/도가 바뀌면 지도도 그 지역으로 이동.
    // 페이지 말고 다른 조건(지역·반경·검색어·필터)이 바뀌면 결과가 달라지므로 1페이지부터 다시 본다
    const changeCond = (next) => {
        if (next.address !== cond.address) {
            const r = REGIONS.find((x) => x.key === next.address);
            if (r) setFocus({locationLat: r.lat, locationLng: r.lng});
        }
        const filterChanged = Object.keys({...cond, ...next})
            .some((k) => k !== 'page' && next[k] !== cond[k]);
        setCond(filterChanged ? {...next, page: 1} : next);
    };

    const handleMapBoundsChange = ({lat, lng}) => {};

    // 가까운 지역 찾기 (문법 오류 수정됨)
    const findClosestRegion = (lat, lng) => {
        if (!REGIONS || REGIONS.length === 0) return '';
        let closest = REGIONS[0];
        let minDist = Number.MAX_VALUE;
        for (const r of REGIONS) {
            const dist = Math.pow(r.lat - lat, 2) + Math.pow(r.lng - lng, 2);
            if (dist < minDist) {
                minDist = dist;
                closest = r;
            }
        }
        return closest.key;
    };

    const handleMapClick = ({lat, lng}) => {
        if (locationMode === 'manual') {
            setFocus({locationLat: lat, locationLng: lng});
            setCond((prev) => ({
                ...prev,
                lat: lat,
                lng: lng,
                address: '',
                page: 1
            }));
            findClosestRegion(lat, lng);
        }
    };

    const handleGetMyCurrentLocation = () => {
        if (!navigator.geolocation) {
            alert('위치정보를 확인할 수 없습니다.');
            return;
        }
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;
                setFocus({ locationLat: lat, locationLng: lng });
                setCond((prev) => ({
                    ...prev,
                    lat: lat,
                    lng: lng,
                    address: '',
                    page: 1
                }));
            }, (error) => {
                alert('위치정보를 가져오는 데 실패했습니다.');
                console.log(error);
            }
        );
    };

    // 리스트/마커 클릭 -> 상세+지도이동
    const pick = (loc) => {
        setSelected(loc);
        setFocus(loc);
    };

    // 북마크 조회
    useEffect(() => {
        if(menu === 'bookmarks') {
            setBookmarkLoading(true);
            setBookmarkError('');
            fetchBookmarks()
                .then(setBookmarks)
                .catch((e) => setBookmarkError(e.message))
                .finally(() => setBookmarkLoading(false));
        }
    }, [menu]);

    // 페이지 변경 함수
    const handlePageChange = (newPage) => {
        setCond(prev => ({ ...prev, page: newPage }));
    };

    //여행 목록 불러오기
     const fetchTravelList = async () => {
         setLoading(true);
         try{
             const memberId = localStorage.getItem('memberId');
             const res = await fetch(`/api/travels?memberId=${memberId}`);

             if(!res.ok){
                 throw new Error('네트워크 응답이 올바르지 않습니다.');
             }

             const data = await res.json();
             setTravelList(data);
         }catch (err) {
             console.error('목록 조회 실패', err);
         }finally{

             setLoading(false);
         }
     };
     //여행 목록 초기화
    useEffect(() => {
        if (loggedIn) fetchTravelList();   // 로그인 전에는 부를 수 없는 목록
    }, []);


    //메뉴별 띄우는 목록
    const getMapLocations = () => {
        switch (menu) {
            case 'plans' : {
                if(!planDetail || !planDetail.schedulePlans) return [];
                // 왼쪽 목록의 번호를 핀 안에 넣고, 일차마다 색을 달리한다 (목록과 같은 색)
                const dayIndex = new Map((planDetail.dailyPlans || []).map((d, i) => [d.dailyId, i]));
                return planDetail.schedulePlans
                    .filter(s => s.location)
                    .map(s => ({
                        ...s.location,
                        markerLabel: s.scheduleNumber,
                        markerColor: dayColor(dayIndex.get(s.dailyId) ?? 0)
                    }));
            }

            case 'place-list' :
                return result.locations || [];

            case 'bookmarks' :
                return bookmarks || [];

            default:
                return result.locations || [];

        }
    };


    const PANEL_TITLE = {
        'plans': ['여행 계획', '일차별로 장소 순서를 끌어서 바꾸고 저장하세요'],
        'place-list': ['관광지 찾기', '내 위치나 지도에서 고른 곳 주변을 찾아요'],
        'bookmarks': ['저장한 장소', '하트를 눌러 저장한 장소예요'],
    };
    const [panelTitle, panelSub] = PANEL_TITLE[menu] || ['', ''];

    return (
        <div className={'loc-app' + (view === 'guide' ? ' guide-on' : '')}>
            {/* 왼쪽 패널: 탭 + 메뉴별 내용 */}
            <aside className="loc-panel">
                <Sidebar
                    active={menu}
                    counts={counts}
                    onChange={(m) => {
                        if (needLogin(m)) return;
                        setMenu(m);
                        history.replaceState(null, '', '#' + m);   // 주소도 맞춰 둬야 상단 메뉴로 다시 들어올 때 해시가 바뀐다
                    }}
                />

                <div className="loc-panel-body">
                    {!selected && panelTitle && (
                        <div className="loc-panel-head">
                            <h2>{panelTitle}</h2>
                            <p>{panelSub}</p>
                        </div>
                    )}

                    {menu === 'place-list' ? (
                        selected ? (
                            <LocationInfo
                                selectedLocation={selected}
                                travelPlansList={travelList}
                                onClose={() => setSelected(null)}/>
                        ) : (
                            <>
                                <LocationSearchBar
                                    cond={cond}
                                    onChange={changeCond}
                                    onMyLocation={handleGetMyCurrentLocation}
                                    locationMode={locationMode}
                                    setLocationMode={setLocationMode}
                                    page={cond.page}
                                    more={result.more}
                                    onPageChange={handlePageChange}
                                />
                                {result.more && (
                                    <p className="loc-hint">결과가 많아 500곳만 보여요. 키워드로 좁혀 보세요!</p>
                                )}
                                {cond.address || (cond.lat && cond.lng) ? (
                                    <>
                                        {!loading && result.locations.length > 0 && (
                                            <div className="loc-result-head">
                                                검색 결과 <b>{result.locations.length}</b>곳
                                            </div>
                                        )}
                                        <LocationList
                                            variant="card"
                                            locations={result.locations}
                                            loading={loading}
                                            error={error}
                                            selectedId={selected?.locationId}
                                            onSelect={pick}
                                            emptyText="조건에 맞는 장소가 없어요. 반경을 넓혀 보세요."
                                        />
                                    </>
                                ) : (
                                    <p className="loc-empty">지역을 선택해 주세요</p>
                                )}
                            </>
                        )
                    ) : menu === 'plans' ? (
                        <LocationPlan
                          travelList={travelList}
                          loading={loading}
                          onFetchTravels={fetchTravelList}
                          onDetailChange={setPlanDetail}
                          onOpenGuide={openGuide}
                          onPickSchedule={openPlanPlace}
                        />
                    ) : menu === 'bookmarks' ? (
                        selected ? (
                            <LocationInfo
                                selectedLocation={selected}
                                travelPlansList={travelList}
                                onClose={() => setSelected(null)}/>
                        ) : (
                            <LocationList
                                variant="card"
                                locations={bookmarks}
                                loading={bookmarkLoading}
                                error={bookmarkError}
                                selectedId={selected?.locationId}
                                onSelect={pick}
                                emptyText="아직 저장한 장소가 없어요. 장소 상세에서 하트를 눌러 보세요."
                            />
                        )
                    ) : (
                        <CommingSoon menu={menu}/>
                    )}
                </div>
            </aside>

            {/* 오른쪽 지도 (상시 노출 + 필터 + 네로 AI) */}
            <main className="loc-map">
                <MapView
                    locations={getMapLocations()}
                    selected={selected}
                    draft={null}
                    focus={focus}
                    onMapClick={handleMapClick}
                    onMarkerClick={menu === 'plans' ? openPlanPlace : pick}
                    onAddSchedule={(loc) => {
                        setSelected(loc);
                        setIsMapModalOpen(true);
                    }}
                    onBoundsChange={handleMapBoundsChange}
                    searchCenter={cond.lat && cond.lng ? { lat: cond.lat, lng: cond.lng } : null}
                    radius={cond.radius ? cond.radius * 1000 : 0}
                    menu={menu}
                />

                {/* 지도 왼쪽 위 필터 (알약 버튼 + 팝오버) */}
                <div className="loc-filter">
                    <button
                        type="button"
                        className={'loc-filter-btn' + (isFilterOpen ? ' on' : '')}
                        onClick={() => setIsFilterOpen(!isFilterOpen)}
                        aria-expanded={isFilterOpen}
                    >
                        <Icon name="sliders" size={14} />필터
                        {(cond.typeId || cond.minRating) && <span className="loc-filter-dot" />}
                    </button>
                    {isFilterOpen && (
                        <div className="loc-filter-pop">
                            <label>장소 타입</label>
                            <select
                                value={cond.typeId}
                                onChange={(e) => changeCond({...cond, typeId: e.target.value})}
                            >
                                <option value="">타입 전체</option>
                                {types.map((t) => (
                                    <option key={t.typeId || t.id } value={t.typeId || t.id }>
                                        {t.typeName}
                                    </option>
                                ))}
                            </select>
                            <label>평점</label>
                            <select
                                value={cond.minRating}
                                onChange={(e) => changeCond({...cond, minRating: e.target.value})}
                            >
                                <option value="">평점 전체</option>
                                <option value="3">★3 이상</option>
                                <option value="4">★4 이상</option>
                                <option value="4.5">★4.5 이상</option>
                            </select>
                            <label>정렬</label>
                            <select
                                value={cond.sort}
                                onChange={(e) => changeCond({...cond, sort: e.target.value})}
                            >
                                <option value="latest">최신순</option>
                                <option value="rating">평점순</option>
                                <option value="name">이름순</option>
                            </select>

                            <button
                                type="button"
                                className="btn btn-primary btn-sm"
                                onClick={() => setIsFilterOpen(false)}
                            >
                                적용하기
                            </button>
                        </div>
                    )}
                </div>

                {/* 우측 하단 말풍선 → 지도 오른쪽 네로 채팅.
                    가이드 화면에선 숨긴다 (가이드가 곧 네로의 답이다). 숨기기만 해서 대화는 남는다 */}
                <div hidden={view === 'guide'}>
                    <PlanAdvisor context={advisorContext} />
                </div>

                {/* 오른쪽 위 전환: 지도 ↔ AI 가이드 (지도 칸 전체를 덮는다) */}
                <div className="loc-view-switch" role="tablist">
                    <button type="button" role="tab" aria-selected={view === 'map'}
                            className={view === 'map' ? 'on' : ''} onClick={() => switchView('map')}>
                        <Icon name="map" size={14} />지도
                    </button>
                    <button type="button" role="tab" aria-selected={view === 'guide'}
                            className={view === 'guide' ? 'on' : ''} onClick={() => switchView('guide')}>
                        <Icon name="sparkles" size={14} />AI 가이드
                    </button>
                </div>

                {/* 가이드는 처음 열 때 만들고, 지도로 돌아가도 지우지 않는다 (초안이 날아가지 않게) */}
                {guideMounted && (
                    <div hidden={view !== 'guide'}>
                        <GuideView planId={planDetail?.planId} refreshKey={guideKey} onPickPlace={showOnMap} />
                    </div>
                )}
            </main>

            {/* 여행 계획 목록·핀을 눌렀을 때 뜨는 장소 상세 (AI 추천·오행여행과 같은 모달) */}
            {planPlace && <PlaceModal place={planPlace} onClose={() => setPlanPlace(null)} />}
        </div>
    );
}
