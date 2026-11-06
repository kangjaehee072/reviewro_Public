// 여행 그룹 화면 (group/read). 초대·공유·초대코드를 새로고침 없이 처리한다.
// 바뀐 뒤에는 이 페이지를 다시 받아서 id 가 붙은 칸만 갈아 끼운다.
// 다른 구성원이 바꾼 것도 그룹 채팅 SSE 의 refresh 이벤트로 받아서 똑같이 다시 그린다.
// sse-chat.js 가 먼저 불려서 sseChat 이 있어야 한다
(function () {
    const page = document.getElementById('groupPage');
    const base = page.dataset.base;
    const listUrl = page.dataset.list;
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
    const SWAP_IDS = ['memberCount', 'memberList', 'inviteCodeBox', 'planArea'];

    // ===== 알림 =====
    const toast = document.getElementById('groupToast');
    let toastTimer = null;

    function showToast(message, isError) {
        toast.textContent = message;
        toast.classList.toggle('error', !!isError);
        toast.hidden = false;
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => { toast.hidden = true; }, 2500);
    }

    // ===== 서버 호출 =====
    async function post(url, params) {
        const res = await fetch(url, {
            method: 'POST',
            headers: {[csrfHeader]: csrfToken},
            body: new URLSearchParams(params || {})
        });
        const body = await res.json().catch(() => ({}));
        showToast(body.message || (res.ok ? '완료했어요.' : '처리하지 못했어요.'), !res.ok);
        if (res.ok) reload();
        return res.ok;
    }

    // 여러 번 연달아 불려도 한 번만 받아 온다
    let reloading = null;

    function reload() {
        if (reloading) return reloading;
        reloading = fetch(location.pathname)
            .then(res => {
                // 그룹에서 빠졌거나 그룹이 없어졌으면 목록으로 보내진다
                if (res.redirected || !res.ok) {
                    location.href = listUrl;
                    return null;
                }
                return res.text();
            })
            .then(html => {
                if (!html) return;
                const doc = new DOMParser().parseFromString(html, 'text/html');
                SWAP_IDS.forEach(id => {
                    const now = document.getElementById(id);
                    const next = doc.getElementById(id);
                    if (now && next) now.replaceWith(next);
                });
                window.lucide && lucide.createIcons();
            })
            .finally(() => { reloading = null; });
        return reloading;
    }

    sseChat.source.addEventListener('refresh', reload);
    sseChat.source.addEventListener('deleted', () => {
        alert('그룹이 삭제됐어요.');
        location.href = listUrl;
    });

    // ===== 클릭 (칸이 갈아 끼워져도 동작하도록 document 에 한 번만 건다) =====
    document.addEventListener('click', e => {
        const unshare = e.target.closest('[data-unshare]');
        if (unshare) {
            post(unshare.dataset.url);
            return;
        }
        if (e.target.closest('#genCode')) {
            post(base + '/invite-code');
            return;
        }
        const copy = e.target.closest('[data-copy], [data-copy-link]');
        if (copy) {
            const text = copy.dataset.copy || (location.origin + copy.dataset.copyLink);
            navigator.clipboard.writeText(text)
                .then(() => showToast(copy.dataset.copy ? '초대코드를 복사했어요.' : '초대 링크를 복사했어요.'))
                .catch(() => showToast(text));
            return;
        }
        // 검색 목록 밖을 누르면 닫는다
        if (!e.target.closest('#inviteForm')) hideSuggest();
    });

    document.addEventListener('submit', e => {
        if (e.target.id === 'shareForm') {
            e.preventDefault();
            post(base + '/plans', {planId: e.target.planId.value});
        }
    });

    // ===== 초대 + 회원 검색 =====
    const inviteForm = document.getElementById('inviteForm');
    const inviteInput = document.getElementById('inviteInput');
    const suggest = document.getElementById('inviteSuggest');
    let searchTimer = null;
    let lastQuery = '';

    function hideSuggest() {
        suggest.hidden = true;
        suggest.replaceChildren();
    }

    async function invite(name) {
        hideSuggest();
        if (await post(base + '/invite', {nickname: name})) {
            inviteInput.value = '';
        }
    }

    inviteForm.addEventListener('submit', e => {
        e.preventDefault();
        const name = inviteInput.value.trim();
        if (name) invite(name);
    });

    inviteInput.addEventListener('input', () => {
        clearTimeout(searchTimer);
        const q = inviteInput.value.trim();
        if (q.length < 2) {
            hideSuggest();
            return;
        }
        searchTimer = setTimeout(() => search(q), 200);
    });

    inviteInput.addEventListener('keydown', e => {
        if (e.key === 'Escape') hideSuggest();
    });

    async function search(q) {
        lastQuery = q;
        const res = await fetch(base + '/invitees?q=' + encodeURIComponent(q));
        if (!res.ok || q !== lastQuery) return; // 늦게 온 예전 검색 결과는 버린다
        renderSuggest(await res.json());
    }

    // 닉네임·아이디는 회원이 정한 값이라 textContent 로만 넣는다
    function renderSuggest(people) {
        suggest.replaceChildren();
        if (!people.length) {
            const empty = document.createElement('li');
            empty.className = 'empty';
            empty.textContent = '일치하는 회원이 없어요 (이미 그룹에 있거나 초대한 회원은 빠져요)';
            suggest.appendChild(empty);
        }
        people.forEach(p => {
            const li = document.createElement('li');
            const btn = document.createElement('button');
            btn.type = 'button';
            const name = document.createElement('strong');
            name.textContent = p.nickname;
            const id = document.createElement('span');
            id.textContent = p.maskedUserId;
            const hint = document.createElement('em');
            hint.textContent = '초대';
            btn.append(name, id, hint);
            btn.addEventListener('click', () => invite(p.nickname));
            li.appendChild(btn);
            suggest.appendChild(li);
        });
        suggest.hidden = false;
    }

    // ===== 나가기·삭제는 한 번 더 묻는다 =====
    document.querySelectorAll('form[data-confirm]').forEach(form => {
        form.addEventListener('submit', e => {
            if (!confirm(form.dataset.confirm)) e.preventDefault();
        });
    });
})();
