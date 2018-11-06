package de.BA.refactoringBot.rest;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.BA.refactoringBot.api.GithubDataGrabber;
import de.BA.refactoringBot.controller.GithubObjectTranslator;
import de.BA.refactoringBot.model.configuration.ConfigurationRepository;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.githubModels.collaboration.invite.Invite;
import de.BA.refactoringBot.model.githubModels.repository.Repository;
import io.swagger.annotations.ApiOperation;

/**
 * Diese Klasse bietet ein CRUD-Interface als REST-Schnittstelle für die
 * Git-Konfigurationen an.
 * 
 * @author Stefan Basaric
 *
 */
@RestController
@RequestMapping(path = "/gitConfiguration")
public class ConfigurationController {

	@Autowired
	ConfigurationRepository repo;
	@Autowired
	GithubDataGrabber grabber;
	@Autowired
	GithubObjectTranslator translator;

	/**
	 * Diese Methode erstellt eine Git-Konfiguration anhand einer Nutzereingabe.
	 * 
	 * @param repoName
	 * @param repoOwner
	 * @param repoService
	 * @return
	 */
	@RequestMapping(value = "/createConfig", method = RequestMethod.POST, produces = "application/json")
	@ApiOperation(value = "Erstelle eine Git-Konfiguration")
	public ResponseEntity<?> add(
			@RequestParam(value = "repoName", required = true, defaultValue = "TestPullRequest") String repoName,
			@RequestParam(value = "ownerName", required = true, defaultValue = "LHommeDeBat") String repoOwner,
			@RequestParam(value = "repoService", required = true, defaultValue = "Github") String repoService,
			@RequestParam(value = "repoAdminToken", required = true, defaultValue = "d2103fa25361561eb0e9a05711f3f43939aad2de") String repoAdminToken) {

		// Akzeptiere vorerst nur Github
		if (!repoService.toLowerCase().equals("github")) {
			return new ResponseEntity<String>("Aktuell wird nur Github unterstützt!", HttpStatus.NOT_ACCEPTABLE);
		}

		// Schaue ob Repository schon existiert
		Optional<GitConfiguration> existsConfig = repo.getConfigByName(repoName, repoOwner);
		// Falls existiert
		if (existsConfig.isPresent()) {
			return new ResponseEntity<String>("Diese Kombination von Repository- und Besitzername existiert schon",
					HttpStatus.CONFLICT);
		}

		// Hole Repo-Daten (aktuell nur Github möglich)
		Repository gitRepo = grabber.checkRepository(repoName, repoOwner, repoService);

		// Falls API nix liefert
		if (gitRepo == null) {
			return new ResponseEntity<String>("Angegebenes Repo existiert bei " + repoService + " nicht",
					HttpStatus.BAD_REQUEST);
		}

		// Baue Konfiguration aus dem Repo-Daten
		GitConfiguration config = translator.createConfiguration(gitRepo, repoService);

		// Speichere Konfiguration in DB
		GitConfiguration savedConfig = repo.save(config);

		// Lade Bot zum neu hinzugefügten Repo ein
		Invite repoInvite = grabber.inviteBotToRepo(savedConfig, repoAdminToken);
		// Akzeptiere Einladung vom Bot (falls möglich)
		if (repoInvite != null) {
			grabber.acceptInvite(savedConfig, repoInvite.getId());
		}

		// Gebe Feedback an Nutzer zurück
		return new ResponseEntity<GitConfiguration>(savedConfig, HttpStatus.CREATED);
	}

	/**
	 * Diese Methode holt eine Git-Konfiguration aus der DB anhand der vom Nutzer
	 * eingegebenen Daten.
	 * 
	 * @param repoName
	 * @param repoOwner
	 * @param repoService
	 * @return
	 */
	@RequestMapping(value = "/getConfig", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Lese eine Git-Konfiguration aus")
	public ResponseEntity<?> getConfig(
			@RequestParam(value = "repoName", required = true, defaultValue = "TestPullRequest") String repoName,
			@RequestParam(value = "ownerName", required = true, defaultValue = "LHommeDeBat") String repoOwner,
			@RequestParam(value = "repoService", required = true, defaultValue = "Github") String repoService) {

		// Schaue ob Repository schon existiert
		Optional<GitConfiguration> existsConfig = repo.getConfigByName(repoName, repoOwner);
		// Falls existiert
		if (existsConfig.isPresent()) {
			return new ResponseEntity<GitConfiguration>(existsConfig.get(), HttpStatus.OK);
		} else {
			return new ResponseEntity<String>("Kein Repository mit angegebenen Eigenschaften gefunden!",
					HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Diese Methode holt alle Konfigurationen aus der DB.
	 * 
	 * @return allConfigs
	 */
	@RequestMapping(value = "/getAllConfigs", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Lese alle Git-Konfiguration aus")
	public ResponseEntity<?> getAllConfigs() {
		// Hole alle Konfigurationen aus DB und gebe sie zurück
		Iterable<GitConfiguration> allConfigs = repo.findAll();
		return new ResponseEntity<Iterable<GitConfiguration>>(allConfigs, HttpStatus.OK);
	}

	/**
	 * Diese Methode löscht eine Konfiguration anhand einer Nutzereingabe von
	 * Konfigurationseigenschaften.
	 * 
	 * @param repoName
	 * @param repoOwner
	 * @param repoService
	 * @return {feedbackString}
	 */
	@RequestMapping(value = "/deleteConfig", method = RequestMethod.DELETE, produces = "application/json")
	@ApiOperation(value = "Lösche Git-Konfiguration")
	public ResponseEntity<?> deleteConfig(
			@RequestParam(value = "repoName", required = true, defaultValue = "TestPullRequest") String repoName,
			@RequestParam(value = "ownerName", required = true, defaultValue = "LHommeDeBat") String repoOwner,
			@RequestParam(value = "repoService", required = true, defaultValue = "Github") String repoService,
			@RequestParam(value = "repoAdminToken", required = true, defaultValue = "d2103fa25361561eb0e9a05711f3f43939aad2de") String repoAdminToken) {
		// Schaue ob Repository schon existiert
		Optional<GitConfiguration> existsConfig = repo.getConfigByName(repoName, repoOwner);
		// Falls existiert
		if (existsConfig.isPresent()) {
			grabber.deleteBotFromRepository(existsConfig.get(), repoAdminToken);
			repo.delete(existsConfig.get());
			return new ResponseEntity<String>("Konfiguration erfolgreich gelöscht!", HttpStatus.OK);
		} else {
			return new ResponseEntity<String>("Kein Repository mit angegebenen Eigenschaften gefunden!",
					HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Diese Methode aktualisiert eine Konfiguration anhand einer eindeutigen ID mit
	 * dem vom Nutzer angegebenen Eigenschaften.
	 * 
	 * @param configID
	 * @param repoName
	 * @param repoOwner
	 * @param repoService
	 * @return updatedConfig
	 */
	@RequestMapping(value = "/updateConfig/{configID}", method = RequestMethod.PUT, produces = "application/json")
	@ApiOperation(value = "Aktualisiere Git-Konfiguration")
	public ResponseEntity<?> updateConfig(@PathVariable Long configID,
			@RequestParam(value = "newRepoName", required = true, defaultValue = "TestPullRequest") String repoName,
			@RequestParam(value = "newOwnerName", required = true, defaultValue = "LHommeDeBat") String repoOwner,
			@RequestParam(value = "newRepoService", required = true, defaultValue = "Github") String repoService) {

		// Akzeptiere vorerst nur Github
		if (!repoService.toLowerCase().equals("github")) {
			return new ResponseEntity<String>("Aktuell wird nur Github unterstützt!", HttpStatus.NOT_ACCEPTABLE);
		}

		// Schaue ob Repository mit dieser ID existiert
		Optional<GitConfiguration> oldConfigExists = repo.findById(configID);
		// Falls nicht existiert
		if (!oldConfigExists.isPresent()) {
			return new ResponseEntity<String>("Konfiguration mit angegebener ID nicht gefunden!", HttpStatus.NOT_FOUND);
		}

		// Schaue ob Repository für neue Daten existiert
		Optional<GitConfiguration> existsConfig = repo.getConfigByName(repoName, repoOwner);
		// Falls existiert
		if (existsConfig.isPresent()) {
			return new ResponseEntity<String>("Konfiguration mit identischen Eigenschaften existiert schon!",
					HttpStatus.CONFLICT);
		} else {
			// Hole Repo-Daten (aktuell nur Github möglich)
			Repository gitRepo = grabber.checkRepository(repoName, repoOwner, repoService);

			// Falls API nix liefert
			if (gitRepo == null) {
				return new ResponseEntity<String>("Angegebenes Repo existiert bei " + repoService + " nicht!",
						HttpStatus.BAD_REQUEST);
			}

			// Baue Konfiguration aus dem Repo-Daten
			GitConfiguration updatedConfig = translator.createConfiguration(gitRepo, repoService);

			// Hole alte Konfigurationsdaten
			GitConfiguration oldConfig = repo.findById(configID).get();

			// Aktualisiere alte Konfiguration
			oldConfig = updatedConfig;
			repo.save(oldConfig);

			return new ResponseEntity<GitConfiguration>(updatedConfig, HttpStatus.OK);
		}
	}
}
