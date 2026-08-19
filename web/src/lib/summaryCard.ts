/**
 * Canvas renderer for the shareable "Daily Summary" card.
 *
 * The card is drawn once and both previewed and exported from the same canvas,
 * so what the user sees is byte-identical to what leaves the app. Colours match
 * the iOS `Theme` values exactly so both platforms produce the same artwork.
 */

export type DailySummary = {
  date: Date;
  eaten: number;
  target: number;
  burned: number;
  protein: number;
  carbs: number;
  fat: number;
  proteinTarget: number;
  carbsTarget: number;
  fatTarget: number;
  mealCount: number;
  water: number;
  streak: number;
};

const INK = "#0B0B0C";
const INK_SOFT = "#6B6B72";
const INK_FAINT = "#A8A8AF";
const WELL = "#F2F2F5";
const FLAME = "#FF6B2C";
const WATER = "#39A0FF";
const PROTEIN = "#FF5A6E";
const CARBS = "#4C8DFF";
const FAT = "#F5A524";
const MINT = "#2FBF71";
const HAIRLINE = "rgba(0,0,0,0.06)";

const WIDTH = 340;
const PAD = 24;
const RADIUS = 26;
const RING = 176;
const RING_STROKE = 15;

const SANS = '-apple-system, "Segoe UI", system-ui, sans-serif';
const METRIC = 'Nunito, "SF Pro Rounded", system-ui, sans-serif';

/** Export scale — 3x keeps the card crisp when reshared or zoomed. */
export const CARD_SCALE = 3;
export const CARD_WIDTH = WIDTH;

function roundRect(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  width: number,
  height: number,
  radius: number,
): void {
  const r = Math.min(radius, width / 2, height / 2);
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + width, y, x + width, y + height, r);
  ctx.arcTo(x + width, y + height, x, y + height, r);
  ctx.arcTo(x, y + height, x, y, r);
  ctx.arcTo(x, y, x + width, y, r);
  ctx.closePath();
}

let iconPromise: Promise<HTMLImageElement | null> | null = null;

/** Loads the app icon once; resolves null when unavailable so the flame fallback draws instead. */
function loadAppIcon(): Promise<HTMLImageElement | null> {
  iconPromise ??= new Promise((resolve) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => resolve(null);
    image.src = "/icon.png";
  });
  return iconPromise;
}

/** Draws the flame mark as a vector so it matches the iOS glyph. */
function drawFlame(
  ctx: CanvasRenderingContext2D,
  cx: number,
  cy: number,
  size: number,
  color: string,
): void {
  ctx.save();
  ctx.translate(cx, cy);
  ctx.scale(size / 24, size / 24);
  ctx.fillStyle = color;
  ctx.beginPath();
  ctx.moveTo(0, -12);
  ctx.bezierCurveTo(4, -6, 9, -4.5, 9, 2);
  ctx.bezierCurveTo(9, 8, 4.5, 12, 0, 12);
  ctx.bezierCurveTo(-4.5, 12, -9, 8, -9, 2);
  ctx.bezierCurveTo(-9, -3.5, -4, -5.5, 0, -12);
  ctx.closePath();
  ctx.fill();
  ctx.restore();
}

/**
 * Draws letter-spaced text centred on `cx`.
 *
 * Canvas `letterSpacing` is still missing on older Safari, so the tracking is
 * applied by hand to keep the header identical everywhere.
 */
function drawTracked(
  ctx: CanvasRenderingContext2D,
  text: string,
  cx: number,
  y: number,
  spacing: number,
): void {
  const chars = [...text];
  const total =
    chars.reduce((sum, char) => sum + ctx.measureText(char).width, 0) +
    spacing * Math.max(chars.length - 1, 0);

  let x = cx - total / 2;
  ctx.textAlign = "left";
  for (const char of chars) {
    ctx.fillText(char, x, y);
    x += ctx.measureText(char).width + spacing;
  }
}

function macroRow(
  ctx: CanvasRenderingContext2D,
  y: number,
  label: string,
  value: number,
  target: number,
  tint: string,
): number {
  const width = WIDTH - PAD * 2;
  const fraction = target > 0 ? Math.min(value / target, 1) : 0;

  ctx.textBaseline = "alphabetic";
  ctx.textAlign = "left";
  ctx.fillStyle = INK;
  ctx.font = `700 15px ${SANS}`;
  ctx.fillText(label, PAD, y + 13);

  ctx.textAlign = "right";
  ctx.fillStyle = INK_FAINT;
  ctx.font = `500 14px ${SANS}`;
  ctx.fillText(`${Math.round(value)}g / ${target}g`, WIDTH - PAD, y + 13);

  const barY = y + 25;
  ctx.fillStyle = tint;
  ctx.globalAlpha = 0.18;
  roundRect(ctx, PAD, barY, width, 8, 4);
  ctx.fill();
  ctx.globalAlpha = 1;

  if (fraction > 0) {
    ctx.fillStyle = tint;
    roundRect(ctx, PAD, barY, Math.max(width * fraction, 8), 8, 4);
    ctx.fill();
  }

  return barY + 8;
}

function chip(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  width: number,
  value: string,
  label: string,
  tint: string,
): number {
  const height = 58;

  ctx.fillStyle = tint;
  ctx.globalAlpha = 0.12;
  roundRect(ctx, x, y, width, height, 14);
  ctx.fill();
  ctx.globalAlpha = 1;

  ctx.textAlign = "center";
  ctx.textBaseline = "alphabetic";
  ctx.fillStyle = INK;
  ctx.font = `800 18px ${METRIC}`;
  ctx.fillText(value, x + width / 2, y + 27);

  ctx.fillStyle = INK_SOFT;
  ctx.font = `500 12px ${SANS}`;
  ctx.fillText(label, x + width / 2, y + 45);

  return y + height;
}

/** Draws the whole card and returns the height it consumed. */
function paint(
  ctx: CanvasRenderingContext2D,
  summary: DailySummary,
  icon: HTMLImageElement | null,
): number {
  const budget = summary.target + summary.burned;
  const remaining = budget - summary.eaten;
  const isOver = remaining < 0;
  const accent = isOver ? FLAME : MINT;
  const progress = budget > 0 ? Math.min(summary.eaten / budget, 1) : 0;
  const cx = WIDTH / 2;

  let y = 26;

  // Date
  ctx.fillStyle = INK_FAINT;
  ctx.font = `600 12px ${SANS}`;
  ctx.textBaseline = "alphabetic";
  const dateLine = summary.date
    .toLocaleDateString(undefined, { weekday: "long", month: "short", day: "numeric" })
    .toUpperCase();
  drawTracked(ctx, dateLine, cx, y + 11, 1.3);
  y += 17;

  // Title
  ctx.textAlign = "center";
  ctx.fillStyle = INK;
  ctx.font = `800 28px ${METRIC}`;
  ctx.fillText("Daily Summary", cx, y + 28);
  y += 40;

  // Ring
  const ringCx = cx;
  const ringCy = y + RING / 2;
  const radius = (RING - RING_STROKE) / 2;

  ctx.lineWidth = RING_STROKE;
  ctx.strokeStyle = WELL;
  ctx.beginPath();
  ctx.arc(ringCx, ringCy, radius, 0, Math.PI * 2);
  ctx.stroke();

  if (progress > 0) {
    ctx.strokeStyle = accent;
    ctx.lineCap = "round";
    ctx.beginPath();
    ctx.arc(ringCx, ringCy, radius, -Math.PI / 2, -Math.PI / 2 + Math.PI * 2 * progress);
    ctx.stroke();
    ctx.lineCap = "butt";
  }

  ctx.fillStyle = INK;
  ctx.font = `800 46px ${METRIC}`;
  ctx.fillText(`${summary.eaten}`, ringCx, ringCy + 8);

  ctx.fillStyle = INK_FAINT;
  ctx.font = `500 14px ${SANS}`;
  ctx.fillText(`of ${budget} kcal`, ringCx, ringCy + 30);
  y += RING + 18;

  // Remaining
  ctx.fillStyle = accent;
  ctx.font = `600 16px ${SANS}`;
  ctx.fillText(
    isOver ? `${-remaining} kcal over` : `${remaining} kcal remaining`,
    cx,
    y + 14,
  );
  y += 40;

  // Macros
  y = macroRow(ctx, y, "Protein", summary.protein, summary.proteinTarget, PROTEIN) + 14;
  y = macroRow(ctx, y, "Carbs", summary.carbs, summary.carbsTarget, CARBS) + 14;
  y = macroRow(ctx, y, "Fat", summary.fat, summary.fatTarget, FAT) + 20;

  // Chips
  const gap = 8;
  const chipWidth = (WIDTH - PAD * 2 - gap * 2) / 3;
  const waterLabel =
    summary.water >= 1000 ? `${(summary.water / 1000).toFixed(1)}L` : `${summary.water}ml`;

  chip(ctx, PAD, y, chipWidth, `${summary.mealCount}`, summary.mealCount === 1 ? "Meal" : "Meals", MINT);
  chip(ctx, PAD + chipWidth + gap, y, chipWidth, waterLabel, "Water", WATER);
  y = chip(ctx, PAD + (chipWidth + gap) * 2, y, chipWidth, `${summary.burned}`, "Burned", FLAME);
  y += 22;

  // Divider
  ctx.fillStyle = HAIRLINE;
  ctx.fillRect(PAD, y, WIDTH - PAD * 2, 1);
  y += 15;

  // Footer — real app icon, with the vector flame as an offline fallback.
  if (icon) {
    ctx.save();
    roundRect(ctx, PAD, y, 28, 28, 8);
    ctx.clip();
    ctx.drawImage(icon, PAD, y, 28, 28);
    ctx.restore();
  } else {
    ctx.fillStyle = INK;
    roundRect(ctx, PAD, y, 28, 28, 8);
    ctx.fill();
    drawFlame(ctx, PAD + 14, y + 14, 15, "#FFFFFF");
  }

  ctx.textAlign = "left";
  ctx.fillStyle = INK_FAINT;
  ctx.font = `500 14px ${SANS}`;
  const prefix = "Tracked with ";
  ctx.fillText(prefix, PAD + 37, y + 19);

  const prefixWidth = ctx.measureText(prefix).width;
  ctx.fillStyle = INK;
  ctx.font = `800 14px ${METRIC}`;
  ctx.fillText("ModernBody", PAD + 37 + prefixWidth, y + 19);

  if (summary.streak > 1) {
    ctx.textAlign = "right";
    ctx.fillStyle = FLAME;
    ctx.font = `800 13px ${METRIC}`;
    ctx.fillText(`${summary.streak}`, WIDTH - PAD, y + 19);

    const streakWidth = ctx.measureText(`${summary.streak}`).width;
    drawFlame(ctx, WIDTH - PAD - streakWidth - 8, y + 14, 11, FLAME);
  }

  return y + 28 + 14;
}

/**
 * Renders the summary onto `canvas`, sizing it to the content.
 *
 * Runs a throwaway measuring pass first so the canvas height always matches the
 * drawing exactly, whatever the numbers are.
 */
export async function drawSummaryCard(
  canvas: HTMLCanvasElement,
  summary: DailySummary,
): Promise<void> {
  if (document.fonts?.ready) {
    try {
      await document.fonts.ready;
    } catch {
      // Fall through to system fonts.
    }
  }

  const icon = await loadAppIcon();

  const measure = document.createElement("canvas").getContext("2d");
  if (!measure) throw new Error("Canvas is unavailable");
  const height = paint(measure, summary, icon);

  canvas.width = WIDTH * CARD_SCALE;
  canvas.height = height * CARD_SCALE;
  canvas.style.width = "100%";
  canvas.style.height = "auto";

  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("Canvas is unavailable");

  ctx.scale(CARD_SCALE, CARD_SCALE);
  ctx.clearRect(0, 0, WIDTH, height);

  ctx.fillStyle = "#FFFFFF";
  roundRect(ctx, 0, 0, WIDTH, height, RADIUS);
  ctx.fill();

  paint(ctx, summary, icon);
}

export function canvasToBlob(canvas: HTMLCanvasElement): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) resolve(blob);
      else reject(new Error("Could not export the summary image"));
    }, "image/png");
  });
}
