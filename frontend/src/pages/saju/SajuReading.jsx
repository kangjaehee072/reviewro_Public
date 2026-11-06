import { elementName } from './saju.js';

// 기본 풀이: 일간 / 일주 / 십신 / 많은 기운 / 부족한 기운 / 채우는 여행
// 문구는 DB(saju_text)에서 온다. 아직 안 넣었으면 그 칸만 빠진다
export default function SajuReading({ saju, texts }) {
    const get = (type, code) => texts[type + ':' + code];

    const dayMaster = get('DAY_MASTER', saju.dayGan.hanja);
    const dayPillar = get('DAY_PILLAR', saju.dayPillar);
    const many = get('ELEMENT_MANY', saju.strong);
    const lack = get('ELEMENT_LACK', saju.weak);
    const trip = get('ELEMENT_TRIP', saju.weak);
    const weakCount = saju.counts[saju.weak];

    if (Object.keys(texts).length === 0) {
        return <p className="hint">풀이 문구를 준비 중이에요</p>;
    }

    return (
        <section className="sj-section sj-reading">
            {dayMaster && (
                <div className="sj-block">
                    <span className="badge badge-tint">나를 나타내는 글자</span>
                    <h4>{dayMaster.title}</h4>
                    <p>{dayMaster.content}</p>
                </div>
            )}

            {dayPillar && (
                <div className="sj-block">
                    <span className="badge badge-tint">나의 일주</span>
                    <h4>{dayPillar.title}</h4>
                    <p>{dayPillar.content}</p>
                </div>
            )}

            {saju.mainGods.map(({ god, count }) => {
                const t = get('TEN_GOD', god);
                return t && (
                    <div key={god} className="sj-block">
                        <span className="badge badge-tint">두드러진 성향 · {count}개</span>
                        <h4>{t.title}</h4>
                        <p>{t.content}</p>
                    </div>
                );
            })}

            {many && (
                <div className="sj-block">
                    <h4>{elementName(saju.strong)} 기운이 가장 많아요 ({saju.counts[saju.strong]}개)</h4>
                    <p>{many.content}</p>
                </div>
            )}

            {lack && (
                <div className="sj-block">
                    <h4>{elementName(saju.weak)} 기운이 {weakCount === 0 ? '없어요' : '부족해요'}</h4>
                    <p>{lack.content}</p>
                </div>
            )}

            {trip && (
                <div className={'sj-trip el-' + saju.weak}>
                    <strong>{trip.title}</strong>
                    <p>{trip.content}</p>
                </div>
            )}

            <p className="sj-note">재미로 보는 풀이예요</p>
        </section>
    );
}
