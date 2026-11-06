function csrf() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    return token && header ? { [header.content]: token.content } : {};
}

// 로그인이 필요한 요청에서 401 이 오면 로그인 페이지로 보낸다
async function send(url, options) {
    const res = await fetch(url, options);
    if (res.status === 401) {
        window.location.href = '/login';
        return null;
    }
    if (!res.ok) throw new Error(await res.text() || '요청에 실패했습니다.');
    return res.status === 200 && res.headers.get('content-length') !== '0'
        ? res.json() : null;
}

export const fetchComments = (type, id) =>
    send(`/api/comments?type=${type}&id=${id}`, {});

export const writeComment = (type, id, content, parentId = null) =>
    send('/api/comments', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...csrf() },
        body: JSON.stringify({ type, id, content, parentId })
    });

export const deleteComment = (commentId) =>
    send(`/api/comments/${commentId}`, { method: 'DELETE', headers: csrf() });

export const fetchReactions = (type, id) =>
    send(`/api/reactions?type=${type}&id=${id}`, {});

export const toggleReaction = (type, id, kind) =>
    send('/api/reactions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...csrf() },
        body: JSON.stringify({ type, id, kind })
    });
