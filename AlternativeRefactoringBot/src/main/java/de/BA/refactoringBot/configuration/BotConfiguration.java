package de.BA.refactoringBot.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Diese Klasse wird von der Konfigurationsdatei application.yml verwendet. Dort
 * können die Bot-Eigenschaften eingestellt werden.
 * 
 * @author Stefan Basaric
 *
 */
@Component
@ConfigurationProperties(prefix = "bot")
public class BotConfiguration {

	private String botUsername = "";
	private String botPassword = "";
	private String botToken = "";
	private String botWorkingDirectory = "";

	public String getBotUsername() {
		return botUsername;
	}

	public void setBotUsername(String botUsername) {
		this.botUsername = botUsername;
	}

	public String getBotPassword() {
		return botPassword;
	}

	public void setBotPassword(String botPassword) {
		this.botPassword = botPassword;
	}

	public String getBotToken() {
		return botToken;
	}

	public void setBotToken(String botToken) {
		this.botToken = botToken;
	}

	public String getBotWorkingDirectory() {
		return botWorkingDirectory;
	}

	public void setBotWorkingDirectory(String botWorkingDirectory) {
		this.botWorkingDirectory = botWorkingDirectory;
	}

}
