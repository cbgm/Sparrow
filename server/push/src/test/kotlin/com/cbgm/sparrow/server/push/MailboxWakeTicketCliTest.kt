package com.cbgm.sparrow.server.push

import com.cbgm.sparrow.server.protocol.MailboxWakeTicket
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.MailboxWakeTicketCli
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertTrue

class MailboxWakeTicketCliTest {
    @Test
    fun theSmokeTestTicketPassesTheWakeUpCheck() {
        val output = ByteArrayOutputStream()
        val previous = System.out
        System.setOut(PrintStream(output))
        try {
            MailboxWakeTicketCli.main(arrayOf("node-a"))
        } finally {
            System.setOut(previous)
        }
        val values = output.toString().lines().filter { '=' in it }.associate { it.substringBefore('=') to it.substringAfter('=') }
        val ticket = serverJson.decodeFromString<MailboxWakeTicket>(values.getValue("ticket"))

        assertTrue(MailboxWakeTicketValidator.allows(ticket, values.getValue("routingId"), "node-a", System.currentTimeMillis()))
    }
}
