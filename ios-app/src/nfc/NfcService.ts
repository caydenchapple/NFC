import NfcManager, { Ndef, NdefRecord, NfcTech } from 'react-native-nfc-manager';
import { parseNdefMessage } from './ndefParser';
import { NfcOpResult, TagInfo } from './types';

let started = false;

export async function ensureStarted(): Promise<boolean> {
  if (started) return true;
  const supported = await NfcManager.isSupported();
  if (!supported) return false;
  await NfcManager.start();
  started = true;
  return true;
}

function idToHex(id?: string): string {
  return id ? id.toUpperCase() : 'UNKNOWN';
}

/** Opens a session, runs `body`, and always tears the session down -- mirrors the
 * try/finally pattern the Android NfcController uses around every tag operation. */
async function withSession<T>(
  tech: NfcTech | NfcTech[],
  alertMessage: string,
  body: () => Promise<T>,
): Promise<T> {
  await NfcManager.requestTechnology(tech, { alertMessage });
  try {
    const result = await body();
    return result;
  } finally {
    await NfcManager.cancelTechnologyRequest().catch(() => undefined);
  }
}

export async function readTag(): Promise<TagInfo> {
  return withSession(NfcTech.Ndef, 'Hold your iPhone near the tag.', async () => {
    const tag = await NfcManager.getTag();
    if (!tag) throw new Error('No tag data received.');
    return {
      idHex: idToHex(tag.id),
      techTypes: tag.techTypes ?? [],
      tagType: tag.type,
      maxSizeBytes: tag.maxSize,
      records: parseNdefMessage(tag.ndefMessage ?? []),
      rawRecords: tag.ndefMessage ?? [],
    };
  });
}

export async function writeRecords(records: NdefRecord[]): Promise<NfcOpResult> {
  try {
    await withSession(NfcTech.Ndef, 'Hold your iPhone near the tag to write.', async () => {
      const bytes = Ndef.encodeMessage(records);
      await NfcManager.ndefHandler.writeNdefMessage(bytes);
    });
    return { ok: true };
  } catch (e) {
    return { ok: false, message: describeError(e) };
  }
}

/** iOS' NDEF write API rejects a genuinely zero-length message, so "erase" writes a
 * single empty (TNF_EMPTY) record instead -- same trick the Android app uses. */
export async function eraseTag(): Promise<NfcOpResult> {
  const emptyRecord = Ndef.record(Ndef.TNF_EMPTY, '', [], []);
  return writeRecords([emptyRecord]);
}

/** Permanently makes the tag read-only via NFCNDEFTag.writeLock. Not reversible, and not
 * every tag/firmware honors it. */
export async function lockTag(): Promise<NfcOpResult> {
  try {
    await withSession(NfcTech.Ndef, 'Hold your iPhone near the tag to lock it.', async () => {
      await NfcManager.ndefHandler.makeReadOnly();
    });
    return { ok: true };
  } catch (e) {
    return { ok: false, message: describeError(e) };
  }
}

export async function cloneCapture(): Promise<{ records: NdefRecord[] } | { error: string }> {
  try {
    const tag = await withSession(NfcTech.Ndef, 'Hold your iPhone near the SOURCE tag.', async () => {
      return NfcManager.getTag();
    });
    if (!tag?.ndefMessage?.length) return { error: 'Source tag has no NDEF data to clone.' };
    return { records: tag.ndefMessage };
  } catch (e) {
    return { error: describeError(e) };
  }
}

export async function cloneWrite(records: NdefRecord[]): Promise<NfcOpResult> {
  return writeRecords(records);
}

function describeError(e: unknown): string {
  if (e instanceof Error) return e.message;
  if (typeof e === 'string') return e;
  return 'Unknown NFC error';
}
