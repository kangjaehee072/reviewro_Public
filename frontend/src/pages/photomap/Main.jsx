import { createRoot } from 'react-dom/client';
import { dom, loadConfig, notify } from './core/state.js';
import { loadMap, bindMapControls } from './core/map.js';
import { initStickers, bindStickerButtons } from './core/stickers.js';
import { bindInteractions } from './core/interactions.js';
import { bindExport } from './core/export.js';
import PhotoList from './PhotoList.jsx';
import TopBar from './TopBar.jsx';
import Hint from './Hint.jsx';
import RegionModal from './RegionModal.jsx';
import StickerPanel from './StickerPanel.jsx';
import Actions from './Actions.jsx';

// 템플릿에 해당 div 가 있을 때만 리액트를 붙임
function mount(id, element) {
    const el = document.getElementById(id);
    if (el) createRoot(el).render(element);
}

if (dom.map) {
    loadConfig();
    bindMapControls();
    bindStickerButtons();
    bindExport();

    // 섬 붙이는 자리
    mount('pm-list-root', <PhotoList />);
    mount('pm-bar-root', <TopBar />);
    mount('pm-hint-root', <Hint />);
    mount('pm-modal-root', <RegionModal />);
    mount('pm-panel-root', <StickerPanel />);
    mount('pm-actions-root', <Actions />);

    loadMap().then(function () {
        initStickers();
        bindInteractions();
        notify();
    });
}
