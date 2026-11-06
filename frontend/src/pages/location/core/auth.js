// 로그인 여부는 서버가 #location-root 의 data-logged-in 에 적어 준다 (location.html)
export function isLoggedIn() {
    return document.getElementById('location-root')?.dataset.loggedIn === 'true';
}

// 로그인 화면으로 보낸다. 로그인하고 나면 next 로 돌아온다
export function goLogin(next = location.pathname + location.search + location.hash) {
    window.location.href = '/login?next=' + encodeURIComponent(next);
}

// 로그인해야 쓰는 메뉴 (여행 계획·저장한 장소)
export const LOGIN_MENUS = ['plans', 'bookmarks'];
