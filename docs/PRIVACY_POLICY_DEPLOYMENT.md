# Privacy Policy Deployment Guide

**Date:** December 1, 2025  
**File:** `docs/privacy.html`  
**Estimated Time:** 15-30 minutes

---

## Quick Start (Choose One Method)

1. **GitHub Pages** (Recommended - Free, 5 minutes)
2. **Firebase Hosting** (Fast, requires Firebase project)
3. **Netlify** (Free tier, drag-and-drop)
4. **Custom Domain** (If you own momoterminal.com)

---

## Option 1: GitHub Pages (Recommended ⭐)

### Step-by-Step Instructions

#### 1. Create gh-pages Branch
```bash
cd /Users/jeanbosco/workspace/MomoTerminal

# Create and switch to gh-pages branch
git checkout -b gh-pages

# Copy privacy policy to root
cp docs/privacy.html index.html

# Add and commit
git add index.html
git commit -m "Add privacy policy for GitHub Pages"

# Push to GitHub
git push origin gh-pages
```

#### 2. Enable GitHub Pages
1. Go to your GitHub repository: `https://github.com/[username]/MomoTerminal`
2. Click **Settings** tab
3. Scroll to **Pages** section (left sidebar)
4. Under **Source**, select:
   - Branch: `gh-pages`
   - Folder: `/ (root)`
5. Click **Save**

#### 3. Wait for Deployment (1-2 minutes)
GitHub will show: "Your site is live at `https://[username].github.io/MomoTerminal/`"

#### 4. Test Your Privacy Policy
```bash
# Open in browser
open "https://[username].github.io/MomoTerminal/"
```

#### 5. Use This URL in Play Console
```
Privacy Policy URL: https://[username].github.io/MomoTerminal/
```

### Pros
- ✅ Free forever
- ✅ Fast (1-2 minute setup)
- ✅ HTTPS by default
- ✅ No configuration needed
- ✅ Can use custom domain later

### Cons
- ⚠️ URL includes GitHub username
- ⚠️ Cannot use if repo is private (must be public)

---

## Option 2: Firebase Hosting

### Prerequisites
- Firebase project created
- Firebase CLI installed (`npm install -g firebase-tools`)

### Step-by-Step Instructions

#### 1. Initialize Firebase Hosting
```bash
cd /Users/jeanbosco/workspace/MomoTerminal

# Login to Firebase
firebase login

# Initialize hosting
firebase init hosting

# Select options:
# - Use existing project (select your project)
# - Public directory: docs
# - Configure as single-page app: No
# - Set up automatic builds: No
# - Overwrite index.html: No
```

#### 2. Deploy
```bash
# Deploy to Firebase
firebase deploy --only hosting

# Output will show:
# Hosting URL: https://[project-id].web.app
```

#### 3. Access Your Privacy Policy
```
https://[project-id].web.app/privacy.html
```

### Set Up Custom Path (Optional)
To make URL cleaner (`/privacy` instead of `/privacy.html`):

**Edit `firebase.json`:**
```json
{
  "hosting": {
    "public": "docs",
    "rewrites": [
      {
        "source": "/privacy",
        "destination": "/privacy.html"
      }
    ]
  }
}
```

**Redeploy:**
```bash
firebase deploy --only hosting
```

**Final URL:**
```
https://[project-id].web.app/privacy
```

### Pros
- ✅ Fast global CDN
- ✅ Free tier (10 GB bandwidth/month)
- ✅ HTTPS by default
- ✅ Easy custom domain setup
- ✅ Automatic SSL certificates

### Cons
- ⚠️ Requires Firebase project
- ⚠️ Slightly more complex setup

---

## Option 3: Netlify

### Step-by-Step Instructions

#### Method A: Drag and Drop (Easiest)

1. Go to [https://app.netlify.com](https://app.netlify.com)
2. Sign up / Log in (GitHub account works)
3. Drag `docs/privacy.html` onto the upload area
4. Netlify deploys instantly
5. Your URL: `https://[random-name].netlify.app`
6. Rename site:
   - Site Settings → Change site name → `momoterminal`
   - New URL: `https://momoterminal.netlify.app`

#### Method B: Git Integration

1. Push your code to GitHub/GitLab
2. New site from Git → Select repo
3. Build settings:
   - Build command: (leave empty)
   - Publish directory: `docs`
4. Deploy site
5. Access at: `https://[site-name].netlify.app/privacy.html`

### Pros
- ✅ Simplest deployment (drag-and-drop)
- ✅ Free tier generous
- ✅ HTTPS by default
- ✅ Custom domain easy
- ✅ Continuous deployment from Git

### Cons
- ⚠️ Another service to manage

---

## Option 4: Custom Domain

If you own `momoterminal.com`:

### Using GitHub Pages with Custom Domain

#### 1. Add CNAME File
```bash
echo "momoterminal.com" > CNAME
git add CNAME
git commit -m "Add custom domain"
git push origin gh-pages
```

#### 2. Configure DNS
Add these records to your domain registrar:

**For apex domain (momoterminal.com):**
```
Type: A
Name: @
Value: 185.199.108.153
Value: 185.199.109.153
Value: 185.199.110.153
Value: 185.199.111.153
```

**For www subdomain:**
```
Type: CNAME
Name: www
Value: [username].github.io
```

#### 3. Enable HTTPS
In GitHub repo settings → Pages:
- Check "Enforce HTTPS" (wait 24 hours for SSL cert)

#### 4. Final URL
```
https://momoterminal.com/privacy
```

### Using Firebase with Custom Domain

#### 1. Add Domain in Firebase Console
```bash
firebase hosting:channel:deploy live
```

Then in Firebase Console:
- Hosting → Add custom domain
- Enter `momoterminal.com`
- Follow DNS verification steps

#### 2. Configure DNS
Firebase will provide TXT record for verification, then:
```
Type: A
Name: @
Value: [Firebase IP addresses provided]
```

#### 3. SSL Certificate
Firebase automatically provisions SSL certificate (takes 24 hours)

---

## Verification Checklist

After deployment, verify:

### ✅ Accessibility
- [ ] URL loads in browser
- [ ] HTTPS works (green padlock)
- [ ] Mobile responsive (test on phone)
- [ ] All sections visible
- [ ] Contact links work

### ✅ Content
- [ ] Date is current (December 1, 2025)
- [ ] All sections present
- [ ] Email addresses correct
- [ ] No placeholder text (like "YOUR_DOMAIN")

### ✅ SEO & Meta
- [ ] Page title correct
- [ ] Meta description present
- [ ] Readable URL (no random strings)

---

## Update Play Console

### 1. Navigate to Data Safety
1. Open [Google Play Console](https://play.google.com/console)
2. Select MomoTerminal app
3. Go to: **Policy** → **App Content** → **Data Safety**

### 2. Add Privacy Policy URL

**Privacy Policy URL:**
```
https://[your-deployed-url]/privacy

Examples:
- https://username.github.io/MomoTerminal/
- https://momoterminal.web.app/privacy
- https://momoterminal.netlify.app/privacy.html
- https://momoterminal.com/privacy
```

### 3. Save and Publish
- Click **Save**
- Submit for review

---

## Troubleshooting

### Issue: 404 Not Found

**GitHub Pages:**
```bash
# Check branch exists
git branch -a | grep gh-pages

# Verify file in root
git checkout gh-pages
ls -la index.html

# Check GitHub Pages settings
# Settings → Pages → Source should be "gh-pages" branch
```

**Firebase:**
```bash
# Check deployment
firebase hosting:channel:list

# Redeploy
firebase deploy --only hosting

# Check firebase.json
cat firebase.json
```

### Issue: CSS Not Loading

Check `privacy.html` has inline styles (it does in provided file).

### Issue: HTTPS Certificate Error

**GitHub Pages:**
- Wait 24 hours after enabling HTTPS
- Enforce HTTPS option might need time

**Firebase:**
- SSL is automatic, should work immediately
- If custom domain, wait 24 hours

### Issue: URL Too Long

Use URL shortener:
1. Deploy to GitHub Pages
2. Create short link: `https://momo.to/privacy`
3. Use short link in Play Console

---

## Future Updates

### When to Update Privacy Policy

Update and redeploy when:
- Adding new features that collect data
- Changing data retention policies
- Adding new third-party services
- Changing contact information
- Legal requirement changes

### How to Update

#### GitHub Pages
```bash
git checkout gh-pages
# Edit index.html
git commit -m "Update privacy policy - [reason]"
git push origin gh-pages
# Changes live in 1-2 minutes
```

#### Firebase
```bash
# Edit docs/privacy.html
firebase deploy --only hosting
```

#### Netlify
- Edit file and drag-drop again, OR
- Push to Git (if using Git integration)

---

## Domain Purchase (Optional)

If you want `momoterminal.com`:

### Recommended Registrars
1. **Namecheap** - $8-12/year
2. **Google Domains** - $12/year
3. **Cloudflare** - $8-10/year (best price)

### Setup After Purchase
1. Buy domain
2. Point to hosting (see "Custom Domain" section above)
3. Wait 24-48 hours for DNS propagation
4. Update Play Console with new URL

---

## Cost Summary

| Option | Setup Time | Annual Cost | Recommended |
|--------|-----------|-------------|-------------|
| **GitHub Pages** | 5 min | $0 | ⭐ **Best for starting** |
| Firebase Hosting | 10 min | $0 (free tier) | Good |
| Netlify | 3 min | $0 (free tier) | Very easy |
| Custom Domain | 30 min | $10-15 | Professional |

---

## Next Steps

1. **Choose deployment method** (GitHub Pages recommended)
2. **Deploy privacy policy** (follow instructions above)
3. **Verify it loads** in browser
4. **Copy final URL**
5. **Add to Play Console** Data Safety section
6. **Update `DATA_SAFETY_FORM_TEMPLATE.md`** with actual URL
7. **Continue to Item 2** (Data Safety Form)

---

## Quick Commands Reference

### GitHub Pages (Complete Workflow)
```bash
# 1. Create branch and deploy
git checkout -b gh-pages
cp docs/privacy.html index.html
git add index.html
git commit -m "Add privacy policy"
git push origin gh-pages

# 2. Go to GitHub repo settings → Pages
# 3. Enable GitHub Pages (gh-pages branch)
# 4. Wait 2 minutes
# 5. Visit: https://[username].github.io/MomoTerminal/
```

### Firebase (Complete Workflow)
```bash
# 1. Setup
firebase login
firebase init hosting
# Select: existing project, docs folder

# 2. Deploy
firebase deploy --only hosting

# 3. Visit: https://[project-id].web.app/privacy.html
```

---

## Support

**Need Help?**
- GitHub Pages: [https://docs.github.com/pages](https://docs.github.com/pages)
- Firebase: [https://firebase.google.com/docs/hosting](https://firebase.google.com/docs/hosting)
- Netlify: [https://docs.netlify.com](https://docs.netlify.com)

**Questions?**
- Email: developer@momoterminal.com
- Documentation: See other guides in `/docs`

---

**Status:** ✅ Ready to deploy  
**Next:** Deploy, then proceed to Data Safety Form

---

_Last Updated: December 1, 2025_
