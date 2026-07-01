import { Ndef, NdefRecord } from 'react-native-nfc-manager';
import { ParsedNdefRecord } from './types';
import { utf16leDecode, utf8Decode } from './textCodec';

function typeToString(type: number[] | string): string {
  return typeof type === 'string' ? type : Ndef.util.bytesToString(type);
}

function payloadBytes(payload: any[]): Uint8Array {
  return Uint8Array.from(payload);
}

export function parseNdefMessage(records: NdefRecord[]): ParsedNdefRecord[] {
  return records.map(parseNdefRecord);
}

export function parseNdefRecord(record: NdefRecord): ParsedNdefRecord {
  const type = typeToString(record.type);
  const bytes = payloadBytes(record.payload);

  if (record.tnf === Ndef.TNF_WELL_KNOWN && type === Ndef.RTD_TEXT) {
    return parseText(bytes);
  }
  if (record.tnf === Ndef.TNF_WELL_KNOWN && type === Ndef.RTD_URI) {
    return { kind: 'uri', uri: Ndef.uri.decodePayload(bytes) };
  }
  if (record.tnf === Ndef.TNF_ABSOLUTE_URI) {
    return { kind: 'uri', uri: Ndef.util.bytesToString(record.payload) };
  }
  if (record.tnf === Ndef.TNF_MIME_MEDIA) {
    return parseMime(type, bytes);
  }
  if (record.tnf === Ndef.TNF_EXTERNAL_TYPE) {
    const domain = type.split(':')[0] ?? type;
    return { kind: 'external', domain, type };
  }
  return { kind: 'unknown', tnf: record.tnf, type, byteLength: bytes.length };
}

function parseText(payload: Uint8Array): ParsedNdefRecord {
  const statusByte = payload[0];
  const isUtf16 = (statusByte & 0x80) !== 0;
  const langLength = statusByte & 0x3f;
  const lang = utf8Decode(payload.slice(1, 1 + langLength));
  const textBytes = payload.slice(1 + langLength);
  const text = isUtf16 ? utf16leDecode(textBytes) : utf8Decode(textBytes);
  return { kind: 'text', text, lang };
}

function parseMime(mimeType: string, payload: Uint8Array): ParsedNdefRecord {
  if (mimeType === 'text/vcard' || mimeType === 'text/x-vcard') {
    return parseVCard(payload);
  }
  if (mimeType === Ndef.MIME_WFA_WSC) {
    return parseWifi(payload);
  }
  return { kind: 'mime', mimeType, byteLength: payload.length };
}

function parseVCard(payload: Uint8Array): ParsedNdefRecord {
  const text = utf8Decode(payload);
  const field = (key: string): string | undefined => {
    const match = new RegExp(`^${key}(?:;[^:]*)?:(.*)$`, 'im').exec(text);
    return match?.[1]?.trim();
  };
  return {
    kind: 'vcard',
    name: field('FN') ?? field('N')?.replace(/;/g, ' ').trim(),
    phone: field('TEL'),
    email: field('EMAIL'),
    org: field('ORG'),
  };
}

function parseWifi(payload: Uint8Array): ParsedNdefRecord {
  try {
    const creds = Ndef.wifiSimple.decodePayload(payload);
    return { kind: 'wifi', ssid: creds.ssid, hasPassword: Boolean(creds.networkKey) };
  } catch {
    return { kind: 'wifi', hasPassword: false };
  }
}
