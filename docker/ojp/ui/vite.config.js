import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'




// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true, // 允许外部访问
    strictPort: true,
    headers: {
      'X-Frame-Options': 'SAMEORIGIN'
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true
  }
})