/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// En desarrollo el proxy cumple la función de Nginx: /api va al backend y no hace falta CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: { '/api': process.env.BACKEND_URL ?? 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/setupTests.ts',
  },
});
