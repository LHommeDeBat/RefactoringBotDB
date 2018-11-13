package de.BA.refactoringBot.scheduling;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Diese Klasse führt geschedulte Funktionen aus.
 * 
 * @author Stefan Basaric
 *
 */
@Component
public class ScheduledOperations {
	
	@Value("${server.port}")
	Integer port;

	/**
	 * Diese Methode startet auf Windows die SwaggerUI im Standardbrowser beim Start
	 * der Anwendung.
	 */
	@PostConstruct
	public void startSwaggerUI() {
		Runtime runtime = Runtime.getRuntime();
		String url = "http://localhost:" + port + "/swagger-ui.html#";
		try {
			runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
		} catch (IOException e) {
			System.err.println("Konnte SwaggerUI nicht automatisch im Browser starten!");
		}
	}

}
