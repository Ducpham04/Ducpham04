import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import zaloMiniApp from "zmp-vite-plugin";

// https://mini.zalo.me/documents/devtools/ – zmp-vite-plugin build ra thư mục www/ đúng chuẩn `zmp deploy`
export default defineConfig({
  root: ".",
  plugins: [zaloMiniApp(), react()],
  server: { port: 3000, host: true },
  preview: { port: 3000 },
  build: { assetsInlineLimit: 0 },
});
