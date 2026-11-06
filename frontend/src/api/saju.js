// 풀이 문구 전체 → { 'DAY_PILLAR:甲子': {title, content}, ... } 로 바꿔서 돌려준다
export async function fetchSajuTexts() {
    const res = await fetch('/api/saju/texts');
    if (!res.ok) throw new Error('풀이를 불러오지 못했습니다.');
    const list = await res.json();
    const texts = {};
    list.forEach((t) => {
        texts[t.sajuTextType + ':' + t.sajuTextCode] = { title: t.sajuTextTitle, content: t.sajuTextContent };
    });
    return texts;
}

// AI 가 찾은 장소에는 좌표가 없어서 지도에 찍을 때 한 곳씩 가져온다
export async function fetchPlace(locationId) {
    const res = await fetch('/api/locations/' + locationId);
    return res.ok ? res.json() : null;
}

// 부족한 오행에 어울리는 DB 장소 (AI 없이 서버가 고른다)
export async function fetchSajuPlaces(element) {
    const res = await fetch('/api/saju/places?element=' + encodeURIComponent(element));
    if (!res.ok) throw new Error('장소를 불러오지 못했습니다.');
    return res.json();
}

function csrf() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    return token && header ? { [header.content]: token.content } : {};
}

// 즐겨찾기 저장 (로그인 필요)
export async function saveBookmark(locationId) {
    const res = await fetch('/api/bookmarks/' + locationId, { method: 'POST', headers: csrf() });
    if (!res.ok) throw new Error('저장하지 못했어요. 로그인했는지 확인해 주세요.');
}
