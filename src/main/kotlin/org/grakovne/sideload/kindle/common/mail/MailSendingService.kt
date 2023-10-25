package org.grakovne.sideload.kindle.common.mail

import arrow.core.Either
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.io.File


@Service
class MailSendingService(
    private val properties: EmailProperties,
    private val mailSender: JavaMailSender
) {

    fun sendFile(address: String, files: List<File>) = try {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true)
        helper.setTo(address)

        helper.setFrom(properties.from)
        helper.setSubject(properties.subject)
        helper.setText(properties.text)

        files.map { helper.addAttachment(it.name, it) }

        mailSender.send(message).let { Either.Right(Unit) }
    } catch (ex: MailException) {
        Either.Left(MailError.DELIVERY_ERROR)
    }
}