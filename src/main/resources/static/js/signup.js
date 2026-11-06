(function () {
    const form = document.getElementById('signupForm');
    if (!form) return;

    const url = form.dataset.checkUrl;

    // 확인할 칸은 HTML의 data-check(서버에 보낼 항목) · data-label(안내 문구)로만 정한다
    const fields = Array.from(form.querySelectorAll('[data-check]')).map(function (box) {
        return {
            box: box,
            input: box.querySelector('input'),
            note: box.querySelector('.check'),
            field: box.dataset.check,
            label: box.dataset.label,
            asked: null,   // 서버에 물어본 값
            result: '',    // 그 값의 결과: ok · dup
            message: ''
        };
    });

    // 칸의 상태 표시 (ok 초록 · dup 빨강 · checking 회색 · 빈값이면 해제)
    function mark(f, result, message) {
        f.box.classList.remove('is-checking', 'is-ok', 'is-dup');
        if (result) f.box.classList.add('is-' + result);
        f.note.textContent = message || '';
    }

    // 서버에 중복 여부를 묻는다. 이미 물어본 값이면 그때 결과를 다시 보여준다
    async function check(f) {
        const value = f.input.value.trim();

        if (!value || !f.input.checkValidity()) {
            f.asked = null;
            f.result = '';
            mark(f, '', '');
            return;
        }
        if (value === f.asked) {
            mark(f, f.result, f.message);
            return;
        }

        mark(f, 'checking', '확인 중…');

        try {
            const res = await fetch(url + '?field=' + f.field + '&value=' + encodeURIComponent(value));
            if (!res.ok) throw new Error();
            const duplicate = await res.json();

            if (f.input.value.trim() !== value) return;   // 그 사이 값이 바뀌었으면 버린다

            f.asked = value;
            f.result = duplicate ? 'dup' : 'ok';
            f.message = duplicate
                ? '이미 사용 중인 ' + f.label + '입니다.'
                : '사용할 수 있는 ' + f.label + '입니다.';
            mark(f, f.result, f.message);
        } catch (err) {
            // 통신이 실패해도 막지 않는다. 최종 판단은 서버가 한다
            f.asked = null;
            f.result = '';
            mark(f, '', '');
        }
    }

    // 지금 입력값이 "중복으로 확인된 값"인가
    function blocked(f) {
        return f.result === 'dup' && f.input.value.trim() === f.asked;
    }

    // 막힌 칸으로 스크롤하고 강조한다
    function focusField(f) {
        mark(f, f.result, f.message);   // 입력 중에 지워졌을 수 있으니 다시 띄운다
        f.input.scrollIntoView({ behavior: 'smooth', block: 'center' });
        f.input.focus({ preventScroll: true });
        f.box.classList.remove('shake');
        void f.box.offsetWidth;         // 애니메이션을 다시 트리거하기 위한 리플로우
        f.box.classList.add('shake');
    }

    fields.forEach(function (f) {
        let timer = null;

        // 타이핑이 멈추면 확인 (한 글자마다 서버를 부르지 않는다)
        f.input.addEventListener('input', function () {
            mark(f, '', '');
            clearTimeout(timer);
            timer = setTimeout(function () { check(f); }, 400);
        });

        // 칸을 벗어나면 기다리지 않고 바로 확인
        f.input.addEventListener('blur', function () {
            clearTimeout(timer);
            check(f);
        });
    });

    form.addEventListener('submit', function (e) {
        const bad = fields.find(blocked);
        if (!bad) return;
        e.preventDefault();
        focusField(bad);
    });
})();
