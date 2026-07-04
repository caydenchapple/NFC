# NFC Kit

A free, open, ad-free app — **for both Android and iOS** — that covers the
everyday NFC workflows people reach for "NFC Tools"-style apps for: reading
tags, writing standardized NDEF records, cloning tags, formatting/erasing/
locking, and per-tag task automation.

| Platform | Where | Stack |
|---|---|---|
| **Android** | this repo (`app/`) | Kotlin + Jetpack Compose, minSdk 24 |
| **iOS** | [`ios-app/`](ios-app/README.md) | React Native + TypeScript, Core NFC (open `ios-app/ios/NFCKit.xcworkspace` in Xcode) |

These are two separate, native codebases rather than one shared cross-platform
app, because the two platforms' NFC APIs don't offer the same capabilities.
Notably, **HCE tag emulation, background/passive scanning, MIFARE Classic raw
sector access, and Wi-Fi/Bluetooth/volume/alarm task actions are Android-only**
— Apple's Core NFC doesn't expose equivalents, so the iOS app re-scopes its
Tasks catalog and tag-maintenance actions to what's actually possible there.
See the [iOS README](ios-app/README.md#whats-different-from-the-android-app)
for the full platform-by-platform feature diff.

The rest of this document covers the **Android app**. For iOS-specific setup,
requirements (a paid Apple Developer account is needed for on-device NFC
testing — the Simulator has no NFC radio), and caveats, see
[`ios-app/README.md`](ios-app/README.md).

## Scope decisions (read this first)

The original brief asked for a literal clone built from an APKMirror download
and APKTool decompile, pixel-matched to a specific commercial app. This project
deliberately does **not** do that, for two reasons:

1. **IP risk.** Decompiling a commercial app to copy its exact UI, icon set,
   and branding is a copyright/trademark exposure that isn't worth taking on.
   What *is* fair game — and what this app does — is reimplementing the
   underlying, unprotectable *functionality* (NDEF read/write, tag formatting,
   automation, HCE) from the public Android NFC APIs and NFC Forum specs.
2. **No test hardware in this environment.** This was built in a sandboxed
   dev environment with no Android device, no NFC hardware, and no Android
   SDK installed (only a JVM + Gradle). Every module here is written to the
   documented Android NFC / HCE APIs and cross-checked for internal
   consistency, but it has **not been run against real NTAG213/215/216,
   MIFARE Classic, or MIFARE Ultralight tags yet**. Treat this as a complete,
   architecturally-sound first build that needs a real-device pass before
   shipping — see "What to test first" below.

Everywhere the OS itself restricts what a third-party app can do (Wi-Fi/
Bluetooth toggling since Android 10/12, brightness needing a granted
permission, exact alarms, Do Not Disturb access, etc.), the code takes the
best available real action — usually opening the relevant system panel — and
surfaces a clear failure reason instead of silently pretending to succeed.
See `TaskExecutor.kt` for the specifics per action.

## Feature coverage

| Area | What's implemented |
|---|---|
| **Read** | Manufacturer + exact chip model (NXP NTAG213/215/216 via the `GET_VERSION` command, MIFARE Classic 1K/4K/Mini, Ultralight/Ultralight C), UID, tech class list, NDEF records (text, URI, vCard, Wi-Fi config, app-launch, MIME, external, unknown), raw hex dump, capacity used/available. |
| **Write** | Plain text, URL, phone call, SMS, email, geo location, app launch, social profile (Facebook/Instagram/X/LinkedIn/TikTok/YouTube/GitHub/WhatsApp), Wi-Fi config (WPA2/WPA/open), vCard contact, video link. Multiple records per write. |
| **Tasks / automation** | ~30 concrete actions across Connectivity, Media & Sound, Device, Apps & Web, Communication, Productivity, plus generic escape hatches (custom intent, broadcast, delay, run-another-profile) for anything not built in. Conditional gating (time window, day of week, Wi-Fi/Bluetooth state, battery level). Chains can be run immediately, saved as a profile, or attached to a tag so scanning it re-runs the chain. |
| **Copy / clone** | Two-tap flow: capture a source tag's NDEF message, then write it to a target tag. |
| **Format / erase / lock** | Format blank/foreign tags to NDEF, erase content, and permanently lock a tag read-only (`Ndef.makeReadOnly()` — irreversible, gated behind a confirmation dialog). |
| **Profiles** | Save a record set and/or task chain, reuse it, write it to any tag, delete it. |
| **Import / export** | Profiles round-trip through a JSON file via the system file picker (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`). |
| **History** | Rolling log (last 500) of reads, writes, clones, formats, erases, locks, and task runs, backed by Room. |
| **Tag emulation (HCE)** | `HostApduService` implementing the NFC Forum Type 4 Tag app (SELECT AID/CC-file/NDEF-file + READ BINARY) so the phone can be tapped like a static NDEF tag. Read-only by design (UPDATE BINARY isn't implemented). |
| **Dark mode / Material You** | Full Material 3 theming, dynamic color on Android 12+, follows system dark mode. |

## Project layout

```
app/src/main/java/com/zephyrcloud/nfckit/
  nfc/          Tag reading, chip identification, NDEF record build/parse, write/format/lock, clone, NfcController (mode + event bus)
  tasks/        TaskActionSpec (action catalog), TaskCondition, TaskChain, TaskExecutor, tag-attached automation
  hce/          HostApduService (Type 4 Tag emulation) + persisted emulation payload
  data/         Room database (profiles, history), repositories, JSON import/export, RecordSpec (serializable NDEF record descriptions)
  ui/           Compose screens + ViewModels per tab (read, write, tasks, more, profiles, history), navigation, theme
  MainActivity  Owns the NFC reader-mode loop; NfcKitApp wires up the DB/repositories/controller singletons
```

The read/write/clone/format flows are mediated by a single `NfcController`:
screens set a `ScanMode` (Read, Write(message), CloneCapture, CloneWrite,
Format, Erase, Lock) before asking the user to tap a tag, `MainActivity`
forwards every discovered `Tag` to the controller, and it dispatches based on
the current mode and emits a typed `NfcEvent` that the active screen's
ViewModel collects. This keeps NFC hardware access in one place instead of
scattered across screens.

## Building

Requires Android Studio (Koala/2024.1+ recommended) with SDK Platform 34 and
build-tools installed. There's no committed Gradle wrapper jar in this repo —
open the project in Android Studio and let it offer to set one up, or run:

```
gradle wrapper --gradle-version 8.7
./gradlew assembleDebug
```

No API keys or backend config needed; everything is on-device.

## What to test first on real hardware

This was written against the documented Android NFC/HCE APIs but never run on
a device, since the build environment had no Android SDK or NFC hardware.
Before treating this as done:

1. **Chip identification** — verify the `GET_VERSION` (0x60) NfcA transceive
   in `ChipIdentifier.kt` actually returns 8 bytes and the storage-size byte
   mapping is right, on real NTAG213/215/216 and a plain MIFARE Ultralight.
   Some tags/readers reject raw `transceive` calls if reader mode flags don't
   include `FLAG_READER_SKIP_NDEF_CHECK` (already set) — confirm.
2. **Write capacity checks** — confirm `Ndef.maxSize` and the "message too
   large" error path behave as expected on a small tag (NTAG213 = 144 bytes
   usable).
3. **Wi-Fi record parsing/writing** — the WSC TLV encode/decode in
   `NdefRecordFactory.wifiConfig` / `NdefRecordParser.parseWifi` is
   hand-rolled from the Wi-Fi Simple Config spec; verify a tag written by
   this app actually triggers Android's native "connect to this Wi-Fi
   network?" prompt when scanned.
4. **HCE emulation** — test phone-to-phone (this app's Read tab reading the
   emulated tag) and phone-to-reader. Requires enabling the HCE service
   toggle in More, having previously scanned a source tag or written one to
   populate the emulated payload, and the phone's screen unlocked (`android:
   requireDeviceUnlock="false"` is set, but many devices still require the
   screen on for HCE regardless).
5. **Lock (`makeReadOnly`)** — test on a cheap/disposable tag first; this is
   irreversible and not all tags support it (`canMakeReadOnly()` gates it,
   but real-world tag firmware varies).
6. **Runtime permissions** — Bluetooth toggle needs `BLUETOOTH_CONNECT`
   granted at runtime on Android 12+; notifications need `POST_NOTIFICATIONS`
   granted at runtime on Android 13+; brightness needs the user to grant
   "modify system settings" via the panel this app opens. None of these
   request flows are wired into the UI yet (only the manifest permissions and
   the executor's runtime checks) — add permission-request prompts before
   shipping.

## Known gaps vs. a full commercial parity target

- Tasks/automation is an extensible ~30-action catalog with generic
  intent/broadcast escape hatches, not a literal 200-entry switch statement.
  Adding a new concrete action is a new `TaskActionSpec` subtype + one branch
  in `TaskExecutor.executeSingle` + one `TaskTemplate` entry.
- No MIFARE Classic sector-level raw read/write (key-based auth, block dump) —
  only NDEF-level operations. Raw MIFARE Classic access is a reasonable
  follow-up (`android.nfc.tech.MifareClassic` is already used for chip ID).
- No home-screen shortcuts for saved profiles/tasks yet (`ShortcutManager`
  integration would slot into the Profiles screen).
- UI is a clean-room Material 3 design, not a pixel match to any existing
  app.
