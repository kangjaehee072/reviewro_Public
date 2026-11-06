import { ELEMENTS } from './saju.js';

// 명식 (시 일 월 년) + 오행 분포
export default function SajuChart({ saju, onReset }) {
    return (
        <section className="sj-section">
            <div className="sj-head">
                <h3 className="sj-title">{saju.name ? saju.name + '님의 사주' : '사주 명식'}</h3>
                <button type="button" className="sj-reset" onClick={onReset} aria-label="초기화" title="초기화">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor"
                         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M21 12a9 9 0 1 1-2.64-6.36" />
                        <path d="M21 3v6h-6" />
                    </svg>
                </button>
            </div>

            <div className="sj-pillars">
                {saju.pillars.map((p) => (
                    <div key={p.label} className="sj-pillar">
                        <div className="sj-label">{p.label}</div>
                        <div className="sj-god">{p.god}</div>
                        {p.gan ? (
                            <>
                                <Char c={p.gan} />
                                <Char c={p.zhi} />
                            </>
                        ) : (
                            <>
                                <div className="sj-char sj-blank">?</div>
                                <div className="sj-char sj-blank">?</div>
                            </>
                        )}
                    </div>
                ))}
            </div>

            <div className="sj-bars">
                {ELEMENTS.map((el) => (
                    <div key={el.key} className={'sj-bar el-' + el.key}>
                        <span className="sj-bar-name">{el.name}({el.hanja})</span>
                        <div className="sj-bar-track">
                            <div className="sj-bar-fill" style={{ width: (saju.counts[el.key] / saju.total * 100) + '%' }} />
                        </div>
                        <span className="sj-bar-count">{saju.counts[el.key]}</span>
                    </div>
                ))}
                <p className="sj-note">글자 {saju.total}개 기준 개수예요</p>
            </div>
        </section>
    );
}

function Char({ c }) {
    return (
        <div className={'sj-char el-' + c.element}>
            <span className="sj-hanja">{c.hanja}</span>
            <span className="sj-hangul">{c.hangul}</span>
        </div>
    );
}
