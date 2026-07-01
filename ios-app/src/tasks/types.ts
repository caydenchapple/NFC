export type TaskCategory = 'COMMUNICATION' | 'WEB_APPS' | 'LOCATION' | 'FEEDBACK' | 'ADVANCED';

/**
 * iOS-feasible automation actions. Deliberately smaller than the Android app's catalog:
 * there's no public API for toggling Wi-Fi/Bluetooth, setting volume/brightness, or
 * scheduling alarms on iOS the way Android's Settings/AudioManager/AlarmClock intents
 * allow. What's here maps to real, working iOS APIs (Linking, Alert, Vibration).
 */
export type TaskActionSpec =
  | { kind: 'openUrl'; url: string }
  | { kind: 'dialNumber'; number: string }
  | { kind: 'composeSms'; number: string; message?: string }
  | { kind: 'composeEmail'; address: string; subject?: string; body?: string }
  | { kind: 'openMaps'; lat: number; lon: number; label?: string }
  | { kind: 'openAppSettings' }
  | { kind: 'showAlert'; title: string; message: string }
  | { kind: 'vibrate' }
  | { kind: 'delay'; ms: number }
  | { kind: 'runProfile'; profileId: string };

export type TaskCondition =
  | { kind: 'always' }
  | { kind: 'timeWindow'; startMinuteOfDay: number; endMinuteOfDay: number }
  | { kind: 'dayOfWeek'; days: number[] }; // 0 = Sunday, matches Date#getDay()

export interface TaskStep {
  action: TaskActionSpec;
  condition: TaskCondition;
}

export interface TaskChain {
  steps: TaskStep[];
}

export type ActionResult = { ok: true } | { ok: false; reason: string };

export type StepResult =
  | { kind: 'ran'; action: TaskActionSpec; outcome: ActionResult }
  | { kind: 'skipped'; action: TaskActionSpec; reason: string };

export function actionLabel(action: TaskActionSpec): string {
  switch (action.kind) {
    case 'openUrl':
      return `Open ${action.url}`;
    case 'dialNumber':
      return `Dial ${action.number}`;
    case 'composeSms':
      return `Text ${action.number}`;
    case 'composeEmail':
      return `Email ${action.address}`;
    case 'openMaps':
      return `Open location in Maps`;
    case 'openAppSettings':
      return `Open app settings`;
    case 'showAlert':
      return `Show alert: ${action.title}`;
    case 'vibrate':
      return `Vibrate`;
    case 'delay':
      return `Wait ${action.ms}ms`;
    case 'runProfile':
      return `Run another profile's tasks`;
  }
}
