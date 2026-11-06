// 사진을 올리기 전에 브라우저에서 줄인다.
// FHD(긴 변 1920, 짧은 변 1080) 안에 들어가게 비율을 지켜 축소하고 WebP 로 바꾼다.
// 폰 원본(수 MB~수십 MB)이 수백 KB 로 줄어 저장이 빨라지고 업로드 한도에 걸리지 않는다
const MAX_LONG = 1920;
const MAX_SHORT = 1080;
const QUALITY = 0.85;

export async function optimizeImage(file) {
    // 움직이는 GIF 는 캔버스를 거치면 첫 장만 남으니 그대로 둔다
    if (!file || !file.type.startsWith('image/') || file.type === 'image/gif') return file;

    let bitmap;
    try {
        // 폰 사진의 회전 정보(EXIF)를 반영해서 연다. 안 그러면 세로 사진이 눕는다
        bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
    } catch (e) {
        return file;   // 브라우저가 못 여는 형식(HEIC 등)은 원본 그대로
    }

    const { width, height } = bitmap;
    const ratio = Math.min(1,
        MAX_LONG / Math.max(width, height),
        MAX_SHORT / Math.min(width, height));
    const w = Math.round(width * ratio);
    const h = Math.round(height * ratio);

    const canvas = document.createElement('canvas');
    canvas.width = w;
    canvas.height = h;
    canvas.getContext('2d').drawImage(bitmap, 0, 0, w, h);
    bitmap.close();

    const blob = await new Promise(function (resolve) { canvas.toBlob(resolve, 'image/webp', QUALITY); });
    // WebP 로 못 굽는 브라우저는 PNG 로 돌려준다. 그럴 땐 원본이 낫다
    if (!blob || blob.type !== 'image/webp') return file;
    // 줄일 필요가 없던 작은 사진인데 오히려 커졌으면 원본을 쓴다
    if (ratio === 1 && blob.size >= file.size) return file;

    const name = file.name.replace(/\.[^.]+$/, '') + '.webp';
    return new File([blob], name, { type: 'image/webp', lastModified: file.lastModified });
}
