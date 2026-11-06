import { MENUS } from './core/menus.js';
import Icon from './core/icons.jsx';

// 왼쪽 패널 맨 위 세그먼트 탭 (여행 계획 / 관광지 찾기 / 저장한 장소)
export default function Sidebar({ active, counts = {}, onChange }) {
    return (
        <nav className="loc-tabs" role="tablist">
            {MENUS.map((m) => (
                <a
                    key={m.key}
                    href={'#' + m.key}
                    role="tab"
                    aria-selected={active === m.key}
                    className={`loc-tab ${active === m.key ? 'active' : ''}`}
                    onClick={(e) => {
                        e.preventDefault();
                        onChange(m.key);
                    }}
                >
                    <Icon name={m.icon} size={15} />
                    <span>{m.label}</span>
                    {counts[m.key] > 0 && <em className="loc-tab-count">{counts[m.key]}</em>}
                </a>
            ))}
        </nav>
    );
}
