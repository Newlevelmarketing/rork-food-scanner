import { useCallback } from "react";

import catalogue from "@/data/strings.json";
import { useAppStore } from "@/store/AppStore";

type StringTable = Record<string, string>;

const tables = catalogue as Record<string, StringTable>;

/** A language Calzy can present itself in. */
export interface AppLanguage {
  code: string;
  /** Name in English, so searching "Spanish" finds Español. */
  englishName: string;
  /** Endonym — what speakers call the language themselves. */
  nativeName: string;
  flag: string;
  isRTL?: boolean;
}

/** Ordered roughly by global reach, English first. Mirrors `AppLanguage.swift`. */
export const languages: AppLanguage[] = [
  { code: "en", englishName: "English", nativeName: "English", flag: "🇺🇸" },
  { code: "zh-Hans", englishName: "Chinese (Simplified)", nativeName: "简体中文", flag: "🇨🇳" },
  { code: "zh-Hant", englishName: "Chinese (Traditional)", nativeName: "繁體中文", flag: "🇹🇼" },
  { code: "es", englishName: "Spanish", nativeName: "Español", flag: "🇪🇸" },
  { code: "hi", englishName: "Hindi", nativeName: "हिन्दी", flag: "🇮🇳" },
  { code: "ar", englishName: "Arabic", nativeName: "العربية", flag: "🇸🇦", isRTL: true },
  { code: "pt", englishName: "Portuguese", nativeName: "Português", flag: "🇧🇷" },
  { code: "bn", englishName: "Bengali", nativeName: "বাংলা", flag: "🇧🇩" },
  { code: "ru", englishName: "Russian", nativeName: "Русский", flag: "🇷🇺" },
  { code: "ja", englishName: "Japanese", nativeName: "日本語", flag: "🇯🇵" },
  { code: "de", englishName: "German", nativeName: "Deutsch", flag: "🇩🇪" },
  { code: "fr", englishName: "French", nativeName: "Français", flag: "🇫🇷" },
  { code: "ko", englishName: "Korean", nativeName: "한국어", flag: "🇰🇷" },
  { code: "it", englishName: "Italian", nativeName: "Italiano", flag: "🇮🇹" },
  { code: "tr", englishName: "Turkish", nativeName: "Türkçe", flag: "🇹🇷" },
  { code: "vi", englishName: "Vietnamese", nativeName: "Tiếng Việt", flag: "🇻🇳" },
  { code: "id", englishName: "Indonesian", nativeName: "Bahasa Indonesia", flag: "🇮🇩" },
  { code: "ms", englishName: "Malay", nativeName: "Bahasa Melayu", flag: "🇲🇾" },
  { code: "th", englishName: "Thai", nativeName: "ไทย", flag: "🇹🇭" },
  { code: "pl", englishName: "Polish", nativeName: "Polski", flag: "🇵🇱" },
  { code: "nl", englishName: "Dutch", nativeName: "Nederlands", flag: "🇳🇱" },
  { code: "uk", englishName: "Ukrainian", nativeName: "Українська", flag: "🇺🇦" },
  { code: "fa", englishName: "Persian", nativeName: "فارسی", flag: "🇮🇷", isRTL: true },
  { code: "he", englishName: "Hebrew", nativeName: "עברית", flag: "🇮🇱", isRTL: true },
  { code: "el", englishName: "Greek", nativeName: "Ελληνικά", flag: "🇬🇷" },
  { code: "cs", englishName: "Czech", nativeName: "Čeština", flag: "🇨🇿" },
  { code: "ro", englishName: "Romanian", nativeName: "Română", flag: "🇷🇴" },
  { code: "hu", englishName: "Hungarian", nativeName: "Magyar", flag: "🇭🇺" },
  { code: "sv", englishName: "Swedish", nativeName: "Svenska", flag: "🇸🇪" },
  { code: "nb", englishName: "Norwegian", nativeName: "Norsk", flag: "🇳🇴" },
  { code: "da", englishName: "Danish", nativeName: "Dansk", flag: "🇩🇰" },
  { code: "fi", englishName: "Finnish", nativeName: "Suomi", flag: "🇫🇮" },
];

const english: AppLanguage = languages[0];

export function languageFor(code: string | undefined): AppLanguage | undefined {
  if (code === undefined) return undefined;
  return languages.find((language) => language.code === code);
}

/**
 * Best match for the browser's preferred languages, falling back to English.
 *
 * Chinese needs script-level matching (`zh-Hans` vs `zh-Hant`); everything else
 * matches on the base language subtag.
 */
export function browserLanguage(): AppLanguage {
  const preferred = typeof navigator === "undefined" ? [] : navigator.languages ?? [navigator.language];
  for (const tag of preferred) {
    if (tag.startsWith("zh")) {
      const isTraditional = /Hant|TW|HK|MO/i.test(tag);
      return languageFor(isTraditional ? "zh-Hant" : "zh-Hans") ?? english;
    }
    const base = tag.split("-")[0];
    const match = languages.find((language) => language.code === base);
    if (match) return match;
    if (base === "no") return languageFor("nb") ?? english;
  }
  return english;
}

/**
 * Looks up `key` in `code`, falling back to English and then to the key itself
 * so a missing entry is visible but never throws.
 */
export function translate(code: string, key: string): string {
  return tables[code]?.[key] ?? tables.en?.[key] ?? key;
}

/** Active language plus a `t()` bound to it. Re-renders when the choice changes. */
export function useLanguage(): { language: AppLanguage; t: (key: string) => string } {
  const store = useAppStore();
  const language = languageFor(store.profile.languageCode) ?? browserLanguage();
  const code = language.code;
  const t = useCallback((key: string) => translate(code, key), [code]);
  return { language, t };
}

/** Shorthand for components that only need the translator. */
export function useT(): (key: string) => string {
  return useLanguage().t;
}
