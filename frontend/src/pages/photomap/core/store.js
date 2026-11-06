import { useSyncExternalStore } from 'react';
import { state, subscribe, getVersion } from './state.js';

// 컴포넌트에서 const s = usePhotoMap(); → notify() 될 때마다 다시 그려짐
export function usePhotoMap() {
    useSyncExternalStore(subscribe, getVersion);
    return state;
}