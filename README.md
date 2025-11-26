# MomoTerminal

Native Android App (Kotlin) - Mobile Money POS with NFC HCE and SMS Relay

## Overview

MomoTerminal is a comprehensive Mobile Money Point-of-Sale (POS) system designed as a native Android application. It acts as a hardware bridge with two core functions:

### 1. NFC Terminal (Host Card Emulation)
Using Host Card Emulation (HCE), the app turns your phone into an NFC tag. Merchants enter a payment amount, and customers simply tap their NFC-enabled phone to automatically launch a pre-filled USSD payment dialer. **No customer app is required** – just a seamless tap-to-pay experience.

### 2. SMS Relay
The app intercepts incoming payment SMS notifications from mobile money providers and pushes them to your PWA database for real-time financial tracking. This enables merchants to have instant visibility into their transactions.

## Features

- **NFC Terminal Mode**: Turn any Android phone into a payment terminal
- **USSD Generation**: Automatically creates pre-filled USSD dial strings for major providers:
  - MTN Mobile Money (Ghana)
  - Vodafone Cash (Ghana)
  - AirtelTigo Money (Ghana)
- **SMS Interception**: Captures payment confirmation SMS in real-time
- **PWA Sync**: Pushes transaction data to your web-based dashboard
- **Merchant Configuration**: Save merchant codes and API endpoints
- **Offline Support**: Local transaction storage with sync when online

## Requirements

- Android 5.0 (API 21) or higher
- NFC-enabled device with HCE support
- SMS permissions for relay functionality
- Internet connection for PWA sync

## Permissions

The app requires the following permissions:

| Permission | Purpose |
|------------|---------|
| `NFC` | Enable NFC communication and HCE |
| `RECEIVE_SMS` | Intercept incoming payment SMS |
| `READ_SMS` | Parse SMS content for transaction details |
| `CALL_PHONE` | Launch USSD dialer for payments |
| `INTERNET` | Sync transactions to PWA backend |

## Project Structure

```
app/
├── src/main/
│   ├── java/com/momoterminal/
│   │   ├── MomoTerminalApp.kt       # Application class
│   │   ├── api/
│   │   │   ├── ApiClient.kt         # Retrofit client
│   │   │   ├── MomoApiService.kt    # API interface
│   │   │   ├── Models.kt            # Data models
│   │   │   └── SyncService.kt       # Background sync service
│   │   ├── nfc/
│   │   │   └── MomoHceService.kt    # NFC HCE service
│   │   ├── sms/
│   │   │   └── SmsReceiver.kt       # SMS broadcast receiver
│   │   ├── ui/
│   │   │   └── MainActivity.kt      # Main UI
│   │   └── ussd/
│   │       └── UssdHelper.kt        # USSD code generation
│   └── res/
│       ├── layout/
│       │   └── activity_main.xml    # Main layout
│       ├── xml/
│       │   └── hce_service.xml      # HCE AID configuration
│       └── values/
│           ├── strings.xml
│           ├── colors.xml
│           └── themes.xml
└── src/test/
    └── java/com/momoterminal/
        ├── UssdHelperTest.kt
        ├── NfcPaymentDataTest.kt
        └── PaymentTransactionTest.kt
```

## How It Works

### NFC Payment Flow

1. Merchant enters the payment amount and their merchant code
2. Merchant taps "Activate NFC Terminal"
3. The app generates a USSD dial string (e.g., `*170*1*1*MERCHANT*50.00#`)
4. Customer taps their NFC-enabled phone
5. Customer's phone receives the USSD code and opens the dialer
6. Customer confirms and completes the payment via USSD
7. Merchant receives SMS confirmation
8. Transaction is synced to PWA dashboard

### SMS Relay Flow

1. App registers as SMS receiver
2. When payment SMS arrives, app parses the content
3. Extracts: amount, sender, transaction ID
4. Broadcasts locally for UI update
5. Pushes to PWA backend via API
6. Dashboard reflects real-time transaction data

## Building the Project

```bash
# Clone the repository
git clone https://github.com/ikanisa/MomoTerminal.git
cd MomoTerminal

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Build release APK
./gradlew assembleRelease
```

## Configuration

### PWA Backend Integration

Configure your API endpoint in the app settings or programmatically:

```kotlin
val prefs = getSharedPreferences("momo_terminal_prefs", MODE_PRIVATE)
prefs.edit()
    .putString("api_endpoint", "https://your-pwa-backend.com/")
    .putString("merchant_code", "YOUR_MERCHANT_CODE")
    .apply()
```

### Supported Mobile Money Providers

| Provider | USSD Format |
|----------|-------------|
| MTN MoMo | `*170*1*1*{merchant}*{amount}#` |
| Vodafone Cash | `*110*1*{merchant}*{amount}#` |
| AirtelTigo | `*500*1*{merchant}*{amount}#` |

## Testing

Run unit tests:

```bash
./gradlew test
```

Run instrumented tests:

```bash
./gradlew connectedAndroidTest
```

## Security Considerations

- SMS permissions are sensitive; the app only processes payment-related messages
- Transaction data is synced over HTTPS
- No customer data is stored permanently on device
- NFC communication uses standard APDU protocols

## License

MIT License

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request
