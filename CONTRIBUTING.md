# Contributing to MomoTerminal

Thank you for your interest in contributing to MomoTerminal! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Setup](#development-setup)
4. [Making Changes](#making-changes)
5. [Coding Standards](#coding-standards)
6. [Testing](#testing)
7. [Pull Request Process](#pull-request-process)
8. [Reporting Issues](#reporting-issues)

---

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment. We expect all contributors to:

- Be respectful and considerate in communications
- Accept constructive criticism gracefully
- Focus on what's best for the project and community
- Show empathy towards other contributors

---

## Getting Started

### Prerequisites

- **Android Studio**: Latest stable version (Electric Eel or newer)
- **JDK 17**: Required for building the project
- **Git**: For version control
- **Android SDK**: API 24 (minimum) to API 35 (target)

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/MomoTerminal.git
   cd MomoTerminal
   ```
3. Add the upstream remote:
   ```bash
   git remote add upstream https://github.com/ikanisa/MomoTerminal.git
   ```

---

## Development Setup

### 1. Open in Android Studio

1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to the cloned repository
4. Wait for Gradle sync to complete

### 2. Create google-services.json

For development, create a placeholder `google-services.json` in `app/`:

```json
{
  "project_info": {
    "project_id": "momoterminal-dev"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:123456789:android:abcdef",
        "android_client_info": {
          "package_name": "com.momoterminal"
        }
      },
      "api_key": [
        {
          "current_key": "AIza_placeholder"
        }
      ]
    }
  ]
}
```

### 3. Build the Project

```bash
./gradlew assembleDebug
```

### 4. Run Tests

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests (requires emulator or device)
./gradlew connectedDebugAndroidTest
```

---

## Making Changes

### Branch Naming Convention

Create a branch from `develop` using the following convention:

- `feature/description` - For new features
- `bugfix/description` - For bug fixes
- `hotfix/description` - For critical production fixes
- `docs/description` - For documentation changes
- `refactor/description` - For code refactoring

Example:
```bash
git checkout develop
git pull upstream develop
git checkout -b feature/add-receipt-printing
```

### Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**
```
feat(auth): add biometric authentication support

fix(sms): correct parsing for MTN transaction messages

docs(readme): update installation instructions

test(money): add precision tests for Money class
```

---

## Coding Standards

### Kotlin Style Guide

We follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with these additions:

1. **Max line length**: 120 characters
2. **Indentation**: 4 spaces (no tabs)
3. **Imports**: Sorted alphabetically, no wildcards
4. **Naming**:
   - Classes: `PascalCase`
   - Functions/Variables: `camelCase`
   - Constants: `SCREAMING_SNAKE_CASE`

### Code Organization

```
app/src/main/java/com/momoterminal/
├── api/              # API models and services
├── auth/             # Authentication logic
├── data/             # Data layer (repository, local, remote)
├── di/               # Dependency injection modules
├── domain/           # Domain models and use cases
├── feature/          # Feature-specific code
├── presentation/     # UI layer (screens, viewmodels)
├── security/         # Security utilities
├── util/             # General utilities
└── ...
```

### Documentation

- Add KDoc comments for public APIs
- Document complex algorithms
- Keep README and docs up to date

```kotlin
/**
 * Parses an SMS message to extract transaction details.
 *
 * @param message The raw SMS message body
 * @param sender The sender phone number or name
 * @return Parsed transaction data or null if parsing fails
 */
fun parseSms(message: String, sender: String): Transaction? {
    // Implementation
}
```

### Resource Naming

- Layouts: `activity_`, `fragment_`, `item_`, `view_`
- Drawables: `ic_`, `bg_`, `img_`
- Strings: `title_`, `msg_`, `btn_`, `hint_`
- Colors: descriptive names in `colors.xml`

---

## Testing

### Unit Tests

- Located in `app/src/test/`
- Use JUnit 4 and MockK
- Aim for 80%+ code coverage

```kotlin
class MoneyTest {
    @Test
    fun `fromCedis creates correct Money object`() {
        val money = Money.fromCedis(50.0)
        assertEquals(5000L, money.amountInSmallestUnit)
    }
}
```

### Instrumented Tests

- Located in `app/src/androidTest/`
- Use Espresso and Compose Testing
- Focus on critical user flows

### Running Tests

```bash
# All unit tests
./gradlew testDebugUnitTest

# Specific test class
./gradlew testDebugUnitTest --tests "*.MoneyTest"

# With coverage report
./gradlew jacocoTestReport
```

---

## Pull Request Process

### Before Submitting

1. ✅ All tests pass locally
2. ✅ No lint errors (`./gradlew lintDebug`)
3. ✅ Code follows style guidelines
4. ✅ Documentation updated if needed
5. ✅ Meaningful commit messages

### PR Checklist

When creating a PR, use this template:

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Manual testing completed
- [ ] Edge cases considered

## Checklist
- [ ] My code follows the project style guidelines
- [ ] I have performed a self-review of my code
- [ ] I have commented my code where necessary
- [ ] I have updated the documentation
- [ ] My changes generate no new warnings
- [ ] New and existing tests pass locally
```

### Review Process

1. Submit PR to `develop` branch
2. Automated checks must pass
3. At least one maintainer approval required
4. Address any review comments
5. Squash and merge when approved

---

## Reporting Issues

### Bug Reports

Use the bug report template with:

- Clear, descriptive title
- Steps to reproduce
- Expected vs actual behavior
- Device/Android version info
- Screenshots if applicable
- Logs if available

### Feature Requests

Use the feature request template with:

- Clear description of the feature
- Use case explanation
- Potential implementation approach
- Any alternatives considered

### Security Issues

**Do NOT report security vulnerabilities as public issues.**

Instead, email security@momoterminal.com. See [SECURITY.md](SECURITY.md) for details.

---

## Questions?

- Check existing issues and discussions
- Join our Discord community
- Email: developers@momoterminal.com

---

Thank you for contributing to MomoTerminal! 🎉
