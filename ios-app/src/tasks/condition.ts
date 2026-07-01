import { TaskCondition } from './types';

export function evaluateCondition(condition: TaskCondition, now: Date = new Date()): boolean {
  switch (condition.kind) {
    case 'always':
      return true;
    case 'timeWindow': {
      const minuteOfDay = now.getHours() * 60 + now.getMinutes();
      const { startMinuteOfDay, endMinuteOfDay } = condition;
      return startMinuteOfDay <= endMinuteOfDay
        ? minuteOfDay >= startMinuteOfDay && minuteOfDay <= endMinuteOfDay
        : minuteOfDay >= startMinuteOfDay || minuteOfDay <= endMinuteOfDay;
    }
    case 'dayOfWeek':
      return condition.days.includes(now.getDay());
  }
}
