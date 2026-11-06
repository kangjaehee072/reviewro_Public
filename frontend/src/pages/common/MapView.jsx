import { useEffect, useRef } from 'react';

// DTO 이름으로 카카오 좌표 만들기 (Y=위도, X=경도)
const toLatLng = (loc) => new kakao.maps.LatLng(loc.locationLat, loc.locationLng);

// 색 핀 안에 번호를 쓴 마커 그림. 같은 번호·색이면 다시 만들지 않는다
const pinCache = new Map();
function numberPin(label, color) {
    const key = label + color;
    if (!pinCache.has(key)) {
        const text = String(label ?? '');
        const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="32" height="42" viewBox="0 0 32 42">`
            + `<path d="M16 41c-1-4-14-14-14-25a14 14 0 0 1 28 0c0 11-13 21-14 25z" fill="${color}" stroke="#fff" stroke-width="2"/>`
            + `<circle cx="16" cy="16" r="10" fill="#fff"/>`
            + `<text x="16" y="20.5" text-anchor="middle" font-family="Pretendard,Malgun Gothic,sans-serif" `
            + `font-size="${text.length > 1 ? 11 : 13}" font-weight="800" fill="${color}">${text}</text></svg>`;
        pinCache.set(key, new kakao.maps.MarkerImage(
            'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg),
            new kakao.maps.Size(32, 42),
            { offset: new kakao.maps.Point(16, 42) }));
    }
    return pinCache.get(key);
}

// focusLevel 을 주면 선택한 곳으로 옮길 때 그만큼 확대한다 (숫자가 작을수록 가깝다).
// 안 주면 지금 배율을 그대로 둔다
// focusOffsetX 는 지도 위에 무언가 덮여 있을 때 쓴다. 양수면 마커를 오른쪽으로, 음수면 왼쪽으로 비켜 놓는다
export default function MapView({ locations, selected, draft, focus, onMapClick, onMarkerClick, searchCenter, radius, focusLevel, focusOffsetX = 0, menu }) {
    const box = useRef(null);
    const map = useRef(null);
    const markers = useRef([]);
    const draftMarker = useRef(null);
    const circle = useRef(null);
    const tip = useRef(null);
    const clickOverlay = useRef(null);// 마커에 마우스를 올렸을 때 뜨는 이름표 {overlay, el}

    const clickRef = useRef(onMapClick);
    useEffect(() => {clickRef.current = onMapClick;});

    // 지도는 처음 한 번만 생성
    useEffect(() => {
        map.current = new kakao.maps.Map(box.current, {
            center: new kakao.maps.LatLng(36.5, 127.8), level: 12
        });
        kakao.maps.event.addListener(map.current, 'click', (e) =>
            clickRef.current({lat: e.latLng.getLat(), lng: e.latLng.getLng()}));

        //클릭 시 나오는 상세설명박스
        const clickDes = document.createElement('div');
        clickDes.className = 'map-click-box';

        clickOverlay.current = new kakao.maps.CustomOverlay({
            content: clickDes,
            yAnchor: 2.2,
            zIndex: 5
        });

        // 이름표는 하나만 만들어 마커마다 돌려 쓴다 (마커 수만큼 만들면 무겁다)
        const el = document.createElement('div');
        el.className = 'map-tip';
        el.className = 'map-tip';
        tip.current = {
            el,
            overlay: new kakao.maps.CustomOverlay({ content: el, yAnchor: 2.4, zIndex: 3, clickable: false })
        };

        // 카카오는 만들어질 때의 크기를 기억한다. 탭을 바꾸거나 창을 줄여서
        // 칸이 달라져도 스스로 다시 재지 않아 마커가 가운데에서 비껴 보인다.
        // 크기가 바뀌면 relayout() 으로 다시 재게 하고, 보던 곳을 그대로 유지한다
        const observer = new ResizeObserver(() => {
            const center = map.current.getCenter();
            map.current.relayout();
            map.current.setCenter(center);
        });
        observer.observe(box.current);
        return () => observer.disconnect();
    }, []);

    const hideTip = () => tip.current?.overlay.setMap(null);

    // 장소 배열이 바뀌면 마커만 다시 그림
    useEffect(() => {
        markers.current.forEach((m) => m.setMap(null));
        hideTip();                  // 가리키던 마커가 사라졌을 수 있다
        markers.current = locations.map((loc) => {
            const m = new kakao.maps.Marker({
                map: map.current, position: toLatLng(loc),
                // 여행 계획처럼 번호·색이 붙은 장소는 번호 핀으로 그린다 (없으면 카카오 기본 핀)
                ...(loc.markerColor ? { image: numberPin(loc.markerLabel, loc.markerColor) } : {})
            });
            kakao.maps.event.addListener(m, 'click', () => onMarkerClick(loc));
            // 브라우저 기본 말풍선(title)은 1초쯤 기다려야 떠서 직접 띄운다
            kakao.maps.event.addListener(m, 'mouseover', () => {
                tip.current.el.textContent = loc.locationName;
                tip.current.overlay.setPosition(toLatLng(loc));
                tip.current.overlay.setMap(map.current);
            });
            kakao.maps.event.addListener(m, 'mouseout', hideTip);
            return m;
        });
    }, [locations]);

    // 새로 찍은 위치 표시
    useEffect(() => {
        if (draftMarker.current) draftMarker.current.setMap(null);
        if (draft) draftMarker.current = new kakao.maps.Marker({
            map: map.current, position: new kakao.maps.LatLng(draft.lat, draft.lng)
        });
    }, [draft]);

    // 선택된 곳으로 이동 (focusLevel 이 있으면 확대도 같이)
    useEffect(() => {
        if (!focus) return;
        if (focusLevel) map.current.setLevel(focusLevel);

        const target = toLatLng(focus);
        if (!focusOffsetX) {
            map.current.panTo(target);
            return;
        }

        // 한쪽을 목록이 덮고 있으면 지도 한가운데에 놓는 순간 마커가 목록 밑으로 들어간다.
        // 중심을 그만큼 반대쪽으로 밀어 두면 마커는 가려지지 않은 쪽의 한가운데로 온다 (오른쪽 목록이면 음수).
        // 픽셀 ↔ 좌표 변환은 배율을 타므로 setLevel 다음에 재야 한다
        const proj = map.current.getProjection();
        const point = proj.containerPointFromCoords(target);
        point.x -= focusOffsetX;
        map.current.panTo(proj.coordsFromContainerPoint(point));
    }, [focus, focusOffsetX]);

    useEffect(() => {
        // 기존에 그려진 원이 있다면 지도에서 제거
        if (circle.current) {
            circle.current.setMap(null);
            circle.current = null;
        }

        // 중심 좌표와 반경 값이 유효할 때만 새로운 원 생성
        if (menu === 'place-list' && searchCenter && radius > 0) {
            circle.current = new kakao.maps.Circle({
                center: new kakao.maps.LatLng(searchCenter.lat, searchCenter.lng), // 원의 중심 좌표
                radius: radius, // 미터 단위 (예: 5000 = 5km)
                strokeWeight: 2, // 선 두께
                strokeColor: '#3b82f6', // 선 색상 (파란색)
                strokeOpacity: 0.8, // 선 투명도
                strokeStyle: 'solid', // 선 스타일
                fillColor: '#3b82f6', // 채우기 색상
                fillOpacity: 0.15 // 채우기 투명도
            });

            // 지도에 원 표시
            circle.current.setMap(map.current);
        }
    }, [searchCenter, radius, menu]);





    return <div ref={box} id="map" />;
}