# SMS Bridge App

A native Android application that forwards incoming SMS messages to a configured Webhook URL via HTTPS POST.

## Features

- **Reliable Forwarding**: Uses Android WorkManager to ensure delivery even if the app is killed. Retries with exponential backoff.
- **Local Logs**: Stores a history of received messages and their delivery status (SENT, FAILED, RETRYING).
- **Resilient**: Auto-starts on boot to retry pending messages.
- **Secure**: Stores webhook secrets in encrypted storage (DataStore).
- **Battery Efficient**: Targeted for Android 8+ (SDK 26+).

## Setup & Installation

### 1. Build and Install

Run the following command in the project root:

```bash
./gradlew :sms-bridge:installDebug
```

Or build the APK:

```bash
./gradlew :sms-bridge:assembleDebug
```

The APK will be located at: `sms-bridge/build/outputs/apk/debug/sms-bridge-debug.apk`

### 2. Permissions

On the first launch, the app will request:

- **SMS Permissions**: Required to read incoming messages.
- **Battery Optimization**: Detailed instructions are provided in the app to disable battery optimizations for reliable background work.

### 3. Configuration

1. Open the **Settings** tab.
2. Enter your **Webhook URL** (must be `https://`).
3. Enter a **Secret Token** (optional, sent as header `X-SMSBRIDGE-SECRET`).
4. Set a **Device Name** (useful if running multiple bridges).
5. Click **Save**.
6. Go to **Home** tab and click **Enable Forwarding**.

## Webhook Payload

The app sends an HTTP POST request with `Content-Type: application/json`.

**Headers:**

- `X-SMSBRIDGE-SECRET`: `<your-secret-token>`
- `X-SMSBRIDGE-DEVICE`: `<device-id>`

**Body:**

```json
{
  "type": "sms",
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "from": "+1234567890",
  "body": "Hello World",
  "receivedAt": "2025-12-12T10:00:00+00:00",
  "deviceId": "androidId:...",
  "deviceName": "My Gateway Phoenix",
  "simSlot": 0
}
```

## Troubleshooting

- **No SMS Received**: Ensure "Forwarding" is ON in the Home screen. Check if the app has SMS permissions in Android Settings.
- **Messages stuck in PENDING**: Check your internet connection. Ensure Battery Optimizations are DISABLED for this app.
- **Webhook not receiving**: Verify the URL is reachable and supports HTTPS. Check `Logs` tab for error codes (e.g. HTTP 404, 500).
