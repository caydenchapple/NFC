import AsyncStorage from '@react-native-async-storage/async-storage';
import { HistoryAction, HistoryEntry, Profile } from './types';

const PROFILES_KEY = 'nfckit.profiles.v1';
const HISTORY_KEY = 'nfckit.history.v1';
const MAX_HISTORY = 500;

function newId(): string {
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

export async function getProfiles(): Promise<Profile[]> {
  const raw = await AsyncStorage.getItem(PROFILES_KEY);
  if (!raw) return [];
  try {
    const profiles: Profile[] = JSON.parse(raw);
    return profiles.sort((a, b) => b.createdAt - a.createdAt);
  } catch {
    return [];
  }
}

export async function saveProfile(input: Omit<Profile, 'id' | 'createdAt'>): Promise<Profile> {
  const profiles = await getProfiles();
  const profile: Profile = { ...input, id: newId(), createdAt: Date.now() };
  await AsyncStorage.setItem(PROFILES_KEY, JSON.stringify([profile, ...profiles]));
  return profile;
}

export async function deleteProfile(id: string): Promise<void> {
  const profiles = await getProfiles();
  await AsyncStorage.setItem(PROFILES_KEY, JSON.stringify(profiles.filter((p) => p.id !== id)));
}

export async function getProfileById(id: string): Promise<Profile | undefined> {
  const profiles = await getProfiles();
  return profiles.find((p) => p.id === id);
}

export async function replaceAllProfiles(profiles: Profile[]): Promise<void> {
  const existing = await getProfiles();
  const byId = new Map(existing.map((p) => [p.id, p]));
  for (const incoming of profiles) {
    byId.set(incoming.id || newId(), { ...incoming, id: incoming.id || newId() });
  }
  await AsyncStorage.setItem(PROFILES_KEY, JSON.stringify(Array.from(byId.values())));
}

export async function getHistory(): Promise<HistoryEntry[]> {
  const raw = await AsyncStorage.getItem(HISTORY_KEY);
  if (!raw) return [];
  try {
    return JSON.parse(raw);
  } catch {
    return [];
  }
}

export async function addHistoryEntry(action: HistoryAction, summary: string, tagId?: string): Promise<void> {
  const history = await getHistory();
  const entry: HistoryEntry = { id: newId(), timestamp: Date.now(), action, summary, tagId };
  const trimmed = [entry, ...history].slice(0, MAX_HISTORY);
  await AsyncStorage.setItem(HISTORY_KEY, JSON.stringify(trimmed));
}

export async function clearHistory(): Promise<void> {
  await AsyncStorage.removeItem(HISTORY_KEY);
}

export interface ProfileBackup {
  formatVersion: 1;
  exportedAt: number;
  profiles: Profile[];
}

export async function exportProfilesJson(): Promise<string> {
  const profiles = await getProfiles();
  const backup: ProfileBackup = { formatVersion: 1, exportedAt: Date.now(), profiles };
  return JSON.stringify(backup, null, 2);
}

export async function importProfilesJson(json: string): Promise<number> {
  const parsed: ProfileBackup = JSON.parse(json);
  if (!parsed || !Array.isArray(parsed.profiles)) {
    throw new Error('Not a valid NFC Kit profile backup.');
  }
  await replaceAllProfiles(parsed.profiles);
  return parsed.profiles.length;
}
