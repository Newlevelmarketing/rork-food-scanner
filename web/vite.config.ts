import path from "path";

import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv, type Plugin, type ViteDevServer } from "vite";

/**
 * Serves `api/analyze.ts` during `vite dev`.
 *
 * Vite's dev server does not run the `api/` functions the host executes in
 * production, so without this the scanner is dead locally: every request to
 * `/api/analyze` returns the SPA's index.html and fails to parse. Mounting the
 * real handler means `npm run dev` exercises the same code the deployment does.
 *
 * `env` is read with an empty prefix so GEMINI_API_KEY is visible here. That is
 * safe: this runs in the dev server process, not in the browser, and the value
 * is never handed to `define` or to client code.
 */
function analyzeApiDevServer(env: Record<string, string>): Plugin {
  return {
    name: "modernbody:analyze-api-dev",
    apply: "serve",
    configureServer(server: ViteDevServer) {
      server.middlewares.use("/api/analyze", (req, res) => {
        void (async () => {
          try {
            let body = "";
            for await (const chunk of req) body += String(chunk);

            const method = req.method ?? "POST";
            const request = new Request("http://localhost/api/analyze", {
              method,
              headers: { "Content-Type": "application/json" },
              body: method === "GET" || method === "HEAD" ? undefined : body,
            });

            const module = await server.ssrLoadModule("/api/analyze.ts");
            const handler = module.default as (
              request: Request,
              env?: Record<string, string>,
            ) => Promise<Response>;

            const response = await handler(request, env);
            res.statusCode = response.status;
            response.headers.forEach((value, key) => res.setHeader(key, value));
            res.end(await response.text());
          } catch (error) {
            server.config.logger.error(`[analyze-api-dev] ${String(error)}`);
            res.statusCode = 500;
            res.setHeader("Content-Type", "application/json");
            res.end(JSON.stringify({ error: "devMiddlewareFailed" }));
          }
        })();
      });
    },
  };
}

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // Empty prefix: read every variable, not just VITE_*. Used only by the dev
  // middleware above, never exposed to the client.
  const env = loadEnv(mode, process.cwd(), "");

  return {
    server: {
      host: "::",
      port: 8080,
      hmr: {
        overlay: false,
      },
    },
    plugins: [react(), analyzeApiDevServer(env)],
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },
    // envPrefix is left at Vite's default of VITE_ on purpose.
    //
    // It previously also exposed EXPO_PUBLIC_*, which is how the model API key
    // ended up inlined in the shipped bundle. The key now lives server-side;
    // widening this again would undo that.
  };
});
