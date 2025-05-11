import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path'; // path 모듈 임포트 필요

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      // tsconfig.app.json의 paths 설정을 여기에 명시적으로 추가합니다.
      // '별칭': 실제_경로
      'pages': path.resolve(__dirname, './src/pages'),
      'components': path.resolve(__dirname, './src/components'),
      'apis': path.resolve(__dirname, './src/apis'),
      'hooks': path.resolve(__dirname, './src/hooks'),
      // 만약 @/src 형태를 사용하고 싶다면 다음 라인을 추가하세요:
      // '@': path.resolve(__dirname, './src'),
    }
  }
});