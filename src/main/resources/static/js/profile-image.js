(function () {
    const input = document.getElementById('memberProfileImage');
    if (!input) return;

    const row = input.closest('.profile-row');
    const preview = row ? row.querySelector('img') : null;
    if (!preview) return;

    // 고른 사진을 바로 옆 미리보기에 보여준다
    input.addEventListener('change', function () {
        if (this.files[0]) preview.src = URL.createObjectURL(this.files[0]);
    });
})();
