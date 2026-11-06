function csrf() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    return token && header ? { [header.content]: token.content } : {};
}


export async function addBookmarks(locationId) {
    const res = await fetch(`/api/bookmarks/${locationId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...csrf() },
    });
    if (!res.ok) {
        const errorText = await res.text();
        throw new Error(errorText || '북마크 추가 실패');
    }
    (await loadBookmarkIds()).add(locationId);
}


export async function removeBookmarks(locationId) {
    const res = await fetch(`/api/bookmarks/${locationId}`, {
    method : 'DELETE',
        headers: csrf(),
    });
    if (!res.ok) {
        const errorText = await res.text();
        throw new Error(errorText || '북마크 삭제 실패');
    }
    (await loadBookmarkIds()).delete(locationId);
}

export async function fetchBookmarks() {
    const res = await fetch('/api/bookmarks');
    if (!res.ok) {
        throw new Error('북마크 목록을 불러오는데 실패했습니다.');
    }

    return res.json();
}

let idsPromise = null;

export function loadBookmarkIds() {
    if (!idsPromise) {
        idsPromise = fetch(`/api/bookmarks`)
            .then((res) => (res.ok ? res.json():[]))
            .then((list) => new Set(list.map((p) => p.locationId)))
            .catch(() => new Set());
    }
    return idsPromise;
}

