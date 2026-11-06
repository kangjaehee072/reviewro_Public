import {useEffect, useState} from 'react';
import {addBookmarks, removeBookmarks, loadBookmarkIds} from "../../api/bookmark.js";
import Icon from "./core/icons.jsx";
import { isLoggedIn, goLogin } from "./core/auth.js";

export default function LocationSaved({ locationId, initialBookmarked = false }) {
    const [Bookmark, setBookmark] = useState(Boolean(initialBookmarked));
    const [loading, setLoading] = useState(true);

    // 부모 컴포넌트에서 비동기로 데이터를 받아와 값이 갱신될 때 동기화
    useEffect(() => {
        let alive = true;
        setLoading(true);
        loadBookmarkIds().then((ids) => {
            //이미 다른 장소로 넘어갔으면 버린다
            if (!alive) return;
            setBookmark(ids.has(locationId));
            setLoading(false);
        });
        return () => {alive = false;};

    }, [locationId]);

    const handleBookmark = async (e) => {
        // 부모요소로 이벤트가 퍼지는 것 방지
        e.stopPropagation();
        if (!isLoggedIn()) { goLogin(); return; }   // 저장은 로그인해야 된다
        if (loading) return;

        setLoading(true);
        try{
            if(Bookmark){
                //이미 북마크된 상태면 삭제 요청
                await removeBookmarks(locationId);
                setBookmark(false);
            }else{
                // 북마크 추가
                await addBookmarks(locationId);
                setBookmark(true);
            }
        }catch (err) {
            alert(err.message);
        }finally {
            setLoading(false);
        }
    };

    return (
        <button
            type="button"
            onClick={handleBookmark}
            disabled={loading}
            className={`bookmark-btn ${Bookmark ? 'active' : ''}`}
            aria-pressed={Bookmark}
            title={Bookmark ? '저장 취소' : '장소 저장'}
            >
            <Icon name="heart" size={16} fill={Bookmark ? 'currentColor' : 'none'} />
            <span>{Bookmark ? '저장됨' : '저장'}</span>
        </button>
    );

}
