import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/list': 'http://localhost:8080',
      '/get': 'http://localhost:8080',
    },
  },
})
