package de.BA.refactoringBot.api.github;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.githubModels.fork.GithubFork;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubUpdateRequest;
import de.BA.refactoringBot.model.githubModels.pullRequest.PullRequest;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubCreateRequest;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubPullRequests;
import de.BA.refactoringBot.model.githubModels.pullRequestComment.EditComment;
import de.BA.refactoringBot.model.githubModels.pullRequestComment.PullRequestComment;
import de.BA.refactoringBot.model.githubModels.pullRequestComment.PullRequestComments;
import de.BA.refactoringBot.model.githubModels.pullRequestComment.ReplyComment;
import de.BA.refactoringBot.model.githubModels.repository.GithubRepository;
import de.BA.refactoringBot.model.githubModels.user.GithubUser;

/**
 * Diese Klasse holt verschiedenste Daten von einer Git-API. Aktuell wird nur
 * Github unterstützt,
 * 
 * @author Stefan Basaric
 *
 */
@Component
public class GithubDataGrabber {

	@Autowired
	ObjectMapper mapper;
	@Autowired
	BotConfiguration botConfig;

	private final String USER_AGENT = "Mozilla/5.0";

	/**
	 * Diese Methode checkt den Userinput beim Erstellen einer Git-Konfiguration.
	 * Dafür ruft sie den passenden API-Link auf und holt sich die Daten welche für
	 * die Erstellung der Konfiguration nötig sind. Aktuell wird nur Github
	 * unterstützt.
	 * 
	 * @param repoName
	 * @param repoOwner
	 * @param repoService
	 * @return {Repository-File}
	 * @throws Exception
	 */
	public void checkRepository(String repoName, String repoOwner) throws Exception {
		// Baue URL
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme("https").host("api.github.com")
				.path("/repos/" + repoOwner + "/" + repoName);

		URI githubURI = apiUriBuilder.build().encode().toUri();

		// Erstelle REST-Template
		RestTemplate rest = new RestTemplate();
		// Baue Header
		HttpHeaders headers = new HttpHeaders();
		headers.set("User-Agent", USER_AGENT);
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		// Sende Anfrage an GitHub-API und hole Json
		try {
			rest.exchange(githubURI, HttpMethod.GET, entity, GithubRepository.class).getBody();
		} catch (RestClientException e) {
			throw new Exception("Repository existiert auf Github nicht!");
		}
	}

	/**
	 * Diese Methode checkt ob der User bei Github existiert und ob der Token gültig
	 * ist.
	 * 
	 * @param botUsername
	 * @param botToken
	 * @return
	 * @throws Exception
	 */
	public void checkGithubUser(String botUsername, String botToken) throws Exception {
		// Baue URL
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme("https").host("api.github.com")
				.path("/user");

		apiUriBuilder.queryParam("access_token", botToken);

		URI githubURI = apiUriBuilder.build().encode().toUri();

		// Erstelle REST-Template
		RestTemplate rest = new RestTemplate();
		// Baue Header
		HttpHeaders headers = new HttpHeaders();
		headers.set("User-Agent", USER_AGENT);
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		// Sende Anfrage an GitHub-API und hole Json
		GithubUser githubUser = null;
		try {
			githubUser = rest.exchange(githubURI, HttpMethod.GET, entity, GithubUser.class).getBody();
		} catch (RestClientException e) {
			throw new Exception("Bot-Token ungültig!");
		}

		// Prüfe Usernamen
		if (!githubUser.getLogin().equals(botUsername)) {
			throw new Exception("Username des Bots ist ungültig!");
		}
	}

	/**
	 * Diese Methode holt alle PullRequests aus dem in der Konfigurationsdatei
	 * eingestellten Repository
	 * 
	 * @return allRequests
	 * @throws Exception 
	 */
	public GithubPullRequests getAllPullRequests(GitConfiguration gitConfig) throws Exception {
		// Lese API-URI aus Konfiguration aus
		URI configUri = null;
		try {
			configUri = new URI(gitConfig.getRepoApiLink());
		} catch (URISyntaxException u) {
			throw new Exception("Konnte URI aus Konfiguration nicht generieren!");
		}

		// Baue URI
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme(configUri.getScheme())
				.host(configUri.getHost()).path(configUri.getPath() + "/pulls");

		apiUriBuilder.queryParam("access_token", gitConfig.getBotToken());

		URI pullsUri = apiUriBuilder.build().encode().toUri();
		// Erstelle REST-Template
		RestTemplate rest = new RestTemplate();
		// Baue Header
		HttpHeaders headers = new HttpHeaders();
		headers.set("User-Agent", USER_AGENT);
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
		// Sende Anfrage an GitHub-API und hole Json
		String json = null;
		try {
			json = rest.exchange(pullsUri, HttpMethod.GET, entity, String.class).getBody();
		} catch (RestClientException e) {
			throw new Exception("Konnte Pull-Requests nicht von Github holen!");
		}
		

		// Erstelle Objekt für Ausgabe
		GithubPullRequests allRequests = new GithubPullRequests();

		// Versuche JSON in Objekte umzuwandeln
		try {
			List<PullRequest> requestList = mapper.readValue(json,
					mapper.getTypeFactory().constructCollectionType(List.class, PullRequest.class));
			allRequests.setAllPullRequests(requestList);
			return allRequests;
		} catch (IOException e) {
			throw new Exception("Konnte Pull-Request JSON nicht auf Objekt mappen!");
		}
	}

	/**
	 * Diese Methode holt alle Kommentare eines PullRequests.
	 * 
	 * @return allRequests
	 * @throws Exception
	 */
	public PullRequestComments getAllPullRequestComments(URI commentsUri, GitConfiguration gitConfig) throws Exception {
		// Baue URI
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme(commentsUri.getScheme())
				.host(commentsUri.getHost()).path(commentsUri.getPath());

		apiUriBuilder.queryParam("access_token", gitConfig.getBotToken());

		URI githubURI = apiUriBuilder.build().encode().toUri();
		// Erstelle REST-Template
		RestTemplate rest = new RestTemplate();
		// Baue Header
		HttpHeaders headers = new HttpHeaders();
		headers.set("User-Agent", USER_AGENT);
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
		// Sende Anfrage an GitHub-API und hole Json
		String json = null;
		try {
			json = rest.exchange(githubURI, HttpMethod.GET, entity, String.class).getBody();
		} catch (RestClientException r) {
			throw new Exception("Konnte Pull-Request-Kommentare nicht holen!");
		}

		// Erstelle Objekt für Ausgabe
		PullRequestComments allComments = new PullRequestComments();

		// Versuche JSON in Objekte umzuwandeln
		try {
			List<PullRequestComment> commentList = mapper.readValue(json,
					mapper.getTypeFactory().constructCollectionType(List.class, PullRequestComment.class));
			allComments.setComments(commentList);
			return allComments;
		} catch (IOException e) {
			throw new Exception("Konnte Kommentar-JSON nicht auf Objekt mappen!");
		}
	}

	/**
	 * Diese Methode aktualisiert den aktuellen Pull-Request auf Github. RequestBody
	 * 
	 * @param send
	 * @param gitConfig
	 * @throws Exception
	 */
	public void updatePullRequest(GithubUpdateRequest send, GitConfiguration gitConfig, Integer requestNumber)
			throws Exception {
		// Baue URI
		URI configUri = null;
		try {
			configUri = new URI(gitConfig.getRepoApiLink());
		} catch (URISyntaxException u) {
			throw new Exception("Konnte URI aus Konfiguration nicht generieren!");
		}

		// Baue URI mit Parametern
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme(configUri.getScheme())
				.host(configUri.getHost()).path(configUri.getPath() + "/pulls/" + requestNumber);

		apiUriBuilder.queryParam("access_token", gitConfig.getBotToken());

		URI pullsUri = apiUriBuilder.build().encode().toUri();

		// Header-Umweg für PATCH-Requests
		HttpHeaders headers = new HttpHeaders();
		MediaType mediaType = new MediaType("application", "merge-patch+json");
		headers.setContentType(mediaType);

		// Erstellen des REST-Template mit PATCH-Umweg
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		RestTemplate rest = new RestTemplate(requestFactory);

		// Sende Anfrage an GitHub-API und hole Json
		try {
			rest.exchange(pullsUri, HttpMethod.PATCH, new HttpEntity<GithubUpdateRequest>(send), String.class);
		} catch (RestClientException e) {
			throw new Exception("Konnte Pull-Request nicht aktualisieren!");
		}
	}

	/**
	 * Diese Methode antwortet den an den Bot gerichteten Kommentar.
	 * 
	 * @param comment
	 * @param gitConfig
	 * @param requestNumber
	 * @throws Exception 
	 */
	public void editToBotComment(EditComment comment, GitConfiguration gitConfig, Integer commentNumber)
			throws Exception {
		// Baue URI aus Konfiguration
		URI configUri = null;
		try {
			configUri = new URI(gitConfig.getRepoApiLink());
		} catch (URISyntaxException u) {
			throw new Exception("Konnte URI aus Konfiguration nicht generieren!");
		}

		// Baue URI
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme(configUri.getScheme())
				.host(configUri.getHost()).path(configUri.getPath() + "/pulls/comments/" + commentNumber);

		apiUriBuilder.queryParam("access_token", gitConfig.getBotToken());

		URI pullsUri = apiUriBuilder.build().encode().toUri();

		// Header-Umweg für PATCH-Requests
		HttpHeaders headers = new HttpHeaders();
		MediaType mediaType = new MediaType("application", "merge-patch+json");
		headers.setContentType(mediaType);

		// Erstellen des REST-Template mit PATCH-Umweg
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		RestTemplate rest = new RestTemplate(requestFactory);

		// Sende Anfrage an GitHub-API und hole Json
		try {
			rest.exchange(pullsUri, HttpMethod.PATCH, new HttpEntity<EditComment>(comment), String.class);
		} catch (RestClientException e) {
			throw new Exception("Konnte Kommentar auf Github nicht bearbeiten!");
		}
	}

	/**
	 * Diese Methode antwortet den an den Bot gerichteten Kommentar.
	 * 
	 * @param comment
	 * @param gitConfig
	 * @param requestNumber
	 * @throws Exception 
	 */
	public void responseToBotComment(ReplyComment comment, GitConfiguration gitConfig, Integer requestNumber)
			throws Exception {
		// Lese URI aus Konfiguration
		URI configUri = null;
		try {
			configUri = new URI(gitConfig.getRepoApiLink());
		} catch (URISyntaxException u) {
			throw new Exception("Konnte URI aus Konfiguration nicht generieren!");
		}

		// Baue URI
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme(configUri.getScheme())
				.host(configUri.getHost()).path(configUri.getPath() + "/pulls/" + requestNumber + "/comments");

		apiUriBuilder.queryParam("access_token", gitConfig.getBotToken());

		URI pullsUri = apiUriBuilder.build().encode().toUri();

		RestTemplate rest = new RestTemplate();

		// Sende Anfrage an GitHub-API und hole Json
		try {
			rest.exchange(pullsUri, HttpMethod.POST, new HttpEntity<ReplyComment>(comment), String.class);
		} catch (RestClientException e) {
			throw new Exception("Konnte auf Kommentar in Github nicht antworten");
		}
	}

	/**
	 * Diese Methode erstellt einen PullRequest auf Github im ParentRepository
	 * 
	 * @param request
	 * @param gitConfig
	 * @throws Exception
	 */
	public void createRequest(GithubCreateRequest request, GitConfiguration gitConfig) throws Exception {

		// Lese URI aus Konfiguration
		URI configUri = null;
		try {
			configUri = new URI(gitConfig.getRepoApiLink());
		} catch (URISyntaxException u) {
			throw new Exception("Konnte URI aus Konfiguration nicht generieren!");
		}

		// Baue URI
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme(configUri.getScheme())
				.host(configUri.getHost()).path(configUri.getPath() + "/pulls");

		apiUriBuilder.queryParam("access_token", gitConfig.getBotToken());

		URI pullsUri = apiUriBuilder.build().encode().toUri();

		RestTemplate rest = new RestTemplate();

		// Sende Anfrage an GitHub-API und hole Json
		try {
			rest.exchange(pullsUri, HttpMethod.POST, new HttpEntity<GithubCreateRequest>(request), String.class)
					.getBody();
		} catch (RestClientException r) {
			throw new Exception("Konnte Pull-Request auf Github nicht erstellen!");
		}
	}

	/**
	 * Diese Methode erstellt für den Bot einen Fork auf Github mit dem vom Nutzer
	 * angegebenen Daten
	 * 
	 * @param gitConfig
	 * @return
	 * @throws Exception
	 */
	public void createFork(GitConfiguration gitConfig) throws Exception {

		// Deklarire URI
		URI configUri = null;

		// Versuche URI zu erstellen anhand der Konfiguration
		try {
			configUri = new URI(gitConfig.getRepoApiLink());
		} catch (URISyntaxException u) {
			throw new Exception("Konnte URI aus Konfiguration nicht generieren!");
		}

		// Baue URI mit Parametern
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme(configUri.getScheme())
				.host(configUri.getHost()).path(configUri.getPath() + "/forks");

		apiUriBuilder.queryParam("access_token", gitConfig.getBotToken());

		URI forksUri = apiUriBuilder.build().encode().toUri();

		RestTemplate rest = new RestTemplate();

		// Versucge Fork auf Github zu erstellen
		try {
			rest.exchange(forksUri, HttpMethod.POST, null, GithubFork.class).getBody();
		} catch (RestClientException r) {
			throw new Exception("Fork konnte nicht erstellt werden!");
		}
	}

	/**
	 * Diese Methode löscht das Fork-Repository des Bots wenn die passende
	 * Konfiguration gelöscht wurde.
	 * 
	 * @param gitConfiguration
	 * @throws Exception
	 */
	public void deleteRepository(GitConfiguration gitConfig) throws Exception {

		// Deklariere URI
		URI configUri = null;

		try {
			configUri = new URI(gitConfig.getForkApiLink());
		} catch (URISyntaxException u) {
			throw new Exception("Konnte URI aus Konfiguration nicht generieren!");
		}

		// Baue URI mit Parametern
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme(configUri.getScheme())
				.host(configUri.getHost()).path(configUri.getPath());

		apiUriBuilder.queryParam("access_token", gitConfig.getBotToken());

		URI repoUri = apiUriBuilder.build().encode().toUri();

		RestTemplate rest = new RestTemplate();

		// Sende Anfrage an GitHub-API und hole Json
		try {
			rest.exchange(repoUri, HttpMethod.DELETE, null, String.class);
		} catch (RestClientException r) {
			throw new Exception("Konnte Repository/Fork nicht löschen!");
		}
	}

}
