// 소셜 버튼을 누를 때 같은 화면의 '자동 로그인' 체크를 주소에 실어 보낸다 (서버가 세션에 적어 두고 돌아오면 쓴다)
document.addEventListener('click', function (e) {
    const link = e.target.closest('.social-icons a[href*="/oauth2/authorization/"]');
    if (!link) return;
    const scope = link.closest('dialog, .card-body') || document;
    const remember = scope.querySelector('input[name="remember-me"]');
    if (remember && remember.checked) {
        e.preventDefault();
        location.href = link.href + (link.href.includes('?') ? '&' : '?') + 'remember-me=true';
    }
});

(function () {
    const modal = document.getElementById('loginModal');
    if (!modal) return;

    const form = document.getElementById('loginForm');
    const error = document.getElementById('loginError');

    function open() {
        error.hidden = true;
        form.reset();
        modal.showModal();
        document.getElementById('modalUserId').focus();
    }

    function close() {
        modal.close();
    }

    document.querySelectorAll('[data-login-open]').forEach(function (el) {
        el.addEventListener('click', function (e) {
            e.preventDefault();
            open();
        });
    });

    document.querySelectorAll('[data-login-close]').forEach(function (el) {
        el.addEventListener('click', close);
    });

    modal.addEventListener('click', function (e) {
        if (e.target === modal) close();
    });

    form.addEventListener('submit', async function (e) {
        e.preventDefault();
        const button = form.querySelector('button[type="submit"]');
        button.disabled = true;
        error.hidden = true;

        try {
            const res = await fetch(form.action, {
                method: 'POST',
                headers: { 'X-Requested-With': 'XMLHttpRequest' },
                body: new URLSearchParams(new FormData(form))
            });
            const data = await res.json();
            if (data.success) {
                location.reload();
                return;
            }
            error.textContent = data.message;
            error.hidden = false;
        } catch (err) {
            error.textContent = '로그인 처리 중 오류가 발생했습니다.';
            error.hidden = false;
        } finally {
            button.disabled = false;
        }
    });

    window.openLoginModal = open;
})();
