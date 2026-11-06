import { useEffect, useState } from 'react';
import { fetchPlace } from '../../api/saju.js';
import MapView from '../common/MapView.jsx';
import AiChat from '../common/AiChat.jsx';
import PlaceCards from '../saju/PlaceCards.jsx';
import PlaceModal from '../common/PlaceModal.jsx';

const noop = () => {};

// 목록이 지도 오른쪽을 이만큼 덮는다 (ai.css 의 .ai-list width 와 같아야 한다).
// 가려지지 않은 쪽의 한가운데에 마커를 놓으려면 그 절반을 비켜 주면 된다
const LIST_COVER = 300;
const FOCUS_OFFSET = LIST_COVER / 2;

// 왼쪽: 일러스트 + 채팅 / 오른쪽: 지도 (그 위에 추천 장소 목록이 떠 있다). 칸 순서는 ai.css 가 정한다
export default function AiApp() {
    const [places, setPlaces] = useState([]);   // AI 가 찾아준 목록
    const [picked, setPicked] = useState(null); // 지도에 띄울 장소
    const [detail, setDetail] = useState(null); // 상세 모달에 띄울 장소

    const [tab, setTab] = useState('map');        // 좁은 화면에서 보여줄 칸
    const [listOpen, setListOpen] = useState(true);   // 지도 오른쪽 목록을 접었는지

    // 좁은 화면에서는 목록이 지도 아래로 내려가 지도를 가리지 않는다. 그땐 비켜 줄 것도 없다
    const [narrow, setNarrow] = useState(false);
    useEffect(() => {
        const mq = window.matchMedia('(max-width: 1100px)');
        const sync = () => setNarrow(mq.matches);
        sync();
        mq.addEventListener('change', sync);
        return () => mq.removeEventListener('change', sync);
    }, []);

    // 지도를 그 곳으로 옮기고 상세 모달을 띄운다 (마커 클릭 · 목록의 상세 아이콘)
    const showDetail = (p) => { setPicked(p); setDetail(p); };

    // 도구 결과에는 좌표가 없어서 한 곳씩 다시 받아온다
    const showPlaces = async (list) => {
        const found = (await Promise.all(list.map((p) => fetchPlace(p.locationId)))).filter(Boolean);
        if (found.length === 0) return;
        setPlaces(found);
        setPicked(found[0]);
    };

    return (
        <div className={'ai tab-' + tab}>
            {/* 좁은 화면 전용. 칸을 나란히 못 두니 탭으로 바꿔 끼운다 */}
            <div className="tabs ai-tabs">
                {[['list', '목록'], ['map', '지도'], ['chat', '채팅']].map(([key, label]) => (
                    <button key={key} type="button"
                            className={'tab' + (tab === key ? ' on' : '')}
                            onClick={() => setTab(key)}>
                        {label}
                    </button>
                ))}
            </div>

            <section className="ai-map">
                <MapView locations={places} draft={null} focus={picked}
                         onMapClick={noop} onMarkerClick={showDetail}
                         focusLevel={5}
                         focusOffsetX={narrow || !listOpen ? 0 : -FOCUS_OFFSET} />

                <div className={'ai-list' + (listOpen ? '' : ' off')}>
                    <h3 className="ai-list-title">
                        추천 여행지
                        {places.length > 0 && <span className="ai-count">{places.length}</span>}
                    </h3>
                    {/* 카드를 누르면 지도를 그 곳으로 옮기고 상세 모달을 띄운다 */}
                    <PlaceCards places={places} picked={picked}
                                onPick={setPicked} onDetail={showDetail} />
                </div>

                {/* 목록 손잡이. 접히면 목록을 따라 지도 오른쪽 끝으로 붙는다 */}
                <button type="button"
                        className={'ai-list-handle' + (listOpen ? '' : ' off')}
                        onClick={() => setListOpen(!listOpen)}
                        aria-expanded={listOpen}
                        aria-label={listOpen ? '목록 접기' : '목록 펼치기'}>
                    {listOpen ? '›' : '‹'}
                </button>
            </section>

            <aside className="ai-side">
                {/* 파일이 없으면 숨긴다 — 그림이 없어도 레이아웃은 멀쩡해야 한다 */}
                <img className="ai-illust" src="/img/char/part-aichat-idle.jpg" alt="파트라슈"
                     onError={(e) => { e.currentTarget.style.display = 'none'; }} />

                <AiChat inline showHeader={false} showChips={false}
                            onPlaces={showPlaces}
                            onPick={setPicked}
                            place={picked}
                            hint="어떤 여행을 하고 싶으세요? 지역이나 분위기를 말해 주시면 찾아볼게요."
                            placeholder="예: 부산에 조용한 카페" />
            </aside>

            {detail && <PlaceModal place={detail} onClose={() => setDetail(null)} />}
        </div>
    );
}
