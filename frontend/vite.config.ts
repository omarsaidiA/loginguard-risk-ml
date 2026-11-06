/// <reference types="vitest/config" />
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react()],
  build: {
    // Source maps would publish the original source code next to the app
    sourcemap: false,
  },
  server: {
    // Development only: the API runs on 8080, the dev server proxies to keep a single origin,
    // so the SameSite=Strict session cookie and the CSRF cookie work as in production
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
  test: {
    environment: "jsdom",
  },
});
