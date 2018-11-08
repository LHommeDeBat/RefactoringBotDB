package de.BA.refactoringBot.rest;

import java.net.URISyntaxException;
import java.util.Optional;

import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import de.BA.refactoringBot.api.main.ApiGrabber;
import de.BA.refactoringBot.controller.github.GithubObjectTranslator;
import de.BA.refactoringBot.model.configuration.ConfigurationRepository;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
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
	ApiGrabber grabber;
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
			@RequestParam(value = "botUsername", required = true, defaultValue = "LHommeDeBot") String botUsername,
			@RequestParam(value = "botPassword", required = true, defaultValue = "Botboy55") String botPassword,
			@RequestParam(value = "botToken", required = true, defaultValue = "Token hier eingeben") String botToken) {
		// Schaue ob Repository schon existiert
		Optional<GitConfiguration> existsConfig = repo.getConfigByName(repoName, repoOwner);
		// Falls existiert
		if (existsConfig.isPresent()) {
			return new ResponseEntity<String>("Diese Kombination von Repository- und Besitzername existiert schon",
					HttpStatus.CONFLICT);
		}

		try {
			// Baue Konfiguration aus dem Repo-Daten
			GitConfiguration config = grabber.createConfigurationForRepo(repoName, repoOwner, repoService, botUsername, botPassword, botToken);
			// Speichere Konfiguration in DB
			repo.save(config);
			// Gebe Feedback an Nutzer zurück
			return new ResponseEntity<GitConfiguration>(config, HttpStatus.CREATED);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return new ResponseEntity<String>("Ungültige URI!", HttpStatus.BAD_REQUEST);
		} catch (RestClientException r) {
			r.printStackTrace();
			return new ResponseEntity<String>("Fehler mit der Verbindung zum angegebenen Repository!", HttpStatus.SERVICE_UNAVAILABLE);
		} catch (OperationNotSupportedException e) {
			return new ResponseEntity<String>("Der Filehoster '" + repoService + "' wird nicht unterstützt!", HttpStatus.BAD_REQUEST);
		}
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
			@RequestParam(value = "botName", required = true, defaultValue = "LHommeDeBot") String botName,
			@RequestParam(value = "repoService", required = true, defaultValue = "Github") String repoService,
			@RequestParam(value = "repoAdminToken", required = true, defaultValue = "Token hier Eingaben") String repoAdminToken) {
		// Schaue ob Repository existiert
		Optional<GitConfiguration> existsConfig = repo.getConfigByFork(repoName, botName);
		// Falls existiert
		if (existsConfig.isPresent()) {
			try {
				grabber.deleteRepository(existsConfig.get());
				repo.delete(existsConfig.get());
				return new ResponseEntity<String>("Konfiguration erfolgreich gelöscht!", HttpStatus.OK);
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return new ResponseEntity<String>("Konnte Fork-Repo nicht löschen!", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else {
			return new ResponseEntity<String>("Kein Repository mit angegebenen Eigenschaften gefunden!",
					HttpStatus.NOT_FOUND);
		}
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
}
