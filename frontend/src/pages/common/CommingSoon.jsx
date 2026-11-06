import { MENUS } from '../location/core/menus.js';

export default function CommingSoon({ menu }) {
    const m = MENUS.find((x) => x.key === menu);
    return (
        <section className="card">
            <div className="card-body empty">
                <div className="empty-icon">{m?.icon}</div>
                <p>{m?.label ?? '이'} 화면은 준비 중이에요</p>
            </div>
        </section>
    );
}
