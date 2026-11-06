import { createRoot } from 'react-dom/client';
import AiApp from './AiApp.jsx';

// 카카오 지도 SDK 를 먼저 띄우고 붙인다
kakao.maps.load(() => {
    createRoot(document.getElementById('ai-root')).render(<AiApp />);
});
