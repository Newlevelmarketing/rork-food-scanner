import path from "path";

import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig(() => ({
  server: {
    host: "::",
    port: 8080,
    hmr: {
      overlay: false,
    },
  },
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  // envPrefix is left at Vite's default of VITE_ on purpose.
  //
  // It previously also exposed EXPO_PUBLIC_*, which is how the model API key
  // ended up inlined in the shipped bundle. The key now lives server-side in
  // api/analyze.ts; widening this again would undo that.
}));
