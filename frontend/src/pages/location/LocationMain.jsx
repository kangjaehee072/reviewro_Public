import { createRoot } from 'react-dom/client';
import LocationApp from './LocationApp.jsx';

kakao.maps.load(() => {
    createRoot(document.getElementById('location-root')).render(<LocationApp />);
});
