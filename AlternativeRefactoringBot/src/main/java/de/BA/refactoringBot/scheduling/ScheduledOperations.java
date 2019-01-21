package de.BA.refactoringBot.scheduling;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Diese Klasse führt geschedulde Funktionen aus.
 * 
 * @author Stefan Basaric
 *
 */
@Component
public class ScheduledOperations {

	@Value("${server.port}")
	Integer port;

	/**
	 * Diese Methode versucht auf allen gängigen OS-Systemen die SwaggerUI im Standardbrowser beim Start
	 * der Anwendung zum Starten.
	 */
	@PostConstruct
	public void startSwaggerUI() {
		// Starte Runtime
		Runtime runtime = Runtime.getRuntime();
		// Hole URL
		String url = "http://localhost:" + port + "/swagger-ui.html#";
		// Prüfe OS-System
		String os = System.getProperty("os.name").toLowerCase();
		try {
			// Falls OS = Windows
			if (os.indexOf("win") >= 0) {
				runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
			}
			// Falls OS = MAX OS
			if (os.indexOf("mac") >= 0) {
				runtime.exec("open " + url);
			}
			// Falls OS = Linux
			if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
				runtime.exec("xdg-open " + url);
			}
		} catch (IOException e) {
			System.err.println("Konnte SwaggerUI nicht automatisch im Browser starten!");
		}
	}

}
