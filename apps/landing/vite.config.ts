import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import tailwindcss from '@tailwindcss/vite';
import { microfrontends } from '@vercel/microfrontends/experimental/vite';

export default defineConfig({
  plugins: [microfrontends(), tailwindcss(), sveltekit()],
  server: {
    port: 3000
  }
});
