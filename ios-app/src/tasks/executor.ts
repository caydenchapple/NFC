import { Alert, Linking, Vibration } from 'react-native';
import { evaluateCondition } from './condition';
import { ActionResult, StepResult, TaskActionSpec, TaskChain } from './types';

export type ProfileChainResolver = (profileId: string) => Promise<TaskChain | null>;

export async function runChain(
  chain: TaskChain,
  resolveProfileChain: ProfileChainResolver = async () => null,
  depth = 0,
): Promise<StepResult[]> {
  if (depth > 5) {
    return [{ kind: 'skipped', action: { kind: 'delay', ms: 0 }, reason: 'Profile chain nesting too deep' }];
  }

  const results: StepResult[] = [];
  for (const step of chain.steps) {
    if (!evaluateCondition(step.condition)) {
      results.push({ kind: 'skipped', action: step.action, reason: 'Condition not met' });
      continue;
    }
    if (step.action.kind === 'runProfile') {
      const nested = await resolveProfileChain(step.action.profileId);
      if (!nested) {
        results.push({ kind: 'ran', action: step.action, outcome: { ok: false, reason: 'Profile has no task chain' } });
      } else {
        await runChain(nested, resolveProfileChain, depth + 1);
        results.push({ kind: 'ran', action: step.action, outcome: { ok: true } });
      }
      continue;
    }
    results.push({ kind: 'ran', action: step.action, outcome: await executeAction(step.action) });
  }
  return results;
}

async function executeAction(action: TaskActionSpec): Promise<ActionResult> {
  try {
    switch (action.kind) {
      case 'openUrl':
        return openUrl(normalizeUrl(action.url));
      case 'dialNumber':
        return openUrl(`telprompt:${action.number}`, `tel:${action.number}`);
      case 'composeSms': {
        const body = action.message ? `&body=${encodeURIComponent(action.message)}` : '';
        return openUrl(`sms:${action.number}${body ? `${body}` : ''}`);
      }
      case 'composeEmail': {
        const params = [
          action.subject ? `subject=${encodeURIComponent(action.subject)}` : null,
          action.body ? `body=${encodeURIComponent(action.body)}` : null,
        ].filter(Boolean);
        const query = params.length ? `?${params.join('&')}` : '';
        return openUrl(`mailto:${action.address}${query}`);
      }
      case 'openMaps': {
        const label = action.label ? `&q=${encodeURIComponent(action.label)}` : '';
        return openUrl(`https://maps.apple.com/?ll=${action.lat},${action.lon}${label}`);
      }
      case 'openAppSettings':
        await Linking.openSettings();
        return { ok: true };
      case 'showAlert':
        Alert.alert(action.title, action.message);
        return { ok: true };
      case 'vibrate':
        Vibration.vibrate();
        return { ok: true };
      case 'delay':
        await new Promise<void>((resolve) => setTimeout(resolve, action.ms));
        return { ok: true };
      case 'runProfile':
        return { ok: false, reason: 'Handled by runChain()' }; // unreachable
    }
  } catch (e) {
    return { ok: false, reason: e instanceof Error ? e.message : 'Unknown error' };
  }
}

function normalizeUrl(url: string): string {
  return /^[a-z][a-z0-9+.-]*:/i.test(url) ? url : `https://${url}`;
}

async function openUrl(primary: string, fallback?: string): Promise<ActionResult> {
  const canOpenPrimary = await Linking.canOpenURL(primary).catch(() => false);
  if (canOpenPrimary) {
    await Linking.openURL(primary);
    return { ok: true };
  }
  if (fallback) {
    const canOpenFallback = await Linking.canOpenURL(fallback).catch(() => false);
    if (canOpenFallback) {
      await Linking.openURL(fallback);
      return { ok: true };
    }
  }
  return { ok: false, reason: `No app can handle "${primary}"` };
}
