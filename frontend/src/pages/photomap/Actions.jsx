import { usePhotoMap } from './core/store.js';
import { cfg } from './core/state.js';
import { savePhotoMap, removePhotoMap } from './core/persist.js';

// 편집 중에 목록으로 가면 저장 안 한 내용이 다 날아가니 한 번 묻는다
function askLeave(e) {
    e.preventDefault();
    window.confirmModal({
        title: '목록으로 갈까요?',
        message: '저장하지 않은 내용은 사라져요.',
        confirmText: '나가기',
        cancelText: '계속 편집',
        danger: true
    }).then((ok) => { if (ok) location.href = '/contents/photomap'; });
}

export default function Actions() {
    const s = usePhotoMap();
    return (
        <>
            <a href="/contents/photomap" className="btn btn-line" onClick={askLeave}>목록</a>
            {cfg.id && <button type="button" className="btn btn-line" onClick={removePhotoMap}>삭제</button>}
            <button type="button" className="btn btn-primary" disabled={s.saving} onClick={savePhotoMap}>
                {s.saving ? '저장 중...' : '저장하기'}
            </button>
        </>
    );
}
