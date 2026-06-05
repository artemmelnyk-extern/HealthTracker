# Firebase Email Deliverability — Options & Trade-offs

**Context:** Verification emails sent from `noreply@test-kotlin-e6963.firebaseapp.com` are landing in users' spam folders. Gmail flags them because the sender domain is a generic Firebase subdomain with no established reputation, and no SPF/DKIM/DMARC authentication tied to our brand.

**Goal:** Ensure transactional emails (email verification, password reset) reach the inbox reliably.

---

## Root Cause

| Problem | Detail |
|---|---|
| Generic sender domain | `firebaseapp.com` is shared by thousands of Firebase projects — high spam association |
| No brand trust | Users don't recognize the sender, increasing manual spam reports |
| Missing/weak DNS auth | SPF/DKIM not tied to our domain; DMARC absent |
| Project name in domain | `test-kotlin-e6963` signals a dev/test project, further hurting reputation |

---

## Option A — Custom Domain via Firebase Hosting (Quick win)

**What it does:** Firebase allows linking a custom domain so the sender becomes `noreply@yourdomain.com` instead of `noreply@firebaseapp.com`.

**Steps:**
1. Add a custom domain in Firebase Hosting (e.g., `healthtracker.app`)
2. Firebase automatically updates the Auth email sender to `noreply@healthtracker.app`
3. Add the DNS records Firebase provides (SPF + DKIM TXT records)

**Pros:**
- No third-party service needed
- Stays within Firebase ecosystem
- Low implementation effort (~1–2 hours)

**Cons:**
- Requires owning a custom domain
- Firebase's built-in email sending has limited deliverability controls
- No email analytics or bounce handling
- Still dependent on Firebase's shared sending infrastructure

**Cost:** Domain registration (~$10–15/year). No additional service cost.

**Effort:** Low

---

## Option B — SendGrid Integration (Recommended)

**What it does:** Replace Firebase's native email sending with SendGrid. Firebase Auth triggers still fire, but emails are dispatched via SendGrid's infrastructure from a verified domain.

**Implementation paths:**
- **Firebase Trigger Email Extension** (Firestore-based): writes email documents to Firestore, extension sends via SendGrid
- **Cloud Functions**: intercept `onCreate` user event, call SendGrid API directly

**Steps:**
1. Create SendGrid account, verify sender domain
2. Add SPF (`include:sendgrid.net`) and DKIM records to DNS
3. Configure DMARC (`v=DMARC1; p=quarantine`)
4. Integrate via Cloud Function or the Firebase Extension
5. Disable Firebase default email templates (or keep as fallback)

**Pros:**
- Industry-standard deliverability (dedicated IPs available)
- Full analytics: opens, clicks, bounces, spam reports
- Unsubscribe management built-in
- Custom HTML templates with branding
- Bounce/spam feedback loops

**Cons:**
- Additional service dependency
- Slightly more implementation work (~4–8 hours)
- Free tier (100/day) may require upgrading for production scale

**Cost:** Free up to 100 emails/day; $19.95/month for 50k/month

**Effort:** Medium

---

## Option C — Resend Integration (Modern alternative to SendGrid)

**What it does:** Same concept as Option B but using Resend, a newer transactional email API designed for developers.

**Steps:**
1. Create Resend account, add and verify domain
2. DNS: SPF, DKIM, DMARC records (Resend provides exact values)
3. Call Resend API from Cloud Functions on auth events

**Pros:**
- Generous free tier (3,000 emails/month, 100/day)
- Simple REST API, excellent developer experience
- React Email support for templating
- Good deliverability out of the box

**Cons:**
- Newer service, smaller track record than SendGrid/Mailgun
- Fewer enterprise features (no dedicated IP on lower plans)
- Custom integration required (no Firebase Extension)

**Cost:** Free up to 3,000/month; $20/month for 50k/month

**Effort:** Medium

---

## Option D — Mailgun Integration

**What it does:** Same pattern as Options B/C using Mailgun's SMTP/API.

**Pros:**
- Reliable, long-established service
- SMTP relay option (easier if team prefers SMTP over API)
- Good EU data residency options (important for GDPR)

**Cons:**
- Free tier limited to 100 emails/day (trial period only, then paid)
- Slightly more expensive than alternatives at low volume
- UI less developer-friendly than Resend

**Cost:** $35/month for 50k emails (no meaningful free tier after trial)

**Effort:** Medium

---

## Option E — AWS SES (Scale solution)

**What it does:** Use Amazon Simple Email Service as the sending backend via Cloud Functions.

**Steps:**
1. Verify domain in AWS SES, configure DNS records
2. Request production access (SES starts in sandbox mode)
3. Call SES API from Firebase Cloud Functions
4. Set up SNS for bounce/complaint notifications

**Pros:**
- Cheapest at scale ($0.10 per 1,000 emails)
- Full control over sending infrastructure
- Dedicated IPs available
- Strong GDPR/compliance tooling

**Cons:**
- Highest setup complexity
- SES sandbox mode requires manual AWS approval to exit
- Adds AWS dependency to a Firebase-first stack
- Bounce/complaint handling requires additional setup (SNS + Lambda or endpoint)

**Cost:** ~$0.10/1,000 emails. Essentially free at startup scale.

**Effort:** High

---

## Option F — Rename / Recreate Firebase Project

**What it does:** The current project ID `test-kotlin-e6963` is embedded in the sender domain. Creating a production Firebase project with a proper ID (e.g., `healthtracker-prod`) removes the "test" signal from the domain.

> **Note:** This alone does not solve deliverability — the domain is still `firebaseapp.com`. It should be combined with Option A or B.

**Pros:**
- Cleaner project naming for production
- Removes "test" flag from sender domain

**Cons:**
- Requires migrating Firestore data, Auth users, and all config
- High disruption, not a standalone fix

**Cost:** None

**Effort:** High (migration risk)

---

## Comparison Summary

| Option | Inbox Reliability | Effort | Cost/month | Analytics | GDPR-friendly |
|---|---|---|---|---|---|
| A — Firebase custom domain | Medium | Low | ~$1 (domain) | None | Yes |
| **B — SendGrid** | **High** | **Medium** | **$0–$20** | **Yes** | **Yes** |
| C — Resend | High | Medium | $0–$20 | Yes | Yes |
| D — Mailgun | High | Medium | $35+ | Yes | Yes (EU region) |
| E — AWS SES | High | High | ~$0 | Partial | Yes |
| F — Rename project | Low (alone) | High | None | None | N/A |

---

## Recommended Decision Path

```
Is the project going to production?
├── No (still in dev/test)
│   └── → Option A: add custom domain to Firebase Hosting
│           Users can mark as non-spam manually for now
│
└── Yes
    ├── Do we already use AWS?
    │   ├── Yes, at scale (>100k emails/month) → Option E (SES)
    │   └── No → continue
    │
    ├── EU data residency required?
    │   └── Yes → Option D (Mailgun EU region)
    │
    └── Default recommendation
        └── Option B (SendGrid) or Option C (Resend)
            Both offer free tiers, good deliverability, and easy integration
```

---

## DNS Records Required (all options)

Regardless of provider, the following DNS records must be configured on the sending domain:

```
# SPF — authorize the provider to send on your behalf
TXT  @   "v=spf1 include:<provider-spf-domain> ~all"

# DKIM — cryptographic signature (provider generates the key)
TXT  <selector>._domainkey   "<provider-dkim-value>"

# DMARC — policy for unauthenticated mail
TXT  _dmarc   "v=DMARC1; p=quarantine; rua=mailto:dmarc@yourdomain.com"
```

---

## Open Questions for Team Discussion

1. **Do we own a custom domain yet?** (Required for any real fix)
2. **What is the expected email volume at launch?** (Determines tier/cost)
3. **Do we need EU data residency** for GDPR compliance?
4. **Is the Firebase project `test-kotlin-e6963` the intended production project**, or do we plan to create a separate prod project?
5. **Do we want email analytics** (open rates, bounces)? If yes, Option A is ruled out.
6. **Who owns DNS management** for the domain? (Needed to add SPF/DKIM records)

---

*Document generated: 2026-06-04 — for internal team discussion only.*
