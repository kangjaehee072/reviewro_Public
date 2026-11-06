// 여행 계획 일차별 색. 지도 번호 핀과 왼쪽 목록(DAY 라벨·번호)이 같은 색을 써서 어느 날 일정인지 한눈에 맞춰 본다.
// 서로 잘 구분되는 순서로 둔다 (1일차 파랑, 2일차 초록, 3일차 주황 …). 일차가 더 많으면 처음부터 다시 쓴다
export const DAY_COLORS = ['#1E6FF4', '#10B981', '#F59E0B', '#8B5CF6', '#E11D48', '#0EA5E9', '#64748B'];

export function dayColor(index) {
    return DAY_COLORS[index % DAY_COLORS.length];
}
