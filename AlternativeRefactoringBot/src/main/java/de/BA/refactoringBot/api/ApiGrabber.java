package de.BA.refactoringBot.api;

import java.net.URISyntaxException;

import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import de.BA.refactoringBot.controller.github.GithubObjectTranslator;
import de.BA.refactoringBot.controller.main.BotController;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubPullRequests;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubUpdateRequest;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequest;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequests;
import de.BA.refactoringBot.model.outputModel.myPullRequestComment.BotPullRequestComment;

/**
 * Diese Klasse leitet alle Anfragen an die passenden APIs weiter.
 * 
 * @author Stefan Basaric
 *
 */
@Component
public class ApiGrabber {

	@Autowired
	GithubDataGrabber githubGrabber;
	@Autowired
	GithubObjectTranslator githubTranslator;
	@Autowired
	BotController botController;

	/**
	 * Diese Methode holt alle Pullrequests mitsamt der Kommentare im Bot-Format vom
	 * passenden Filehoster-Service.
	 * 
	 * @param gitConfig
	 * @return botRequests
	 * @throws URISyntaxException
	 */
	public BotPullRequests getRequestsWithComments(GitConfiguration gitConfig) throws URISyntaxException {
		// Erstelle Request-Objekt
		BotPullRequests botRequests = null;

		// Wähle passenden Service aus
		switch (gitConfig.getRepoService()) {
		case "github":
			// Hole Requests von Github
			GithubPullRequests githubRequests = githubGrabber.getAllPullRequests(gitConfig);
			// Wandle Objekt in Bot-Objekt um
			botRequests = githubTranslator.translateRequests(githubRequests, gitConfig);
			break;
		}
		return botRequests;
	}

	/**
	 * Diese Methode erstellt einen Request der genutzt wird um einen Request beim
	 * Filehoster zu Aktualisieren.
	 * 
	 * @param request
	 * @param gitConfig
	 * @throws URISyntaxException
	 * @throws OperationNotSupportedException
	 */
	public void makeUpdateRequest(BotPullRequest request, BotPullRequestComment comment, GitConfiguration gitConfig)
			throws URISyntaxException {
		// Wähle passenden Service aus
		switch (gitConfig.getRepoService()) {
		case "github":
			// Erstelle aktualisierten Request
			GithubUpdateRequest updateRequest = githubTranslator.makeUpdateRequest(request, gitConfig);
			// Aktualisiere Request
			githubGrabber.updatePullRequest(updateRequest, gitConfig, request.getRequestNumber());
			// Aktualisiere an Bot gerichteten Request-Kommentar
			githubGrabber.editToBotComment(githubTranslator.editComment(comment), gitConfig, comment.getCommentID());
			// Antworte auf an Bot gerichteten Request-Kommentar
			githubGrabber.responseToBotComment(githubTranslator.createReplyComment(comment, gitConfig), gitConfig,
					request.getRequestNumber());
			break;
		}
	}

	/**
	 * Schaue ob das Repository existiert und erstelle dafür eine Konfiguration.
	 * 
	 * @param repoName
	 * @param repoOwner
	 * @param repoService
	 * @param botToken
	 * @return gitConfig
	 * @throws URISyntaxException
	 * @throws OperationNotSupportedException
	 */
	public GitConfiguration createConfigurationForRepo(String repoName, String repoOwner, String repoService,
			String botUsername, String botPassword, String botToken)
			throws RestClientException, URISyntaxException, OperationNotSupportedException {

		// Initiiere Konfiguration
		GitConfiguration gitConfig = null;

		// Wähle passenden Service aus
		switch (repoService.toLowerCase()) {
		case "github":
			// Versuche Repo zu holen
			githubGrabber.checkRepository(repoName, repoOwner, repoService, botToken);

			// Erstelle Konfiguration und den Fork
			gitConfig = githubTranslator.createConfiguration(repoName, repoOwner, botUsername, botPassword, botToken, repoService);
			githubGrabber.createFork(gitConfig);
            return gitConfig;
		default:
			throw new OperationNotSupportedException();
		}
	}

	/**
	 * Diese Methode löscht das Repository eines Filehosters nachdem die
	 * Konfiguration aus der DB des Serivices entfernt wurde.
	 * 
	 * @param gitConfig
	 * @throws URISyntaxException
	 * @throws OperationNotSupportedException
	 */
	public void deleteRepository(GitConfiguration gitConfig) throws URISyntaxException {
		// Wähle passenden Service aus
		switch (gitConfig.getRepoService()) {
		case "github":
			// Versuche Repo zu löschen
			githubGrabber.deleteRepository(gitConfig);
			break;
		}
	}
}
