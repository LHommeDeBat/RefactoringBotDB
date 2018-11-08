package de.BA.refactoringBot.controller.github;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.BA.refactoringBot.api.github.GithubDataGrabber;
import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubUpdateRequest;
import de.BA.refactoringBot.model.githubModels.pullRequest.PullRequest;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubCreateRequest;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubPullRequests;
import de.BA.refactoringBot.model.githubModels.pullRequestComment.EditComment;
import de.BA.refactoringBot.model.githubModels.pullRequestComment.PullRequestComment;
import de.BA.refactoringBot.model.githubModels.pullRequestComment.PullRequestComments;
import de.BA.refactoringBot.model.githubModels.pullRequestComment.ReplyComment;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequest;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequests;
import de.BA.refactoringBot.model.outputModel.myPullRequestComment.BotPullRequestComment;
import de.BA.refactoringBot.model.outputModel.myPullRequestComment.BotPullRequestComments;

/**
 * Diese Klasse ist dafür zuständig, GitHub Objekte in eigene Objekte
 * umzuwandeln.
 * 
 * @author Stefan Basaric
 *
 */
@Component
public class GithubObjectTranslator {

	@Autowired
	GithubDataGrabber grabber;
	@Autowired
	BotConfiguration botConfig;

	/**
	 * Diese Methode ertellt eine Konfiguration eines Repos.
	 * 
	 * @param repo
	 * @param repoService
	 * @return
	 */
	public GitConfiguration createConfiguration(String repoName, String repoOwner, String botUsername,
			String botPassword, String botToken, String repoService) {
		// Erstelle Konfiguration
		GitConfiguration config = new GitConfiguration();

		// Erstelle ApiLink (aktuell nur Github)
		config.setRepoApiLink("https://api.github.com/repos/" + repoOwner + "/" + repoName);
		config.setRepoGitLink("https://github.com/" + repoOwner + "/" + repoName + ".git");
		config.setForkApiLink("https://api.github.com/repos/" + botUsername + "/" + repoName);
		config.setForkGitLink("https://github.com/" + botUsername + "/" + repoName + ".git");
		config.setRepoName(repoName);
		config.setRepoOwner(repoOwner);
		config.setRepoService(repoService.toLowerCase());
		config.setBotName(botUsername);
		config.setBotPassword(botPassword);
		config.setBotToken(botToken);

		// Gebe Konfiguration zurück
		return config;
	}

	/**
	 * Diese Methode nimmt eine Liste von GitHub-PullRequests und übersetzt sie in
	 * das eigene Java-Objekt.
	 * 
	 * @param githubRequests
	 * @return translatedRequests
	 */
	public BotPullRequests translateRequests(GithubPullRequests githubRequests, GitConfiguration gitConfig) {
		// Erstelle Liste von übersetzten Objekten
		BotPullRequests translatedRequests = new BotPullRequests();

		// Gehe alle Github-Requests durch
		for (PullRequest githubRequest : githubRequests.getAllPullRequests()) {
			// Erstelle für jeden ein PullRequest
			BotPullRequest pullRequest = new BotPullRequest();

			// Fülle es mit wichtigsten Daten
			pullRequest.setRequestName(githubRequest.getTitle());
			pullRequest.setRequestDescription(githubRequest.getBody());
			pullRequest.setRequestNumber(githubRequest.getNumber());
			pullRequest.setRequestStatus(githubRequest.getState());
			pullRequest.setCreatorName(githubRequest.getUser().getLogin());
			pullRequest.setDateCreated(githubRequest.getCreatedAt());
			pullRequest.setDateUpdated(githubRequest.getUpdatedAt());
			pullRequest.setBranchName(githubRequest.getHead().getRef());
			pullRequest.setBranchCreator(githubRequest.getHead().getUser().getLogin());
			pullRequest.setMergeBranchName(githubRequest.getBase().getRef());
			pullRequest.setRepoName(githubRequest.getBase().getRepo().getFullName());

			// Versuche die Kommentare des PullRequests zu holen und zu übersetzen
			try {
				URI commentUri = new URI(githubRequest.getReviewCommentsUrl());
				PullRequestComments githubComments = grabber.getAllPullRequestComments(commentUri, gitConfig);
				BotPullRequestComments comments = translatePullRequestComments(githubComments);
				pullRequest.setAllComments(comments.getComments());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

			// Füge erstellten Request zur Liste hinzu
			translatedRequests.addPullRequest(pullRequest);
		}

		// Gebe übersetzte PullRequests zurück
		return translatedRequests;
	}

	/**
	 * Diese Methode wandelt eine Liste von GitHub Pull-Request-Kommentaren in eine
	 * Liste von Kommentaren des eigenen Models um.
	 * 
	 * @param githubComments
	 * @return translatedComments
	 */
	public BotPullRequestComments translatePullRequestComments(PullRequestComments githubComments) {
		// Erstelle Ausgabeliste
		BotPullRequestComments translatedComments = new BotPullRequestComments();

		// Gehe alle GitHub-Kommentare durch
		for (PullRequestComment githubComment : githubComments.getComments()) {
			// Erstelle neuen Kommentar mit eigenem Model
			BotPullRequestComment translatedComment = new BotPullRequestComment();

			// Fülle eigenes Model mit Daten
			translatedComment.setCommentID(githubComment.getId());
			translatedComment.setFilepath(githubComment.getPath());
			translatedComment.setUsername(githubComment.getUser().getLogin());
			translatedComment.setCommentBody(githubComment.getBody());
			translatedComment.setPosition(calculateTrueCommentPosition(githubComment.getBody()));

			// Füge Kommentar zur Ergebnisliste hinzu
			translatedComments.addComment(translatedComment);
		}

		// Gebe übersetzte Kommentarliste zurück
		return translatedComments;
	}

	/**
	 * Diese Methode erstellt einen Pull-Request im Github-Format damit der Bot
	 * diesen Pull-Request nach erfolgreichem Refactoring auf Github aktualisieren
	 * kann.
	 * 
	 * @param refactoredRequest
	 * @return sendRequest
	 */
	public GithubUpdateRequest makeUpdateRequest(BotPullRequest refactoredRequest, GitConfiguration gitConfig) {
		// Erstelle Request
		GithubUpdateRequest sendRequest = new GithubUpdateRequest();

		// Erstelle heutiges Datum
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		Date now = new Date();
		String date = sdf.format(now);

		// Fülle Request mit Daten
		sendRequest.setTitle("Bot Pull-Request");
		sendRequest.setBody("Von " + gitConfig.getBotName() + " am " + date + " aktualisiert.");
		sendRequest.setMaintainer_can_modify(true);

		// Gebe Request zurück
		return sendRequest;
	}

	/**
	 * Diese Methode erstellt einen Pull-Request im Github-Format damit der Bot
	 * diesen Pull-Request auf Github erstellen kann.
	 * 
	 * @param gitConfig
	 * @return createRequest
	 */
	public GithubCreateRequest makeCreateRequest(BotPullRequest refactoredRequest, GitConfiguration gitConfig) {
		// Erstelle Request
		GithubCreateRequest createRequest = new GithubCreateRequest();

		// Erstelle heutiges Datum
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		Date now = new Date();
		String date = sdf.format(now);

		// Fülle Request mit Daten
		createRequest.setTitle("Bot Pull-Request Refactoring für PullRequest #" + refactoredRequest.getRequestNumber());
		createRequest.setBody("Von " + gitConfig.getBotName() + " am " + date + " erstellt.");
		createRequest.setHead(gitConfig.getBotName() + ":" + refactoredRequest.getBranchName());
		createRequest.setBase(refactoredRequest.getBranchName());
		createRequest.setMaintainer_can_modify(true);

		// Gebe Request zurück
		return createRequest;
	}

	/**
	 * Diese Methode holt die tatsächliche Kommentarposition in der Datei.
	 * 
	 * @param githubPosition
	 * @param diffHunk
	 * @return truePosition
	 */
	public Integer calculateTrueCommentPosition(String commentBody) {

		// Init Code-Position
		int codePosition = -1;

		// Splitte Kommentar nach Leerzeichen
		String[] splitString = commentBody.split(" ");

		// Durchsuche Kommentar
		for (int i = 0; i < splitString.length - 1; i++) {
			// Falls Zeilenangabe gefunden
			if (splitString[i].equals("LINE")) {
				// Versuche in Unteger umzuwandlen
				try {
					codePosition = Integer.parseInt(splitString[i + 1]);
				} catch (Exception e) {
					e.printStackTrace();
					codePosition = -1;
				}
			}
		}

		// Gebe tatsächliche Position zurück
		return codePosition;
	}

	/**
	 * 
	 * @param replyTo
	 * @return comment
	 */
	public ReplyComment createReplyComment(BotPullRequestComment replyTo, GitConfiguration gitConfig) {
		// Erstelle Kommentar
		ReplyComment comment = new ReplyComment();
		// Fülle mit Daten
		comment.setIn_reply_to(replyTo.getCommentID());

		// Erstelle heutiges Datum
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		Date now = new Date();
		String date = sdf.format(now);

		// Erstelle Antwort
		comment.setBody("Refactored von " + gitConfig.getBotName() + " am " + date);

		// Gebe Kommentar zurück
		return comment;
	}

	/**
	 * Diese Methode erstellt einen bearbeiteten Kommentar welcher genutzt wird um
	 * einen an den Bot gerichteten Kommentar nach Abarbeitung der Aufgabe als
	 * bearbeitet zu markieren.
	 * 
	 * @param toEdit
	 * @return
	 */
	public EditComment editComment(BotPullRequestComment toEdit) {
		// Erstelle EditComment
		EditComment comment = new EditComment();

		// Aktualisiere Kommentar
		String editedBody = "DONE " + toEdit.getCommentBody();
		comment.setBody(editedBody);

		// Gebe Kommentar zurück
		return comment;
	}
}