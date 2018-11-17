package de.BA.refactoringBot.api.main;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.BA.refactoringBot.api.github.GithubDataGrabber;
import de.BA.refactoringBot.api.sonarCube.SonarCubeDataGrabber;
import de.BA.refactoringBot.controller.github.GithubObjectTranslator;
import de.BA.refactoringBot.controller.main.BotController;
import de.BA.refactoringBot.controller.sonarCube.SonarCubeObjectTranslator;
import de.BA.refactoringBot.model.botIssue.BotIssue;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubCreateRequest;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubPullRequest;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubPullRequests;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubUpdateRequest;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequest;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequests;
import de.BA.refactoringBot.model.outputModel.myPullRequestComment.BotPullRequestComment;
import de.BA.refactoringBot.model.sonarQube.SonarCubeIssues;

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
	SonarCubeDataGrabber sonarCubeGrabber;
	@Autowired
	GithubObjectTranslator githubTranslator;
	@Autowired
	SonarCubeObjectTranslator sonarCubeTranslator;
	@Autowired
	BotController botController;

	/**
	 * Diese Methode holt alle Pullrequests mitsamt der Kommentare im Bot-Format vom
	 * passenden Filehoster-Service.
	 * 
	 * @param gitConfig
	 * @return botRequests
	 * @throws Exception
	 */
	public BotPullRequests getRequestsWithComments(GitConfiguration gitConfig) throws Exception {
		// Erstelle Request-Objekt
		BotPullRequests botRequests = null;

		// Wähle passenden Service aus
		switch (gitConfig.getRepoService()) {
		case "github":
			// Hole Requests von Github
			GithubPullRequests githubRequests = githubGrabber.getAllPullRequests(gitConfig);
			// Wandle Objekt in Bot-Objekt um
			botRequests = githubTranslator.translateRequests(githubRequests, gitConfig);
			// Checke ob Maximale Anzahl erreicht
			botController.checkAmountOfBotRequests(botRequests, gitConfig);
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
	 * @throws Exception
	 * @throws OperationNotSupportedException
	 */
	public void makeUpdateRequest(BotPullRequest request, BotPullRequestComment comment, GitConfiguration gitConfig)
			throws Exception {
		// Wähle passenden Service aus
		switch (gitConfig.getRepoService()) {
		case "github":
			// Erstelle aktualisierten Request
			GithubUpdateRequest updateRequest = githubTranslator.makeUpdateRequest(request, gitConfig);
			// Aktualisiere Request
			githubGrabber.updatePullRequest(updateRequest, gitConfig, request.getRequestNumber());
			// Antworte auf an Bot gerichteten Request-Kommentar
			githubGrabber.responseToBotComment(githubTranslator.createReplyComment(comment, gitConfig, null), gitConfig,
					request.getRequestNumber());
			break;
		}
	}

	/**
	 * Diese Methode erstellt einen Request auf einem Filehoster, falls der Request,
	 * welcher Refactored wurde, nicht dem Bot gehöhrt und er dementsprechend für
	 * die Bearbeitung keine Rechte hat,´.
	 * 
	 * @param request
	 * @param gitConfig
	 * @throws Exception
	 */
	public void makeCreateRequest(BotPullRequest request, BotPullRequestComment comment, GitConfiguration gitConfig,
			String botBranchName) throws Exception {
		// Wähle passenden Service aus
		switch (gitConfig.getRepoService()) {
		case "github":
			// Erstelle Request-Objekt
			GithubCreateRequest createRequest = githubTranslator.makeCreateRequest(request, gitConfig, botBranchName);
			// Erstelle Request auf Github
			GithubPullRequest newGithubRequest = githubGrabber.createRequest(createRequest, gitConfig);
			// Antworte auf an Bot gerichteten Request-Kommentar
			githubGrabber.responseToBotComment(
					githubTranslator.createReplyComment(comment, gitConfig, newGithubRequest.getHtmlUrl()), gitConfig,
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
	 * @param sonarCubeProjectKey
	 * @param maxAmountRequests
	 * @param projectRootFolder
	 * @return gitConfig
	 * @throws Exception
	 */
	public GitConfiguration createConfigurationForRepo(String repoName, String repoOwner, String repoService,
			String botUsername, String botPassword, String botToken, String analysisService,
			String analysisServiceProjectKey, Integer maxAmountRequests, String projectRootFolder) throws Exception {

		// Initiiere Konfiguration
		GitConfiguration gitConfig = null;

		// Wähle passenden Service aus
		switch (repoService.toLowerCase()) {
		case "github":
			// Prüfe Repo-Existenz
			githubGrabber.checkRepository(repoName, repoOwner);

			// Prüfe Bot-User-Existenz + Token gültigkeit
			githubGrabber.checkGithubUser(botUsername, botToken);

			// Erstelle Konfiguration und den Fork
			gitConfig = githubTranslator.createConfiguration(repoName, repoOwner, botUsername, botPassword, botToken,
					repoService, analysisService, analysisServiceProjectKey, maxAmountRequests, projectRootFolder);
			githubGrabber.createFork(gitConfig);
			return gitConfig;
		default:
			throw new Exception("Filehoster " + "'" + repoService + "' wird nicht unterstützt!");
		}
	}

	/**
	 * Diese Methode löscht das Repository eines Filehosters nachdem die
	 * Konfiguration aus der DB des Serivices entfernt wurde.
	 * 
	 * @param gitConfig
	 * @throws Exception
	 * @throws OperationNotSupportedException
	 */
	public void deleteRepository(GitConfiguration gitConfig) throws Exception {
		// Wähle passenden Service aus
		switch (gitConfig.getRepoService()) {
		case "github":
			// Versuche Repo zu löschen
			githubGrabber.deleteRepository(gitConfig);
			break;
		}
	}

	/**
	 * Diese Methode resettet den Fork bei jedem Refactoring um Merge-Konflikte zu
	 * vermeiden.
	 * 
	 * @param gitConfig
	 * @throws Exception
	 */
	public void resetFork(GitConfiguration gitConfig) throws Exception {
		// Wähle passenden Service aus
		switch (gitConfig.getRepoService()) {
		case "github":
			// Versuche Repo (Fork) zu löschen
			githubGrabber.deleteRepository(gitConfig);
			// Versuche neuen Fork zu erstellen
			githubGrabber.createFork(gitConfig);
			break;
		}
	}

	/**
	 * Diese Methode erstellt den Fork für das in das in der GitConfig angegebene
	 * Repository.
	 * 
	 * @param gitConfig
	 * @throws Exception
	 */
	public void createFork(GitConfiguration gitConfig) throws Exception {
		// Wähle passenden Service aus
		switch (gitConfig.getRepoService()) {
		case "github":
			// Versuche neuen Fork zu erstellen
			githubGrabber.createFork(gitConfig);
			break;
		}
	}

	/**
	 * Diese Methode holt die Issues von einer API eines Analysis-Services und gibt
	 * sie übersetzt zuück.
	 * 
	 * @param gitConfig
	 * @return botIssues
	 * @throws Exception
	 */
	public List<BotIssue> getAnalysisServiceIssues(GitConfiguration gitConfig) throws Exception {
		// Wähle passenden Service aus
		switch (gitConfig.getAnalysisService()) {
		case "sonarcube":
			// Versuche neuen Fork zu erstellen
			SonarCubeIssues issues = sonarCubeGrabber.getIssues(gitConfig.getAnalysisServiceProjectKey());
			List<BotIssue> botIssues = sonarCubeTranslator.translateSonarIssue(issues, gitConfig);
			return botIssues;
		default:
			return null;
		}
	}
	
	/**
	 * Diese Methode erstellt einen Request auf einem Filehoster, falls SonarCube zu
	 * refactorende Issues gefunden hat.
	 * 
	 * @param request
	 * @param gitConfig
	 * @param newBranch
	 * @throws Exception
	 */
	public void makeCreateRequestWithAnalysisService(BotIssue issue, GitConfiguration gitConfig, String newBranch)
			throws Exception {
		// Wähle passenden Service aus
		switch (gitConfig.getRepoService()) {
		case "github":
			// Erstelle Request-Objekt
			GithubCreateRequest createRequest = githubTranslator.makeCreateRequestWithAnalysisService(issue, gitConfig,
					newBranch);
			// Erstelle Request auf Github
			githubGrabber.createRequest(createRequest, gitConfig);
			break;
		}
	}
}
