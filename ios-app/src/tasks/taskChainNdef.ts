import { Ndef, NdefRecord } from 'react-native-nfc-manager';
import { utf8Decode, utf8Encode } from '../nfc/textCodec';
import { TaskChain } from './types';

/** A TaskChain can ride along on a tag as an extra MIME record, so scanning it both shows
 * the tag's normal NDEF content and offers to run the attached automation. */
export const TASKS_MIME_TYPE = 'application/vnd.nfckit.tasks+json';

export function taskChainToRecord(chain: TaskChain): NdefRecord {
  return Ndef.record(Ndef.TNF_MIME_MEDIA, TASKS_MIME_TYPE, [], utf8Encode(JSON.stringify(chain)));
}

export function findTaskChain(records: NdefRecord[]): TaskChain | null {
  const typeToString = (type: number[] | string) => (typeof type === 'string' ? type : Ndef.util.bytesToString(type));
  const record = records.find((r) => r.tnf === Ndef.TNF_MIME_MEDIA && typeToString(r.type) === TASKS_MIME_TYPE);
  if (!record) return null;
  try {
    return JSON.parse(utf8Decode(Uint8Array.from(record.payload)));
  } catch {
    return null;
  }
}
