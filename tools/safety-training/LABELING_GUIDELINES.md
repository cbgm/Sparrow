# Sparrow Safety Label Definitions

These definitions are used by the automatic dual-pass teacher in `auto_label.py` and by any later human audit/gold benchmark.

## URGENT_ACTION_REQUEST

`true` only when the sender pressures the recipient to act immediately or very soon, often with threatened consequences.

Positive:

- "Act immediately or your account will be disabled."
- "Überweise das Geld innerhalb von zehn Minuten, sonst wird dein Konto gesperrt."

Negative:

- "The invoice is due Friday."
- "Das ist dringend, ruf mich an wenn du Zeit hast."

## CREDENTIAL_REQUEST

`true` only when the sender asks/instructs the recipient to reveal, send, share, or disclose credentials such as password, PIN, OTP, verification/login/security code.

Positive:

- "Send me the verification code you just received."
- "Schick mir den Code aus der SMS."

Negative:

- "Never share your verification code."
- "Your verification code is 123456."
- "I changed my password yesterday."

## PAYMENT_REQUEST

`true` only when the sender asks/instructs the recipient to transfer/send/pay money or value, buy gift cards, or transfer cryptocurrency.

Positive:

- "Transfer 200 euros to this account."
- "Buy gift cards and send me the codes."

Negative:

- "I transferred 200 euros yesterday."
- "Did the payment arrive?"
- "Please send me the receipt."

## PRIVATE_KEY_REQUEST

`true` only when the sender asks/instructs the recipient to disclose a private key, seed phrase, recovery phrase, mnemonic, wallet seed, backup words, or equivalent secret.

Positive:

- "Send me your 12-word recovery phrase."
- "Gib mir deinen privaten Wallet-Schlüssel."

Negative:

- "Never share your seed phrase."
- "Store your recovery phrase offline."

## Multi-label examples

"Send me the verification code immediately or your account will be locked."

- `urgent_action_request=true`
- `credential_request=true`
- `payment_request=false`
- `private_key_request=false`

"Transfer 500 euros right now or you will lose access."

- `urgent_action_request=true`
- `credential_request=false`
- `payment_request=true`
- `private_key_request=false`

## Important

Generic `spam`/`ham` labels are never converted directly into Sparrow labels. The automatic teacher sees only the message text and must classify Sparrow's four semantic reasons independently.
