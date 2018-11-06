package de.BA.refactoringBot.api;

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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.githubModels.collaboration.invite.Invite;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubSendPullRequest;
import de.BA.refactoringBot.model.githubModels.pullRequest.PullRequest;
import de.BA.refactoringBot.model.githubModels.pullRequest.PullRequests;
import de.BA.refactoringBot.model.githubModels.pullRequestComment.EditComment;
import de.BA.refactoringBot.model.githubModels.pullRequestComment.PullRequestComment;
import de.BA.refactoringBot.model.githubModels.pullRequestComment.PullRequestComments;
import de.BA.refactoringBot.model.githubModels.pullRequestComment.ReplyComment;
import de.BA.refactoringBot.model.githubModels.repository.Repository;

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
	 */
	public Repository checkRepository(String repoName, String repoOwner, String repoService) {
		// Baue URL
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme("https").host("api.github.com")
				.path("/repos/" + repoOwner + "/" + repoName);

		apiUriBuilder.queryParam("access_token", botConfig.getBotToken());

		URI githubURI = apiUriBuilder.build().encode().toUri();
		// Erstelle REST-Template
		RestTemplate rest = new RestTemplate();
		// Baue Header
		HttpHeaders headers = new HttpHeaders();
		headers.set("User-Agent", USER_AGENT);
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		// Sende Anfrage an GitHub-API und hole Json
		try {
			return rest.exchange(githubURI, HttpMethod.GET, entity, Repository.class).getBody();
		} catch (Exception e) {
			return null;
		}

	}

	/**
	 * Diese Methode lädt den Bot zum neu-hinzugeüften Repo ein (falls dieser nicht
	 * schon eingeladen bzw. Collaborator des Repos ist).
	 * 
	 * @param gitConfig
	 * @param repoAdminToken
	 * @return invite
	 */
	public Invite inviteBotToRepo(GitConfiguration gitConfig, String repoAdminToken) {
		// Baue URL
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme("https").host("api.github.com")
				.path("/repos/" + gitConfig.getRepoOwner() + "/" + gitConfig.getRepoName() + "/collaborators/"
						+ botConfig.getBotUsername());

		apiUriBuilder.queryParam("permission", "push");
		apiUriBuilder.queryParam("access_token", repoAdminToken);

		URI githubURI = apiUriBuilder.build().encode().toUri();
		// Erstelle REST-Template
		RestTemplate rest = new RestTemplate();

		// Sende Anfrage an GitHub-API und hole Json
		try {
			return rest.exchange(githubURI, HttpMethod.PUT, null, Invite.class).getBody();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Diese Methode ermöglicht dem Bot eine Einladung in ein Repo anzunehmen.
	 * 
	 * @param savedConfig
	 * @param id
	 */
	public void acceptInvite(GitConfiguration gitConfig, Integer inviteID) {
		// Baue URL
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme("https").host("api.github.com")
				.path("/user/repository_invitations/" + inviteID);

		apiUriBuilder.queryParam("access_token", botConfig.getBotToken());

		URI githubURI = apiUriBuilder.build().encode().toUri();

		// Header-Umweg für PATCH-Requests
		HttpHeaders headers = new HttpHeaders();
		MediaType mediaType = new MediaType("application", "merge-patch+json");
		headers.setContentType(mediaType);

		// Erstellen des REST-Template mit PATCH-Umweg
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		RestTemplate rest = new RestTemplate(requestFactory);

		// Sende Anfrage an GitHub-API und hole Json
		try {
			rest.exchange(githubURI, HttpMethod.PATCH, null, String.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Diese Methode löscht den Bot aus dem Repository wenn die Konfiguration des
	 * Repository gelöscht wird.
	 * 
	 * @param gitConfig
	 * @param repoAdminToken
	 */
	public void deleteBotFromRepository(GitConfiguration gitConfig, String repoAdminToken) {
		// Baue URL
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme("https").host("api.github.com")
				.path("/repos/" + gitConfig.getRepoOwner() + "/" + gitConfig.getRepoName() + "/collaborators/"
						+ botConfig.getBotUsername());

		apiUriBuilder.queryParam("access_token", repoAdminToken);

		// Baue URI
		URI githubURI = apiUriBuilder.build().encode().toUri();

		// Erstelle Rest-Template
		RestTemplate rest = new RestTemplate();
		// Sende Anfrage an GitHub-API und hole Json
		try {
			rest.exchange(githubURI, HttpMethod.DELETE, null, String.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Diese Methode holt alle PullRequests aus dem in der Konfigurationsdatei
	 * eingestellten Repository
	 * 
	 * @return allRequests
	 * @throws URISyntaxException
	 */
	public PullRequests getAllPullRequests(GitConfiguration gitConfig) throws URISyntaxException {
		// Lese API-URI aus Konfiguration aus
		URI configUri = new URI(gitConfig.getRepoApiLink());

		// Baue URI
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme(configUri.getScheme())
				.host(configUri.getHost()).path(configUri.getPath() + "/pulls");

		apiUriBuilder.queryParam("access_token", botConfig.getBotToken());

		URI pullsUri = apiUriBuilder.build().encode().toUri();
		// Erstelle REST-Template
		RestTemplate rest = new RestTemplate();
		// Baue Header
		HttpHeaders headers = new HttpHeaders();
		headers.set("User-Agent", USER_AGENT);
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
		// Sende Anfrage an GitHub-API und hole Json
		String json = rest.exchange(pullsUri, HttpMethod.GET, entity, String.class).getBody();

		// Erstelle Objekt für Ausgabe
		PullRequests allRequests = new PullRequests();

		// Versuche JSON in Objekte umzuwandeln
		try {
			List<PullRequest> requestList = mapper.readValue(json,
					mapper.getTypeFactory().constructCollectionType(List.class, PullRequest.class));
			allRequests.setAllPullRequests(requestList);
			return allRequests;
		} catch (IOException e) {
			e.printStackTrace();
			return allRequests;
		}
	}

	/**
	 * Diese Methode holt alle Kommentare eines PullRequests.
	 * 
	 * @return allRequests
	 */
	public PullRequestComments getAllPullRequestComments(URI commentsUri) {
		// Baue URI
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme(commentsUri.getScheme())
				.host(commentsUri.getHost()).path(commentsUri.getPath());

		apiUriBuilder.queryParam("access_token", botConfig.getBotToken());

		URI githubURI = apiUriBuilder.build().encode().toUri();
		// Erstelle REST-Template
		RestTemplate rest = new RestTemplate();
		// Baue Header
		HttpHeaders headers = new HttpHeaders();
		headers.set("User-Agent", USER_AGENT);
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
		// Sende Anfrage an GitHub-API und hole Json
		String json = rest.exchange(githubURI, HttpMethod.GET, entity, String.class).getBody();

		// Erstelle Objekt für Ausgabe
		PullRequestComments allComments = new PullRequestComments();

		// Versuche JSON in Objekte umzuwandeln
		try {
			List<PullRequestComment> commentList = mapper.readValue(json,
					mapper.getTypeFactory().constructCollectionType(List.class, PullRequestComment.class));
			allComments.setComments(commentList);
			return allComments;
		} catch (IOException e) {
			e.printStackTrace();
			return allComments;
		}
	}

	/**
	 * Diese Methode aktualisiert den aktuellen Pull-Request auf Github. RequestBody
	 * 
	 * @param send
	 * @param gitConfig
	 * @throws URISyntaxException
	 */
	public void updatePullRequest(GithubSendPullRequest send, GitConfiguration gitConfig, Integer requestNumber)
			throws URISyntaxException {
		URI configUri = new URI(gitConfig.getRepoApiLink());

		// Baue URI
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme(configUri.getScheme())
				.host(configUri.getHost()).path(configUri.getPath() + "/pulls/" + requestNumber);

		apiUriBuilder.queryParam("access_token", botConfig.getBotToken());

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
			rest.exchange(pullsUri, HttpMethod.PATCH, new HttpEntity<GithubSendPullRequest>(send), String.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Diese Methode antwortet den an den Bot gerichteten Kommentar.
	 * 
	 * @param comment
	 * @param gitConfig
	 * @param requestNumber
	 * @throws URISyntaxException
	 */
	public void editToBotComment(EditComment comment, GitConfiguration gitConfig, Integer commentNumber)
			throws URISyntaxException {
		URI configUri = new URI(gitConfig.getRepoApiLink());

		// Baue URI
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme(configUri.getScheme())
				.host(configUri.getHost()).path(configUri.getPath() + "/pulls/comments/" + commentNumber);

		apiUriBuilder.queryParam("access_token", botConfig.getBotToken());

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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Diese Methode antwortet den an den Bot gerichteten Kommentar.
	 * 
	 * @param comment
	 * @param gitConfig
	 * @param requestNumber
	 * @throws URISyntaxException
	 */
	public void responseToBotComment(ReplyComment comment, GitConfiguration gitConfig, Integer requestNumber)
			throws URISyntaxException {
		URI configUri = new URI(gitConfig.getRepoApiLink());

		// Baue URI
		UriComponentsBuilder apiUriBuilder = UriComponentsBuilder.newInstance().scheme(configUri.getScheme())
				.host(configUri.getHost()).path(configUri.getPath() + "/pulls/" + requestNumber + "/comments");

		apiUriBuilder.queryParam("access_token", botConfig.getBotToken());

		URI pullsUri = apiUriBuilder.build().encode().toUri();

		RestTemplate rest = new RestTemplate();

		// Sende Anfrage an GitHub-API und hole Json
		try {
			rest.exchange(pullsUri, HttpMethod.POST, new HttpEntity<ReplyComment>(comment), String.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
