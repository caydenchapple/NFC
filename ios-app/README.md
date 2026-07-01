# NFC Kit — iOS

A React Native + TypeScript rebuild of the [NFC Kit](../README.md) Android app, scoped to
what Core NFC actually allows on iOS. This lives alongside the Kotlin/Compose Android app
in `../` — nothing there was touched.

## Why a separate iOS build instead of "just" cross-platform

Android's NFC APIs (`android.nfc`) and Apple's Core NFC are different enough in capability
that a single feature set can't honestly serve both. Rather than pretend otherwise, this
app's Tasks catalog, tag-maintenance actions, and even NDEF operations were re-scoped to
what's real on iOS. See "What's different from the Android app" below before assuming
parity.

## Requirements

- **A Mac with Xcode** (this project was written and typechecked in a Linux container with
  no Xcode/macOS available — it has never been built or run. See "What's been verified"
  below for exactly what that means.)
- Node.js ≥ 22.11 (matches `engines` in `package.json`)
- CocoaPods (`sudo gem install cocoapods` if you don't have it)
- A physical iPhone 7 or later for anything NFC-related — **the iOS Simulator has no NFC
  radio at all**, full stop. You'll see the whole UI in the simulator; scanning will just
  never do anything there.
- A paid Apple Developer Program membership ($99/yr) — the NFC entitlement requires a real
  provisioning profile, free personal-team signing won't get you tag scanning on-device.

## First-time setup

```bash
cd ios-app
npm install
cd ios && pod install && cd ..
open ios/NFCKit.xcworkspace
```

Then in Xcode:

1. Select the `NFCKit` target → **Signing & Capabilities**.
2. Set your Team under Signing.
3. Click **+ Capability** → **Near Field Communication Tag Reading**. This creates/links
   an entitlements file automatically. This repo also ships
   `ios/NFCKit/NFCKit.entitlements` with the right content
   (`com.apple.developer.nfc.readersession.formats = [NDEF, TAG]`) as a reference — if
   Xcode generates its own file with a different name, either point
   `CODE_SIGN_ENTITLEMENTS` at the shipped one or merge the array into whichever file wins.
4. Build & run on a real device (⌘R). Running on "Any iOS Simulator Device" will build and
   launch fine; NFC calls will just fail silently/no-op since there's no radio.

`Info.plist` already has `NFCReaderUsageDescription` set — that's the one piece that's
mandatory for *any* Core NFC usage and doesn't need an Xcode UI step.

## What's been verified (and what hasn't)

This was built in a sandboxed Linux dev environment with **no Xcode, no macOS, no
simulator, and no physical iPhone**. What that means concretely:

- ✅ `npx tsc --noEmit` passes clean across the whole `src/` tree and `App.tsx`.
- ✅ `npx eslint src App.tsx` passes with 0 errors (54 warnings, all either idiomatic
  React Navigation patterns — inline `tabBarIcon` functions, which is how every React
  Navigation example does it — or expected `no-bitwise` hits inside the byte-level NDEF
  codec, where bit-shifting is the entire point of the code).
- ✅ `npx jest` passes — `__tests__/App.test.tsx` renders the full component tree
  (navigation, all six screens) against a manual mock of `react-native-nfc-manager`
  (`__mocks__/react-native-nfc-manager.js`), since the real native module can't load
  outside a compiled app.
- ✅ The `react-native-nfc-manager` API calls (`requestTechnology`, `getTag`,
  `ndefHandler.writeNdefMessage`, `ndefHandler.makeReadOnly`, the `Ndef` helper object)
  were written against that package's actual shipped `index.d.ts` (v3.17.2), not
  recalled/guessed from memory.
- ❌ **Nothing has been run in Xcode, the simulator, or a device.** No `pod install`, no
  actual compile of the native iOS project, no real NFC session has ever been opened. The
  `ios/` folder is the real output of the React Native community CLI's `init` template
  (a genuine `.xcodeproj`/`.xcworkspace`, `AppDelegate.swift`, `Podfile`) — it hasn't been
  hand-edited except for `Info.plist` (added `NFCReaderUsageDescription`) and the new
  `NFCKit.entitlements` file.

Treat this as a strong first pass, not a "verified working" build. The very first thing to
do is open it in Xcode, get it compiling and signed, and run it on a real device.

## What's different from the Android app

Core NFC's public API is meaningfully smaller than `android.nfc`, and iOS has no
equivalent to Android's Settings intents, `AlarmManager`, `WifiManager`, etc. Concretely:

| Feature | Android | iOS |
|---|---|---|
| Tag emulation (HCE) | ✅ `HostApduService` | ❌ Not implemented — Apple doesn't expose tag emulation to third-party apps at all. Omitted entirely, not stubbed. |
| Passive/background scanning | ✅ `enableReaderMode` while app is foregrounded | ❌ Every scan is an explicit `NFCTagReaderSession` the user triggers, which shows an Apple system sheet you can't fully suppress. |
| Format a blank tag | ✅ `NdefFormatable` | ❌ No public formatting API. Not implemented (most tags ship pre-formatted anyway). |
| Erase a tag | ✅ write empty message | ✅ same trick, works — but iOS rejects a truly zero-length NDEF message, so it writes a single `TNF_EMPTY` record instead. |
| Lock a tag read-only | ✅ `Ndef.makeReadOnly()` | ✅ `NFCNDEFTag.writeLock` via the library's `ndefHandler.makeReadOnly()` — untested on real hardware here, tag support varies. |
| MIFARE Classic raw sector access | ✅ | ❌ Not exposed by Core NFC's public API (crypto1 export restrictions). |
| Tasks: WiFi/Bluetooth toggle, volume, brightness, alarms | ✅ via Settings intents / `AudioManager` / `AlarmClock` | ❌ No public iOS equivalents. Not attempted. |
| Tasks: open URL, dial, SMS, email, maps, alert, vibrate, delay, run-nested-profile | ✅ | ✅ via `Linking`, `Alert`, `Vibration` (all core React Native, no extra native deps) |
| "Open app" record/action | Android application record (`android.com:pkg`) | Re-modeled as a plain URL/URI record — point it at a custom URL scheme or universal link instead of a package name, since iOS has no package-name launch mechanism. |
| Profiles / history storage | Room (SQLite) | `@react-native-async-storage/async-storage`, JSON-encoded. Functionally equivalent for this scale of data; swap for SQLite later if you need querying. |
| Export/import | System file picker (`ACTION_CREATE_DOCUMENT`) | `Share.share()` for export (native share sheet — Files, AirDrop, Mail, etc. all work as a save target) and a paste-into-a-textbox modal for import, to avoid pulling in a document-picker dependency for a v1. |

## Project layout

```
src/
  nfc/          NfcService (session wrapper around react-native-nfc-manager), record
                builders/parser, textCodec (dependency-free UTF-8/UTF-16 codec --
                Hermes has no Buffer, so this is hand-rolled instead of polyfilled)
  tasks/        TaskActionSpec catalog, condition evaluation, executor (Linking/Alert/
                Vibration), taskChainNdef (attach a task chain to a tag as a custom
                MIME record, mirroring the Android app's tag-triggered automation)
  data/         AsyncStorage-backed profile/history repository + JSON backup format
  screens/      Read, Write, Tasks, More, Profiles, History
  navigation/   Bottom tabs (Read/Write/Tasks/More) + a native-stack nested under More
                for Profiles/History
  components/   Shared Card/Button/EmptyState/etc. primitives
  theme/        Color palette
```

Unlike the Android app's always-on `NfcController` (reader mode runs continuously while
the app is foregrounded), every iOS screen opens its own short-lived `NFCTagReaderSession`
on a button press via `NfcService`, because that's the only model Core NFC supports.

## Known follow-ups

- `Alert.prompt` (used for naming a profile) is iOS-only in React Native, which is fine
  here since this is an iOS-only build — just don't copy that call if this ever gets
  merged into a cross-platform codebase without a platform check.
- Custom URL schemes used in "open app" records/tasks need to be added to
  `LSApplicationQueriesSchemes` in `Info.plist` for `Linking.canOpenURL` to reliably see
  them (standard `https`/`tel`/`mailto`/`sms` schemes don't need this).
- No conditions beyond time-window/day-of-week (Android also has Wi-Fi/Bluetooth/battery
  conditions, which need extra native deps like `@react-native-community/netinfo` here —
  left out to keep the dependency surface small for a first iOS pass).
