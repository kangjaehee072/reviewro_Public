import { useState } from 'react';

// 별점. 두 가지로 쓴다.
//   보기용  <StarRating value={4.5} />
//   입력용  <StarRating value={0} name="reviewStarRating" editable />
// 입력용은 숨은 input 에 값을 넣으므로, 기존 타임리프 <form> 이 그대로 받는다
export default function StarRating({ value = 0, name, editable = false, size = 28 }) {
    const [score, setScore] = useState(value);
    const [hover, setHover] = useState(0);

    const shown = hover || score;

    return (
        <div className="stars" style={{ fontSize: size }}>
            {[1, 2, 3, 4, 5].map((n) => (
                <span key={n}
                      className={'star' + (shown >= n ? ' on' : shown >= n - 0.5 ? ' half' : '')}
                      onMouseLeave={editable ? () => setHover(0) : undefined}
                      onMouseMove={editable ? (e) => setHover(pick(e, n)) : undefined}
                      onClick={editable ? (e) => setScore(pick(e, n)) : undefined}>
                         ★
                     </span>
            ))}

            <span className="stars-num">{shown ? shown.toFixed(1) : '-'}</span>

            {editable && <input type="hidden" name={name} value={score} />}
        </div>
    );
}

// 별의 왼쪽 절반에 있으면 0.5, 오른쪽이면 1.0
function pick(e, n) {
    const box = e.currentTarget.getBoundingClientRect();
    return e.clientX - box.left < box.width / 2 ? n - 0.5 : n;
}