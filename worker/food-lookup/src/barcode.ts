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

const GTIN_14_LENGTH = 14;

/**
 * Returns the GTIN-14 canonical form of a barcode, or null when the value is
 * not a bare ASCII-digit string of an allowed GTIN length (8/12/13/14).
 *
 * This is identity-only comparison: it never mutates the caller's barcode and
 * never performs check-digit validation.
 */
export function canonicalizeGtin(barcode: string): string | null {
  if (!/^\d+$/.test(barcode)) {
    return null;
  }
  if (!ALLOWED_LENGTHS.has(barcode.length)) {
    return null;
  }
  return barcode.padStart(GTIN_14_LENGTH, "0");
}

/**
 * Two barcode representations identify the same product when both are valid
 * GTIN lengths and their GTIN-14 canonical forms match.
 *
 * Examples of equivalents:
 *   810104461665  <-> 0810104461665
 *   681131077637  <-> 0681131077637
 *
 * Malformed input on either side yields false (never throws).
 */
export function areGtinEquivalent(a: string, b: string): boolean {
  const canonicalA = canonicalizeGtin(a);
  const canonicalB = canonicalizeGtin(b);
  if (canonicalA === null || canonicalB === null) {
    return false;
  }
  return canonicalA === canonicalB;
}
