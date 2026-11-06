// 네로(PLANNER)에게 넘길 "지금 화면에 있는 데이터".
// 서버가 메시지 한 개를 10000자까지 받으므로 넉넉히 아래로 자른다
const MAX_CHARS = 6000;

const WEEK = ['일', '월', '화', '수', '목', '금', '토'];

// '2026-10-24' -> '2026-10-24(토)'. 요일이 있어야 휴무일과 맞춰 볼 수 있다
function withWeekday(date) {
    if (!date) return null;
    const [y, m, d] = String(date).slice(0, 10).split('-').map(Number);
    if (!y || !m || !d) return String(date);
    return `${String(date).slice(0, 10)}(${WEEK[new Date(y, m - 1, d).getDay()]})`;
}

// 두 좌표 사이 직선거리(km). 모델은 좌표로 거리 계산을 잘 못해서 여기서 미리 해 둔다
function km(a, b) {
    if (a?.lat == null || a?.lng == null || b?.lat == null || b?.lng == null) return null;
    const rad = (x) => (x * Math.PI) / 180;
    const dLat = rad(b.lat - a.lat);
    const dLng = rad(b.lng - a.lng);
    const h = Math.sin(dLat / 2) ** 2
            + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLng / 2) ** 2;
    return Math.round(12742 * Math.asin(Math.sqrt(h)) * 10) / 10;
}

function hours(open, close) {
    return open || close ? `${open || '?'}~${close || '?'}` : undefined;
}

// 장소 하나를 AI 가 볼 필드만 남긴다
function place(p) {
    if (!p) return null;
    return {
        이름: p.locationName,
        주소: p.locationAddress,
        평점: p.locationReviewAvg || undefined,
        휴무일: p.locationDayoff || undefined,
        영업시간: hours(p.locationOpentime, p.locationClosetime),
        설명: p.locationDescription ? String(p.locationDescription).slice(0, 80) : undefined
    };
}

function planData(detail) {
    const schedules = detail.schedulePlans || [];
    return {
        제목: detail.planTitle,
        기간: `${withWeekday(detail.planStartDate)} ~ ${withWeekday(detail.planEndDate)}`,
        인원: detail.planMember || undefined,
        유형: detail.planType || undefined,
        설명: detail.planDescription || undefined,
        일차: (detail.dailyPlans || []).map((daily, i) => {
            let prev = null;
            const items = schedules
                .filter((s) => s.dailyId === daily.dailyId)
                .sort((a, b) => a.scheduleNumber - b.scheduleNumber)
                .map((s) => {
                    const loc = s.location || {};
                    const here = { lat: loc.locationLat, lng: loc.locationLng };
                    const item = {
                        순서: s.scheduleNumber,
                        장소: loc.locationName || s.locationName || s.scheduleDescription,
                        주소: loc.locationAddress,
                        시작: s.scheduleStartTime || undefined,
                        종료: s.scheduleEndTime || undefined,
                        메모: s.scheduleDescription || undefined,
                        휴무일: loc.locationDayoff || undefined,
                        영업시간: hours(loc.locationOpentime, loc.locationClosetime),
                        이전장소에서_km: prev ? km(prev, here) ?? undefined : undefined
                    };
                    prev = here;
                    return item;
                });
            return { 일차: i + 1, 날짜: withWeekday(daily.dailyDate), 일정: items };
        })
    };
}

const MENU_NAMES = { plans: '여행 계획 짜기', 'place-list': '관광지 찾기', bookmarks: '저장한 장소' };

// 화면에 지금 보이는 것만이 아니라 페이지가 들고 있는 데이터를 전부 모은다.
// (다른 메뉴로 넘어가도 고른 계획·검색 결과·즐겨찾기가 남아 있으므로 같이 보낸다)
function collect({ menu, travelList, planDetail, locations, selected, bookmarks, cond }) {
    return {
        지금_보는_메뉴: MENU_NAMES[menu] || menu,
        보고_있는_장소: place(selected) || undefined,
        선택한_계획: planDetail ? planData(planDetail) : '아직 선택 안 함',
        내_계획_목록: (travelList || []).map((t) => ({
            제목: t.planTitle,
            기간: t.planStartDate ? `${withWeekday(t.planStartDate)} ~ ${withWeekday(t.planEndDate)}` : undefined,
            인원: t.planMember || undefined,
            유형: t.planType || undefined
        })),
        검색_지역: cond?.address || (cond?.lat ? '내 주변' : '선택 안 함'),
        검색_결과: (locations || []).map(place),
        // 즐겨찾기는 그 메뉴를 한 번 열어야 불러온다
        저장한_장소: bookmarks?.length ? bookmarks.map(place) : '아직 안 불러옴'
    };
}

// 지금 보는 메뉴의 목록은 가장 나중에 자른다
const LISTS_BY_MENU = {
    plans: ['검색_결과', '저장한_장소', '내_계획_목록'],
    'place-list': ['저장한_장소', '내_계획_목록', '검색_결과'],
    bookmarks: ['검색_결과', '내_계획_목록', '저장한_장소']
};

// 너무 길면 목록 뒤쪽부터 잘라 낸다
export function buildAdvisorContext(state) {
    const data = collect(state);
    const lists = LISTS_BY_MENU[state.menu] || LISTS_BY_MENU['place-list'];
    let text = JSON.stringify(data);

    for (const key of lists) {
        while (text.length > MAX_CHARS && Array.isArray(data[key]) && data[key].length > 0) {
            data[key] = data[key].slice(0, Math.floor(data[key].length / 2));
            data[key + '_생략'] = '뒤쪽은 길어서 뺐음';
            text = JSON.stringify(data);
        }
    }
    return text.length > MAX_CHARS ? text.slice(0, MAX_CHARS) : text;
}
