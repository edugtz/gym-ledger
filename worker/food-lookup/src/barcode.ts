export interface BarcodeValidationSuccess {
  ok: true;
  barcode: string;
}

export interface BarcodeValidationFailure {
  ok: false;
}

export type BarcodeValidationResult =
  | BarcodeValidationSuccess
  | BarcodeValidationFailure;

const ALLOWED_LENGTHS = new Set([8, 12, 13, 14]);

export function normalizeAndValidateBarcode(
  raw: string
): BarcodeValidationResult {
  const trimmed = raw.trim();
  if (!trimmed) {
    return { ok: false };
  }

  if (!/^\d+$/.test(trimmed)) {
    return { ok: false };
  }

  if (!ALLOWED_LENGTHS.has(trimmed.length)) {
    return { ok: false };
  }

  return { ok: true, barcode: trimmed };
}
