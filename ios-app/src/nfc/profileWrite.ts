import { NdefRecord } from 'react-native-nfc-manager';
import { Profile } from '../data/types';
import { taskChainToRecord } from '../tasks/taskChainNdef';
import { writeRecords } from './NfcService';
import { toNdefRecord } from './recordSpec';
import { NfcOpResult } from './types';

/** True for a profile saved from the Read tab's "Save scanned tag" action --
 * an exact byte-for-byte capture rather than a hand-built record set. */
export function isScannedCopy(profile: Profile): boolean {
  return (profile.rawRecords?.length ?? 0) > 0;
}

/** Builds the NDEF records a profile should write. Scanned tag copies replay
 * their captured bytes verbatim; authored profiles rebuild from `records`
 * (plus an attached task chain, if any -- scanned copies keep whatever
 * chain record was already baked into their raw bytes, so it isn't
 * duplicated here). */
export function buildProfileRecords(profile: Profile): NdefRecord[] {
  if (isScannedCopy(profile)) return profile.rawRecords!;
  const records = profile.records.map(toNdefRecord);
  if (profile.taskChain) records.push(taskChainToRecord(profile.taskChain));
  return records;
}

export async function writeProfileToTag(profile: Profile): Promise<NfcOpResult> {
  const records = buildProfileRecords(profile);
  if (records.length === 0) {
    return { ok: false, message: 'This saved tag has nothing to write.' };
  }
  return writeRecords(records);
}
