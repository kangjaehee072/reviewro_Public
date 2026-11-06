import { usePhotoMap } from './core/store.js';
import { cfg } from './core/state.js';

export default function Hint() {
    const s = usePhotoMap();
    if (!s.activeProvince) return '시·도를 클릭하면 확대됩니다';
    if (!cfg.editable) return null;
    return s.mergeMode ? '합칠 구를 2개 이상 클릭하세요' : '시·군·구를 클릭해 사진을 넣어보세요';
}
