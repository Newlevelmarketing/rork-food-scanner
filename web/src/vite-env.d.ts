/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * Where meal analysis is posted. Defaults to the same-origin `/api/analyze`.
   *
   * Set this only for a split deployment where the proxy is not served from the
   * app's own origin. It is a URL, not a credential -- no secret may ever be
   * given a `VITE_` prefix, because Vite inlines those into the client bundle.
   */
  readonly VITE_ANALYZE_ENDPOINT?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module "*.json" {
  const value: unknown;
  export default value;
}
