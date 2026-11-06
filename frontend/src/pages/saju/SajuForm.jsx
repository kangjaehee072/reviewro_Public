import { useState } from 'react';

export default function SajuForm({ onSubmit }) {
    const [name, setName] = useState('');
    const [date, setDate] = useState('');
    const [time, setTime] = useState('');
    const [timeUnknown, setTimeUnknown] = useState(false);

    const submit = (e) => {
        e.preventDefault();
        onSubmit({ name: name.trim(), date, time: timeUnknown ? '' : time });
    };

    return (
        <form className="sj-form" onSubmit={submit}>
            <h3 className="sj-title">사주 정보 입력</h3>
            <div>
                <div className="field">
                    <label>이름 <span className="opt">선택</span></label>
                    <input value={name} maxLength={20} placeholder="결과에 표시돼요"
                           onChange={(e) => setName(e.target.value)} />
                </div>

                <div className="field">
                    <label>생년월일 (양력)<span className="req">*</span></label>
                    <input type="date" min="1900-01-01" max="2100-12-31" required value={date}
                           onChange={(e) => setDate(e.target.value)} />
                </div>

                <div className="field">
                    <label>태어난 시각</label>
                    <input type="time" value={time} disabled={timeUnknown} required={!timeUnknown}
                           onChange={(e) => setTime(e.target.value)} />
                    <label className="sj-check">
                        <input type="checkbox" checked={timeUnknown}
                               onChange={(e) => setTimeUnknown(e.target.checked)} />
                        시각을 몰라요
                    </label>
                </div>

                <button className="btn btn-primary btn-full">추천받기</button>
                <p className="sj-note">서울 기준 진태양시(약 30분)와 서머타임을 자동으로 보정해요</p>
            </div>
        </form>
    );
}
