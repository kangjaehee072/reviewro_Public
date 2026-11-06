import React, { useState, useEffect, useRef } from 'react';
import Icon from './core/icons.jsx';
import { dayColor } from './core/dayColors.js';

function csrf() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    return token && header ? { [header.content]: token.content } : {};
}

function useOnClickOutside(ref, handler) {
    useEffect(() => {
        const listener = (event) => {
            if (!ref.current || ref.current.contains(event.target)) {
                return;
            }
            handler(event);
        };
        document.addEventListener('mousedown', listener);
        document.addEventListener('touchstart', listener);

        return () => {
            document.removeEventListener('mousedown', listener);
            document.removeEventListener('touchstart', listener);
        };
    }, [ref, handler]);
}

export default function LocationPlan({ travelList, loading, onFetchTravels, onDetailChange, onOpenGuide, onPickSchedule }) {
    const [isOpen, setIsOpen] = useState(false);
    const [selectedTitle, setSelectedTitle] = useState("여행계획 선택"); // 선택된 타이틀 고정용
    const [selectedDetail, setSelectedDetail] = useState(null);
    const [draggedScheduleId, setDraggedScheduleId] = useState(null);
    const [saving, setSaving] = useState(false);


    // 상세 일정을 부모에게도 알린다 (네로 AI 가 읽는다).
    // 다른 메뉴에 갔다 오면 이 컴포넌트가 빈 상태로 다시 만들어지는데,
    // 그때 null 을 올리면 고른 계획을 AI 가 잊으므로 고른 값만 올린다
    useEffect(() => {
        if (selectedDetail) onDetailChange?.(selectedDetail);
    }, [selectedDetail]);
    const [detailLoading, setDetailLoading] = useState(false);
    const dropdownRef = useRef(null);

    // 새 여행 계획 만들기 (만들면 바로 그 계획을 고른다)
    const today = new Date().toISOString().slice(0, 10);
    const emptyForm = { planTitle: '', planStartDate: today, planEndDate: today, planMember: '', planType: '' };
    const [showCreate, setShowCreate] = useState(false);
    const [createForm, setCreateForm] = useState(emptyForm);
    const [createError, setCreateError] = useState('');
    const [creating, setCreating] = useState(false);

    const openCreate = () => {
        setIsOpen(false);
        setCreateForm(emptyForm);
        setCreateError('');
        setShowCreate(true);
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        setCreateError('');
        setCreating(true);
        try {
            const res = await fetch('/api/travels', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', ...csrf() },
                // 고르지 않은 칸은 빈 글자 대신 null 로 보낸다 (상세 화면이 '자유 여행' 같은 기본값을 보여 주게)
                body: JSON.stringify({
                    ...createForm,
                    planTitle: createForm.planTitle.trim(),
                    planMember: createForm.planMember || null,
                    planType: createForm.planType || null
                })
            });
            if (res.status === 401) {
                window.location.href = '/login';
                return;
            }
            if (!res.ok) throw new Error(await res.text() || '여행 계획을 만들지 못했어요.');
            const planId = await res.json();
            setShowCreate(false);
            onFetchTravels?.();
            await handleSelectTravel({ planId, planTitle: createForm.planTitle.trim() });
        } catch (err) {
            setCreateError(err.message);
        } finally {
            setCreating(false);
        }
    };

    const setField = (key) => (e) => setCreateForm((f) => ({ ...f, [key]: e.target.value }));

    useOnClickOutside(dropdownRef, () => setIsOpen(false));

    const handleToggle = () => {
        if (!isOpen && travelList.length === 0 && onFetchTravels) {
            onFetchTravels();
        }
        setIsOpen((prev) => !prev);
    };

    // 여행 선택 시 상세 스케줄 조회 및 드롭다운 닫기
    const handleSelectTravel = async (item) => {
        setSelectedTitle(item.planTitle); // 버튼 텍스트를 선택한 여행 제목으로 변경
        setIsOpen(false); // 드롭다운 닫기

        setDetailLoading(true);
        try {
            const memberId = localStorage.getItem('memberId') || 1;
            const res = await fetch(`/api/travels/${item.planId}?memberId=${memberId}`);
            if (!res.ok) {
                throw new Error('상세 일정을 불러오지 못했습니다.');
            }

            const detailData = await res.json();
            setSelectedDetail(detailData);
        } catch (err) {
            console.error('상세 일정 조회 실패', err);
        } finally {
            setDetailLoading(false);
        }
    };

    //일정 순서 바꾸기
    const handleDragStart = (e, scheduleId) => {
        e.stopPropagation();
        setDraggedScheduleId(scheduleId);
    };

    const handleDragOver = (e) => {
        e.preventDefault();
    };

    const handleDrop = (e, targetDailyId, targetIndex = 0) => {
        e.preventDefault();
        e.stopPropagation();
        if (!draggedScheduleId || !selectedDetail) return;

        const schedules = [...selectedDetail.schedulePlans];

        //드래그 중인 아이템 찾기
        const draggedIndex = schedules.findIndex(s => s.scheduleId === draggedScheduleId);
        if (draggedIndex === -1) return;

        //드래그 된 아이템 추출 후 제거
        const [draggedItem] = schedules.splice(draggedIndex, 1);
        const movedItem ={...draggedItem, dailyId: targetDailyId};

        //전체 일자 목록을 돌며 각 일차별 스케줄 재구성
        const dailyPlans = selectedDetail.dailyPlans || [];
        let newSchedulePlans = [];

        //일자별로 스케줄 재조립
        dailyPlans.forEach((daily) => {
            //일차별 스케줄 필터링
            let daySchedules = schedules.filter(s => s.dailyId === daily.dailyId);

            //드롭된 위치와 일차가 일치할 경우, 삽입
            if (daily.dailyId === targetDailyId) {
                const insertIndex = typeof targetIndex === 'number'
                    ? Math.min(Math.max(0, targetIndex), daySchedules.length)
                    : daySchedules.length;
                daySchedules.splice(insertIndex, 0, movedItem);
            }

            //해당 일차의 scheduleNumber 재정렬
            const orderScheduleNumber = daySchedules.map((s, index) => ({
                ...s,
                scheduleNumber : index + 1,
            }));
            newSchedulePlans.push(...orderScheduleNumber);
        });

        const updateDetail = {
            ...selectedDetail,
            schedulePlans: newSchedulePlans,
        };
        setSelectedDetail(updateDetail);
        onDetailChange?.(updateDetail);
        setDraggedScheduleId(null);
    };




    // 일정 삭제
    const handleDeleteSchedule = (scheduleId) => {
        if (!selectedDetail) return;

        const updatedSchedulePlans = selectedDetail.schedulePlans
            .filter((schedule) => schedule.scheduleId !== scheduleId)
            .map((schedule, index) => ({
                ...schedule,
                scheduleNumber : index + 1,
            }));

        const updatedDetail = {
            ...selectedDetail,
            schedulePlans: updatedSchedulePlans,
        };

        setSelectedDetail(updatedDetail);
        onDetailChange?.(updatedDetail);
    };


    // 시계 버튼: 그 일정의 시간을 바로 고쳐 저장한다 ([저장] 버튼은 순서만 저장하므로 시간은 따로 바로 저장)
    const [timeEdit, setTimeEdit] = useState(null);   // { scheduleId, start, end, saving, error }

    const openTimeEdit = (schedule) => setTimeEdit({
        scheduleId: schedule.scheduleId,
        start: schedule.scheduleStartTime || '',
        end: schedule.scheduleEndTime || '',
        saving: false,
        error: ''
    });

    const saveTimeEdit = async () => {
        const { scheduleId, start, end } = timeEdit;
        setTimeEdit((t) => ({ ...t, saving: true, error: '' }));
        try {
            const res = await fetch(`/api/travels/schedules/${scheduleId}/time`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json', ...csrf() },
                body: JSON.stringify({ scheduleStartTime: start || null, scheduleEndTime: end || null })
            });
            if (res.status === 401) {
                window.location.href = '/login';
                return;
            }
            if (!res.ok) throw new Error(await res.text() || '시간을 바꾸지 못했어요.');
            const updatedDetail = {
                ...selectedDetail,
                schedulePlans: selectedDetail.schedulePlans.map((s) => s.scheduleId === scheduleId
                    ? { ...s, scheduleStartTime: start || null, scheduleEndTime: end || null } : s)
            };
            setSelectedDetail(updatedDetail);
            onDetailChange?.(updatedDetail);
            setTimeEdit(null);
        } catch (err) {
            setTimeEdit((t) => t && { ...t, saving: false, error: err.message });
        }
    };

    //변경된 일정 DB저장(확정)
    const handleSaveSchedules = async () => {
        if (await saveSchedules()) {
            alert('여행 일정이 성공적으로 저장되었습니다.');
        }
    };

    // AI 가이드: 지금 순서를 먼저 저장해야 가이드가 바뀐 일정으로 만들어진다. 저장되면 오른쪽 칸을 가이드로 바꾼다
    const handleGoGuide = async () => {
        if (await saveSchedules()) {
            onOpenGuide?.();
        }
    };

    // 성공하면 true. 실패 알림은 여기서 띄운다
    const saveSchedules = async () => {
        if (!selectedDetail) return false;
        setSaving(true);

        try{
            const payload = selectedDetail.schedulePlans.map((s, index) => ({
                scheduleId: s.scheduleId,
                dailyId: s.dailyId,
                locationId: s.location.locationId,
                scheduleNumber: index + 1,
                scheduleStartTime: s.scheduleStartTime,
                scheduleEndTime: s.scheduleEndTime,
                scheduleDescription: s.scheduleDescription,
                scheduleRunTime: s.scheduleRunTime,
                scheduleDistance: s.scheduleDistance
            }));

            const res = await fetch(`/api/travels/${selectedDetail.planId}/schedules`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                    ,...csrf()
                },
                body: JSON.stringify(payload),
            });
            if (!res.ok) {
                throw new Error('일정 저장에 실패했습니다.');
            }
            return true;
        }catch (err){
            console.error('일정 저장 오류',err);
            alert('저장 중 오류가 발생했습니다.');
            return false;
        }finally {
            setSaving(false);
        }
    };



    const days = selectedDetail?.dailyPlans || [];

    return (
        <div className="loc-plan">
            {/* 1. 내 여행 계획 고르기 */}
            <div className="loc-picker" ref={dropdownRef}>
                <button type="button" onClick={handleToggle}
                        className={'loc-picker-btn' + (isOpen ? ' open' : '')} aria-expanded={isOpen}>
                    <Icon name="route" size={16} />
                    <span className="loc-picker-label">{selectedTitle}</span>
                    <Icon name="chevronDown" size={16} className="loc-picker-caret" />
                </button>

                {isOpen && (
                    <div className="loc-picker-menu">
                        <div className="loc-picker-head">
                            <span>내 여행 목록</span>
                            <em>{travelList.length}</em>
                            <button type="button" className="loc-picker-new" onClick={openCreate}>
                                <Icon name="plus" size={13} />새 계획
                            </button>
                        </div>

                        {loading ? (
                            <div className="loc-empty">불러오는 중...</div>
                        ) : travelList.length > 0 ? (
                            <ul>
                                {travelList.map((item) => (
                                    <li key={item.planId}>
                                        <button type="button" onClick={() => handleSelectTravel(item)}>
                                            <strong>{item.planTitle}</strong>
                                            {item.planStartDate && (
                                                <span>{item.planStartDate} ~ {item.planEndDate}</span>
                                            )}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        ) : (
                            <div className="loc-empty">등록된 여행 계획이 없습니다.</div>
                        )}
                    </div>
                )}
            </div>

            {showCreate && (
                <form className="loc-create" onSubmit={handleCreate}>
                    <strong>새 여행 계획</strong>
                    <label>
                        <span>여행 제목</span>
                        <input type="text" maxLength={100} required autoFocus
                               value={createForm.planTitle} onChange={setField('planTitle')}
                               placeholder="예: 부산 1박 2일" />
                    </label>
                    <div className="loc-create-dates">
                        <label>
                            <span>출발일</span>
                            <input type="date" required value={createForm.planStartDate}
                                   onChange={(e) => {
                                       const v = e.target.value;
                                       // 출발일을 도착일 뒤로 옮기면 도착일도 같이 민다
                                       setCreateForm((f) => ({ ...f, planStartDate: v, planEndDate: f.planEndDate < v ? v : f.planEndDate }));
                                   }} />
                        </label>
                        <label>
                            <span>도착일</span>
                            <input type="date" required min={createForm.planStartDate}
                                   value={createForm.planEndDate} onChange={setField('planEndDate')} />
                        </label>
                    </div>
                    <div className="loc-create-dates">
                        <label>
                            <span>인원</span>
                            <select value={createForm.planMember} onChange={setField('planMember')}>
                                <option value="">선택 안 함</option>
                                <option value="혼자 (1인)">혼자 (1인)</option>
                                <option value="2인">2인</option>
                                <option value="3~4인">3~4인</option>
                                <option value="5인 이상 단체">5인 이상 단체</option>
                            </select>
                        </label>
                        <label>
                            <span>여행 유형</span>
                            <select value={createForm.planType} onChange={setField('planType')}>
                                <option value="">선택 안 함</option>
                                <option value="자유 여행">자유 여행</option>
                                <option value="가족 여행">가족 여행</option>
                                <option value="커플/우정 여행">커플/우정 여행</option>
                                <option value="식도락/맛집">식도락/맛집</option>
                                <option value="휴양/힐링">휴양/힐링</option>
                            </select>
                        </label>
                    </div>
                    {createError && <div className="notice notice-error">{createError}</div>}
                    <div className="loc-create-actions">
                        <button type="button" className="btn btn-line btn-sm" onClick={() => setShowCreate(false)}>취소</button>
                        <button type="submit" className="btn btn-primary btn-sm" disabled={creating}>
                            {creating ? '만드는 중…' : '만들기'}
                        </button>
                    </div>
                </form>
            )}

            {!selectedDetail && !detailLoading && !showCreate && (
                <div className="loc-empty loc-empty-lg">
                    <Icon name="map" size={28} />
                    <p>여행 계획을 고르면<br/>일차별 일정이 여기에 나와요.</p>
                    <button type="button" className="btn btn-primary btn-sm" onClick={openCreate}>
                        <Icon name="plus" size={14} />새 여행 계획 만들기
                    </button>
                </div>
            )}

            {/* 2. 상세 일정 (일차별 타임라인, 끌어서 순서·일차 변경) */}
            {selectedDetail && (
                <div className="loc-plan-detail">
                    <div className="loc-plan-head">
                        <div>
                            <strong>{selectedDetail.planTitle}</strong>
                            <span>
                                <Icon name="calendar" size={13} />
                                {selectedDetail.planStartDate} ~ {selectedDetail.planEndDate} · {days.length}일
                            </span>
                        </div>
                        <div className="loc-plan-actions">
                            <button type="button" onClick={handleGoGuide} disabled={saving}
                                    className="btn btn-line btn-sm">
                                <Icon name="sparkles" size={14} />AI 가이드
                            </button>
                            <button type="button" onClick={handleSaveSchedules} disabled={saving}
                                    className="btn btn-primary btn-sm">
                                <Icon name="save" size={14} />{saving ? '저장 중…' : '저장'}
                            </button>
                        </div>
                    </div>

                    {detailLoading ? (
                        <div className="loc-empty">상세 일정을 불러오는 중...</div>
                    ) : (
                        <div className="loc-days">
                            {days.map((daily, index) => {
                                const items = (selectedDetail.schedulePlans || [])
                                    .filter((schedule) => schedule.dailyId === daily.dailyId)
                                    .sort((a, b) => a.scheduleNumber - b.scheduleNumber);
                                return (
                                    <section key={daily.dailyId} className="loc-day"
                                             onDragOver={handleDragOver}
                                             onDrop={(e) => handleDrop(e, daily.dailyId, 0)}>
                                        <header className="loc-day-head">
                                            <span className="loc-day-no" style={{ background: dayColor(index) }}>DAY {index + 1}</span>
                                            <span className="loc-day-date">{daily.dailyDate || '날짜 없음'}</span>
                                            <span className="loc-day-count">{items.length}곳</span>
                                        </header>

                                        {items.length === 0 ? (
                                            <p className="loc-day-empty">장소를 이곳으로 끌어다 놓거나, 관광지 찾기에서 추가하세요.</p>
                                        ) : (
                                            <ol className="loc-stops">
                                                {items.map((schedule, scheduleIndex) => (
                                                    <li key={schedule.scheduleId}
                                                        className={'loc-stop' + (draggedScheduleId === schedule.scheduleId ? ' dragging' : '')
                                                            + (timeEdit?.scheduleId === schedule.scheduleId ? ' editing' : '')}
                                                        draggable={timeEdit?.scheduleId !== schedule.scheduleId}
                                                        onDragStart={(e) => handleDragStart(e, schedule.scheduleId)}
                                                        onDragEnd={(e) => handleDrop(e, daily.dailyId, scheduleIndex)}
                                                        onClick={() => onPickSchedule?.(schedule)}
                                                        title="지도에서 보기">
                                                        <span className="loc-stop-no" style={{ background: dayColor(index) }}>{schedule.scheduleNumber}</span>
                                                        <span className="loc-stop-body">
                                                            <strong>{schedule.location?.locationName || schedule.scheduleDescription || '일정 내용'}</strong>
                                                            {(schedule.scheduleStartTime || schedule.scheduleEndTime) && (
                                                                <em className="loc-stop-time">
                                                                    <Icon name="clock" size={11} />
                                                                    {schedule.scheduleStartTime || ''}
                                                                    {schedule.scheduleEndTime ? ` ~ ${schedule.scheduleEndTime}` : ''}
                                                                </em>
                                                            )}
                                                            {schedule.location?.locationAddress && (
                                                                <span>{schedule.location.locationAddress}</span>
                                                            )}
                                                        </span>
                                                        <span className="loc-stop-grip" title="끌어서 순서 바꾸기">
                                                            <Icon name="grip" size={15} />
                                                        </span>
                                                        <button type="button" className="loc-stop-del loc-stop-clock"
                                                                onClick={(e) => { e.stopPropagation(); openTimeEdit(schedule); }}
                                                                title="시간 바꾸기" aria-label="시간 바꾸기">
                                                            <Icon name="clock" size={14} />
                                                        </button>
                                                        <button type="button" className="loc-stop-del"
                                                                onClick={(e) => { e.stopPropagation(); handleDeleteSchedule(schedule.scheduleId); }}
                                                                title="일정 삭제" aria-label="일정 삭제">
                                                            <Icon name="x" size={14} />
                                                        </button>

                                                        {/* 시간 고치기 (이 일정에서 시계 버튼을 눌렀을 때만) */}
                                                        {timeEdit?.scheduleId === schedule.scheduleId && (
                                                            <div className="loc-time-edit" onClick={(e) => e.stopPropagation()}>
                                                                <input type="time" aria-label="시작 시간" value={timeEdit.start} autoFocus
                                                                       onChange={(e) => setTimeEdit((t) => ({ ...t, start: e.target.value }))} />
                                                                <span className="loc-time-edit-sep">~</span>
                                                                <input type="time" aria-label="종료 시간" value={timeEdit.end} min={timeEdit.start || undefined}
                                                                       onChange={(e) => setTimeEdit((t) => ({ ...t, end: e.target.value }))} />
                                                                <div className="loc-time-edit-actions">
                                                                    <button type="button" className="btn btn-line btn-sm"
                                                                            onClick={() => setTimeEdit((t) => ({ ...t, start: '', end: '' }))}>비우기</button>
                                                                    <button type="button" className="btn btn-line btn-sm" onClick={() => setTimeEdit(null)}>취소</button>
                                                                    <button type="button" className="btn btn-primary btn-sm" onClick={saveTimeEdit}
                                                                            disabled={timeEdit.saving}>{timeEdit.saving ? '저장 중…' : '저장'}</button>
                                                                </div>
                                                                {timeEdit.error && <p className="loc-time-edit-error">{timeEdit.error}</p>}
                                                            </div>
                                                        )}
                                                    </li>
                                                ))}
                                            </ol>
                                        )}
                                    </section>
                                );
                            })}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
