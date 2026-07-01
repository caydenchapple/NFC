export type ParsedNdefRecord =
  | { kind: 'text'; text: string; lang: string }
  | { kind: 'uri'; uri: string }
  | { kind: 'vcard'; name?: string; phone?: string; email?: string; org?: string }
  | { kind: 'wifi'; ssid?: string; hasPassword: boolean }
  | { kind: 'mime'; mimeType: string; byteLength: number }
  | { kind: 'external'; domain: string; type: string }
  | { kind: 'unknown'; tnf: number; type: string; byteLength: number };

export interface TagInfo {
  idHex: string;
  techTypes: string[];
  tagType?: string;
  maxSizeBytes?: number;
  usedSizeBytes?: number;
  records: ParsedNdefRecord[];
  /** Raw records as returned by react-native-nfc-manager, kept around so callers can look
   * for app-specific MIME records (e.g. an attached task chain) without re-parsing. */
  rawRecords: import('react-native-nfc-manager').NdefRecord[];
}

export type NfcOpResult = { ok: true } | { ok: false; message: string };
