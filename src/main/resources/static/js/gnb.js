(function () {
    const btn = document.getElementById('gnbProfileBtn');
    const menu = document.getElementById('gnbDropdown');
    if (!btn || !menu) return;

    function toggle(open) {
        menu.hidden = !open;
        btn.setAttribute('aria-expanded', open);
    }

    btn.addEventListener('click', function (e) {
        e.stopPropagation();
        toggle(menu.hidden);
    });

    document.addEventListener('click', function (e) {
        if (!menu.hidden && !menu.contains(e.target)) toggle(false);
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') toggle(false);
    });
})();

// 좁은 화면 메뉴 버튼: 상단 메뉴를 전체 패널로 펼치고 접는다
(function () {
    const toggleBtn = document.getElementById('gnbToggle');
    const header = toggleBtn && toggleBtn.closest('.gnb');
    if (!header) return;

    function setOpen(open) {
        header.classList.toggle('menu-open', open);
        document.body.classList.toggle('gnb-locked', open);
        toggleBtn.setAttribute('aria-expanded', open);
        toggleBtn.setAttribute('aria-label', open ? '메뉴 닫기' : '메뉴 열기');
    }

    toggleBtn.addEventListener('click', function () {
        setOpen(!header.classList.contains('menu-open'));
    });

    // 메뉴 안 링크를 누르면 접는다 (같은 페이지의 #해시 이동도 패널이 남지 않게)
    document.getElementById('gnbMenu').addEventListener('click', function (e) {
        if (e.target.closest('a')) setOpen(false);
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') setOpen(false);
    });

    // 화면을 넓히면 패널 상태를 풀어 준다
    window.matchMedia('(min-width: 961px)').addEventListener('change', function (e) {
        if (e.matches) setOpen(false);
    });
})();
