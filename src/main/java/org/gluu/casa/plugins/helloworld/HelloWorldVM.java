package org.gluu.casa.plugins.helloworld;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.gluu.casa.misc.Utils;
import org.gluu.casa.service.IPersistenceService;
import org.gluu.casa.service.ISessionContext;
import org.gluu.util.security.StringEncrypter.EncryptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;

/**
 * A ZK
 * <a href="http://books.zkoss.org/zk-mvvm-book/8.0/viewmodel/index.html" target
 * ="_blank">ViewModel</a> that acts as the "controller" of page
 * <code>index.zul</code> in this sample plugin. See <code>viewModel</code>
 * attribute of panel component of <code>index.zul</code>.
 * 
 * @author jgomer
 */
public class HelloWorldVM {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private String message;
	private String organizationName;
	private IPersistenceService persistenceService;
	private ISessionContext sessionContext;

	/**
	 * Getter of private class field <code>organizationName</code>.
	 * 
	 * @return A string with the value of the organization name found in your Gluu
	 *         installation. Find this value in Gluu Server oxTrust GUI at
	 *         "Configuration" &gt; "Organization configuration"
	 */
	public String getOrganizationName() {
		return organizationName;
	}

	/**
	 * Getter of private class field <code>message</code>.
	 * 
	 * @return A string value
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Setter of private class field <code>message</code>.
	 * 
	 * @param message A string with the contents typed in text box of page index.zul
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Initialization method for this ViewModel.
	 */
	@Init
	public void init() {
		logger.info("Hello World ViewModel inited");
		persistenceService = Utils.managedBean(IPersistenceService.class);

		sessionContext = Utils.managedBean(ISessionContext.class);
		if (sessionContext.getLoggedUser() != null) {
			logger.info("There is a user logged in!");
		}
		sendEmailWithOTP("madhumitas.work@gmail.com", "Hello World from Gluu", "Hey from Gluu");
	}

	/**
	 * The method called when the button on page <code>index.zul</code> is pressed.
	 * It sets the value for <code>organizationName</code>.
	 */
	@NotifyChange("organizationName")
	public void loadOrgName() {
		logger.debug("You typed {}", message);
		organizationName = persistenceService.getOrganization().getDisplayName();
	}

	public boolean sendEmailWithOTP(String emailId, String subject, String body) {
		logger.debug("sendEmailWithOTP");
		SmtpConfiguration smtpConfiguration = getConfiguration().getSmtpConfiguration();
		logger.debug("SmtpConfiguration - " + smtpConfiguration.getHost());
		if (smtpConfiguration == null) {
			logger.error("Failed to send email. SMTP settings not found. Please configure SMTP settings in oxTrust");
			return false;
		}
		Properties prop = new Properties();
		prop.put("mail.smtp.auth", true);
		prop.put("mail.smtp.starttls.enable", smtpConfiguration.isRequiresSsl());
		prop.put("mail.smtp.host", smtpConfiguration.getHost());
		prop.put("mail.smtp.port", smtpConfiguration.getPort());
		prop.put("mail.smtp.ssl.trust", smtpConfiguration.isServerTrust());

		Session session = Session.getInstance(prop, new Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(smtpConfiguration.getUserName(),
						decrypt(smtpConfiguration.getPassword()));
			}
		});

		Message message = new MimeMessage(session);
		logger.debug("Session created");
		try {
			message.setFrom(new InternetAddress(smtpConfiguration.getFromEmailAddress()));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailId));
			message.setSubject(subject);

			MimeBodyPart mimeBodyPart = new MimeBodyPart();
			mimeBodyPart.setContent(body, "text/html; charset=utf-8");

			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(mimeBodyPart);
			logger.debug("Before sending");
			message.setContent(multipart);

			Transport.send(message);
			logger.debug("after sending");
		} catch (MessagingException e) {
			logger.error("Failed to send OTP: " + e.getMessage());
			return false;
		}

		return true;
	}

	public GluuConfiguration getConfiguration() {

		GluuConfiguration result = persistenceService.find(GluuConfiguration.class, "ou=configuration,o=gluu", null)
				.get(0);
		return result;
	}

	public String encrypt(String password) {

		try {
			return Utils.stringEncrypter().encrypt(password);
		} catch (EncryptionException ex) {
			logger.error("Failed to encrypt SMTP password: ", ex);
			return null;
		}
	}

	public String decrypt(String password) {
		try {
			return Utils.stringEncrypter().decrypt(password);
		} catch (EncryptionException e) {
			logger.error("Unable to decrypt :" + e.getMessage());
			return null;
		}
	}
}
