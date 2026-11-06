import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  base: '/js/dist/',
  build: {
    sourcemap: true,
    outDir: '../src/main/resources/static/js/dist',
    emptyOutDir: true,
    rollupOptions: {
      input: {
        adminlocation: 'src/pages/adminlocation/AdminMain.jsx',
        location: 'src/pages/location/LocationMain.jsx',
        photomap: 'src/pages/photomap/Main.jsx',
        saju: 'src/pages/saju/Main.jsx',
        ai: 'src/pages/ai/Main.jsx',
        board: 'src/pages/board/Main.jsx',
        // 새 리액트 페이지 추가 = 여기 한 줄 추가
      },
      output: { entryFileNames: '[name].js', chunkFileNames: '[name].js', assetFileNames: '[name][extname]' }
    }
  }
});