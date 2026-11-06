import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// O proxy evita CORS: /api/* é encaminhado ao Spring Boot.
export default defineConfig({
  plugins: [react()],
  server: { proxy: { '/api': process.env.VITE_API_URL || 'http://localhost:8080' } },
});
