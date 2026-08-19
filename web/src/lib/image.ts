/** Client-side image downscaling so photos stay small enough for the AI
 *  gateway payload budget and for localStorage persistence. */

async function loadImage(source: Blob | string): Promise<HTMLImageElement> {
  const url = typeof source === "string" ? source : URL.createObjectURL(source);
  try {
    return await new Promise<HTMLImageElement>((resolve, reject) => {
      const image = new Image();
      image.onload = () => resolve(image);
      image.onerror = () => reject(new Error("Could not read that image."));
      image.src = url;
    });
  } finally {
    if (typeof source !== "string") {
      // Revoke on the next tick so decoding has finished.
      setTimeout(() => URL.revokeObjectURL(url), 0);
    }
  }
}

function drawToDataURL(image: HTMLImageElement, maxEdge: number, quality: number): string | null {
  const longest = Math.max(image.naturalWidth, image.naturalHeight);
  if (longest === 0) return null;
  const scale = Math.min(1, maxEdge / longest);
  const width = Math.max(1, Math.round(image.naturalWidth * scale));
  const height = Math.max(1, Math.round(image.naturalHeight * scale));

  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const context = canvas.getContext("2d");
  if (!context) return null;
  context.fillStyle = "#000";
  context.fillRect(0, 0, width, height);
  context.drawImage(image, 0, 0, width, height);
  return canvas.toDataURL("image/jpeg", quality);
}

/** Walks a resize/quality ladder until the JPEG fits inside the byte budget. */
export async function toBudgetedDataURL(
  source: Blob | string,
  maxBytes = 2_600_000,
): Promise<string | null> {
  const image = await loadImage(source);
  const ladder: Array<[number, number]> = [
    [1280, 0.82],
    [1024, 0.78],
    [832, 0.74],
    [640, 0.7],
    [512, 0.65],
  ];

  for (const [maxEdge, quality] of ladder) {
    const dataURL = drawToDataURL(image, maxEdge, quality);
    if (!dataURL) continue;
    // base64 expands bytes by ~4/3.
    const bytes = Math.ceil((dataURL.length * 3) / 4);
    if (bytes <= maxBytes) return dataURL;
  }
  return null;
}

/** Small JPEG suitable for persisting in localStorage. */
export async function toThumbnail(source: Blob | string, maxEdge = 420): Promise<string | null> {
  const image = await loadImage(source);
  return drawToDataURL(image, maxEdge, 0.72);
}

export function captureVideoFrame(video: HTMLVideoElement): string | null {
  const width = video.videoWidth;
  const height = video.videoHeight;
  if (!width || !height) return null;

  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const context = canvas.getContext("2d");
  if (!context) return null;
  context.drawImage(video, 0, 0, width, height);
  return canvas.toDataURL("image/jpeg", 0.86);
}
