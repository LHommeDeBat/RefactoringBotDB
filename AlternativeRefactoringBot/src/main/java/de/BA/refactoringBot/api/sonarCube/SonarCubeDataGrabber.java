package de.BA.refactoringBot.api.sonarCube;

import java.net.URI;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import de.BA.refactoringBot.model.sonarQube.SonarCubeIssues;

/**
 * Diese Klasse holt verschiedenste Daten von der SonarCube-API.
 * 
 * @author Stefan Basaric
 *
 */
@Component
public class SonarCubeDataGrabber {

	private final String USER_AGENT = "Mozilla/5.0";

	/**
	 * Diese Methode holt die SonarCube-Issues eines Projekts.
	 * 
	 * @param sonarCubeProjectKey
	 * @return allIssues
	 * @throws Exception
	 */
	public SonarCubeIssues getIssues(String sonarCubeProjectKey) throws Exception {
		// Baue URL
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme("https")
				.host("sonarcloud.io").path("api/issues/search");
		
		apiUriBuilder.queryParam("componentRoots", sonarCubeProjectKey);
		apiUriBuilder.queryParam("statuses", "OPEN,REOPENED");

		URI sonarCubeURI = apiUriBuilder.build().encode().toUri();

		// Erstelle REST-Template
		RestTemplate rest = new RestTemplate();
		// Baue Header
		HttpHeaders headers = new HttpHeaders();
		headers.set("User-Agent", USER_AGENT);
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		// Sende Anfrage an GitHub-API und hole Json
		try {
			return rest.exchange(sonarCubeURI, HttpMethod.GET, entity, SonarCubeIssues.class).getBody();
		} catch (RestClientException e) {
			throw new Exception("SonarCube API nicht erreichbar!");
		}
	}
}
