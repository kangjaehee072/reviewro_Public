import { createRoot } from 'react-dom/client';
import AdminLocationApp from './AdminLocationApp.jsx';

kakao.maps.load(() => {
    createRoot(document.getElementById('admin-root')).render(<AdminLocationApp />);
});
