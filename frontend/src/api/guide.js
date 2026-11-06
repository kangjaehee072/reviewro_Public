// AI 여행 가이드 (여행 계획 짜기 → 가이드 화면)
function csrf() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    return token && header ? { [header.content]: token.content } : {};
}

// 서버가 준 문장(text/plain)을 그대로 오류로 올린다. 연결 자체가 안 되면 TypeError 라 문장을 바꿔 준다
async function call(url, options) {
    let res;
    try {
        res = await fetch(url, options);
    } catch (e) {
        throw new Error('서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.');
    }
    if (res.status === 401) {
        window.location.href = '/login';
        throw new Error('로그인이 필요해요.');
    }
    if (!res.ok) throw new Error(await res.text() || '요청을 처리하지 못했어요.');
    return res;
}

// { facts, saved }
export async function fetchGuide(planId) {
    return (await call(`/api/travels/${planId}/guide`)).json();
}

// instruction 이 없으면 새로 만들고, 있으면 current 를 그 요청대로 고친 초안을 돌려준다
export async function draftGuide(planId, instruction, current) {
    const body = instruction ? { instruction, current } : {};
    return (await call(`/api/travels/${planId}/guide/draft`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...csrf() },
        body: JSON.stringify(body)
    })).json();
}

export async function saveGuide(planId, guide) {
    await call(`/api/travels/${planId}/guide`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...csrf() },
        body: JSON.stringify(guide)
    });
}
