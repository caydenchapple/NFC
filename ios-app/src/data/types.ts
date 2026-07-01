import { RecordSpec } from '../nfc/recordSpec';
import { TaskChain } from '../tasks/types';

export interface Profile {
  id: string;
  name: string;
  createdAt: number;
  records: RecordSpec[];
  taskChain?: TaskChain;
}

export type HistoryAction = 'READ' | 'WRITE' | 'CLONE' | 'ERASE' | 'LOCK' | 'TASK_RUN';

export interface HistoryEntry {
  id: string;
  timestamp: number;
  action: HistoryAction;
  summary: string;
  tagId?: string;
}
