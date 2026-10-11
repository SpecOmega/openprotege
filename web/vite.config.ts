import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "VITE_");
  const apiTarget = process.env.VITE_API_TARGET ?? env.VITE_API_TARGET ?? "http://localhost:8080";
  return {
    plugins: [react()],
    server: {
      proxy: {
        "/api": apiTarget,
        "/actuator": apiTarget,
      },
    },
  };
});
