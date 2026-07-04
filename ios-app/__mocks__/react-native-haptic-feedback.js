/**
 * Manual Jest mock for react-native-haptic-feedback. It resolves a native
 * TurboModule at import time, which doesn't exist in the Jest environment.
 * This stub reproduces just the surface our code calls.
 */

const trigger = jest.fn();

module.exports = {
  trigger,
  stop: jest.fn(),
  isSupported: jest.fn(() => Promise.resolve(true)),
  triggerPattern: jest.fn(),
  getSystemHapticStatus: jest.fn(() => Promise.resolve(true)),
  setEnabled: jest.fn(),
  isEnabled: jest.fn(() => true),
  impact: jest.fn(),
  playAHAP: jest.fn(),
  default: { trigger, stop: jest.fn(), isSupported: jest.fn(() => Promise.resolve(true)) },
};
