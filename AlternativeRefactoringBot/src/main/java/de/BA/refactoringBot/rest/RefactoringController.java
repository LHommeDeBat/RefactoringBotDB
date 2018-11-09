package de.BA.refactoringBot.rest;

import java.io.File;
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
import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.controller.main.BotController;
import de.BA.refactoringBot.controller.main.GitController;
import de.BA.refactoringBot.model.configuration.ConfigurationRepository;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequest;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequests;
import de.BA.refactoringBot.model.outputModel.myPullRequestComment.BotPullRequestComment;
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
	GitController dataGetter;
	@Autowired
	ConfigurationRepository configRepo;
	@Autowired
	BotConfiguration botConfig;
	@Autowired
	BotController botController;

	/**
	 * Diese Methode testet die GitHub API und einige Bot-Funktionen.
	 * 
	 * @param configID
	 * @return allRequests
	 */
	@RequestMapping(value = "/refactorWithComments/{configID}", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Gibt Repoliste von Github zurück")
	public ResponseEntity<?> refactorWithComments(@PathVariable Long configID) {

		// Hole Git-Konfiguration für Bot falls Existiert
		Optional<GitConfiguration> gitConfig = configRepo.findById(configID);
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
						File f = new File(botConfig.getBotWorkingDirectory() + "\\TestPullRequest\\src\\text"
								+ randomFileNumber + ".txt");
						f.getParentFile().mkdirs();
						f.createNewFile();

						// Pushe Änderungen
						dataGetter.pushChanges(gitConfig.get());

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
}
