package de.BA.refactoringBot.rest;

import java.io.File;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.BA.refactoringBot.api.main.ApiGrabber;
import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.controller.github.GithubObjectTranslator;
import de.BA.refactoringBot.controller.main.GitController;
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
	@Autowired
	BotConfiguration botConfig;
	@Autowired
	GitController gitController;

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
			@RequestParam(value = "repoService", required = true, defaultValue = "Github") String repoService,
			@RequestParam(value = "repoName", required = true, defaultValue = "RefactoringTest") String repoName,
			@RequestParam(value = "ownerName", required = true, defaultValue = "TimoPfaff") String repoOwner,
			@RequestParam(value = "ProjectRootFolder", required = true, defaultValue = "Calculator") String projectRootFolder,
			@RequestParam(value = "botUsername", required = true, defaultValue = "LHommeDeBot") String botUsername,
			@RequestParam(value = "botPassword", required = true, defaultValue = "Botboy55") String botPassword,
			@RequestParam(value = "botToken", required = true) String botToken,
			@RequestParam(value = "analysisService", required = false, defaultValue = "SonarCube") String analysisService,
			@RequestParam(value = "analysisServiceProjectKey", required = false) String analysisServiceProjectKey,
			@RequestParam(value = "maxAmountRequests", required = true, defaultValue = "5") Integer maxAmountRequests) {
		// Schaue ob Repository schon existiert
		Optional<GitConfiguration> existsConfig = repo.getConfigByName(repoName, repoOwner);
		// Falls existiert
		if (existsConfig.isPresent()) {
			return new ResponseEntity<String>("Diese Kombination von Repository- und Besitzername existiert schon",
					HttpStatus.CONFLICT);
		}

		try {
			// Baue Konfiguration aus dem Repo-Daten
			GitConfiguration config = grabber.createConfigurationForRepo(repoName, repoOwner, repoService, botUsername,
					botPassword, botToken, analysisService, analysisServiceProjectKey, maxAmountRequests, projectRootFolder);
			// Speichere Konfiguration in DB
			GitConfiguration savedConfig = repo.save(config);
			// Erstelle Ordner für Fork
			File dir = new File(botConfig.getBotRefactoringDirectory() + savedConfig.getConfigurationId());
			dir.mkdir();
			// Erstelle Fork auf Filehoster
			grabber.createFork(savedConfig);
			// Hole Fork
			gitController.initLocalWorkspace(savedConfig);
			// Gebe Feedback an Nutzer zurück
			return new ResponseEntity<GitConfiguration>(config, HttpStatus.CREATED);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
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
			@RequestParam(value = "configurationId", required = true) Long configurationId) {
		// Schaue ob Repository existiert
		Optional<GitConfiguration> existsConfig = repo.getByID(configurationId);
		// Falls existiert
		if (existsConfig.isPresent()) {
			try {
				// Lösche Repository beim Filehoster
				grabber.deleteRepository(existsConfig.get());
				// Lösche Konfiguration aus der DB
				repo.delete(existsConfig.get());
				// Lösche lokalen Ordner
				File forkFolder = new File(botConfig.getBotRefactoringDirectory()
						+ existsConfig.get().getConfigurationId());
                FileUtils.deleteDirectory(forkFolder);
				
				return new ResponseEntity<String>("Konfiguration erfolgreich gelöscht!", HttpStatus.OK);
			} catch (Exception e) {
				e.printStackTrace();
				return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
