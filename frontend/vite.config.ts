import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5175,
    proxy: {
      "/api/v1/procesos": {
        target: process.env.NEXTDOCS_WORKFLOW ?? "http://localhost:8091",
        changeOrigin: true,
      },
      "/api/v1/instancias": {
        target: process.env.NEXTDOCS_WORKFLOW ?? "http://localhost:8091",
        changeOrigin: true,
      },
      "/api/v1/tareas": {
        target: process.env.NEXTDOCS_WORKFLOW ?? "http://localhost:8091",
        changeOrigin: true,
      },
      "/api": {
        target: process.env.NEXTDOCS_API ?? "http://localhost:8090",
        changeOrigin: true,
      },
    },
  },
});
