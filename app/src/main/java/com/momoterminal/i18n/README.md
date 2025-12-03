# MomoTerminal i18n/l10n Strategy

## Overview

This document outlines the internationalization (i18n) and localization (l10n) strategy for MomoTerminal, designed to support multiple African markets and potentially global expansion.

---

## 1. String Resources Organization

### Directory Structure
```
res/
├── values/                    # Default (English)
│   └── strings.xml
├── values-fr/                 # French
│   └── strings.xml
├── values-sw/                 # Swahili
│   └── strings.xml
├── values-rw/                 # Kinyarwanda
│   └── strings.xml
├── values-pt/                 # Portuguese
│   └── strings.xml
├── values-ar/                 # Arabic (RTL)
│   └── strings.xml
├── values-es/                 # Spanish
│   └── strings.xml
└── values-am/                 # Amharic (future)
    └── strings.xml
```

### Naming Convention

Use a hierarchical naming pattern: `{scope}_{feature}_{element}_{modifier}`

| Prefix | Usage | Example |
|--------|-------|---------|
| `screen_` | Screen-specific strings | `screen_home_title`, `screen_terminal_amount_hint` |
| `nav_` | Navigation labels | `nav_home`, `nav_settings` |
| `action_` | Button/action labels | `action_save`, `action_cancel` |
| `error_` | Error messages | `error_network_unavailable`, `error_sms_permission_denied` |
| `hint_` | Input hints | `hint_enter_amount`, `hint_merchant_code` |
| `label_` | Form labels | `label_phone_number`, `label_currency` |
| `msg_` | User messages/toasts | `msg_transaction_synced`, `msg_settings_saved` |
| `dialog_` | Dialog content | `dialog_logout_title`, `dialog_logout_message` |
| `status_` | Status indicators | `status_pending`, `status_completed` |
| `provider_` | Provider names | `provider_mtn`, `provider_orange` |
| `a11y_` | Accessibility descriptions | `a11y_nfc_scan_button`, `a11y_balance_hidden` |

### Plurals Example
```xml
<plurals name="transactions_synced_count">
    <item quantity="zero">No transactions synced</item>
    <item quantity="one">%d transaction synced</item>
    <item quantity="other">%d transactions synced</item>
</plurals>
```

### Formatted Strings
```xml
<!-- Amount with currency -->
<string name="format_amount_currency">%1$s %2$s</string>

<!-- Welcome message -->
<string name="screen_home_welcome">Welcome, %1$s</string>

<!-- Transaction count -->
<string name="format_transaction_count">%1$d of %2$d</string>

<!-- Date relative -->
<string name="format_time_ago">%1$s ago</string>
```

---

## 2. Locale Handling Architecture

### Components
1. **LocaleManager** - Manages language selection and application
2. **UserPreferences** - Persists language choice via DataStore
3. **LocaleProvider** - Compose CompositionLocal for reactive UI updates

### Flow
```
User selects language
    ↓
LocaleManager.setLanguage()
    ↓
UserPreferences.setLanguage() [DataStore]
    ↓
AppCompatDelegate.setApplicationLocales()
    ↓
Activity recreates with new locale
    ↓
Compose UI recomposes via LocaleProvider
```

### Fallback Behavior
1. User-selected language (stored in DataStore)
2. Country-recommended language (based on MoMo country)
3. Device language (if supported)
4. English (default)

---

## 3. Formatting Strategy

### Currency Formatting
- Use `CurrencyFormatter` utility class
- Format based on locale conventions (decimal separator, grouping)
- Support for African currencies: RWF, XOF, XAF, KES, GHS, etc.

### Date/Time Formatting
- Use `DateTimeFormatter` utility
- Relative time for recent transactions ("2 min ago")
- Full date for older transactions
- Respect 12/24 hour preference

---

## 4. SMS Parsing Architecture

### Per-Locale Pattern System
```
sms/
├── patterns/
│   ├── SmsPatternRegistry.kt      # Central registry
│   ├── SmsPattern.kt              # Pattern data class
│   └── providers/
│       ├── GhanaPatterns.kt       # Ghana: MTN, Vodafone, AirtelTigo
│       ├── RwandaPatterns.kt      # Rwanda: MTN
│       ├── KenyaPatterns.kt       # Kenya: M-Pesa, Airtel
│       └── SenegalPatterns.kt     # Senegal: Orange, Wave
```

### Pattern Selection Logic
```
1. Get user's MoMo country
2. Get selected provider (or detect from SMS sender)
3. Load patterns for country + provider
4. Apply patterns to parse SMS
```

---

## 5. RTL & Accessibility

### RTL Preparation
- Use `start`/`end` instead of `left`/`right`
- Use `Modifier.layoutDirection` in Compose
- Test with Arabic locale

### Accessibility Requirements
- Content descriptions for all interactive elements
- Semantic grouping for related content
- Support for TalkBack
- Dynamic font scaling support

---

## File Index

| File | Purpose |
|------|---------|
| `i18n/LocaleProvider.kt` | Compose locale state management |
| `i18n/CurrencyFormatter.kt` | Locale-aware currency formatting |
| `i18n/DateTimeFormatter.kt` | Locale-aware date/time formatting |
| `sms/patterns/SmsPatternRegistry.kt` | SMS pattern management |
| `util/LocaleManager.kt` | Core locale management (existing) |
