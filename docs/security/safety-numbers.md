# Safety Numbers

## Overview

End-to-end encryption ensures that messages remain confidential.

However, encryption alone cannot guarantee **who** is on the other end of the conversation.

Safety Numbers solve this problem by allowing two users to verify that they possess each other's expected public identity keys.

Verification protects against identity substitution attacks.

---

# Why Safety Numbers?

Imagine Alice starts an encrypted conversation with Bob.

Without verification there is no guarantee that Bob's public key actually belongs to Bob.

A malicious actor could attempt to replace Bob's public identity with another key.

Safety Numbers detect this situation.

---

# What Is a Safety Number?

A Safety Number is a deterministic fingerprint generated from both participants' public identity keys.

```
Alice Public Keys

↓

Bob Public Keys

↓

Fingerprint

↓

Safety Number
```

Both users independently calculate the same value.

If both values match, both users possess the same identities.

---

# Characteristics

A Safety Number is

- deterministic
- reproducible
- independent of transport
- independent of the gateway
- derived only from public information

Private keys are never used directly.

---

# Verification Process

The verification process is intentionally simple.

```
Alice

↓

Generate Safety Number

↓

Compare

↓

Bob

↓

Generate Safety Number
```

If the displayed numbers are identical, the identities can be marked as verified.

---

# Trust Establishment

Receiving a public identity is **not** the same as trusting it.

The normal lifecycle is

```
Import Identity

↓

Unverified

↓

Compare Safety Number

↓

Verified
```

Only after successful verification should the identity be considered trusted.

---

# Verified State

Once verification succeeds

- the contact is marked as verified
- future conversations use the verified identity
- unexpected identity changes become detectable

Verification is stored locally.

---

# Identity Changes

If another user's public identity changes, the previously verified Safety Number becomes invalid.

Possible causes include

- application reinstallation
- identity reset
- migration to a new identity
- malicious impersonation

Whenever the public identity changes, verification should be performed again.

---

# User Experience

SecureChat should clearly distinguish between

- Unverified Identity
- Verified Identity

Users should immediately understand the current trust state of a conversation.

The application should avoid technical cryptographic terminology whenever possible.

---

# Gateway Independence

The gateway plays no role in Safety Number verification.

It

- does not calculate Safety Numbers
- does not store verification state
- cannot influence the generated value

Verification is performed entirely by the communicating devices.

---

# Offline Verification

Because the Safety Number depends only on public identity information, users may verify identities through any trusted communication channel.

Examples include

- meeting in person
- voice call
- video call
- another trusted communication channel

The verification channel is independent of SecureChat itself.

---

# Security Benefits

Safety Numbers protect against

- identity substitution
- impersonation
- unexpected key replacement
- man-in-the-middle attacks involving substituted identity keys

They provide an additional layer of trust on top of end-to-end encryption.

---

# Limitations

Safety Numbers do **not**

- prevent device compromise
- prevent malware on a user's phone
- guarantee physical identity
- replace good operational security

They verify cryptographic identities, not real-world identities.

---

# Regeneration

Safety Numbers are regenerated automatically whenever either participant's public identity changes.

Applications should clearly notify users when previously verified identities require verification again.

---

# Storage

Verification status may be stored locally.

Typical states include

```
Unknown

↓

Unverified

↓

Verified
```

The Safety Number itself can always be recalculated from the public identities.

---

# Best Practices

Users should

- verify important contacts
- repeat verification after identity changes
- investigate unexpected verification resets

Applications should encourage—but not force—verification for security-sensitive conversations.

---

# Summary

Safety Numbers establish trust between communicating users by verifying that both parties possess the expected public identity keys.

They complement end-to-end encryption by protecting against identity substitution attacks and allowing users to detect unexpected identity changes.
