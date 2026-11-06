import { createRoot } from 'react-dom/client';
import CommentList from '../../components/CommentList.jsx';
import ReactionBar from '../../components/ReactionBar.jsx';
import StarRating from '../../components/StarRating.jsx';

// HTML 에서 data-widget 이 붙은 자리를 찾아 알아서 붙는다.
// 게시판이 늘어나도 이 파일은 안 건드린다
document.querySelectorAll('[data-widget]').forEach((el) => {
    const d = el.dataset;
    const root = createRoot(el);

    if (d.widget === 'comments') {
        root.render(<CommentList type={d.type} id={Number(d.id)} />);

    } else if (d.widget === 'reactions') {
        root.render(<ReactionBar type={d.type} id={Number(d.id)} />);

    } else if (d.widget === 'rating') {
        root.render(<StarRating value={Number(d.value || 0)}
                                name={d.name}
                                editable={d.editable === 'true'} />);
    }
});