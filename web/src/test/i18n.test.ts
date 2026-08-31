import catalogue from "@/data/strings.json";
import { browserLanguage, languageFor, languages, translate } from "@/lib/i18n";

const tables = catalogue as Record<string, Record<string, string>>;

/** Replaces `navigator` for one assertion; restored in afterEach. */
function withPreferredLanguages(preferred: string[] | undefined): void {
  vi.stubGlobal("navigator", preferred === undefined ? undefined : { languages: preferred });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("languages", () => {
  it("ships 32 languages with English first", () => {
    expect(languages).toHaveLength(32);
    expect(languages[0].code).toBe("en");
  });

  it("has no duplicate codes", () => {
    const codes = languages.map((language) => language.code);
    expect(new Set(codes).size).toBe(codes.length);
  });

  it("gives every language a name, endonym and flag", () => {
    for (const language of languages) {
      expect(language.englishName.length).toBeGreaterThan(0);
      expect(language.nativeName.length).toBeGreaterThan(0);
      expect(language.flag.length).toBeGreaterThan(0);
    }
  });

  it("marks exactly Arabic, Persian and Hebrew as right-to-left", () => {
    const rtl = languages.filter((language) => language.isRTL === true).map((l) => l.code).sort();
    expect(rtl).toEqual(["ar", "fa", "he"]);
  });
});

describe("catalogue integrity", () => {
  // `context/execution-standards.md` requires shipped copy to reach all 32
  // locales. This turns that rule from honour-system into a failing test.
  const englishKeys = Object.keys(tables.en).sort();

  it("has a string table for every declared language", () => {
    const missing = languages.filter((language) => tables[language.code] === undefined);
    expect(missing.map((language) => language.code)).toEqual([]);
  });

  it("has no string table for an undeclared language", () => {
    const declared = new Set(languages.map((language) => language.code));
    const orphans = Object.keys(tables).filter((code) => !declared.has(code));
    expect(orphans).toEqual([]);
  });

  it("gives every language the same keys as English", () => {
    const offenders: string[] = [];
    for (const language of languages) {
      const table = tables[language.code];
      if (table === undefined) continue;
      const keys = Object.keys(table).sort();
      const missing = englishKeys.filter((key) => !(key in table));
      const extra = keys.filter((key) => !(key in tables.en));
      if (missing.length > 0 || extra.length > 0) {
        offenders.push(`${language.code}: missing [${missing.join(", ")}] extra [${extra.join(", ")}]`);
      }
    }
    expect(offenders).toEqual([]);
  });

  it("leaves no translation empty", () => {
    const empty: string[] = [];
    for (const [code, table] of Object.entries(tables)) {
      for (const [key, value] of Object.entries(table)) {
        if (value.trim() === "") empty.push(`${code}.${key}`);
      }
    }
    expect(empty).toEqual([]);
  });
});

describe("languageFor", () => {
  it("finds a declared language", () => {
    expect(languageFor("fr")?.englishName).toBe("French");
  });

  it("returns undefined for an unknown code", () => {
    expect(languageFor("xx")).toBeUndefined();
  });

  it("returns undefined when given undefined", () => {
    expect(languageFor(undefined)).toBeUndefined();
  });
});

describe("browserLanguage", () => {
  it("matches on the base subtag", () => {
    withPreferredLanguages(["es-ES"]);
    expect(browserLanguage().code).toBe("es");
  });

  it("matches an exact base tag", () => {
    withPreferredLanguages(["fr"]);
    expect(browserLanguage().code).toBe("fr");
  });

  describe("Chinese needs script-level matching", () => {
    it("routes Taiwan to Traditional", () => {
      withPreferredLanguages(["zh-TW"]);
      expect(browserLanguage().code).toBe("zh-Hant");
    });

    it("routes Hong Kong to Traditional", () => {
      withPreferredLanguages(["zh-HK"]);
      expect(browserLanguage().code).toBe("zh-Hant");
    });

    it("respects an explicit Hant script tag", () => {
      withPreferredLanguages(["zh-Hant-HK"]);
      expect(browserLanguage().code).toBe("zh-Hant");
    });

    it("routes mainland China to Simplified", () => {
      withPreferredLanguages(["zh-CN"]);
      expect(browserLanguage().code).toBe("zh-Hans");
    });

    it("defaults a bare zh to Simplified", () => {
      withPreferredLanguages(["zh"]);
      expect(browserLanguage().code).toBe("zh-Hans");
    });
  });

  it("maps the Norwegian macrolanguage tag to Bokmal", () => {
    withPreferredLanguages(["no"]);
    expect(browserLanguage().code).toBe("nb");
  });

  it("falls through an unsupported tag to the next preference", () => {
    withPreferredLanguages(["cy-GB", "fr-FR"]);
    expect(browserLanguage().code).toBe("fr");
  });

  it("falls back to English when nothing matches", () => {
    withPreferredLanguages(["cy-GB", "ga-IE"]);
    expect(browserLanguage().code).toBe("en");
  });

  it("falls back to English for an empty preference list", () => {
    withPreferredLanguages([]);
    expect(browserLanguage().code).toBe("en");
  });

  it("falls back to English when navigator is absent", () => {
    withPreferredLanguages(undefined);
    expect(browserLanguage().code).toBe("en");
  });
});

describe("translate", () => {
  it("returns the string for a known key in a known language", () => {
    expect(translate("en", "tab.home")).toBe("Home");
    expect(translate("es", "tab.home")).toBe(tables.es["tab.home"]);
  });

  it("falls back to English for a language it does not know", () => {
    expect(translate("xx", "tab.home")).toBe("Home");
  });

  it("returns the key itself when nothing has it, rather than throwing", () => {
    expect(translate("en", "definitely.not.a.key")).toBe("definitely.not.a.key");
    expect(translate("xx", "definitely.not.a.key")).toBe("definitely.not.a.key");
  });
});
