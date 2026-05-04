// vite.config.ts
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Backend-Konfigurationen direkt in vite.config.ts
const BACKEND = {
    LOCALHOST: "http://localhost:8080/",
    IPV6: "http://[2001:7c0:2320:1:f816:3eff:fed3:ec51]:8080/",
    IPV4: "http://192.168.178.53:8080/"
};

// Wählen Sie hier das gewünschte Backend
const SELECTED_BACKEND = BACKEND.LOCALHOST;

export default defineConfig({
    plugins: [
        vue(),
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    server: {
        host: '0.0.0.0',
        port: 5173,
        strictPort: true,
        proxy: {
            '/api/v1': {
                target: SELECTED_BACKEND,
                changeOrigin: true
            }
        }
    }
})
