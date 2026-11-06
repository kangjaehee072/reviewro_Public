import { useEffect, useState } from 'react';
import { calcSaju, sajuSummary } from './saju.js';
import { fetchSajuTexts, fetchPlace, saveBookmark } from '../../api/saju.js';
import { chat } from '../../api/ai.js';
import MapView from '../common/MapView.jsx';
import AiChat from '../common/AiChat.jsx';
import SajuForm from './SajuForm.jsx';
import SajuChart from './SajuChart.jsx';
import SajuReading from './SajuReading.jsx';
import PlaceCards from './PlaceCards.jsx';
import PlaceModal from '../common/PlaceModal.jsx';

const noop = () => {};

// 목록이 지도 오른쪽을 이만큼 덮는다 (saju.css 의 .sj-list width 와 같아야 한다).
// 가려지지 않은 쪽의 한가운데에 마커를 놓으려면 그 절반을 비켜 주면 된다
const LIST_COVER = 300;
const FOCUS_OFFSET = LIST_COVER / 2;

// 왼쪽: 일러스트 + 채팅 · 오른쪽: 탭(지도+추천목록 / 사주팔자). 칸 순서는 saju.css 가 정한다
// 좁은 화면에서는 칸을 나란히 못 두므로 목록·채팅도 탭으로 바꿔 끼운다
export default function SajuApp() {
    const [saju, setSaju] = useState(null);
    const [texts, setTexts] = useState({});
    const [textError, setTextError] = useState('');
    const [detailOpen, setDetailOpen] = useState(false);

    const [aiBusy, setAiBusy] = useState(false);
    const [aiError, setAiError] = useState('');
    const [history, setHistory] = useState([]);     // 첫 추천까지의 대화. 채팅에 맥락으로 넘긴다
    const [round, setRound] = useState(0);          // 다시 추천받을 때마다 채팅을 새로 시작

    const [places, setPlaces] = useState([]);       // 장소추천 (DB)
    const [picked, setPicked] = useState(null);     // 고른 장소 (지도·저장 대상)
    const [notice, setNotice] = useState(null);     // {type, text}
    const [detail, setDetail] = useState(null);     // 상세 모달에 띄울 장소
    const [tab, setTab] = useState('travel');       // 'travel' | 'saju' | 'list' | 'chat'
    const [listOpen, setListOpen] = useState(true); // 지도 오른쪽 목록을 접었는지

    // 좁은 화면에서는 목록이 지도를 덮지 않고 탭으로 바뀐다. 그땐 비켜 줄 것도 없다
    const [narrow, setNarrow] = useState(false);
    useEffect(() => {
        const mq = window.matchMedia('(max-width: 1100px)');
        const sync = () => setNarrow(mq.matches);
        sync();
        mq.addEventListener('change', sync);
        return () => mq.removeEventListener('change', sync);
    }, []);

    // 넓어지면 좁은 화면 전용 탭(목록·채팅)은 갈 곳이 없어진다
    useEffect(() => {
        if (!narrow && (tab === 'list' || tab === 'chat')) setTab('travel');
    }, [narrow, tab]);

    useEffect(() => {
        fetchSajuTexts().then(setTexts).catch((e) => setTextError(e.message));
    }, []);

    // 사주를 보내 첫 추천을 받는다. 답변은 왼쪽 채팅에 뜨고,
    // AI 가 도구로 찾은 장소는 showPlaces 로 들어와 목록이 된다
    const recommend = async (s) => {
        setAiError('');
        setAiBusy(true);
        setPicked(null);
        setNotice(null);
        setHistory([]);
        setTab('travel');       // 새로 추천받으면 결과부터 보여준다
        setPlaces([]);          // 지난 추천 결과를 지운다. AI 가 찾아줄 때만 채운다

        const first = { role: 'user', content: sajuSummary(s), display: null };
        const appended = [];
        try {
            const text = await chat('SAJU', [{ role: 'user', content: first.content }], {
                context: (added) => appended.push(...added.map((m) => ({ ...m, display: null })))
            });
            if (!text) return;                       // 401 → 로그인 페이지로 이동함
            // 첫 답변은 화면에 안 두고 채팅에서 보여준다 (display 를 안 주면 보인다)
            setHistory([first, ...appended, { role: 'assistant', content: text }]);
        } catch (e) {
            setAiError(e.message);
        } finally {
            setAiBusy(false);
            setRound((r) => r + 1);
        }
    };

    const submit = (input) => {
        const s = calcSaju(input);
        setSaju(s);
        setDetailOpen(false);
        recommend(s);
    };

    // 초기화 → 입력 화면으로
    const reset = () => {
        window.confirmModal({ title: '사주 정보를 초기화 하시겠습니까?', confirmText: '예', cancelText: '아니오' })
            .then((ok) => {
                if (!ok) return;
                setSaju(null);
                setHistory([]);
                setPlaces([]);
                setPicked(null);
                setNotice(null);
            });
    };

    // 지도를 그 곳으로 옮기고 상세 모달을 띄운다 (마커 클릭 · 목록의 상세 아이콘)
    const showDetail = (p) => { setPicked(p); setDetail(p); };

    // 채팅에서 지역을 말해 AI 가 장소를 찾으면 장소추천을 그 결과로 바꾼다
    const showPlaces = async (list) => {
        const found = (await Promise.all(list.map((p) => fetchPlace(p.locationId)))).filter(Boolean);
        if (found.length === 0) return;
        setPlaces(found);
        setPicked(found[0]);
    };

    const save = async () => {
        if (!picked) return;
        try {
            await saveBookmark(picked.locationId);
            setNotice({ type: 'ok', text: picked.locationName + '을(를) 저장했어요' });
        } catch (e) {
            setNotice({ type: 'error', text: e.message });
        }
    };

    // 추천 장소 목록. 넓은 화면에서는 지도 위에 띄우고, 좁으면 탭 하나를 통째로 쓴다
    const list = (
        <>
            <h3 className="sj-title">추천 여행지</h3>
            {aiBusy ? (
                <div className="chat-typing sj-typing"><span /><span /><span /></div>
            ) : aiError ? (
                <p className="notice notice-error">{aiError}</p>
            ) : (
                <PlaceCards places={places} picked={picked}
                            onPick={setPicked} onDetail={showDetail} />
            )}
        </>
    );

    const chatPane = (
        <>
            <img className="sj-illust" src="/img/char/part-saju-idle.jpg" alt="사주를 보는 파트라슈" />

            {!saju ? (
                <p className="sj-bubble">생년월일을 알려주시면 사주를 보고 여행지를 골라드릴게요!</p>
            ) : aiBusy || !history.length ? (
                <div className="sj-chat-wait">
                    {aiBusy ? '사주를 읽고 있어요...' : '추천을 받으면 여기서 대화를 이어갈 수 있어요'}
                </div>
            ) : (
                <AiChat key={round} inline showHeader={false} showChips={false}
                            persona="SAJU" icon="🐶" name="파트라슈"
                            hint="가고 싶은 지역이나 분위기를 말해 주세요"
                            placeholder="가고 싶은 지역 (예: 부산)"
                            initialMessages={history}
                            onPlaces={showPlaces}
                            onPick={setPicked} />
            )}
        </>
    );

    // 사주를 넣기 전에는 탭이 없다. 좁은 화면에서 채팅 칸이 숨어 버리지 않게 sj-intro 를 붙인다
    return (
        <div className={'sj tab-' + tab + (saju ? '' : ' sj-intro')}>
            <section className="card sj-main">
                <div className="card-body">
                    {!saju ? (
                        <SajuForm onSubmit={submit} />
                    ) : (
                        <div className="sj-result">
                            <div className="tabs">
                                {narrow && (
                                    <button type="button"
                                            className={'tab' + (tab === 'list' ? ' on' : '')}
                                            onClick={() => setTab('list')}>
                                        목록
                                    </button>
                                )}
                                <button type="button"
                                        className={'tab' + (tab === 'travel' ? ' on' : '')}
                                        onClick={() => setTab('travel')}>
                                    {narrow ? '지도' : '여행지 추천'}
                                </button>
                                <button type="button"
                                        className={'tab' + (tab === 'saju' ? ' on' : '')}
                                        onClick={() => setTab('saju')}>
                                    사주팔자
                                </button>
                                {narrow && (
                                    <button type="button"
                                            className={'tab' + (tab === 'chat' ? ' on' : '')}
                                            onClick={() => setTab('chat')}>
                                        채팅
                                    </button>
                                )}
                            </div>

                            {/* 탭을 바꿔도 지우지 않고 CSS 로만 가린다.
                                지우면 지도를 매번 새로 만들고 채팅 대화도 날아간다 */}
                            <div className="sj-minimap">
                                <MapView locations={places} draft={null} focus={picked}
                                         onMapClick={noop} onMarkerClick={showDetail}
                                         focusLevel={5}
                                         focusOffsetX={narrow || !listOpen ? 0 : -FOCUS_OFFSET} />

                                <div className={'sj-list' + (listOpen ? '' : ' off')}>{list}</div>

                                <button type="button"
                                        className={'sj-list-handle' + (listOpen ? '' : ' off')}
                                        onClick={() => setListOpen(!listOpen)}
                                        aria-expanded={listOpen}
                                        aria-label={listOpen ? '목록 접기' : '목록 펼치기'}>
                                    {listOpen ? '›' : '‹'}
                                </button>
                            </div>

                            <div className="sj-box">
                                <SajuChart saju={saju} onReset={reset} />
                                <button type="button" className="btn btn-line btn-full sj-detail-btn"
                                        onClick={() => setDetailOpen(!detailOpen)}>
                                    {detailOpen ? '풀이 접기 ▲' : '상세 풀이 보기 ▼'}
                                </button>
                                {detailOpen && (textError
                                    ? <p className="notice notice-error">{textError}</p>
                                    : <SajuReading saju={saju} texts={texts} />)}
                            </div>

                            {notice && <p className={'notice notice-' + notice.type}>{notice.text}</p>}

                            <div className="sj-actions">
                                <button type="button" className="btn btn-line" disabled={aiBusy}
                                        onClick={() => recommend(saju)}>
                                    다시 추천받기
                                </button>
                                <button type="button" className="btn btn-primary" disabled={!picked}
                                        onClick={save}>
                                    장소 저장하기
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </section>

            <aside className="sj-side">{chatPane}</aside>

            {detail && <PlaceModal place={detail} onClose={() => setDetail(null)} />}
        </div>
    );
}
