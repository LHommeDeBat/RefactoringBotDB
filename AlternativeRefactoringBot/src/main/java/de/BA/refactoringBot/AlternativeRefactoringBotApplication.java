package de.BA.refactoringBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Diese Klasse startet Spring und damit auch den Webservice.
 * 
 * @author Stefan Basaric
 *
 */
@SpringBootApplication
public class AlternativeRefactoringBotApplication {

	/**
	 * Diese Methode startet die Anwendung. Dabei wird auch Spring mit allen
	 * Funktionen gestartet und automatisch konfiguriert.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(AlternativeRefactoringBotApplication.class, args);
	}
}
