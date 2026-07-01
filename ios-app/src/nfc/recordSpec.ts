import { Ndef, NdefRecord } from 'react-native-nfc-manager';
import { utf8Encode } from './textCodec';

export type SocialNetwork =
  | 'FACEBOOK'
  | 'INSTAGRAM'
  | 'X_TWITTER'
  | 'LINKEDIN'
  | 'TIKTOK'
  | 'YOUTUBE'
  | 'GITHUB'
  | 'WHATSAPP';

const SOCIAL_URL_TEMPLATES: Record<SocialNetwork, string> = {
  FACEBOOK: 'https://facebook.com/%s',
  INSTAGRAM: 'https://instagram.com/%s',
  X_TWITTER: 'https://x.com/%s',
  LINKEDIN: 'https://linkedin.com/in/%s',
  TIKTOK: 'https://tiktok.com/@%s',
  YOUTUBE: 'https://youtube.com/@%s',
  GITHUB: 'https://github.com/%s',
  WHATSAPP: 'https://wa.me/%s',
};

export type WifiAuthType = 'OPEN' | 'WPA_PERSONAL' | 'WPA2_PERSONAL';

/**
 * A "standardized record" the Write tab can build, kept as a plain serializable object
 * (JSON.stringify works directly -- no serialization library needed) so it round-trips
 * through AsyncStorage as a saved Profile just as easily as it becomes an NdefRecord.
 */
export type RecordSpec =
  | { kind: 'text'; text: string; lang?: string }
  | { kind: 'url'; url: string }
  | { kind: 'phone'; number: string }
  | { kind: 'sms'; number: string; message?: string }
  | { kind: 'email'; address: string; subject?: string; body?: string }
  | { kind: 'geo'; lat: number; lon: number; label?: string }
  | { kind: 'social'; network: SocialNetwork; username: string }
  | { kind: 'wifi'; ssid: string; password?: string; auth: WifiAuthType }
  | { kind: 'vcard'; name: string; phone?: string; email?: string; org?: string; url?: string }
  | { kind: 'video'; url: string }
  | { kind: 'appLink'; url: string };

function encodeUri(value: string): string {
  return encodeURIComponent(value).replace(/%20/g, '+');
}

export function recordLabel(spec: RecordSpec): string {
  switch (spec.kind) {
    case 'text':
      return `Text: ${spec.text}`;
    case 'url':
      return `URL: ${spec.url}`;
    case 'phone':
      return `Call: ${spec.number}`;
    case 'sms':
      return `SMS: ${spec.number}`;
    case 'email':
      return `Email: ${spec.address}`;
    case 'geo':
      return `Location: ${spec.lat}, ${spec.lon}`;
    case 'social':
      return `${spec.network}: ${spec.username}`;
    case 'wifi':
      return `Wi-Fi: ${spec.ssid}`;
    case 'vcard':
      return `Contact: ${spec.name}`;
    case 'video':
      return `Video: ${spec.url}`;
    case 'appLink':
      return `App link: ${spec.url}`;
  }
}

export function toNdefRecord(spec: RecordSpec): NdefRecord {
  switch (spec.kind) {
    case 'text':
      return Ndef.textRecord(spec.text, spec.lang ?? 'en');
    case 'url': {
      const normalized = /^https?:\/\//i.test(spec.url) ? spec.url : `https://${spec.url}`;
      return Ndef.uriRecord(normalized);
    }
    case 'phone':
      return Ndef.uriRecord(`tel:${spec.number}`);
    case 'sms': {
      const body = spec.message ? `?body=${encodeUri(spec.message)}` : '';
      return Ndef.uriRecord(`sms:${spec.number}${body}`);
    }
    case 'email': {
      const params = [
        spec.subject ? `subject=${encodeUri(spec.subject)}` : null,
        spec.body ? `body=${encodeUri(spec.body)}` : null,
      ].filter(Boolean);
      const query = params.length ? `?${params.join('&')}` : '';
      return Ndef.uriRecord(`mailto:${spec.address}${query}`);
    }
    case 'geo': {
      const query = spec.label ? `?q=${encodeUri(spec.label)}` : '';
      return Ndef.uriRecord(`geo:${spec.lat},${spec.lon}${query}`);
    }
    case 'social':
      return Ndef.uriRecord(SOCIAL_URL_TEMPLATES[spec.network].replace('%s', spec.username.trim()));
    case 'wifi':
      return Ndef.wifiSimpleRecord({
        ssid: spec.ssid,
        networkKey: spec.password ?? '',
        authType: authTypeBytes(spec.auth),
      });
    case 'vcard':
      return Ndef.record(Ndef.TNF_MIME_MEDIA, 'text/vcard', [], utf8Encode(buildVCardText(spec)));
    case 'video':
      return Ndef.uriRecord(spec.url);
    case 'appLink':
      return Ndef.uriRecord(spec.url);
  }
}

function authTypeBytes(auth: WifiAuthType): number[] {
  switch (auth) {
    case 'OPEN':
      return [0x00, 0x01];
    case 'WPA_PERSONAL':
      return [0x00, 0x02];
    case 'WPA2_PERSONAL':
      return [0x00, 0x20];
  }
}

function buildVCardText(spec: Extract<RecordSpec, { kind: 'vcard' }>): string {
  const lines = [
    'BEGIN:VCARD',
    'VERSION:3.0',
    `FN:${spec.name}`,
    `N:${spec.name};;;;`,
  ];
  if (spec.org) lines.push(`ORG:${spec.org}`);
  if (spec.phone) lines.push(`TEL;TYPE=CELL:${spec.phone}`);
  if (spec.email) lines.push(`EMAIL:${spec.email}`);
  if (spec.url) lines.push(`URL:${spec.url}`);
  lines.push('END:VCARD', '');
  return lines.join('\r\n');
}
