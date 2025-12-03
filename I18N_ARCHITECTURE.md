# Internationalization & Localization Architecture

## Overview

A **complete, domain-agnostic i18n/l10n system** that supports:
- User-selectable language (independent of device)
- Multi-region support
- RTL/LTR layouts
- Locale-aware formatting
- Backend integration
- AI/LLM readiness

## Architecture Principles

1. **User Choice First**: Language selection in-app, not device-dependent
2. **Region Independent**: Language ≠ Country (e.g., Spanish in Spain vs Mexico)
3. **Generic Resources**: No domain-specific strings
4. **RTL Ready**: Full bidirectional support
5. **Format Aware**: Dates, numbers, currency adapt to locale
6. **Backend Sync**: Locale preferences sent to API

## High-Level Flow

```
┌─────────────────────────────────────────────────────────┐
│              User Selects Language                       │
│  (Settings Screen → Language Picker)                    │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│              Store in DataStore                          │
│  languageCode: "es"                                     │
│  regionCode: "MX"                                       │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│              Apply to App Context                        │
│  LocaleManager.setLocale(locale)                        │
│  Recreate Activity / Recompose                          │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│              Use in App                                  │
│  • Load strings from res/values-{lang}/                 │
│  • Format dates/numbers with locale                     │
│  • Apply RTL if needed                                  │
│  • Send to backend in API calls                         │
└─────────────────────────────────────────────────────────┘
```

## Module Structure

```
:core:i18n/
├── locale/
│   ├── LocaleManager.kt
│   ├── LocalePreferences.kt
│   └── SupportedLocales.kt
├── formatting/
│   ├── DateFormatter.kt
│   ├── NumberFormatter.kt
│   ├── CurrencyFormatter.kt
│   └── UnitFormatter.kt
├── rtl/
│   ├── LayoutDirectionProvider.kt
│   └── RtlUtils.kt
└── api/
    ├── LocaleInterceptor.kt
    └── LocaleModels.kt
```

## Resource Structure

```
res/
├── values/                    # Default (English)
│   ├── strings.xml
│   ├── plurals.xml
│   └── arrays.xml
├── values-es/                 # Spanish
│   └── strings.xml
├── values-fr/                 # French
│   └── strings.xml
├── values-ar/                 # Arabic (RTL)
│   └── strings.xml
├── values-zh/                 # Chinese
│   └── strings.xml
├── values-es-rMX/            # Spanish (Mexico)
│   └── strings.xml
└── values-es-rES/            # Spanish (Spain)
    └── strings.xml
```

## String Key Conventions

```
{screen}_{section}_{element}_{attribute}

Examples:
- home_header_title
- settings_language_label
- item_list_empty_message
- button_save_text
- error_network_message
- dialog_confirm_title
- form_field_hint
```

## Data Models

```kotlin
data class AppLocale(
    val languageCode: String,      // ISO 639-1 (e.g., "en", "es")
    val regionCode: String?,        // ISO 3166-1 (e.g., "US", "MX")
    val displayName: String,        // "English", "Español"
    val isRtl: Boolean = false
)

data class UserLocalePreferences(
    val languageCode: String,
    val regionCode: String?,
    val dateFormat: DateFormat = DateFormat.DEFAULT,
    val numberFormat: NumberFormat = NumberFormat.DEFAULT,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
)
```

## Key Features

### 1. In-App Language Selection
- User can change language without changing device settings
- Immediate UI update (no app restart)
- Persisted across sessions

### 2. Locale-Aware Formatting
- Dates: "Dec 3, 2025" vs "3 déc. 2025"
- Numbers: "1,234.56" vs "1.234,56"
- Currency: "$1,234" vs "1.234 €"
- Units: "5 mi" vs "8 km"

### 3. RTL Support
- Automatic layout mirroring
- Text alignment
- Icon flipping
- Scroll direction

### 4. Backend Integration
- Accept-Language header
- User locale in profile
- Localized content from API

### 5. AI/LLM Ready
- Pass user language to AI prompts
- Format AI responses per locale
- Translate AI-generated content

## Implementation Checklist

- [ ] Create :core:i18n module
- [ ] Define supported locales
- [ ] Implement LocaleManager
- [ ] Add language picker UI
- [ ] Store preferences in DataStore
- [ ] Apply locale at runtime
- [ ] Add formatters (date, number, currency)
- [ ] Implement RTL support
- [ ] Add locale to API calls
- [ ] Test all locales
