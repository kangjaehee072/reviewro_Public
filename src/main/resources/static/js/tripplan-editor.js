// 템플릿 마켓 등록/수정 폼: 일자(Day)와 장소 입력칸을 늘리고 지운다.
// 서버는 tripDays[i].tripPlaces[j].필드 이름으로 받는다.
// 수정 화면은 서버가 그려 둔 칸이 있으므로 그 개수에서 이어서 번호를 매긴다
(function () {
    const container = document.getElementById('dayContainer');
    if (!container) return;

    let dayIndex = container.querySelectorAll('.day-editor').length;
    const placeIndex = {};
    container.querySelectorAll('.day-editor').forEach((box, i) => {
        placeIndex[i] = box.querySelectorAll('.place-editor').length;
    });

    function refreshIcons() {
        if (window.lucide) lucide.createIcons();
    }

    window.addDay = function () {
        const d = dayIndex++;
        placeIndex[d] = 0;

        const box = document.createElement('div');
        box.className = 'day-editor';
        box.innerHTML = `
            <div class="day-editor-head">
                <div class="field">
                    <label>일자</label>
                    <input type="text" name="tripDays[${d}].tripDayNumber" value="DAY ${d + 1}" required>
                </div>
                <div class="btns">
                    <button type="button" class="btn btn-sm btn-line" onclick="addPlace(this, ${d})">
                        <i data-lucide="map-pin-plus" class="ico"></i>장소 추가</button>
                    <button type="button" class="btn btn-sm btn-danger-ghost" onclick="removeBox(this, '.day-editor')">
                        <i data-lucide="trash-2" class="ico"></i>일자 삭제</button>
                </div>
            </div>
            <div class="place-list"></div>`;
        container.appendChild(box);
        addPlace(box.querySelector('.btns button'), d);   // 빈 일자보다 장소 칸 하나가 있는 게 편하다
    };

    window.addPlace = function (button, d) {
        const list = button.closest('.day-editor').querySelector('.place-list');
        if (placeIndex[d] === undefined) placeIndex[d] = 0;
        const p = placeIndex[d]++;
        const prefix = `tripDays[${d}].tripPlaces[${p}]`;

        const row = document.createElement('div');
        row.className = 'place-editor';
        row.innerHTML = `
            <div class="field">
                <label>장소 번호</label>
                <input type="number" name="${prefix}.locationId" placeholder="장소 ID">
            </div>
            <div class="field">
                <label>순서</label>
                <input type="text" name="${prefix}.tripPlaceSequence" value="${list.children.length + 1}">
            </div>
            <div class="field">
                <label>메모</label>
                <input type="text" name="${prefix}.tripPlaceMemo" placeholder="예: 해운대 해수욕장 산책">
            </div>
            <button type="button" class="btn btn-sm btn-danger-ghost" onclick="removeBox(this, '.place-editor')"
                    aria-label="장소 삭제"><i data-lucide="x" class="ico"></i></button>`;
        list.appendChild(row);
        refreshIcons();
    };

    window.removeBox = function (button, selector) {
        button.closest(selector).remove();
    };
})();
