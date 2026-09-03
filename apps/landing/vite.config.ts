import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [tailwindcss(), sveltekit()],
  server: {
    port: 3000,
    proxy: {
      '^/app(/|$)': {
        target: 'http://localhost:3001',
        changeOrigin: true
      }
    }
  }
});
