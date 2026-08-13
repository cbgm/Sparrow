package com.cbgm.securechat.feature.settings.domain.model

object DisclaimerContent {
    val privacyPolicy =
        """
        # Privacy Policy

        _Last updated: [DATE]_

        This Privacy Policy explains what information SecureChat collects,
        how it is used, and the choices available to you.

        ## Information we do not collect

        SecureChat is designed around end-to-end encryption. Message
        content is encrypted on your device and cannot be read by us,
        the SecureChat server infrastructure, or any third party in transit.

        ## Information we do collect

        - **Phone number**: used as your routing address so contacts can
          reach you. This is stored on our servers in a form necessary
          to route messages to your device.
        - **Public keys**: your encryption and signing public keys are
          shared with contacts you choose to share your identity with.
          Private keys never leave your device.
        - **Device metadata**: limited technical information (app
          version, OS version) may be collected for crash reporting and
          diagnostics, if you have not disabled this in Settings.

        ## How we use this information

        Collected information is used solely to operate the messaging
        gateway, deliver encrypted messages, and maintain and improve the
        app. We do not sell personal data to third parties.

        ## Data retention

        Undelivered messages are retained by SecureChat server infrastructure only until
        successful delivery, after which they are deleted. You can
        delete your local message history and identity at any time from
        Settings.

        ## Your choices

        You may delete your account and associated data at any time.
        You may also enable or disable diagnostic data collection in
        Settings → Privacy & data.

        ## Contact

        Questions about this policy can be sent to [CONTACT EMAIL].
        """.trimIndent()

    val dataDisclaimer =
        """
        # Data Disclaimer

        This page explains, in plain terms, what SecureChat stores and
        where.

        ## Stored on your device

        - Your private encryption and signing keys (never transmitted)
        - Message history for your conversations
        - Your contact list and their public keys
        - App preferences (language, notification settings)

        ## Stored on the gateway service

        - Your phone number, used as a routing address
        - Your public keys, shared only with contacts you approve
        - Messages, temporarily, until they are delivered to the
          recipient's device — then deleted

        ## What we cannot see

        Because messages are end-to-end encrypted before they leave your
        device, message content is never visible to the gateway service or
        to SecureChat as an organization.

        ## Backups

        If you enable backups, an encrypted copy of your message history
        may be stored [LOCATION — e.g. on your device only / in your
        chosen cloud provider]. Backups are encrypted with a key only
        you control.

        ## Clearing your data

        You can clear local data at any time from Settings. This is
        irreversible and will remove your local message history and
        stored keys from this device.
        """.trimIndent()
}
