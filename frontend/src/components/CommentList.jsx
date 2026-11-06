import { useEffect, useState } from 'react';
import { fetchComments, writeComment, deleteComment } from '../api/board.js';

export default function CommentList({ type, id }) {
    const [items, setItems] = useState([]);
    const [text, setText] = useState('');
    const [replyTo, setReplyTo] = useState(null);   // 답글 달 원댓글 id
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        if (!type || !id) return;
        let alive = true;
        fetchComments(type, id).then((d) => { if (alive) setItems(d || []); });
        return () => { alive = false; };
    }, [type, id]);

    const submit = async (e) => {
        e.preventDefault();
        if (!text.trim() || busy) return;
        setBusy(true);
        try {
            const next = await writeComment(type, id, text, replyTo);
            if (next) setItems(next);      // 서버가 새 목록을 준다
            setText('');
            setReplyTo(null);
        } catch (err) {
            alert(err.message);
        } finally {
            setBusy(false);
        }
    };

    const remove = async (commentId) => {
        if (!confirm('댓글을 지울까요?')) return;
        try {
            await deleteComment(commentId);
            setItems(await fetchComments(type, id));
        } catch (err) {
            alert(err.message);
        }
    };

    // 원댓글과 답글을 나눠서 원댓글 밑에 답글이 오게 배치한다
    const roots = items.filter((c) => c.parentId === null);
    const repliesOf = (rootId) => items.filter((c) => c.parentId === rootId);

    return (
        <section className="comments">
            <h3 className="comments-head">댓글 {items.length}</h3>

            <ul className="comment-list">
                {roots.map((c) => (
                    <li key={c.commentId}>
                        <Item c={c} onReply={setReplyTo} onDelete={remove} />
                        {repliesOf(c.commentId).map((r) => (
                            <div className="comment-reply" key={r.commentId}>
                                <Item c={r} onDelete={remove} />
                            </div>
                        ))}
                    </li>
                ))}
            </ul>

            {items.length === 0 && <p className="hint">첫 댓글을 남겨보세요.</p>}

            <form className="comment-form" onSubmit={submit}>
                {replyTo && (
                    <div className="comment-replying">
                        답글 작성 중
                        <button type="button" onClick={() => setReplyTo(null)}>취소</button>
                    </div>
                )}
                <textarea value={text} rows={2} maxLength={1000} disabled={busy}
                          placeholder="댓글을 입력하세요"
                          onChange={(e) => setText(e.target.value)} />
                <button type="submit" className="btn btn-primary"
                        disabled={busy || !text.trim()}>등록</button>
            </form>
        </section>
    );
}

function Item({ c, onReply, onDelete }) {
    return (
        <div className={'comment-item' + (c.deleted ? ' is-deleted' : '')}>
            <div className="comment-meta">
                <strong>{c.writer || '익명'}</strong>
                <span>{c.createdAt?.replace('T', ' ').slice(0, 16)}</span>
            </div>
            <p className="comment-body">{c.content}</p>
            <div className="comment-actions">
                {onReply && !c.deleted &&
                    <button type="button" onClick={() => onReply(c.commentId)}>답글</button>}
                {c.mine &&
                    <button type="button" onClick={() => onDelete(c.commentId)}>삭제</button>}
            </div>
        </div>
    );
}
