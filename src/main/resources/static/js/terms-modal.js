// 푸터 '이용약관'·'개인정보처리방침' 을 누르면 페이지 이동 대신 모달로 띄운다.
// 처음 누를 때 /terms/modal 을 한 번 불러와 끼워 두고, 실패하면 원래 링크(/terms)로 간다
(function () {
    let dialog = null;

    async function open(link) {
        if (!dialog) {
            const res = await fetch(link.dataset.termsModal);
            if (!res.ok) throw new Error(res.status);
            dialog = document.createElement('dialog');
            dialog.className = 'terms-modal terms-modal-wide';
            dialog.innerHTML = await res.text();
            document.body.appendChild(dialog);
            dialog.querySelector('[data-terms-close]').addEventListener('click', () => dialog.close());
            // 바깥(어두운 곳)을 누르면 닫힌다
            dialog.addEventListener('click', (e) => { if (e.target === dialog) dialog.close(); });
        }
        dialog.showModal();
        dialog.querySelector('.terms-modal-body').scrollTop = 0;
    }

    document.querySelectorAll('[data-terms-modal]').forEach((link) => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            open(link).catch(() => { location.href = link.href; });
        });
    });
})();
