function csrf() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    return token && header ? { [header.content]: token.content } : {};
}

// 장소 하나 (사진 목록 images 포함)
export async function fetchLocation(id) {
    const res = await fetch(`/api/locations/${id}`);
    if (!res.ok) throw new Error('장소 정보를 불러오지 못했습니다.');
    return res.json();
}

export async function fetchLocations() {
    const res = await fetch('/api/locations');
    if (!res.ok) throw new Error('목록을 불러오지 못했습니다.');
    return res.json();
}

export async function createLocation(location) {
    const res = await fetch('/api/locations', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...csrf() },
        body: JSON.stringify(location)
    });
    if (res.status === 401) {
        window.location.href = '/login';   // 로그인 안 했으면 보내버림
        return null;
    }
    if (!res.ok) throw new Error(await res.text() || '저장에 실패했습니다.');
    return res.json();// 서버가 돌려준 새 장소 id
}

export async function updateLocation(id, location) {

    const res = await fetch(`/api/locations/${id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json', ...csrf() },
        body: JSON.stringify(location)
    });
    if (res.status === 401) {
        window.location.href = '/login';
        return false;
    }
    if (!res.ok) throw new Error(await res.text() || '수정에 실패했습니다.');
    return true;
}

export async function deleteLocation(id) {
    const res = await fetch(`/api/locations/${id}`, {
        method: 'DELETE',
        headers: csrf()});
    if (res.status === 401) {
        window.location.href = '/login';
        return false;
    }
    if (!res.ok) throw new Error(await res.text() || '삭제에 실패했습니다.');
    return true;
}

export async function searchLocations(cond, signal) {

    const query = new URLSearchParams(
        Object.entries(cond).filter(([,v]) => v !== ''&&v != null));

    const res = await fetch('/api/locations/search?'+query,{signal});
    if (!res.ok) {

        const errorBody = await res.text();
        console.error("🚨 백엔드 에러 상세 내용:", errorBody);
        throw new Error('목록을 불러오지 못했습니다.');
    }
    return res.json();
}


export async function fetchTypes(){
    const res = await fetch('/api/types');
    if (!res.ok) throw new Error ('타입 정보를 불러오지 못했습니다.');
    return res.json();
}