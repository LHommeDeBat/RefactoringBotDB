package de.BA.refactoringBot.rest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import de.BA.refactoringBot.api.GithubDataGrabber;
import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.controller.GitController;
import de.BA.refactoringBot.controller.GithubObjectTranslator;
import de.BA.refactoringBot.model.configuration.ConfigurationRepository;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.githubModels.pullRequest.GithubSendPullRequest;
import de.BA.refactoringBot.model.githubModels.pullRequest.PullRequests;
import de.BA.refactoringBot.model.outputModel.myPullRequest.MyPullRequest;
import de.BA.refactoringBot.model.outputModel.myPullRequest.MyPullRequests;
import de.BA.refactoringBot.model.outputModel.myPullRequestComment.MyPullRequestComment;
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
	GithubDataGrabber grabber;
	@Autowired
	GithubObjectTranslator translator;
	@Autowired
	GitController dataGetter;
	@Autowired
	ConfigurationRepository configRepo;
	@Autowired
	BotConfiguration botConfig;

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

		// Hole PullRequests von GitHub
		PullRequests response = null;
		try {
			response = grabber.getAllPullRequests(gitConfig.get());
		} catch (URISyntaxException e1) {
			return new ResponseEntity<String>("Etwas ist mit den URLs schiefgelaufen!",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// Übersetze GitHub-Objekt in Eigenes
		MyPullRequests allRequests = translator.translateRequests(response, gitConfig.get());

		// Gehe alle PullRequests durch
		for (MyPullRequest request : allRequests.getAllPullRequests()) {
			// Gehe alle Kommentare eines Requests durch
			for (MyPullRequestComment comment : request.getAllComments()) {
				// Falls Kommentar für Bot bestimmt
				if (translator.checkIfCommentForBot(comment)) {
					// Versuche zu Pullen
					try {
						// Pulle Repo zum Arbeiten
						dataGetter.pullGithubRepo(gitConfig.get().getForkGitLink());
						// Wechsle zum Branch des PullRequests
						dataGetter.checkoutBranch(request.getBranchName());

						// TODO: Später durch Refactoring ersetzen - Erstelle File
						File f = new File(botConfig.getBotWorkingDirectory() + "\\TestPullRequest\\src\\text5.txt");
						f.getParentFile().mkdirs();
						f.createNewFile();

						// Pushe Änderungen
						dataGetter.pushChanges(gitConfig.get());

						// Erstelle Request-Objekt zum Senden
						GithubSendPullRequest sendRequest = translator.createSendRequest(request, gitConfig.get());
						// Aktualisiere Pull-Request auf Github
						grabber.updatePullRequest(sendRequest, gitConfig.get(), request.getRequestNumber());
						// Bearbeite Kommentar
						grabber.editToBotComment(translator.editComment(comment), gitConfig.get(),
								comment.getCommentID());
						// Antworte auf Kommentar
						grabber.responseToBotComment(translator.createReplyComment(comment, gitConfig.get()), gitConfig.get(),
								request.getRequestNumber());
					} catch (GitAPIException | IOException e) {
						e.printStackTrace();
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				}
			}
		}

		// Gebe übersetzte Requests zurück
		return new ResponseEntity<MyPullRequests>(allRequests, HttpStatus.OK);
	}
}
