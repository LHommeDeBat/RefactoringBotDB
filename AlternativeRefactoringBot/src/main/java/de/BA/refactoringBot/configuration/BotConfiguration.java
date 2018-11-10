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

	private String botWorkingDirectory = "";
	private String botRefactoringDirectory = "";

	public String getBotWorkingDirectory() {
		return botWorkingDirectory;
	}

	public void setBotWorkingDirectory(String botWorkingDirectory) {
		this.botWorkingDirectory = botWorkingDirectory;
	}

	public String getBotRefactoringDirectory() {
		return botRefactoringDirectory;
	}

	public void setBotRefactoringDirectory(String botRefactoringDirectory) {
		this.botRefactoringDirectory = botRefactoringDirectory;
	}

}
