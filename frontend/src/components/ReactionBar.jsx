import { useEffect, useState } from 'react';
import { fetchReactions, toggleReaction } from '../api/board.js';

export default function ReactionBar({ type, id }) {
    const [data, setData] = useState(null);
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        let alive = true;
        fetchReactions(type, id).then((d) => { if (alive) setData(d); });
        return () => { alive = false; };
    }, [type, id]);

    const press = async (kind) => {
        if (busy) return;
        setBusy(true);
        try {
            // 서버가 돌려준 숫자를 그대로 쓴다. 직접 계산하지 않는다
            const next = await toggleReaction(type, id, kind);
            if (next) setData(next);
        } catch (e) {
            alert(e.message);
        } finally {
            setBusy(false);
        }
    };

    if (!data) return null;

    return (
        <div className="reaction-bar">
            <button type="button" disabled={busy}
                    className={'reaction' + (data.myKind === 'LIKE' ? ' on' : '')}
                    onClick={() => press('LIKE')}>
                👍 <span>{data.likeCount}</span>
            </button>
            <button type="button" disabled={busy}
                    className={'reaction' + (data.myKind === 'DISLIKE' ? ' on' : '')}
                    onClick={() => press('DISLIKE')}>
                👎 <span>{data.dislikeCount}</span>
            </button>
        </div>
    );
}
