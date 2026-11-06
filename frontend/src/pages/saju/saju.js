// 사주 계산 (lunar-javascript) + 한국 시각 보정 + 한글 변환
import { Solar } from 'lunar-javascript';

const MINUTE = 60 * 1000;

// 동경 127.5° 평균태양시 = UTC+8:30 (한국 만세력의 진태양시 보정)
const SOLAR_TIME_OFFSET = 510;

// 오행 (프로젝트 locationElement 값과 같은 key)
export const ELEMENTS = [
    { key: 'wood', name: '목', hanja: '木' },
    { key: 'fire', name: '화', hanja: '火' },
    { key: 'earth', name: '토', hanja: '土' },
    { key: 'metal', name: '금', hanja: '金' },
    { key: 'water', name: '수', hanja: '水' }
];

// 천간·지지 → [한글, 오행]
const CHARS = {
    '甲': ['갑', 'wood'], '乙': ['을', 'wood'], '丙': ['병', 'fire'], '丁': ['정', 'fire'],
    '戊': ['무', 'earth'], '己': ['기', 'earth'], '庚': ['경', 'metal'], '辛': ['신', 'metal'],
    '壬': ['임', 'water'], '癸': ['계', 'water'],
    '子': ['자', 'water'], '丑': ['축', 'earth'], '寅': ['인', 'wood'], '卯': ['묘', 'wood'],
    '辰': ['진', 'earth'], '巳': ['사', 'fire'], '午': ['오', 'fire'], '未': ['미', 'earth'],
    '申': ['신', 'metal'], '酉': ['유', 'metal'], '戌': ['술', 'earth'], '亥': ['해', 'water']
};

// 십신 (라이브러리가 간체로 줌)
const SHISHEN = {
    '比肩': '비견', '劫财': '겁재', '食神': '식신', '伤官': '상관', '偏财': '편재',
    '正财': '정재', '七杀': '편관', '正官': '정관', '偏印': '편인', '正印': '정인'
};

// 그 시절 서울 시계가 UTC 보다 몇 분 빨랐는지 (서머타임, 1954~61년 표준시 변경 포함)
function seoulOffsetMinutes(utcMs) {
    const zone = new Intl.DateTimeFormat('en-US', { timeZone: 'Asia/Seoul', timeZoneName: 'longOffset' })
        .formatToParts(new Date(utcMs))
        .find((p) => p.type === 'timeZoneName').value;   // "GMT+09:00"
    const m = zone.match(/GMT([+-])(\d{2}):(\d{2})/);
    if (!m) return 540;
    return (m[1] === '-' ? -1 : 1) * (Number(m[2]) * 60 + Number(m[3]));
}

// 입력한 서울 시각 → 진태양시
function toSolarTime(y, m, d, h, mi) {
    const wall = Date.UTC(y, m - 1, d, h, mi);
    const utc = wall - seoulOffsetMinutes(wall - 540 * MINUTE) * MINUTE;
    return new Date(utc + SOLAR_TIME_OFFSET * MINUTE);
}

function toChar(hanja) {
    const [hangul, element] = CHARS[hanja];
    return { hanja, hangul, element };
}

function toPillar(label, ganZhi, god) {
    return { label, god, gan: toChar(ganZhi[0]), zhi: toChar(ganZhi[1]) };
}

// { name, date: 'YYYY-MM-DD', time: 'HH:mm' 또는 '' } → 명식
export function calcSaju({ name, date, time }) {
    const [y, m, d] = date.split('-').map(Number);

    // 시각을 모르면 정오로 계산하고 시주는 비운다
    const t = time
        ? toSolarTime(y, m, d, ...time.split(':').map(Number))
        : new Date(Date.UTC(y, m - 1, d, 12, 0));

    const ec = Solar.fromYmdHms(
        t.getUTCFullYear(), t.getUTCMonth() + 1, t.getUTCDate(), t.getUTCHours(), t.getUTCMinutes(), 0
    ).getLunar().getEightChar();

    // 화면 순서: 시 일 월 년 (오른쪽에서 왼쪽으로 읽는 전통 순서)
    const pillars = [
        time ? toPillar('시주', ec.getTime(), SHISHEN[ec.getTimeShiShenGan()]) : { label: '시주', god: '시각 모름', gan: null },
        toPillar('일주', ec.getDay(), '나'),
        toPillar('월주', ec.getMonth(), SHISHEN[ec.getMonthShiShenGan()]),
        toPillar('년주', ec.getYear(), SHISHEN[ec.getYearShiShenGan()])
    ];

    const counts = { wood: 0, fire: 0, earth: 0, metal: 0, water: 0 };
    pillars.filter((p) => p.gan).forEach((p) => {
        counts[p.gan.element]++;
        counts[p.zhi.element]++;
    });

    // 가장 많은 / 가장 적은 오행 (같으면 목화토금수 순서로 앞의 것)
    // 부족한 쪽이 동점이면 일간의 오행은 고르지 않는다 (나 자신이 그 기운이라서)
    const dayElement = pillars[1].gan.element;
    const strong = ELEMENTS.reduce((a, b) => (counts[b.key] > counts[a.key] ? b : a));
    const weak = [...ELEMENTS]
        .sort((a, b) => (a.key === dayElement) - (b.key === dayElement))
        .reduce((a, b) => (counts[b.key] < counts[a.key] ? b : a));

    // 십신 개수: 천간(일간 제외) + 지지 본기(지장간 첫 글자)
    // 동점이면 앞에 둔 것이 먼저 뽑힌다 → 성향을 가장 잘 드러내는 월지·월간을 앞에 둔다
    const gods = [
        ec.getMonthShiShenZhi()[0], ec.getMonthShiShenGan(), ec.getDayShiShenZhi()[0],
        ...(time ? [ec.getTimeShiShenGan(), ec.getTimeShiShenZhi()[0]] : []),
        ec.getYearShiShenGan(), ec.getYearShiShenZhi()[0]
    ].map((g) => SHISHEN[g]);
    const godCounts = {};
    gods.forEach((g) => { godCounts[g] = (godCounts[g] || 0) + 1; });

    // 많이 나온 십신 2개
    const mainGods = Object.entries(godCounts)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 2)
        .map(([god, count]) => ({ god, count }));

    return {
        name,
        pillars,
        counts,
        total: time ? 8 : 6,
        dayGan: pillars[1].gan,
        dayPillar: pillars[1].gan.hanja + pillars[1].zhi.hanja,
        strong: strong.key,
        weak: weak.key,
        mainGods
    };
}

export function elementName(key) {
    const el = ELEMENTS.find((x) => x.key === key);
    return el.name + '(' + el.hanja + ')';
}

// AI 에게 넘길 사주 요약 (계산은 여기서 끝내고 AI 는 해석만 한다)
export function sajuSummary(saju) {
    const pillarText = saju.pillars
        .map((p) => p.label + ' ' + (p.gan ? p.gan.hangul + p.zhi.hangul + '(' + p.gan.hanja + p.zhi.hanja + ')' : '모름'))
        .join(', ');
    const countText = ELEMENTS.map((el) => el.name + ' ' + saju.counts[el.key]).join(', ');

    return [
        '내 사주 정보야. 이걸 보고 여행을 추천해 줘.',
        '- 사주: ' + pillarText,
        '- 일간(나): ' + saju.dayGan.hangul + elementName(saju.dayGan.element),
        '- 오행 개수: ' + countText,
        '- 가장 많은 기운: ' + elementName(saju.strong) + ' / 가장 부족한 기운: ' + elementName(saju.weak),
        '- 두드러진 십신: ' + saju.mainGods.map((g) => g.god).join(', ')
    ].join('\n');
}
