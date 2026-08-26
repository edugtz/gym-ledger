import { describe, it, expect } from "vitest";
import { normalizeAndValidateBarcode, areGtinEquivalent, canonicalizeGtin } from "./barcode";

describe("normalizeAndValidateBarcode", () => {
  it("accepts valid 13-digit EAN-13", () => {
    const result = normalizeAndValidateBarcode("3017620422003");
    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.barcode).toBe("3017620422003");
    }
  });

  it("accepts valid 12-digit UPC-A", () => {
    const result = normalizeAndValidateBarcode("012345678905");
    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.barcode).toBe("012345678905");
    }
  });

  it("accepts valid 8-digit GTIN-8", () => {
    const result = normalizeAndValidateBarcode("12345670");
    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.barcode).toBe("12345670");
    }
  });

  it("accepts valid 14-digit GTIN-14", () => {
    const result = normalizeAndValidateBarcode("00012345600012");
    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.barcode).toBe("00012345600012");
    }
  });

  it("preserves leading zeroes", () => {
    const result = normalizeAndValidateBarcode("00012345600012");
    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.barcode).toBe("00012345600012");
    }
  });

  it("trims surrounding whitespace", () => {
    const result = normalizeAndValidateBarcode("  3017620422003  ");
    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.barcode).toBe("3017620422003");
    }
  });

  it("rejects empty string", () => {
    expect(normalizeAndValidateBarcode("").ok).toBe(false);
  });

  it("rejects whitespace-only string", () => {
    expect(normalizeAndValidateBarcode("   ").ok).toBe(false);
  });

  it("rejects letters", () => {
    expect(normalizeAndValidateBarcode("ABC123").ok).toBe(false);
  });

  it("rejects mixed alphanumeric", () => {
    expect(normalizeAndValidateBarcode("301762ABC03").ok).toBe(false);
  });

  it("rejects punctuation", () => {
    expect(normalizeAndValidateBarcode("3017-6204-2200").ok).toBe(false);
  });

  it("rejects too short (4 digits)", () => {
    expect(normalizeAndValidateBarcode("1234").ok).toBe(false);
  });

  it("rejects unsupported length (9 digits)", () => {
    expect(normalizeAndValidateBarcode("123456789").ok).toBe(false);
  });

  it("rejects unsupported length (11 digits)", () => {
    expect(normalizeAndValidateBarcode("12345678901").ok).toBe(false);
  });

  it("rejects unsupported length (15 digits)", () => {
    expect(normalizeAndValidateBarcode("123456789012345").ok).toBe(false);
  });

  it("rejects embedded spaces", () => {
    expect(normalizeAndValidateBarcode("301 762 042 2003").ok).toBe(false);
  });

  it("rejects very long input", () => {
    expect(normalizeAndValidateBarcode("1".repeat(50)).ok).toBe(false);
  });
});

describe("canonicalizeGtin", () => {
  it("pads valid GTIN lengths to GTIN-14", () => {
    expect(canonicalizeGtin("12345670")).toBe("00000012345670");
    expect(canonicalizeGtin("810104461665")).toBe("00810104461665");
    expect(canonicalizeGtin("0810104461665")).toBe("00810104461665");
    expect(canonicalizeGtin("00012345600012")).toBe("00012345600012");
  });

  it("returns null for non-digit input", () => {
    expect(canonicalizeGtin("ABC123")).toBeNull();
  });

  it("returns null for unsupported lengths", () => {
    expect(canonicalizeGtin("123")).toBeNull();
    expect(canonicalizeGtin("123456789")).toBeNull();
    expect(canonicalizeGtin("1".repeat(15))).toBeNull();
  });
});

describe("areGtinEquivalent", () => {
  it("treats exact match as equivalent", () => {
    expect(areGtinEquivalent("3017620422003", "3017620422003")).toBe(true);
  });

  it("treats UPC-12 vs zero-prefixed EAN-13 as equivalent", () => {
    expect(areGtinEquivalent("810104461665", "0810104461665")).toBe(true);
  });

  it("is symmetric for reverse representation", () => {
    expect(areGtinEquivalent("0810104461665", "810104461665")).toBe(true);
  });

  it("treats GTIN-14 equivalent padding as equivalent", () => {
    expect(areGtinEquivalent("681131077637", "0681131077637")).toBe(true);
    expect(areGtinEquivalent("12345670", "00000012345670")).toBe(true);
  });

  it("rejects genuinely different codes", () => {
    expect(areGtinEquivalent("3017620422003", "9999999999999")).toBe(false);
  });

  it("rejects malformed provider code", () => {
    expect(areGtinEquivalent("ABC123", "3017620422003")).toBe(false);
    expect(areGtinEquivalent("123", "3017620422003")).toBe(false);
  });
});
