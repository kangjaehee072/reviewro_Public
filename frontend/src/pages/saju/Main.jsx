import { createRoot } from 'react-dom/client';
import SajuApp from './SajuApp.jsx';

kakao.maps.load(() => {
    createRoot(document.getElementById('saju-root')).render(<SajuApp />);
});
