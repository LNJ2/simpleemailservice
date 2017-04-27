package com.searchmetrics.simpleEmailService.dto;

import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 */
public class SendEmailRequest {
    private final static String FROM_EMAIL = "linus.jahn@searchmetrics.com";
    private final static String EMAIL_REPLY_TO = "noreply@dev.searchmetrics.space";

    private final List<String> toEmailList;
    private final String subject;
    private final String messageBody;
    private final Optional<List<Attachment>> optionalAttachmentList;

    public static class Attachment {
        private final String name;
        private final String mimeType;
        private final String data;

        @JsonCreator
        public Attachment(
                @JsonProperty String name,
                @JsonProperty String mimeType,
                @JsonProperty String data
        ) {
            this.name = name;
            this.mimeType = mimeType;
            this.data = data;
        }

        @JsonProperty
        public final String getName() {
            return name;
        }

        @JsonProperty
        public final String getMimeType() {
            return mimeType;
        }

        @JsonProperty
        public final String getData() {
            return data;
        }
    }

    @JsonCreator
    public SendEmailRequest(
            @JsonProperty("toEmailList") List<String> toEmailList,
            @JsonProperty("subject") String subject,
            @JsonProperty("messageBody") String messageBody,
            @JsonProperty("optionalAttachmentList") Optional<List<Attachment>> optionalAttachmentList
    ) {
        this.toEmailList = checkNotNull(toEmailList);
        this.subject = checkNotNull(subject);
        this.messageBody = checkNotNull(messageBody);
        this.optionalAttachmentList = optionalAttachmentList;

        if (toEmailList.size() < 1) {
            throw new IllegalArgumentException("toEmailList must contain 1 or more values");
        }
    }

//    @JsonCreator
//    public SendEmailRequest(
//            @JsonProperty String toEmail,
//            @JsonProperty String subject,
//            @JsonProperty String messageBody,
//            @JsonProperty Optional<List<Attachment>> optionalAttachmentList
//    ) {
//        List<String> newList = new ArrayList<>();
//        newList.add(checkNotNull(toEmail));
//        this.toEmailList = newList;
//        this.subject = checkNotNull(subject);
//        this.messageBody = checkNotNull(messageBody);
//        this.optionalAttachmentList = optionalAttachmentList;
//
//        if (toEmailList.size() < 1) {
//            throw new IllegalArgumentException("toEmailList must contain 1 or more values");
//        }
//    }

    @JsonProperty
    public List<String> getToEmailList() {
        return toEmailList;
    }

    @JsonProperty
    public String getSubject() {
        return subject;
    }

    @JsonProperty
    public String getMessageBody() {
        return messageBody;
    }

    public SendRawEmailRequest toAWSRawEmailRequest() throws RuntimeException {
        try {
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session);

            message.setSubject(subject, "UTF-8");
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setReplyTo(new Address[]{new InternetAddress(EMAIL_REPLY_TO)});

            Iterator<String> iterator = toEmailList.iterator();
            while (iterator.hasNext()) {
                message.addRecipient(
                        Message.RecipientType.TO, // recipient type To
                        new InternetAddress(iterator.next())
                );
            }

            // Cover wrap
            MimeBodyPart wrap = new MimeBodyPart();

            // Alternative TEXT/HTML content
            MimeMultipart cover = new MimeMultipart("alternative");
            MimeBodyPart html = new MimeBodyPart();
            cover.addBodyPart(html);

            wrap.setContent(cover);

            MimeMultipart content = new MimeMultipart("related");
            message.setContent(content);
            content.addBodyPart(wrap);

            html.setContent("<html><body><h1>HTML</h1>\n" + messageBody + "</body></html>", "text/html");


            //
            // Add all existing attachments
            //

            if (optionalAttachmentList.isPresent()) {
                for (Attachment localAttachment : optionalAttachmentList.get()) {
                    String id = UUID.randomUUID().toString();

                    MimeBodyPart attachment = new MimeBodyPart();

                    ByteArrayDataSource bds = new ByteArrayDataSource(localAttachment.getData(), localAttachment.getMimeType());
                    attachment.setDataHandler(new DataHandler(bds));
                    attachment.setHeader("Content-ID", "<" + id + ">");
                    attachment.setFileName(localAttachment.getName());

                    content.addBodyPart(attachment);
                }
            }

            // print raw email to the console
            message.writeTo(System.out);


            //
            // Create the raw message & email request
            //

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);
            RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

            SendRawEmailRequest emailRequest = new SendRawEmailRequest(rawMessage);

            return emailRequest;
        } catch (MessagingException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
