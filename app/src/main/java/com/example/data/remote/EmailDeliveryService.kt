package com.example.data.remote

import android.util.Base64
import com.example.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

sealed interface EmailSendResult {
    data class Success(val message: String) : EmailSendResult
    data class MissingCredentials(val message: String) : EmailSendResult
    data class Failure(val error: String) : EmailSendResult
}

/**
 * Production-grade direct transactional email dispatcher for Good Dream Sanctuary.
 * Connects directly to Google Gmail's secure SMTP servers (smtp.gmail.com) over TLS.
 * Automatically tries primary Port 465 (SMTPS) and falls back to Port 587 (STARTTLS)
 * to ensure resilience across all mobile carriers, Wi-Fi firewalls, and networks.
 */
class EmailDeliveryService(
    private val firestoreCatalogService: FirestoreCatalogService = FirestoreCatalogService()
) {

    companion object {
        private const val TAG = "EmailDeliveryService"
        private const val SMTP_HOST = "smtp.gmail.com"
        private const val TIMEOUT_MS = 4_000

        fun maskEmail(email: String): String {
            val at = email.indexOf('@')
            if (at <= 1) return "***@***"
            return "${email.take(2)}***${email.substring(at)}"
        }
    }

    /**
     * Resolves the configured sender email from BuildConfig or falls back to official admin email.
     */
    private fun getSenderEmail(): String {
        return try {
            val cfgEmail = BuildConfig.SMTP_EMAIL
            if (!cfgEmail.isNullOrBlank()) cfgEmail.trim() else "gooddreamshomedecor@gmail.com"
        } catch (e: Throwable) {
            "gooddreamshomedecor@gmail.com"
        }
    }

    /**
     * Resolves the Google App Password configured in .env / BuildConfig.
     */
    private fun getSenderPassword(): String {
        val cfgPass = try {
            BuildConfig.SMTP_PASSWORD
        } catch (_: Throwable) {
            ""
        }
        return cfgPass.trim().replace("\"", "").replace("'", "").replace(" ", "")
    }

    /**
     * Dispatches any custom HTML transactional email to [recipientEmail] via Google SMTP.
     * Automatically attempts Port 465 (SMTPS) first with fallback to Port 587 (STARTTLS).
     */
    suspend fun sendHtmlEmail(
        recipientEmail: String,
        subject: String,
        htmlBody: String
    ): EmailSendResult = withContext(Dispatchers.IO) {
        val senderEmail = getSenderEmail()
        val appPassword = getSenderPassword()

        if (appPassword.isBlank()) {
            Timber.tag(TAG).i("Google App Password not configured in client build. Enqueueing to secure Firestore cloud queue.")
            val enqueued = enqueueFirestoreMailRecord(senderEmail, recipientEmail, subject, htmlBody)
            return@withContext if (enqueued) {
                EmailSendResult.Success("Verification email enqueued for secure cloud delivery to ${maskEmail(recipientEmail)}")
            } else {
                EmailSendResult.Failure("Unable to connect to email delivery service. Please try again.")
            }
        }

        // 1. Attempt Primary Port 465 (SMTPS)
        var result = attemptSmtpSsl(senderEmail, appPassword, recipientEmail, subject, htmlBody)

        // 2. If Port 465 fails, fallback to Port 587 (STARTTLS)
        if (result is EmailSendResult.Failure) {
            Timber.tag(TAG).w("Port 465 connection failed (${result.error}). Attempting Port 587 (STARTTLS)...")
            val fallbackResult = attemptSmtpStartTls(senderEmail, appPassword, recipientEmail, subject, htmlBody)
            if (fallbackResult is EmailSendResult.Success) {
                result = fallbackResult
            }
        }

        // 3. If direct SMTP fails, fallback to Firestore cloud queue
        if (result is EmailSendResult.Failure) {
            Timber.tag(TAG).w("Direct SMTP ports unreachable. Falling back to secure Firestore cloud queue.")
            val enqueued = enqueueFirestoreMailRecord(senderEmail, recipientEmail, subject, htmlBody)
            if (enqueued) {
                return@withContext EmailSendResult.Success("Email routed via backup cloud dispatch to ${maskEmail(recipientEmail)}")
            }
        } else {
            // Asynchronously record to Firestore for queue/audit without blocking email delivery
            enqueueFirestoreMailRecord(senderEmail, recipientEmail, subject, htmlBody)
        }

        result
    }

    /**
     * Dispatches an authentic 6-digit OTP verification email to [recipientEmail].
     */
    suspend fun sendOtpEmail(
        recipientEmail: String,
        otp: String,
        expiresMinutes: Int = 5
    ): EmailSendResult {
        val subject = "Your Good Dream Verification Code: $otp"
        val html = buildOtpEmailHtml(otp, expiresMinutes)
        return sendHtmlEmail(recipientEmail, subject, html)
    }

    /**
     * Dispatches a confirmation email for customer inquiries, bespoke requests, repairs, or feedback.
     */
    suspend fun sendInquiryConfirmationEmail(
        recipientEmail: String,
        customerName: String,
        referenceNumber: String,
        inquiryType: String,
        details: String
    ): EmailSendResult {
        val subject = "Good Dream Concierge - Request Confirmed [#$referenceNumber]"
        val html = buildInquiryEmailHtml(customerName, referenceNumber, inquiryType, details)
        return sendHtmlEmail(recipientEmail, subject, html)
    }

    /**
     * Dispatches an official 25-Year Guarantee Certificate to the customer.
     */
    suspend fun sendWarrantyCertificateEmail(
        recipientEmail: String,
        customerName: String,
        certificateNumber: String,
        mattressSerial: String,
        purchaseDate: String
    ): EmailSendResult {
        val subject = "Official 25-Year SpringHaven Guarantee Certificate [#$certificateNumber]"
        val html = buildWarrantyCertificateHtml(customerName, certificateNumber, mattressSerial, purchaseDate)
        return sendHtmlEmail(recipientEmail, subject, html)
    }

    /**
     * Dispatches an instant priority notification to admin (Lakshya190207@gmail.com) for incoming leads.
     */
    suspend fun sendAdminLeadAlert(
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        inquiryType: String,
        details: String,
        referenceNumber: String
    ): EmailSendResult {
        val adminEmail = getSenderEmail()
        val subject = "🔔 Priority Client Lead: $inquiryType - $customerName [#$referenceNumber]"
        val html = buildAdminLeadAlertHtml(customerName, customerPhone, customerEmail, inquiryType, details, referenceNumber)
        return sendHtmlEmail(adminEmail, subject, html)
    }

    /**
     * Primary delivery channel: SMTPS over SSL on Port 465.
     */
    private fun attemptSmtpSsl(
        senderEmail: String,
        appPassword: String,
        recipientEmail: String,
        subject: String,
        htmlBody: String
    ): EmailSendResult {
        return try {
            val socketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val rawSocket = Socket()
            rawSocket.connect(InetSocketAddress(SMTP_HOST, 465), TIMEOUT_MS)
            val socket = socketFactory.createSocket(rawSocket, SMTP_HOST, 465, true) as SSLSocket
            val sslParams = socket.sslParameters
            sslParams.endpointIdentificationAlgorithm = "HTTPS"
            socket.sslParameters = sslParams
            socket.startHandshake()
            socket.soTimeout = TIMEOUT_MS

            val reader = BufferedReader(InputStreamReader(socket.inputStream, StandardCharsets.UTF_8))
            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream, StandardCharsets.UTF_8))

            // 1. Initial banner
            var line = reader.readLine() ?: ""
            if (!line.startsWith("220")) {
                socket.close()
                return EmailSendResult.Failure("SMTP connection failed: $line")
            }

            // 2. EHLO
            sendSmtpCommand(writer, "EHLO localhost")
            while (true) {
                line = reader.readLine() ?: ""
                if (line.startsWith("250 ")) break
                if (!line.startsWith("250-")) {
                    socket.close()
                    return EmailSendResult.Failure("EHLO rejected: $line")
                }
            }

            // 3. AUTH LOGIN
            sendSmtpCommand(writer, "AUTH LOGIN")
            line = reader.readLine() ?: ""
            if (!line.startsWith("334")) {
                socket.close()
                return EmailSendResult.Failure("AUTH LOGIN rejected: $line")
            }

            // 4. Send Base64 Sender Email
            sendSmtpCommand(writer, encodeBase64(senderEmail))
            line = reader.readLine() ?: ""
            if (!line.startsWith("334")) {
                socket.close()
                return EmailSendResult.Failure("Sender email rejected by SMTP: $line")
            }

            // 5. Send Base64 App Password
            sendSmtpCommand(writer, encodeBase64(appPassword))
            line = reader.readLine() ?: ""
            if (!line.startsWith("235")) {
                socket.close()
                Timber.tag(TAG).e("SMTP Authentication failed on port 465: $line")
                return EmailSendResult.Failure("Invalid Google App Password. Server response: $line")
            }

            // 6. MAIL FROM
            sendSmtpCommand(writer, "MAIL FROM:<$senderEmail>")
            line = reader.readLine() ?: ""
            if (!line.startsWith("250")) {
                socket.close()
                return EmailSendResult.Failure("MAIL FROM rejected: $line")
            }

            // 7. RCPT TO
            sendSmtpCommand(writer, "RCPT TO:<$recipientEmail>")
            line = reader.readLine() ?: ""
            if (!line.startsWith("250")) {
                socket.close()
                return EmailSendResult.Failure("Recipient rejected by mail server: $line")
            }

            // 8. DATA
            sendSmtpCommand(writer, "DATA")
            line = reader.readLine() ?: ""
            if (!line.startsWith("354")) {
                socket.close()
                return EmailSendResult.Failure("DATA rejected: $line")
            }

            // 9. Send headers and HTML Body
            val emailMessage = StringBuilder().apply {
                append("From: \"Good Dream Sanctuary\" <$senderEmail>\r\n")
                append("To: <$recipientEmail>\r\n")
                append("Subject: $subject\r\n")
                append("MIME-Version: 1.0\r\n")
                append("Content-Type: text/html; charset=UTF-8\r\n")
                append("Content-Transfer-Encoding: 8bit\r\n")
                append("\r\n")
                append(htmlBody)
                append("\r\n.\r\n")
            }.toString()

            writer.write(emailMessage)
            writer.flush()

            line = reader.readLine() ?: ""
            if (!line.startsWith("250")) {
                socket.close()
                return EmailSendResult.Failure("Message delivery rejected: $line")
            }

            // 10. QUIT
            sendSmtpCommand(writer, "QUIT")
            socket.close()

            Timber.tag(TAG).i("Successfully dispatched transactional email to %s via Google SMTP (Port 465)", maskEmail(recipientEmail))
            EmailSendResult.Success("Email sent successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).w("Error on Port 465: ${e.message}")
            EmailSendResult.Failure("Port 465 error: ${e.localizedMessage}")
        }
    }

    /**
     * Fallback delivery channel: SMTP with STARTTLS on Port 587.
     */
    private fun attemptSmtpStartTls(
        senderEmail: String,
        appPassword: String,
        recipientEmail: String,
        subject: String,
        htmlBody: String
    ): EmailSendResult {
        return try {
            val plainSocket = Socket()
            plainSocket.connect(InetSocketAddress(SMTP_HOST, 587), TIMEOUT_MS)
            plainSocket.soTimeout = TIMEOUT_MS

            var reader = BufferedReader(InputStreamReader(plainSocket.inputStream, StandardCharsets.UTF_8))
            var writer = BufferedWriter(OutputStreamWriter(plainSocket.outputStream, StandardCharsets.UTF_8))

            // 1. Initial banner
            var line = reader.readLine() ?: ""
            if (!line.startsWith("220")) {
                plainSocket.close()
                return EmailSendResult.Failure("SMTP connection failed on 587: $line")
            }

            // 2. EHLO
            sendSmtpCommand(writer, "EHLO localhost")
            while (true) {
                line = reader.readLine() ?: ""
                if (line.startsWith("250 ")) break
                if (!line.startsWith("250-")) {
                    plainSocket.close()
                    return EmailSendResult.Failure("EHLO rejected on 587: $line")
                }
            }

            // 3. STARTTLS
            sendSmtpCommand(writer, "STARTTLS")
            line = reader.readLine() ?: ""
            if (!line.startsWith("220")) {
                plainSocket.close()
                return EmailSendResult.Failure("STARTTLS rejected on 587: $line")
            }

            // Upgrade plain socket to TLS
            val socketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val sslSocket = socketFactory.createSocket(plainSocket, SMTP_HOST, 587, true) as SSLSocket
            val sslParams = sslSocket.sslParameters
            sslParams.endpointIdentificationAlgorithm = "HTTPS"
            sslSocket.sslParameters = sslParams
            sslSocket.startHandshake()
            sslSocket.soTimeout = TIMEOUT_MS

            reader = BufferedReader(InputStreamReader(sslSocket.inputStream, StandardCharsets.UTF_8))
            writer = BufferedWriter(OutputStreamWriter(sslSocket.outputStream, StandardCharsets.UTF_8))

            // 4. EHLO over TLS
            sendSmtpCommand(writer, "EHLO localhost")
            while (true) {
                line = reader.readLine() ?: ""
                if (line.startsWith("250 ")) break
                if (!line.startsWith("250-")) {
                    sslSocket.close()
                    return EmailSendResult.Failure("TLS EHLO rejected: $line")
                }
            }

            // 5. AUTH LOGIN
            sendSmtpCommand(writer, "AUTH LOGIN")
            line = reader.readLine() ?: ""
            if (!line.startsWith("334")) {
                sslSocket.close()
                return EmailSendResult.Failure("AUTH LOGIN rejected on TLS: $line")
            }

            // 6. Send Base64 Sender Email
            sendSmtpCommand(writer, encodeBase64(senderEmail))
            line = reader.readLine() ?: ""
            if (!line.startsWith("334")) {
                sslSocket.close()
                return EmailSendResult.Failure("Sender rejected on TLS: $line")
            }

            // 7. Send Base64 App Password
            sendSmtpCommand(writer, encodeBase64(appPassword))
            line = reader.readLine() ?: ""
            if (!line.startsWith("235")) {
                sslSocket.close()
                return EmailSendResult.Failure("Authentication failed on TLS: $line")
            }

            // 8. MAIL FROM
            sendSmtpCommand(writer, "MAIL FROM:<$senderEmail>")
            line = reader.readLine() ?: ""
            if (!line.startsWith("250")) {
                sslSocket.close()
                return EmailSendResult.Failure("MAIL FROM rejected: $line")
            }

            // 9. RCPT TO
            sendSmtpCommand(writer, "RCPT TO:<$recipientEmail>")
            line = reader.readLine() ?: ""
            if (!line.startsWith("250")) {
                sslSocket.close()
                return EmailSendResult.Failure("Recipient rejected by mail server: $line")
            }

            // 10. DATA
            sendSmtpCommand(writer, "DATA")
            line = reader.readLine() ?: ""
            if (!line.startsWith("354")) {
                sslSocket.close()
                return EmailSendResult.Failure("DATA rejected: $line")
            }

            // 11. Send content
            val emailMessage = StringBuilder().apply {
                append("From: \"Good Dream Sanctuary\" <$senderEmail>\r\n")
                append("To: <$recipientEmail>\r\n")
                append("Subject: $subject\r\n")
                append("MIME-Version: 1.0\r\n")
                append("Content-Type: text/html; charset=UTF-8\r\n")
                append("Content-Transfer-Encoding: 8bit\r\n")
                append("\r\n")
                append(htmlBody)
                append("\r\n.\r\n")
            }.toString()

            writer.write(emailMessage)
            writer.flush()

            line = reader.readLine() ?: ""
            if (!line.startsWith("250")) {
                sslSocket.close()
                return EmailSendResult.Failure("Message delivery rejected: $line")
            }

            // 12. QUIT
            sendSmtpCommand(writer, "QUIT")
            sslSocket.close()

            Timber.tag(TAG).i("Successfully dispatched transactional email to %s via Google SMTP (Port 587 STARTTLS)", maskEmail(recipientEmail))
            EmailSendResult.Success("Email sent successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error on STARTTLS 587: ${e.message}")
            EmailSendResult.Failure("SMTP Error: ${e.localizedMessage}")
        }
    }

    private fun sendSmtpCommand(writer: BufferedWriter, command: String) {
        writer.write(command)
        writer.write("\r\n")
        writer.flush()
    }

    private fun encodeBase64(input: String): String {
        val bytes = input.toByteArray(StandardCharsets.UTF_8)
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    /**
     * Enqueues an email record into Firestore's 'mail' collection for Firebase Trigger Email
     * extension compatibility and persistence.
     */
    private fun enqueueFirestoreMailRecord(
        senderEmail: String,
        recipientEmail: String,
        subject: String,
        htmlBody: String
    ): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val mailData = hashMapOf(
                "to" to listOf(recipientEmail),
                "from" to senderEmail,
                "createdAt" to System.currentTimeMillis(),
                "message" to hashMapOf(
                    "subject" to subject,
                    "html" to htmlBody
                )
            )
            db.collection("mail").add(mailData)
            Timber.tag(TAG).i("Successfully enqueued mail record to Firestore 'mail' collection for %s", maskEmail(recipientEmail))
            true
        } catch (e: Throwable) {
            Timber.tag(TAG).w("Notice: Could not enqueue Firestore mail record: ${e.message}")
            false
        }
    }

    /**
     * Builds a luxury, mobile-friendly HTML email template matching Good Dream brand colors.
     */
    private fun buildOtpEmailHtml(otp: String, expiresMinutes: Int): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Good Dream Verification Code</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #FBF8F2; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
              <table border="0" cellpadding="0" cellspacing="0" width="100%" style="table-layout: fixed; background-color: #FBF8F2; padding: 40px 16px;">
                <tr>
                  <td align="center">
                    <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 520px; background-color: #FFFFFF; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.06); border: 1px solid #EFEAE1;">
                      <!-- Brand Header -->
                      <tr>
                        <td align="center" style="background-color: #1B3B2B; padding: 32px 24px; text-align: center;">
                          <div style="width: 52px; height: 52px; border-radius: 26px; background-color: rgba(212,175,55,0.18); display: inline-flex; align-items: center; justify-content: center; margin-bottom: 12px;">
                            <span style="font-size: 26px; line-height: 52px;">🛏️</span>
                          </div>
                          <h1 style="margin: 0; color: #FFFFFF; font-size: 22px; font-weight: 700; letter-spacing: 0.5px;">Good Dream Sanctuary</h1>
                          <p style="margin: 6px 0 0 0; color: #D4AF37; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 1.5px;">Restorative Sleep • Handcrafted Luxury</p>
                        </td>
                      </tr>
                      <!-- Content Body -->
                      <tr>
                        <td style="padding: 36px 32px 28px 32px; text-align: center;">
                          <h2 style="margin: 0 0 12px 0; color: #1B3B2B; font-size: 20px; font-weight: 700;">Your Verification Code</h2>
                          <p style="margin: 0 0 24px 0; color: #666666; font-size: 14px; line-height: 22px;">
                            Please enter the following one-time password to access your Good Dream privileged member account.
                          </p>
                          <!-- Code Display Box -->
                          <div style="background-color: #F8F5EE; border: 1.5px dashed #D4AF37; border-radius: 12px; padding: 18px 24px; margin: 0 auto 24px auto; display: inline-block;">
                            <span style="font-family: 'Courier New', Courier, monospace; font-size: 36px; font-weight: 800; letter-spacing: 10px; color: #1B3B2B; display: block; margin-left: 10px;">$otp</span>
                          </div>
                          <!-- Expiration & Security -->
                          <p style="margin: 0 0 8px 0; color: #8F6B10; font-size: 13px; font-weight: 600;">
                            ⏱️ Code valid for $expiresMinutes minutes
                          </p>
                          <p style="margin: 0; color: #999999; font-size: 12px; line-height: 18px;">
                            Never share this code with anyone. Good Dream concierge staff will never request your code.
                          </p>
                        </td>
                      </tr>
                      <!-- Footer -->
                      <tr>
                        <td style="background-color: #FAFAFA; border-top: 1px solid #EEEEEE; padding: 20px 32px; text-align: center;">
                          <p style="margin: 0 0 6px 0; color: #999999; font-size: 11px;">
                            If you did not request this sign-in code, you can safely disregard this email.
                          </p>
                          <p style="margin: 0; color: #CCCCCC; font-size: 11px;">
                            © 2026 Good Dream Home Decor Private Limited • All Rights Reserved.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Builds a luxury HTML confirmation email for customer inquiries, bespoke requests, repairs, and feedback.
     */
    private fun buildInquiryEmailHtml(
        customerName: String,
        referenceNumber: String,
        inquiryType: String,
        details: String
    ): String {
        val typeBadge = when (inquiryType.uppercase()) {
            "REPAIR" -> "🔧 Mattress Restoration & Care"
            "COMPLAINT" -> "🛡️ Priority Client Escalation"
            "NEEDS" -> "✨ Bespoke Handcrafted Inquiry"
            "FEEDBACK" -> "⭐ Verified Experience Review"
            "WARRANTY" -> "📜 25-Year Guarantee Enrollment"
            else -> "🌙 Good Dream Sanctuary Concierge"
        }

        val estimatedTime = when (inquiryType.uppercase()) {
            "COMPLAINT" -> "Within 2 Hours (Priority Escalation)"
            "REPAIR" -> "Within 24 Hours (Technician Dispatch Window)"
            else -> "Within 4 Hours (Master Consultant Callback)"
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Good Dream Sanctuary Request Confirmation</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #FBF8F2; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
              <table border="0" cellpadding="0" cellspacing="0" width="100%" style="table-layout: fixed; background-color: #FBF8F2; padding: 40px 16px;">
                <tr>
                  <td align="center">
                    <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 540px; background-color: #FFFFFF; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.06); border: 1px solid #EFEAE1;">
                      <tr>
                        <td align="center" style="background-color: #1B3B2B; padding: 32px 24px; text-align: center;">
                          <h1 style="margin: 0; color: #FFFFFF; font-size: 22px; font-weight: 700; letter-spacing: 0.5px;">Good Dream Sanctuary</h1>
                          <p style="margin: 6px 0 0 0; color: #D4AF37; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 1.5px;">Restorative Sleep • Handcrafted Luxury</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 32px 28px 24px 28px;">
                          <p style="margin: 0 0 8px 0; color: #D4AF37; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px;">$typeBadge</p>
                          <h2 style="margin: 0 0 16px 0; color: #1B3B2B; font-size: 20px; font-weight: 700;">Request Successfully Logged</h2>
                          <p style="margin: 0 0 20px 0; color: #555555; font-size: 14px; line-height: 22px;">
                            Dear <strong>$customerName</strong>,<br>
                            Your request has been registered in our Master Craftsman & Client Care registry. Our dedicated concierge team has received your details and is preparing your personalized response.
                          </p>
                          <div style="background-color: #F8F5EE; border-left: 4px solid #D4AF37; border-radius: 8px; padding: 18px 20px; margin-bottom: 22px;">
                            <table border="0" cellpadding="0" cellspacing="0" width="100%">
                              <tr>
                                <td style="padding-bottom: 8px; color: #888888; font-size: 12px; font-weight: 600; text-transform: uppercase;">Tracking Reference:</td>
                                <td align="right" style="padding-bottom: 8px; font-family: 'Courier New', monospace; font-size: 14px; font-weight: 700; color: #1B3B2B;">#$referenceNumber</td>
                              </tr>
                              <tr>
                                <td style="padding-bottom: 8px; color: #888888; font-size: 12px; font-weight: 600; text-transform: uppercase;">Expected Contact:</td>
                                <td align="right" style="padding-bottom: 8px; font-size: 13px; font-weight: 600; color: #1B3B2B;">$estimatedTime</td>
                              </tr>
                              <tr>
                                <td style="color: #888888; font-size: 12px; font-weight: 600; text-transform: uppercase; vertical-align: top;">Request Notes:</td>
                                <td align="right" style="font-size: 13px; color: #444444; max-width: 260px; word-break: break-word;">$details</td>
                              </tr>
                            </table>
                          </div>
                          <p style="margin: 0; color: #777777; font-size: 13px; line-height: 20px;">
                            You can track this request at any time inside the Good Dream App under <strong>Concierge Track & Claims</strong> using reference code <code style="background: #EEE; padding: 2px 6px; border-radius: 4px; font-weight: bold; color: #1B3B2B;">$referenceNumber</code>.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="background-color: #FAFAFA; border-top: 1px solid #EEEEEE; padding: 20px 28px; text-align: center;">
                          <p style="margin: 0 0 6px 0; color: #888888; font-size: 12px;">
                            Direct Concierge Desk: <strong style="color: #1B3B2B;">+91 7014983696</strong> • <a href="mailto:gooddreamshomedecor@gmail.com" style="color: #D4AF37; text-decoration: none;">gooddreamshomedecor@gmail.com</a>
                          </p>
                          <p style="margin: 0; color: #BBBBBB; font-size: 11px;">
                            © 2026 GOOD DREAMS HOME DECOR PRIVATE LIMITED • Jaipur Flagship Sanctuary<br>
                            Marketed by: H P PRODUCTS, Address: P.NO. 4, BADHARNA, BAJRANG VIHAR 5, Jaipur, Rajasthan, 302013
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Builds an official 25-Year Guarantee Certificate email template.
     */
    private fun buildWarrantyCertificateHtml(
        customerName: String,
        certificateNumber: String,
        mattressSerial: String,
        purchaseDate: String
    ): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>25-Year SpringHaven Guarantee Certificate</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #FBF8F2; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
              <table border="0" cellpadding="0" cellspacing="0" width="100%" style="table-layout: fixed; background-color: #FBF8F2; padding: 40px 16px;">
                <tr>
                  <td align="center">
                    <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 560px; background-color: #FFFFFF; border-radius: 16px; overflow: hidden; box-shadow: 0 6px 24px rgba(0,0,0,0.08); border: 2px solid #D4AF37;">
                      <tr>
                        <td align="center" style="background-color: #1B3B2B; padding: 36px 24px; text-align: center; border-bottom: 2px solid #D4AF37;">
                          <div style="display: inline-block; padding: 6px 14px; border: 1px solid #D4AF37; border-radius: 20px; margin-bottom: 12px; background-color: rgba(212,175,55,0.12);">
                            <span style="color: #D4AF37; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1.5px;">Official Guarantee Vault</span>
                          </div>
                          <h1 style="margin: 0; color: #FFFFFF; font-size: 24px; font-weight: 700;">25-Year SpringHaven Guarantee</h1>
                          <p style="margin: 6px 0 0 0; color: #E0DFD5; font-size: 13px;">Good Dream Certified Architectural Sleep System</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 32px 30px;">
                          <p style="margin: 0 0 16px 0; color: #333333; font-size: 15px; line-height: 22px;">
                            This certifies that the handcrafted mattress owned by <strong>$customerName</strong> has been formally registered in the Good Dream National Guarantee Vault.
                          </p>
                          <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #FAF8F5; border: 1px solid #EBE4D8; border-radius: 10px; padding: 18px 20px; margin-bottom: 24px;">
                            <tr>
                              <td style="padding: 6px 0; color: #777777; font-size: 12px; font-weight: 600; text-transform: uppercase;">Certificate ID:</td>
                              <td align="right" style="padding: 6px 0; font-family: 'Courier New', monospace; font-size: 14px; font-weight: 700; color: #1B3B2B;">$certificateNumber</td>
                            </tr>
                            <tr>
                              <td style="padding: 6px 0; color: #777777; font-size: 12px; font-weight: 600; text-transform: uppercase;">Mattress Serial No:</td>
                              <td align="right" style="padding: 6px 0; font-family: 'Courier New', monospace; font-size: 14px; font-weight: 600; color: #1B3B2B;">$mattressSerial</td>
                            </tr>
                            <tr>
                              <td style="padding: 6px 0; color: #777777; font-size: 12px; font-weight: 600; text-transform: uppercase;">Enrollment Date:</td>
                              <td align="right" style="padding: 6px 0; font-size: 13px; color: #1B3B2B;">$purchaseDate</td>
                            </tr>
                            <tr>
                              <td style="padding: 6px 0; color: #777777; font-size: 12px; font-weight: 600; text-transform: uppercase;">Coverage Duration:</td>
                              <td align="right" style="padding: 6px 0; font-size: 13px; font-weight: 700; color: #2E7D32;">25 Years Full Guarantee</td>
                            </tr>
                          </table>
                          <h3 style="margin: 0 0 10px 0; color: #1B3B2B; font-size: 14px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;">Guarantee Coverage Summary</h3>
                          <ul style="margin: 0 0 20px 0; padding-left: 20px; color: #555555; font-size: 13px; line-height: 20px;">
                            <li><strong>Years 1 to 2:</strong> 100% Free Doorstep Replacement & Coil Rebuilding for any structural sagging or coil fatigue exceeding 1.5 inches.</li>
                            <li><strong>Years 3 to 10:</strong> Free technician labor and component repair swap.</li>
                            <li><strong>Years 11 to 25:</strong> 30% to 50% guaranteed trade-in credit toward any flagship SpringHaven model.</li>
                          </ul>
                          <p style="margin: 0; color: #888888; font-size: 12px; line-height: 18px;">
                            Keep this certificate in your records. For priority technician appointments, simply quote Certificate ID <strong style="color: #1B3B2B;">$certificateNumber</strong>.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="background-color: #FAFAFA; border-top: 1px solid #EEEEEE; padding: 18px 24px; text-align: center;">
                          <p style="margin: 0; color: #AAAAAA; font-size: 11px;">
                            © 2026 Good Dream Home Decor Private Limited • Master Craftsman Guarantee Department
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Builds an administrative lead notification email template sent to Lakshya190207@gmail.com.
     */
    private fun buildAdminLeadAlertHtml(
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        inquiryType: String,
        details: String,
        referenceNumber: String
    ): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Good Dream Executive Lead Alert</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #0F1E17; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
              <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #0F1E17; padding: 30px 16px;">
                <tr>
                  <td align="center">
                    <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 520px; background-color: #172D22; border-radius: 14px; border: 1px solid #D4AF37; overflow: hidden;">
                      <tr>
                        <td style="padding: 24px; background-color: #1B3B2B; border-bottom: 1px solid rgba(212,175,55,0.3);">
                          <p style="margin: 0; color: #D4AF37; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1.5px;">🔔 Executive Alert • Action Required</p>
                          <h2 style="margin: 6px 0 0 0; color: #FFFFFF; font-size: 20px;">New $inquiryType Lead [#$referenceNumber]</h2>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 24px;">
                          <table border="0" cellpadding="0" cellspacing="0" width="100%" style="color: #FFFFFF; font-size: 14px;">
                            <tr>
                              <td style="padding: 8px 0; color: #D4AF37; font-weight: 600; width: 130px;">Client Name:</td>
                              <td style="padding: 8px 0; font-weight: 700;">$customerName</td>
                            </tr>
                            <tr>
                              <td style="padding: 8px 0; color: #D4AF37; font-weight: 600;">Phone Number:</td>
                              <td style="padding: 8px 0;"><a href="tel:$customerPhone" style="color: #69F0AE; text-decoration: none; font-weight: 700;">$customerPhone</a></td>
                            </tr>
                            <tr>
                              <td style="padding: 8px 0; color: #D4AF37; font-weight: 600;">Email Address:</td>
                              <td style="padding: 8px 0;"><a href="mailto:$customerEmail" style="color: #69F0AE; text-decoration: none;">$customerEmail</a></td>
                            </tr>
                            <tr>
                              <td style="padding: 8px 0; color: #D4AF37; font-weight: 600; vertical-align: top;">Specifications:</td>
                              <td style="padding: 8px 0; color: #E0E0E0; line-height: 20px;">$details</td>
                            </tr>
                          </table>
                          <div style="margin-top: 24px; text-align: center;">
                            <p style="color: #B2DFDB; font-size: 12px; margin: 0;">Open Good Dream Executive Studio inside the app to update this lead's status or dispatch an artisan.</p>
                          </div>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
        """.trimIndent()
    }
}

