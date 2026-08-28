import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5175,
    proxy: {
      "/api": {
        target: process.env.NEXTDOCS_API ?? "http://localhost:8090",
        changeOrigin: true,
      },
    },
  },
});
