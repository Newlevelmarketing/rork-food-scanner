import { ArrowLeft } from "lucide-react";
import type { JSX } from "react";

import type { LegalDocument } from "@/lib/legal";
import { appName, appVersion } from "@/lib/legal";

/**
 * Standalone legal document page.
 *
 * Reached at `/privacy` and `/terms`. These are public URLs on purpose — the App
 * Store and Play Console both require a reachable privacy policy link, and the
 * in-app Settings rows open these same routes so the two can never drift.
 *
 * The global stylesheet pins `body` to the visual viewport for the app shell, so
 * this page owns its own scroll container rather than relying on the doc.
 */
export function LegalPage({ doc }: { doc: LegalDocument }): JSX.Element {
  return (
    <div className="calzy-backdrop h-full w-full overflow-y-auto">
      <main className="mx-auto flex w-full max-w-[680px] flex-col gap-6 px-5 pb-16 pt-8">
        <a
          href="/"
          className="pressable flex w-fit items-center gap-2 text-[14px] font-medium text-ink-soft"
        >
          <ArrowLeft size={16} strokeWidth={2.6} />
          Back to {appName}
        </a>

        <header className="flex flex-col gap-1">
          <h1 className="metric text-[32px] leading-tight text-ink">{doc.title}</h1>
          <p className="text-[13px] text-ink-faint">
            {appName} {appVersion} · Last updated {doc.lastUpdated}
          </p>
        </header>

        <article className="flex flex-col gap-4">
          {doc.blocks.map((block, index) => {
            if (block.kind === "heading") {
              return (
                <h2
                  key={index}
                  className="mt-2 text-[15px] font-bold uppercase tracking-wide text-ink"
                >
                  {block.text}
                </h2>
              );
            }

            if (block.kind === "bullets") {
              return (
                <ul key={index} className="flex flex-col gap-2 pl-1">
                  {block.items.map((item) => (
                    <li key={item} className="flex gap-3 text-[15px] leading-relaxed text-ink-soft">
                      <span aria-hidden="true" className="select-none text-ink-faint">
                        •
                      </span>
                      <span className="min-w-0">{item}</span>
                    </li>
                  ))}
                </ul>
              );
            }

            return (
              <p key={index} className="text-[15px] leading-relaxed text-ink-soft">
                {block.text}
              </p>
            );
          })}
        </article>
      </main>
    </div>
  );
}
