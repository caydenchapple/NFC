import { TaskActionSpec, TaskCategory } from './types';

export interface TaskTemplate {
  title: string;
  category: TaskCategory;
  description: string;
  default: TaskActionSpec;
}

export const TASK_TEMPLATES: TaskTemplate[] = [
  { title: 'Dial a number', category: 'COMMUNICATION', description: 'Opens the Phone app pre-filled.', default: { kind: 'dialNumber', number: '' } },
  { title: 'Compose text message', category: 'COMMUNICATION', description: 'Opens Messages pre-filled; sending needs manual confirmation.', default: { kind: 'composeSms', number: '' } },
  { title: 'Compose email', category: 'COMMUNICATION', description: 'Opens Mail pre-filled.', default: { kind: 'composeEmail', address: '' } },

  { title: 'Open website', category: 'WEB_APPS', description: 'Opens a URL, an app URL scheme, or a universal link.', default: { kind: 'openUrl', url: 'https://' } },
  { title: 'Open app settings', category: 'WEB_APPS', description: "Jumps to this app's page in Settings.", default: { kind: 'openAppSettings' } },

  { title: 'Open location in Maps', category: 'LOCATION', description: 'Opens coordinates in Apple/Google Maps.', default: { kind: 'openMaps', lat: 0, lon: 0 } },

  { title: 'Show alert', category: 'FEEDBACK', description: 'An in-app pop-up confirming the scan.', default: { kind: 'showAlert', title: 'NFC Kit', message: 'Tag scanned' } },
  { title: 'Vibrate', category: 'FEEDBACK', description: 'A haptic buzz.', default: { kind: 'vibrate' } },

  { title: 'Wait', category: 'ADVANCED', description: 'Pause before the next step.', default: { kind: 'delay', ms: 1000 } },
  { title: "Run another profile's tasks", category: 'ADVANCED', description: 'Chains a saved profile.', default: { kind: 'runProfile', profileId: '' } },
];

export function templatesByCategory(): Array<[TaskCategory, TaskTemplate[]]> {
  const map = new Map<TaskCategory, TaskTemplate[]>();
  for (const template of TASK_TEMPLATES) {
    const list = map.get(template.category) ?? [];
    list.push(template);
    map.set(template.category, list);
  }
  return Array.from(map.entries());
}
