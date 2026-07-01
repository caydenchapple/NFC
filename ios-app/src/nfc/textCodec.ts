/**
 * Small dependency-free UTF-8 / UTF-16LE decoders. Hermes (RN's JS engine) doesn't ship
 * Node's `Buffer`, and `TextDecoder` availability varies by RN version, so NDEF text
 * payloads are decoded by hand instead of pulling in a polyfill.
 */
export function utf8Decode(bytes: Uint8Array): string {
  let result = '';
  let i = 0;
  while (i < bytes.length) {
    const b0 = bytes[i];
    if (b0 < 0x80) {
      result += String.fromCharCode(b0);
      i += 1;
    } else if ((b0 & 0xe0) === 0xc0 && i + 1 < bytes.length) {
      const b1 = bytes[i + 1];
      result += String.fromCharCode(((b0 & 0x1f) << 6) | (b1 & 0x3f));
      i += 2;
    } else if ((b0 & 0xf0) === 0xe0 && i + 2 < bytes.length) {
      const b1 = bytes[i + 1];
      const b2 = bytes[i + 2];
      result += String.fromCharCode(((b0 & 0x0f) << 12) | ((b1 & 0x3f) << 6) | (b2 & 0x3f));
      i += 3;
    } else if ((b0 & 0xf8) === 0xf0 && i + 3 < bytes.length) {
      const b1 = bytes[i + 1];
      const b2 = bytes[i + 2];
      const b3 = bytes[i + 3];
      const codepoint =
        ((b0 & 0x07) << 18) | ((b1 & 0x3f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f);
      result += String.fromCodePoint(codepoint);
      i += 4;
    } else {
      result += String.fromCharCode(b0);
      i += 1;
    }
  }
  return result;
}

export function utf16leDecode(bytes: Uint8Array): string {
  let result = '';
  for (let i = 0; i + 1 < bytes.length; i += 2) {
    result += String.fromCharCode(bytes[i] | (bytes[i + 1] << 8));
  }
  return result;
}

export function utf8Encode(text: string): number[] {
  const bytes: number[] = [];
  for (const char of text) {
    const codepoint = char.codePointAt(0)!;
    if (codepoint < 0x80) {
      bytes.push(codepoint);
    } else if (codepoint < 0x800) {
      bytes.push(0xc0 | (codepoint >> 6), 0x80 | (codepoint & 0x3f));
    } else if (codepoint < 0x10000) {
      bytes.push(0xe0 | (codepoint >> 12), 0x80 | ((codepoint >> 6) & 0x3f), 0x80 | (codepoint & 0x3f));
    } else {
      bytes.push(
        0xf0 | (codepoint >> 18),
        0x80 | ((codepoint >> 12) & 0x3f),
        0x80 | ((codepoint >> 6) & 0x3f),
        0x80 | (codepoint & 0x3f),
      );
    }
  }
  return bytes;
}
