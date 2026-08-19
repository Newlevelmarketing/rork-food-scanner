/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly EXPO_PUBLIC_TOOLKIT_URL?: string;
  readonly EXPO_PUBLIC_RORK_TOOLKIT_SECRET_KEY?: string;
  readonly EXPO_PUBLIC_PROJECT_ID?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module "*.json" {
  const value: unknown;
  export default value;
}
