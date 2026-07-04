import { NdefRecord } from 'react-native-nfc-manager';
import { RecordSpec } from '../nfc/recordSpec';
import { TaskChain } from '../tasks/types';

export interface Profile {
  id: string;
  name: string;
  createdAt: number;
  records: RecordSpec[];
  taskChain?: TaskChain;
  /** Raw NDEF records captured verbatim from a scanned tag via "Save scanned tag" on
   * the Read tab. When present, writing this profile replays these bytes directly
   * instead of rebuilding them from `records`, so byte-for-byte tag copies (including
   * record types the Write tab doesn't have a builder for) round-trip correctly. */
  rawRecords?: NdefRecord[];
  /** The ID of the physical tag this was scanned from, shown for context. */
  sourceTagId?: string;
}

export type HistoryAction = 'READ' | 'WRITE' | 'CLONE' | 'ERASE' | 'LOCK' | 'TASK_RUN' | 'SAVE';

export interface HistoryEntry {
  id: string;
  timestamp: number;
  action: HistoryAction;
  summary: string;
  tagId?: string;
}
