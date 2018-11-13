package de.BA.refactoringBot.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import de.BA.refactoringBot.api.main.ApiGrabber;
import de.BA.refactoringBot.api.sonarCube.SonarCubeDataGrabber;
import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.controller.main.BotController;
import de.BA.refactoringBot.controller.main.GitController;
import de.BA.refactoringBot.model.configuration.ConfigurationRepository;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequest;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequests;
import de.BA.refactoringBot.model.outputModel.myPullRequestComment.BotPullRequestComment;
import de.BA.refactoringBot.model.refactoredIssue.RefactoredIssue;
import de.BA.refactoringBot.model.refactoredIssue.RefactoredIssueRepository;
import de.BA.refactoringBot.model.sonarQube.Issue;
import de.BA.refactoringBot.model.sonarQube.SonarCubeIssues;
import de.BA.refactoringBot.refactoring.RefactoringPicker;
import io.swagger.annotations.ApiOperation;

/**
 * Dieser REST-Controller ermöglicht die Nutzung des Bots durch den Nutzer via.
 * REST-Schnittstellen.
 * 
 * @author Stefan Basaric
 *
 */
@RestController
@RequestMapping(path = "/refactorings")
public class RefactoringController {

	@Autowired
	ApiGrabber grabber;
	@Autowired
	SonarCubeDataGrabber sonarCubeGrabber;
	@Autowired
	GitController dataGetter;
	@Autowired
	ConfigurationRepository configRepo;
	@Autowired
	RefactoredIssueRepository refactoredIssues;
	@Autowired
	BotConfiguration botConfig;
	@Autowired
	BotController botController;
	@Autowired
	RefactoringPicker refactoring;

	/**
	 * Diese Methode testet die GitHub API und einige Bot-Funktionen.
	 * 
	 * @param configID
	 * @return allRequests
	 */
	@RequestMapping(value = "/refactorWithComments/{configID}", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Führt Refactoring anhand der Pull-Request-Kommentare in einem Repository aus.")
	public ResponseEntity<?> refactorWithComments(@PathVariable Long configID) {

		// Hole Git-Konfiguration für Bot falls Existiert
		Optional<GitConfiguration> gitConfig = configRepo.getByID(configID);
		// Falls nicht existiert
		if (!gitConfig.isPresent()) {
			return new ResponseEntity<String>("Konfiguration mit angegebener ID existiert nicht!",
					HttpStatus.NOT_FOUND);
		}

		// Initiiere Objekt
		BotPullRequests allRequests = null;
		try {
			// Resette/Synchronisiere Fork mit Parent um Merge-Konflikte zu vermeiden
			grabber.resetFork(gitConfig.get());
			// Hole Requests mit Kommentaren vom Filehoster im Bot-Format
			allRequests = grabber.getRequestsWithComments(gitConfig.get());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// Gehe alle PullRequests durch
		for (BotPullRequest request : allRequests.getAllPullRequests()) {
			// Gehe alle Kommentare eines Requests durch
			for (BotPullRequestComment comment : request.getAllComments()) {
				// Falls Kommentar für Bot bestimmt
				if (botController.checkIfCommentForBot(comment)) {
					// Versuche zu Pullen
					try {
						// Pulle Repo zum Arbeiten
						dataGetter.pullGithubRepo(gitConfig.get().getForkGitLink());
						// Wechsle zum Branch des PullRequests
						dataGetter.checkoutBranch(request.getBranchName());

						// TODO: Später durch Refactoring ersetzen - Erstelle File
						Random rand = new Random();
						int randomFileNumber = rand.nextInt(100000) + 1;
						File f = new File(botConfig.getBotRefactoringDirectory()
								+ gitConfig.get().getProjectRootFolder() + "/src/text" + randomFileNumber + ".txt");
						f.getParentFile().mkdirs();
						f.createNewFile();

						// Pushe Änderungen TODO: dynamische Commitnachricht
						dataGetter.pushChanges(gitConfig.get(), "Bot hat eine Datei hinzugefügt");

						// Aktuallisiere Pullrequest und Kommentar + Antworte (falls Bot-Request)
						if (request.getCreatorName().equals(gitConfig.get().getBotName())) {
							grabber.makeUpdateRequest(request, comment, gitConfig.get());
						} else {
							grabber.makeCreateRequest(request, gitConfig.get());
						}
					} catch (Exception e) {
						e.printStackTrace();
						return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
					}
				}
			}
		}

		// Gebe übersetzte Requests zurück
		return new ResponseEntity<BotPullRequests>(allRequests, HttpStatus.OK);
	}

	@RequestMapping(value = "/refactorWithSonarCube/{configID}", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Führt Refactoring anhand der SonarCube-Issues in einem Repository aus.")
	public ResponseEntity<?> refactorWithSonarCube(@PathVariable Long configID) {

		// Erstelle Liste von Refactored Issues
		List<RefactoredIssue> allRefactoredIssues = new ArrayList<RefactoredIssue>();

		// Hole Git-Konfiguration für Bot falls Existiert
		Optional<GitConfiguration> gitConfig = configRepo.getByID(configID);
		// Falls nicht existiert
		if (!gitConfig.isPresent()) {
			return new ResponseEntity<String>("Konfiguration mit angegebener ID existiert nicht!",
					HttpStatus.NOT_FOUND);
		}
		// Falls Projekt nicht auf SonarCube gehostet
		if (gitConfig.get().getSonarCubeProjectKey() == null) {
			return new ResponseEntity<String>(
					"Konfiguration besitzt kein SonarCube-Projektkey! Führen Sie Refactorings über Request-Kommentare aus.",
					HttpStatus.BAD_GATEWAY);
		}

		try {
			// Resette/Synchronisiere Fork mit Parent um Merge-Konflikte zu vermeiden
			grabber.resetFork(gitConfig.get());
			// Hole Requests vom Filehoster (und teste ob Limit erreicht)
			grabber.getRequestsWithComments(gitConfig.get());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// Initiiere Datenobjekt für die SonarCubeIssues
		SonarCubeIssues allIssues = null;

		try {
			// Hole Issues von der SonarCube-API
			allIssues = sonarCubeGrabber.getIssues(gitConfig.get().getSonarCubeProjectKey());

			// Gehe alle Issues durch
			for (Issue issue : allIssues.getIssues()) {
				// Pulle Repo zum Arbeiten
				dataGetter.pullGithubRepo(gitConfig.get().getForkGitLink());
				// TODO: Dynamischer Branch
				dataGetter.checkoutBranch("master");

				// Versuche Refactoring auszuführen
				String commitMessage = refactoring.pickRefactoring(issue, gitConfig.get());

				// Falls Refactoring für Issue ausgeführt wurde
				if (commitMessage != null) {
					// Baue RefactoredIssue-Objekt
					RefactoredIssue refactoredIssue = botController.buildRefactoredIssue(issue, gitConfig.get());

					// Pushe Änderungen
					/*
					 * dataGetter.pushChanges(gitConfig.get(), refactoredIssue.getCommitMessage());
					 * 
					 * // Erstelle PullRequest grabber.makeCreateRequestWithSonarQube(issue,
					 * gitConfig.get());
					 */

					// Speichere den RefactoredIssue in die DB
					RefactoredIssue savedIssue = refactoredIssues.save(refactoredIssue);
					allRefactoredIssues.add(savedIssue);
				}
			}
			return new ResponseEntity<List<RefactoredIssue>>(allRefactoredIssues, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
