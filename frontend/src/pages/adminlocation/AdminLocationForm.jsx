import { useEffect, useState } from 'react';

const EMPTY = {
    locationName: '',
    locationType: '',
    locationAddress: '',
    locationAddressTw: '',
    locationNumber: '',
    locationDayofweek: [],
    locationDayoff: [],
    locationOpentime: '',
    locationClosetime: '',
    locationURL: '',
    locationElement: '',
    locationDescription: '',
    locationCategory: [] // 카테고리(다중 선택) 배열
};

export default function AdminLocationForm({draft, initial, onSave, onCancel}) {
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState(null);

    const [typeOptions, setTypeOptions] = useState([]);
    const [categoryOptions, setCategoryOptions] = useState([]);

    useEffect(() => {
        fetch('/api/types')
            .then(res => res.json())
            .then(data => setTypeOptions(data))
            .catch(err => console.log('타입 목록을 불러오는데 실패했습니다.', err));

        fetch('/api/categories')
            .then(res => res.json())
            .then(data => setCategoryOptions(data))
            .catch(err => console.log('카테고리 목록을 불러오는데 실패했습니다.', err));
    },[]);

    // initial 데이터 병합 및 배열 처리
    const [form, setForm] = useState(() =>{
        const base ={...EMPTY, ...initial};
        return {
            ...base,
            locationDayofweek: typeof base.locationDayofweek === 'string'
                ? base.locationDayofweek.split(',').filter(Boolean)
                : Array.isArray(base.locationDayofweek) ? base.locationDayofweek : [],
            locationDayoff: typeof base.locationDayoff === 'string'
                ? base.locationDayoff.split(',').filter(Boolean)
                : Array.isArray(base.locationDayoff) ? base.locationDayoff : [],
            // 장소 카테고리도 문자열/배열 안전하게 배열로 변환 (숫자로 매핑)
            locationCategory: typeof base.locationCategory === 'string'
                ? base.locationCategory.split(',').filter(Boolean).map(Number)
                : Array.isArray(base.locationCategory) ? base.locationCategory.map(Number) : [],
        }
    });

    const editing = !!initial;

    // 입력칸 name 과 form 키를 맞춰두면 이 함수 하나로 전부 처리
    const change = (e) => setForm({ ...form, [e.target.name]: e.target.value });

    // 입력칸 다중 선택용 핸들러
    const changCheckbox = (name, value) => {
        const numValue = Number(value);
        const list = form[name] || [];
        const updated = list.includes(value)
            ? list.filter((item) => item !== value)
            : [...list, value];
        setForm({ ...form, [name]: updated });
    }

    const submit = async (e) => {
        e.preventDefault();
        if ((!draft && !editing)|| saving) return;
        setSaving(true);
        setMessage(null);
        try {
            const payload = {
                ...form,
                // 좌표 반영
                locationLat: draft?.lat ?? initial?.locationLat,
                locationLng: draft?.lng ?? initial?.locationLng,

                // 타입 ID 숫자 변환
                typeId: form.locationType ? Number(form.locationType) : null,

                // 카테고리 ID 배열을 확실한 숫자 리스트(List<Long> 매핑용)로 전달
                categories: Array.isArray(form.locationCategory)
                    ? form.locationCategory.map(Number).filter(n => !isNaN(n))
                    : [],

                // 요일/휴무일은 문자열로 변환
                locationDayofweek: Array.isArray(form.locationDayofweek)
                    ? form.locationDayofweek.join(',')
                    : form.locationDayofweek,
                locationDayoff: Array.isArray(form.locationDayoff)
                    ? form.locationDayoff.join(',')
                    : form.locationDayoff,
            };

            await onSave(payload);
            if(!editing) setForm (EMPTY);
        } catch (err) {
            setMessage({ type: 'error', text: err.message });
        } finally {
            setSaving(false);
        }
    };

    return (
        <form onSubmit={submit}>
            <p className="hint">
                {draft
                    ? `위치 ${draft.lat.toFixed(5)}, ${draft.lng.toFixed(5)}`
                    : '지도를 클릭해 위치를 찍어주세요'}
            </p>

            <div className="field">
                <label>장소명<span className="req">*</span></label>
                <input name="locationName" value={form.locationName}
                       onChange={change} required/>
            </div>
            <div className="field">
                <label>주소</label>
                <input name="locationAddress" value={form.locationAddress}
                       onChange={change}/>
            </div>
            <div className="field">
                <label>구주소</label>
                <input name="locationAddressTw" value={form.locationAddressTw}
                       onChange={change}/>
            </div>
            <div className="field">
                <label>전화번호</label>
                <input type="tel" name="locationNumber" value={form.locationNumber}
                       onChange={change} required/>
            </div>

            <div className="field">
                <label>장소 타입</label>
                <select name="locationType" value={form.locationType}
                        onChange={change}>
                    <option value="">선택</option>
                    {typeOptions.map(item => (
                        <option key={item.typeId} value={item.typeId}>
                            {item.typeName}
                        </option>
                    ))}
                </select>
            </div>

            {/* 장소 카테고리 체크박스 영역 수정 완료 */}
            <div className="field">
                <label>장소 카테고리 <span className="req">*</span> </label>
                <div className="checkbox-group">
                    {categoryOptions.map(item => (
                        <label key={item.typeId}>
                            <input
                                type="checkbox"
                                checked={form.locationCategory?.includes(item.categoryId)}
                                onChange={() => changCheckbox('locationCategory', item.categoryId)}
                            />
                            {item.categoryName}
                        </label>
                    ))}
                </div>
            </div>

            <div className="field">
                <label>운영일 <span className="req">*</span> </label>
                <div className="checkbox-group">
                    {[
                        {label:'월', val:'mon'},{label:'화', val:'tue'},{label:'수', val:'wed'},
                        {label:'목', val:'thu'},{label:'금', val:'fri'},{label:'토', val:'sat'},
                        {label:'일', val:'sun'},{label:'연중무휴', val:'every'}
                    ].map(item => (
                        <label key={item.label}>
                            <input
                                type="checkbox"
                                checked={form.locationDayofweek?.includes(item.val)}
                                onChange={() => changCheckbox('locationDayofweek',item.val)}
                            />{item.label}
                        </label>
                    ))}
                </div>
            </div>

            <div className="field">
                <label>휴무일 <span className="req">*</span> </label>
                <div className="checkbox-group">
                    {[
                        {label:'월', val:'mon'},{label:'화', val:'tue'},{label:'수', val:'wed'},
                        {label:'목', val:'thu'},{label:'금', val:'fri'},{label:'토', val:'sat'},
                        {label:'일', val:'sun'},{label:'공휴일', val:'holiday'}
                    ].map(item => (
                        <label key={item.label}>
                            <input
                                type="checkbox"
                                checked={form.locationDayoff?.includes(item.val)}
                                onChange={() => changCheckbox('locationDayoff',item.val)}
                            />{item.label}
                        </label>
                    ))}
                </div>
            </div>

            <div className="field">
                <label>운영시간<span className="req">*</span></label>
                <input type="time" id="locationOpentime" name="locationOpentime" value={form.locationOpentime || ''} onChange={change} required/> ~
                <input type="time" id="locationClosetime" name="locationClosetime" value={form.locationClosetime || ''} onChange={change} required/>
            </div>

            <div className="field">
                <label>홈페이지 URL</label>
                <input type="text" name="locationURL" value={form.locationURL}
                       onChange={change}/>
            </div>

            <div className="field">
                <label>오행</label>
                <select name="locationElement" value={form.locationElement}
                        onChange={change}>
                    <option value="" disabled>장소에 맞는 오행을 선택해주세요</option>
                    <option value="wood">목(木)</option>
                    <option value="fire">화(火)</option>
                    <option value="earth">토(土)</option>
                    <option value="metal">금(金)</option>
                    <option value="water">수(水)</option>
                </select>
            </div>

            <div className="field">
                <label>설명</label>
                <textarea name="locationDescription" value={form.locationDescription}
                          onChange={change}/>
            </div>

            <button className="btn btn-primary btn-full" disabled={!draft && !editing || saving}>
                {saving ? '저장 중...' : '저장'}
            </button>

            {message && (
                <div className={'notice notice-' + message.type}>{message.text}</div>
            )}
        </form>
    );
}