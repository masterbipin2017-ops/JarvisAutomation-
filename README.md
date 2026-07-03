# Jarvis Automation

Personal hands-free automation app: Bluetooth-triggered voice control (VIP calling
+ media playback), WhatsApp quick-auto-reply, and shake-to-torch.

## Before you build

Edit `app/src/main/java/com/jarvis/automation/VipConfig.java`:
- `TRUSTED_DEVICE_NAME` / `TRUSTED_DEVICE_ADDRESS` — your headset's Bluetooth name/MAC
- `VIP_CONTACTS` — voice phrase → phone number
- `PLAYLISTS` — voice phrase → Spotify URI
- `AUTO_REPLY_TEXT` — the WhatsApp auto-reply message

## Building via GitHub Actions

Push this repo to GitHub — `.github/workflows/build.yml` runs `gradle assembleDebug`
and uploads `app-debug.apk` as a workflow artifact (Actions tab → run → Artifacts).

No Gradle wrapper jar is committed (binary files can't be generated in this
environment); the workflow provisions Gradle itself via `gradle/actions/setup-gradle`.
If you'd rather build locally with `./gradlew`, run `gradle wrapper` once inside the
project to generate the wrapper files, then commit them.

## After installing — manual steps the OS requires

None of these can be automated by the app itself; Android requires explicit,
per-permission user action for anything this sensitive:

1. **Runtime permissions** — tap "Grant Runtime Permissions" in the app
   (microphone, phone, camera, Bluetooth, notifications).
2. **Notification access** — tap "Enable Notification Access", then enable
   "Jarvis Auto-Reply" in the system list. Required for the WhatsApp auto-reply
   feature; this is the same permission screen any notification-reading app
   (e.g. Pushbullet, Wear OS) sends you to.
3. **Battery optimization** — on some OEM skins (Xiaomi, Samsung, OnePlus)
   you'll also want to disable battery optimization for the app in
   Settings > Apps > Jarvis Automation > Battery, or the OS may still kill it
   under memory pressure. This is a per-OEM control, not something the app
   can override — Android does not let apps grant themselves immunity from
   the system's memory manager.
4. Tap **Start Jarvis Service**.

## What's intentionally NOT included

OTP/SMS auto-copy was dropped. Reading arbitrary SMS/notification content for
one-time-passcodes and pushing it to the clipboard is the same technique used
by banking-trojan malware, can't be safely scoped to "only my bank," and gets
apps removed from Play Protect. If you need OTP autofill for your *own* app,
use Android's [SMS Retriever API](https://developers.google.com/identity/sms-retriever/overview) —
it's scoped by design and needs no SMS/notification permission at all.

## Permission summary (why each one is requested)

| Permission | Used for |
|---|---|
| RECORD_AUDIO | On-device speech recognition for voice commands |
| CALL_PHONE | Placing VIP calls |
| CAMERA | Torch toggle (flash unit only, no image capture) |
| BLUETOOTH_CONNECT / SCAN | Reading headset name to filter the trigger |
| POST_NOTIFICATIONS | The persistent foreground-service notification |
| BIND_NOTIFICATION_LISTENER_SERVICE | WhatsApp quick-reply (user-granted separately) |
