import { useEffect, useRef, useState } from 'react';
import { toPng } from 'html-to-image';
import Icon from './core/icons.jsx';
import { fetchGuide, draftGuide, saveGuide } from '../../api/guide.js';

// 지도 칸을 덮는 AI 가이드 화면 (네로가 내 일정을 보고 가이드를 써 준다).
// 사실(일정·운영시간·휴무·거리)은 facts(서버 계산), 문장은 guide(AI) 에서 가져온다.
// 초안은 저장 전까지 이 화면에만 있다. 마음에 들면 저장, 아니면 다시 만들거나 일부만 고쳐 달라고 한다
const CHIPS = ['팁을 더 자세히 써 줘', '요약을 더 재밌게', '체크리스트를 늘려 줘', '비 오는 날 대안도 넣어 줘'];

export default function GuideView({ planId, refreshKey, onPickPlace }) {
    const [facts, setFacts] = useState(null);
    const [guide, setGuide] = useState(null);
    const [dirty, setDirty] = useState(false);       // 저장 안 한 초안
    const [loading, setLoading] = useState(false);   // 일정·저장본 불러오는 중
    const [busy, setBusy] = useState('');            // AI 가 쓰는 중이면 안내 문구
    const [error, setError] = useState('');
    const [request, setRequest] = useState('');
    const [exporting, setExporting] = useState(false);
    const sheetRef = useRef(null);

    // 계획이 바뀌거나, 계획 패널에서 저장하고 넘어오면 다시 읽는다
    useEffect(() => {
        if (!planId) return;
        let alive = true;
        setLoading(true);
        setError('');
        fetchGuide(planId)
            .then((data) => {
                if (!alive) return;
                setFacts(data.facts);
                setGuide(data.saved);
                setDirty(false);
            })
            .catch((e) => alive && setError(e.message))
            .finally(() => alive && setLoading(false));
        return () => { alive = false; };
    }, [planId, refreshKey]);

    // 저장 안 한 초안을 두고 페이지를 떠나려 하면 브라우저가 한 번 묻는다
    useEffect(() => {
        if (!dirty) return;
        const warn = (e) => { e.preventDefault(); e.returnValue = ''; };
        window.addEventListener('beforeunload', warn);
        return () => window.removeEventListener('beforeunload', warn);
    }, [dirty]);

    const ask = async (instruction) => {
        setBusy(instruction ? '말씀하신 부분을 고치는 중이에요…' : '네로가 일정을 살펴보는 중이에요…');
        setError('');
        try {
            setGuide(await draftGuide(planId, instruction, guide));
            setDirty(true);
            if (instruction) setRequest('');
        } catch (e) {
            setError(e.message);
        } finally {
            setBusy('');
        }
    };

    const make = () => {
        if (dirty && !window.confirm('저장 안 한 초안이 있어요. 새로 만들까요?')) return;
        ask(null);
    };

    // 지금 보이는 가이드를 한 장짜리 PNG 로 내려받는다. 버튼·고쳐 달라기 칸(lg-no-capture)은 빼고 찍는다.
    // 걸러 내기만 하면 그 칸 높이만큼 아래가 비어서, 찍는 동안만 실제로 숨겨 높이를 맞춘다
    const exportPng = async () => {
        const sheet = sheetRef.current;
        if (!sheet) return;
        setExporting(true);
        setError('');
        sheet.classList.add('capturing');
        try {
            const dataUrl = await toPng(sheet, {
                pixelRatio: 2,
                backgroundColor: '#F4F6F9',
                filter: (node) => !node.classList?.contains('lg-no-capture')
            });
            const a = document.createElement('a');
            a.href = dataUrl;
            a.download = ((facts?.title || '여행') + ' 가이드').replace(/[\\/:*?"<>|]/g, '_') + '.png';
            a.click();
        } catch (e) {
            setError('이미지로 저장하지 못했어요. 다시 시도해 주세요.');
        } finally {
            sheet.classList.remove('capturing');
            setExporting(false);
        }
    };

    const save = async () => {
        setError('');
        try {
            await saveGuide(planId, guide);
            setDirty(false);
        } catch (e) {
            setError(e.message);
        }
    };

    if (!planId) {
        return (
            <div className="loc-guide">
                <div className="lg-inner">
                    <div className="lg-card lg-empty">
                        <Icon name="route" size={28} />
                        <strong>어떤 여행의 가이드를 볼까요?</strong>
                        <p>왼쪽 '여행 계획 짜기'에서 계획을 하나 고르면, 네로가 그 일정으로 가이드를 만들어 드려요.</p>
                    </div>
                </div>
            </div>
        );
    }

    const tipOf = (id) => guide?.tips?.find((t) => t.scheduleId === id)?.tip || '';
    const titleOf = (day) => guide?.dayTitles?.find((t) => t.day === day)?.title || '';
    const warnings = facts ? facts.days.flatMap((d) => d.places.flatMap((p) =>
        p.warnings.map((w) => ({ key: p.scheduleId + w, day: d, place: p, text: w })))) : [];
    const places = [];
    if (facts) {
        const seen = new Set();
        facts.days.forEach((d) => d.places.forEach((p) => {
            if (!seen.has(p.locationId)) { seen.add(p.locationId); places.push(p); }
        }));
    }

    return (
        <div className="loc-guide">
            <div className="lg-inner" ref={sheetRef}>

                {/* 머리 */}
                <div className="lg-card lg-head">
                    <div className="lg-head-text">
                        <span className="lg-eyebrow"><Icon name="sparkles" size={13} />네로의 여행 가이드</span>
                        <strong>{facts?.title || '여행 가이드'}</strong>
                        {facts && <span className="lg-sub">{facts.start} ~ {facts.end} · {facts.days.length}일</span>}
                    </div>
                    <div className="lg-head-actions lg-no-capture">
                        {guide && <span className={'lg-status' + (dirty ? ' draft' : '')}>{dirty ? '저장 안 된 초안' : '저장됨'}</span>}
                        {guide && !busy && (
                            <button type="button" className="btn btn-line btn-sm" onClick={exportPng} disabled={exporting}>
                                {exporting ? '만드는 중…' : 'PNG로 저장'}
                            </button>
                        )}
                        <button type="button" className="btn btn-line btn-sm" onClick={make} disabled={!!busy || loading}>
                            {guide ? '다시 만들기' : '가이드 만들기'}
                        </button>
                        {dirty && !busy && (
                            <button type="button" className="btn btn-primary btn-sm" onClick={save}>
                                <Icon name="save" size={14} />저장
                            </button>
                        )}
                    </div>
                </div>

                {error && <div className="notice notice-error lg-no-capture">{error}</div>}

                {(loading || busy) ? (
                    <div className="lg-card lg-empty">
                        <span className="lg-spinner" />
                        <strong>{busy || '일정을 불러오는 중이에요…'}</strong>
                        {busy && <p>일정이 길면 1분쯤 걸릴 수 있어요.</p>}
                    </div>
                ) : !guide ? (
                    <div className="lg-card lg-empty">
                        <Icon name="sparkles" size={28} />
                        <strong>이 일정으로 여행 가이드를 만들어 드려요</strong>
                        <p>휴무·운영시간 체크, 일차별 팁, 밀리면 지킬 순서, 체크리스트까지 한 장에 정리해요.
                            마음에 들면 저장하고, 아니면 다시 만들거나 일부만 고쳐 달라고 할 수 있어요.</p>
                    </div>
                ) : (
                    <>
                        {guide.summary && (
                            <div className="lg-hero">
                                <span>{facts?.title}</span>
                                <strong>{guide.summary}</strong>
                            </div>
                        )}

                        {/* 쉬는 날·시간 체크 (서버 계산) */}
                        <section className="lg-card">
                            <h3><Icon name="calendar" size={15} />쉬는 날 · 시간 체크</h3>
                            {warnings.length ? (
                                <ul className="lg-warns">
                                    {warnings.map((w) => (
                                        <li key={w.key}>
                                            <b>{w.day.day}일차{w.day.weekday ? ` (${w.day.weekday})` : ''}</b> · {w.place.name} — {w.text}
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <p className="lg-ok">여행 날짜에 쉬는 곳이나 시간이 안 맞는 곳은 없어요.</p>
                            )}
                        </section>

                        {/* 일차별 코스 */}
                        <section className="lg-card">
                            <h3><Icon name="route" size={15} />일차별 코스</h3>
                            {facts.days.map((d) => (
                                <div className="lg-day" key={d.day}>
                                    <div className="lg-day-head">
                                        <b>{d.day}일차</b>
                                        {d.date && <span>{d.date.slice(5).replace('-', '.')} ({d.weekday})</span>}
                                        {titleOf(d.day) && <em>{titleOf(d.day)}</em>}
                                    </div>
                                    {!d.places.length ? (
                                        <p className="lg-muted">이 날은 담은 장소가 없어요.</p>
                                    ) : (
                                        <ol className="lg-stops">
                                            {d.places.map((p) => (
                                                <li key={p.scheduleId} className="lg-stop">
                                                    <span className="lg-num">{p.order}</span>
                                                    <div className="lg-stop-body">
                                                        <div className="lg-stop-top">
                                                            <button type="button" className="lg-place"
                                                                    onClick={() => onPickPlace(p)} title="지도에서 보기">
                                                                {p.name}<Icon name="pin" size={12} />
                                                            </button>
                                                            {p.time && <span className="lg-time"><Icon name="clock" size={12} />{p.time}</span>}
                                                        </div>
                                                        {p.kmFromPrev != null && <span className="lg-muted">앞 장소에서 {p.kmFromPrev}km</span>}
                                                        {p.warnings.map((w) => <span key={w} className="lg-warn-tag">{w}</span>)}
                                                        {tipOf(p.scheduleId) && <p className="lg-tip">{tipOf(p.scheduleId)}</p>}
                                                    </div>
                                                </li>
                                            ))}
                                        </ol>
                                    )}
                                </div>
                            ))}
                        </section>

                        {guide.alternatives?.length > 0 && (
                            <section className="lg-card">
                                <h3><Icon name="sparkles" size={15} />이렇게 바꿔 보면 어때요</h3>
                                <ul className="lg-list">{guide.alternatives.map((t, i) => <li key={i}>{t}</li>)}</ul>
                            </section>
                        )}

                        {guide.priorities?.length > 0 && (
                            <section className="lg-card">
                                <h3><Icon name="clock" size={15} />밀리면 이 순서로 지키기</h3>
                                <ol className="lg-list ordered">{guide.priorities.map((t, i) => <li key={i}>{t}</li>)}</ol>
                            </section>
                        )}

                        {/* 장소 정보 (DB 그대로) — 칸 폭에 맞춰 카드가 줄을 바꾼다 */}
                        {places.length > 0 && (
                            <section className="lg-card">
                                <h3><Icon name="pin" size={15} />장소 정보</h3>
                                <div className="lg-places">
                                    {places.map((p) => (
                                        <div className="lg-place-card" key={p.locationId}>
                                            <button type="button" className="lg-place" onClick={() => onPickPlace(p)}>{p.name}</button>
                                            <dl>
                                                <dt>운영</dt><dd>{p.hours || '정보 없음'}</dd>
                                                <dt>휴무</dt><dd>{p.dayoff || '-'}</dd>
                                                <dt>연락처</dt><dd>{p.phone || '-'}</dd>
                                                <dt>주소</dt><dd>{p.address || '-'}</dd>
                                            </dl>
                                        </div>
                                    ))}
                                </div>
                                <p className="lg-muted">리뷰로에 등록된 장소 정보 기준이에요. 가기 전에 한 번 더 확인해 주세요.</p>
                            </section>
                        )}

                        {guide.souvenirs?.length > 0 && (
                            <section className="lg-card">
                                <h3><Icon name="heart" size={15} />먹거리 · 포장 · 선물</h3>
                                <ul className="lg-list">{guide.souvenirs.map((t, i) => <li key={i}>{t}</li>)}</ul>
                            </section>
                        )}

                        {guide.checklist?.length > 0 && (
                            <section className="lg-card">
                                <h3><Icon name="calendar" size={15} />출발 전 체크리스트</h3>
                                <ul className="lg-check">
                                    {guide.checklist.map((t, i) => (
                                        <li key={i}><label><input type="checkbox" />{t}</label></li>
                                    ))}
                                </ul>
                            </section>
                        )}

                        {/* 고쳐 달라기 */}
                        {/* 꼬리말 (PNG 로 내려받을 때 출처가 남게) */}
                        <p className="lg-stamp">리뷰로 · 네로가 만든 여행 가이드</p>

                        <section className="lg-card lg-revise lg-no-capture">
                            <h3>이 가이드에서 바꾸고 싶은 점</h3>
                            <div className="lg-chips">
                                {CHIPS.map((c) => <button type="button" key={c} onClick={() => setRequest(c)}>{c}</button>)}
                            </div>
                            <div className="lg-revise-row">
                                <textarea rows={2} maxLength={500} value={request}
                                          onChange={(e) => setRequest(e.target.value)}
                                          placeholder="예: 둘째 날 팁에 사진 찍기 좋은 시간도 넣어 줘" />
                                <button type="button" className="btn btn-primary btn-sm"
                                        disabled={!request.trim()} onClick={() => ask(request.trim())}>고쳐 달라기</button>
                            </div>
                            <p className="lg-muted">고친 결과도 저장하기 전까지는 초안이에요. 가이드는 저장된 일정 기준으로 만들어요.</p>
                        </section>
                    </>
                )}
            </div>
        </div>
    );
}
