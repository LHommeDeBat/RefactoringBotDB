package de.BA.refactoringBot.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import de.BA.refactoringBot.controller.main.GrammerController;
import de.BA.refactoringBot.controller.sonarCube.SonarCubeObjectTranslator;
import de.BA.refactoringBot.model.botIssue.BotIssue;
import de.BA.refactoringBot.model.configuration.ConfigurationRepository;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequest;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequests;
import de.BA.refactoringBot.model.outputModel.myPullRequestComment.BotPullRequestComment;
import de.BA.refactoringBot.model.refactoredIssue.RefactoredIssue;
import de.BA.refactoringBot.model.refactoredIssue.RefactoredIssueRepository;
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
	RefactoredIssueRepository issueRepo;
	@Autowired
	RefactoredIssueRepository refactoredIssues;
	@Autowired
	BotConfiguration botConfig;
	@Autowired
	BotController botController;
	@Autowired
	GrammerController grammarController;
	@Autowired
	RefactoringPicker refactoring;
	@Autowired
	SonarCubeObjectTranslator sonarTranslator;

	/**
	 * Diese Methode führt Refactorings anhand von Kommentaren in Pull-Requests aus.
	 * 
	 * @param configID
	 * @return allRequests
	 */
	@RequestMapping(value = "/refactorWithComments/{configID}", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Führt Refactoring anhand der Pull-Request-Kommentare in einem Repository aus.")
	public ResponseEntity<?> refactorWithComments(@PathVariable Long configID) {

		// Erstelle Liste von Refactored Issues
		List<RefactoredIssue> allRefactoredIssues = new ArrayList<RefactoredIssue>();

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
			// Hole aktuellste OG-Repo-Daten
			dataGetter.fetchRemote(gitConfig.get());
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
				// Falls Kommentar für Bot bestimmt und nicht schon Refactored wurde
				if (grammarController.checkComment(comment.getCommentBody()) && !issueRepo
						.refactoredComment(gitConfig.get().getRepoService(), comment.getCommentID().toString())
						.isPresent()) {
					// Erstelle Issue
					BotIssue botIssue = grammarController.createIssueFromComment(comment);
					// Versuche zu Pullen
					try {
						// Falls Request nicht vom Bot erstellt wurde
						if (!request.getCreatorName().equals(gitConfig.get().getBotName())) {
							// Erstelle Branch für das Kommentar-Refactoring
							String newBranch = gitConfig.get().getRepoService() + "_Refactoring_"
									+ comment.getCommentID().toString();
							dataGetter.createBranch(gitConfig.get(), request.getBranchName(), newBranch);

							// Versuche Refactoring auszuführen
							String commitMessage = refactoring.pickRefactoring(botIssue, gitConfig.get());

							// Falls erfolgreich
							if (commitMessage != null) {
								// Baue RefactoredIssue-Objekt
								RefactoredIssue refactoredIssue = botController.buildRefactoredIssue(botIssue,
										gitConfig.get());

								// Speichere den RefactoredIssue in die DB
								RefactoredIssue savedIssue = refactoredIssues.save(refactoredIssue);
								allRefactoredIssues.add(savedIssue);

								// Pushe Änderungen + erstelle Request
								dataGetter.pushChanges(gitConfig.get(), commitMessage);
								grabber.makeCreateRequest(request, comment, gitConfig.get(), newBranch);
							}
						} else {
							// Wechsle zum Refactoring-Branch
							dataGetter.switchBranch(gitConfig.get(), request.getBranchName());

							// Versuche Refactoring auszuführen
							String commitMessage = refactoring.pickRefactoring(botIssue, gitConfig.get());

							// Falls erfolgreich
							if (commitMessage != null) {
								// Baue RefactoredIssue-Objekt
								RefactoredIssue refactoredIssue = botController.buildRefactoredIssue(botIssue,
										gitConfig.get());

								// Speichere den RefactoredIssue in die DB
								RefactoredIssue savedIssue = refactoredIssues.save(refactoredIssue);
								allRefactoredIssues.add(savedIssue);

								// Pushe Änderungen + aktualisiere Request
								dataGetter.pushChanges(gitConfig.get(), commitMessage);
								grabber.makeUpdateRequest(request, comment, gitConfig.get());
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
					}
				}
			}
		}

		// Gebe übersetzte Requests zurück
		return new ResponseEntity<List<RefactoredIssue>>(allRefactoredIssues, HttpStatus.OK);
	}

	/**
	 * Diese Methode führt Refactorings anhand von SonarQube-Befunden aus.
	 * 
	 * @param configID
	 * @return allRefactoredIssues
	 */
	@RequestMapping(value = "/refactorWithAnalysisService/{configID}", method = RequestMethod.GET, produces = "application/json")
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
		// Falls Projekt nicht auf einem AnalysisService gehostet
		if (gitConfig.get().getAnalysisService() == null || gitConfig.get().getAnalysisServiceProjectKey() == null) {
			return new ResponseEntity<String>("Konfiguration besitzt nicht alle Daten für den Analysis-Service!",
					HttpStatus.BAD_REQUEST);
		}

		try {
			// Hole aktuellste OG-Repo-Daten
			dataGetter.fetchRemote(gitConfig.get());
			// Hole Requests vom Filehoster (und teste ob Limit erreicht)
			grabber.getRequestsWithComments(gitConfig.get());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		try {
			// Hole Issues von der AnalysisService-API
			List<BotIssue> botIssues = grabber.getAnalysisServiceIssues(gitConfig.get());

			// Falls AnalysisService nicht unterstützt
			if (botIssues == null) {
				return new ResponseEntity<String>(
						"Analysis-Service '" + gitConfig.get().getAnalysisService() + "' wird nicht unterstützt!",
						HttpStatus.BAD_REQUEST);
			}

			// Gehe alle Issues durch
			for (BotIssue botIssue : botIssues) {
				// Wenn SonarCube Issue nicht schonmal bearbeitet worden ist
				if (!issueRepo.refactoredSonarCube(botIssue.getCommentServiceID()).isPresent()) {
					// TODO: Dynamischer Branch
					// Erstelle Branch für das Kommentar-Refactoring
					String newBranch = "sonarCube_Refactoring_" + botIssue.getCommentServiceID();
					dataGetter.createBranch(gitConfig.get(), "master", newBranch);
					// Versuche Refactoring auszuführen
					String commitMessage = refactoring.pickRefactoring(botIssue, gitConfig.get());

					// Falls Refactoring für Issue ausgeführt wurde
					if (commitMessage != null) {
						// Baue RefactoredIssue-Objekt
						RefactoredIssue refactoredIssue = botController.buildRefactoredIssue(botIssue, gitConfig.get());

						// Speichere den RefactoredIssue in die DB
						RefactoredIssue savedIssue = refactoredIssues.save(refactoredIssue);
						allRefactoredIssues.add(savedIssue);

						// Pushe Änderungen + erstelle Request
						dataGetter.pushChanges(gitConfig.get(), commitMessage);
						// Erstelle PullRequest
						grabber.makeCreateRequestWithAnalysisService(botIssue, gitConfig.get(), newBranch);
					}
				}
			}

			return new ResponseEntity<List<RefactoredIssue>>(allRefactoredIssues, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
