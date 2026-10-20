# Onboarding

`:feature:onboarding` owns the first-run UI. The current pages are implemented by:

- `WelcomePage`
- `PrivacyPage`
- `PermissionsPage`
- `PhonePage`

`OnboardingViewModel` coordinates the page order and the permission/phone-number requests through
platform ports rather than putting persistence or Android APIs directly into the composables.
Identity creation and restore are owned by `:feature:identity` (`IdentityCreateViewModel`,
`IdentityRestoreViewModel`); `OnboardingRoute` hosts them and places `IdentitySetupDialogs` (restore
and phone verification dialogs) on top of the onboarding screen.

## Typical first-run sequence

```mermaid
flowchart LR
    W[Welcome] --> P[Privacy / disclaimers]
    P --> PERM[Permissions]
    PERM --> PHONE[Phone number + name]
    PHONE --> V[SMS verification]
    V --> ID[Create or restore identity]
    ID --> MAIN[Main application]
```

The exact navigation condition is driven by startup/identity/onboarding state, so this diagram is
conceptual rather than a persisted protocol state machine.

## Phone page

The phone page asks for the phone number and name. With the phone-number permission the SIM
number is filled in automatically (`AutomaticPhoneNumberReader`); otherwise the user can pick a
number (`PhoneNumberHintLauncher`) or type it. The page offers two actions:

- **Approve and create identity** — the number is normalized, the phone verification dialog
  opens, and the identity keys are created only after the number was verified.
- **Restore** — select an identity backup file, enter its password; the backup is decrypted and
  checked in memory, then the same phone verification runs, and only after it succeeds is the
  restored identity saved.

## Phone verification

Verification happens once, at sign-up (create and restore). `PhoneVerificationDialog` /
`PhoneVerificationViewModel` (in `:feature:identity`):

1. opens with "We will send a code by SMS to …"; tapping *Verify* sends the SMS;
2. shows six code fields (`SparrowCodeField` in `:core:ui`); entering the sixth digit, or an SMS
   read automatically by the platform, confirms the code;
3. *Send again* becomes available after a 60-second countdown;
4. if the SMS cannot be sent, the dialog returns to the first step with an error; a wrong code
   clears the fields.

Cancelling the dialog creates or restores nothing. Debug builds show an additional
*Skip (debug)* button. Details, including how the result reaches the server, are in
[Identity](identity.md#phone-verification-at-sign-up).

## Platform notes

Android has the usable onboarding permission/phone/contact integrations and the Firebase-based SMS
verification. Location permission for sharing the current location is requested at runtime by
the attachment action rather than being a required onboarding permission. The KMP project
contains iOS source sets, but iOS has no phone verification implementation yet and is not
currently a usable client; it should not be documented as having completed onboarding parity.
