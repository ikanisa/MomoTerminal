# MomoTerminal - Play Store Graphics Specs

## 1. Feature Graphic (Required)
**Dimensions:** 1024 x 500 px (PNG or JPEG)

### AI Image Prompt (for Midjourney/DALL-E):
```
Professional app store feature graphic for mobile payment app called "MomoTerminal". 
Show a modern Android smartphone with NFC tap-to-pay animation, glowing contactless 
payment symbol. Include subtle African-inspired gradient colors (orange, green, gold). 
Text overlay: "Tap to Pay" in clean white font. Mobile money concept, fintech style, 
clean minimal design, 1024x500 banner format, no mockup frame.
```

### Design Elements:
- **Background:** Gradient (dark blue → teal or orange → gold)
- **Center:** Phone with NFC waves/tap animation
- **Text:** "MomoTerminal" logo + "Tap to Pay" tagline
- **Icons:** Small MTN, Vodafone, Airtel logos (optional)
- **Style:** Clean, professional, fintech

---

## 2. Screenshots (Minimum 2, Recommended 5-8)

### Required Screenshots:

#### Screenshot 1: Home/Terminal Screen
- Show main payment entry screen
- Amount input field visible
- Provider selection buttons
- "Activate NFC" button prominent

#### Screenshot 2: NFC Active
- NFC terminal activated state
- Animated tap indicator
- "Ready to receive payment" message

#### Screenshot 3: Transaction List
- List of recent transactions
- Status indicators (Pending, Sent, Failed)
- Amount and timestamp visible

#### Screenshot 4: Analytics Dashboard
- Transaction summary charts
- Total amounts
- Provider breakdown

#### Screenshot 5: Login Screen
- WhatsApp OTP authentication
- Phone number input
- Clean, secure appearance

### Screenshot Specs:
- **Phone:** 1080 x 1920 px minimum (16:9)
- **Tablet (optional):** 1200 x 1920 px
- **Format:** PNG or JPEG
- **No alpha/transparency**

---

## 3. App Icon (Already exists)
- Location: `app/src/main/res/mipmap-*/ic_launcher.png`
- Should already be configured

---

## Quick Screenshot Commands

### Using ADB (connected device/emulator):
```bash
# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png ~/Desktop/screenshot1.png

# Or use Android Studio: View > Tool Windows > Device File Explorer
```

### Using Emulator:
1. Run app in Android Studio emulator
2. Click camera icon in emulator toolbar
3. Screenshots saved to Desktop

---

## Free Tools for Feature Graphic

1. **Canva** (canva.com) - Free templates, easy to use
2. **Figma** (figma.com) - Free, professional
3. **Photopea** (photopea.com) - Free Photoshop alternative

### Canva Quick Steps:
1. Go to canva.com → Create Design → Custom (1024x500)
2. Search templates: "app banner" or "tech banner"
3. Customize colors, add phone mockup, add text
4. Download as PNG

---

## Color Palette (Brand)
- Primary: `#1E88E5` (Blue)
- Secondary: `#FF9800` (Orange/MTN)
- Accent: `#4CAF50` (Green/Success)
- Background: `#121212` (Dark) or `#FFFFFF` (Light)

---

## Checklist

- [ ] Feature Graphic (1024x500)
- [ ] Screenshot 1: Home Screen
- [ ] Screenshot 2: NFC Active
- [ ] Screenshot 3: Transactions
- [ ] Screenshot 4: Analytics
- [ ] Screenshot 5: Login
