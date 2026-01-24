import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'


// https://vitejs.dev/config/
export default defineConfig({
    plugins: [react()],
    server: {
        port: 3000,
        host: true, // 允许外部访问
        strictPort: true,
        headers: {
            'X-Frame-Options': 'SAMEORIGIN'
        },
        watch: {
            usePolling: true,
            interval: 1000 // 轮询间隔，单位毫秒
        }
    },
    build: {
        outDir: 'dist',
        sourcemap: true
    }
})