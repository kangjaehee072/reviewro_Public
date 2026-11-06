import React, { useState, useEffect } from "react";
import axios from "axios";

function csrf() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    return token && header ? { [header.content]: token.content } : {};
}

export default function AddSchedule({ place, travelPlansList, onClose, onScheduleAdded }) {
    const [targetPlanId, setTargetPlanId] = useState('');
    const [planDetail, setPlanDetail] = useState(null);
    const [targetDailyId, setTargetDailyId] = useState('');
    const [scheduleStartTime, setScheduleStartTime] = useState('');
    const [scheduleEndTime, setScheduleEndTime] = useState('');
    const [scheduleDescription, setScheduleDescription] = useState('');

    // selectedPlan 찾기
    const selectedPlan = (travelPlansList || []).find(
        p => String(p.planId || p.id) === String(targetPlanId)
    );

    //Plan이 바뀔때마다 DailyPlan 불러오기
    useEffect(() => {
        if (!targetPlanId) {
            setPlanDetail(null);
            setTargetDailyId('');
            return;
        }

        const fetchDetail = async () => {
            try {
                const memberId = localStorage.getItem('memberId') || 1;
                const res = await fetch(`/api/travels/${targetPlanId}?memberId=${memberId}`);

                if (!res.ok) {
                    throw new Error('여행 상세 조회에 실패했습니다.');
                }

                const data = await res.json();
                setPlanDetail(data); // 일차 목록이 포함된 상세 데이터 저장
            } catch (err) {
                console.error('여행 상세 조회 실패', err);
            }
        };
        fetchDetail();
    }, [targetPlanId]);



    // 일정 추가 제출 핸들러
    const handleAddLocationToSchedule = async () => {
        if (!targetDailyId) {
            alert('일차(Day)를 선택해주세요.');
            return;
        }

        const locationId = place?.locationId || place?.id;
        if(!locationId) {
            alert('장소 ID 정보를 찾을 수 없습니다.');
            return;
        }

        // 백엔드 SchedulePlansDTO 매핑 구조 (locationDTO 객체)
        const schedulePayload = {
            location: {
                locationId: locationId
            },
            scheduleStartTime: scheduleStartTime,
            scheduleEndTime: scheduleEndTime,
            scheduleDescription: scheduleDescription || place.locationName || '장소 방문'
        };

        try {
           const res =  await fetch(`/api/travels/dailies/${targetDailyId}/schedules`, {
                method: 'POST',
                headers:{
                    'Content-type': 'application/json',
                    ...csrf()
                },
                body:JSON.stringify(schedulePayload)

            });

            if(!res.ok){
                throw new Error('일정 추가에 실패했습니다.');
            }
            alert('일정에 성공적으로 추가되었습니다!');
            onClose(); // 모달 닫기

            if (onScheduleAdded) {
                onScheduleAdded(targetPlanId);
            }
        } catch (error) {
            console.error('일정 추가 실패:', error);
            alert('일정 추가 중 오류가 발생했습니다.');
        }
    };

    return (
        <div className="modal-overlay">
            <div className="modal-box">
                <h3>어느 여행 일정에 추가할까요?</h3>

                {/* 1. 여행 선택 */}
                <div style={{ marginBottom: '8px' }}>
                    <select
                        value={targetPlanId}
                        onChange={(e) => {
                            setTargetPlanId(e.target.value);
                            setTargetDailyId('');
                        }}
                        style={{ width: '100%', padding: '6px' }}
                    >
                        <option value="">-- 여행 계획 선택 --</option>
                        {(travelPlansList || []).map(plan => (
                            <option key={plan.planId || plan.id} value={plan.planId || plan.id}>
                                {plan.title || plan.planTitle} ({plan.startDate || plan.planStartDate} ~ {plan.endDate || plan.planEndDate})
                            </option>
                        ))}
                    </select>
                </div>

                {/* 2. 일차(Day) 선택 */}
                {planDetail && (
                    <div style={{ marginBottom: '8px' }}>
                        <select
                            value={targetDailyId}
                            onChange={(e) => setTargetDailyId(e.target.value)}
                            style={{ width: '100%', padding: '6px' }}
                        >
                            <option value="">-- 일차(Day) 선택 --</option>
                            {planDetail.dailyPlans?.map(daily => (
                                <option key={daily.dailyId} value={daily.dailyId}>
                                    {daily.dailyNumber}일차 ({daily.dailyDate})
                                </option>
                            ))}
                        </select>
                    </div>
                )}

                {/* 3. 시간 및 설명 입력 */}
                {targetDailyId && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginBottom: '8px' }}>
                        {/* 시계로 고르게 해서 늘 "10:00" 모양으로 저장된다 (AI 가이드가 운영시간과 비교한다) */}
                        <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px' }}>
                            시작
                            <input
                                type="time"
                                aria-label="시작 시간"
                                value={scheduleStartTime}
                                onChange={(e) => setScheduleStartTime(e.target.value)}
                                style={{ padding: '6px', flex: 1 }}
                            />
                        </label>
                        <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px' }}>
                            종료
                            <input
                                type="time"
                                aria-label="종료 시간"
                                min={scheduleStartTime || undefined}
                                value={scheduleEndTime}
                                onChange={(e) => setScheduleEndTime(e.target.value)}
                                style={{ padding: '6px', flex: 1 }}
                            />
                        </label>
                        <input
                            type="text"
                            placeholder="설명 / 메모"
                            value={scheduleDescription}
                            onChange={(e) => setScheduleDescription(e.target.value)}
                            style={{ padding: '6px' }}
                        />
                    </div>
                )}

                <div style={{ display: 'flex', gap: '6px' }}>
                    <button
                        type="button"
                        onClick={handleAddLocationToSchedule}
                        style={{ flex: 1, background: '#339af0', color: '#fff', border: 'none', padding: '6px', borderRadius: '4px', cursor: 'pointer' }}
                    >
                        확인 및 추가
                    </button>
                    <button
                        type="button"
                        onClick={onClose}
                        style={{ background: '#868e96', color: '#fff', border: 'none', padding: '6px 12px', borderRadius: '4px', cursor: 'pointer' }}
                    >
                        취소
                    </button>
                </div>
            </div>
        </div>
    );
}