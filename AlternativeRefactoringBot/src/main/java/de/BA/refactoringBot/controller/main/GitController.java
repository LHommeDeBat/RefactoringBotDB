package de.BA.refactoringBot.controller.main;

import java.io.File;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.model.configuration.GitConfiguration;

/**
 * Diese Klasse nutzt Git programmatisch mittels JGit.
 * 
 * @author Stefan Basaric
 *
 */
@Component
public class GitController {

	@Autowired
	BotConfiguration botConfig;

	/**
	 * Diese Methode initiiert das lokale Workspace.
	 * 
	 * @param gitConfig
	 * @throws Exception
	 */
	public void initLocalWorkspace(GitConfiguration gitConfig) throws Exception {
		// Klone Fork
		cloneRepository(gitConfig);
		// Füge Remote (OG-Repo) zum Fork hinzu
		addRemote(gitConfig);
	}

	/**
	 * Diese Methode fügt ein Remote zum Projekt hinzu.
	 * 
	 * @param gitConfig
	 * @throws Exception
	 */
	public void addRemote(GitConfiguration gitConfig) throws Exception {
		// Öffne Arbeitsverzeichnis
		Git git;
		try {
			git = Git.open(new File(botConfig.getBotRefactoringDirectory() + gitConfig.getConfigurationId()));
			RemoteAddCommand remoteAddCommand = git.remoteAdd();
			remoteAddCommand.setName("upstream");
			remoteAddCommand.setUri(new URIish(gitConfig.getRepoGitLink()));
			remoteAddCommand.call();
			git.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(
					"Konnte Remote " + "'" + gitConfig.getRepoGitLink() + "' nicht erfolgreich hinzufügen!");
		}
	}

	/**
	 * Diese Methode fetcht die Daten eines Repotes.
	 * 
	 * @param gitConfig
	 * @throws Exception
	 */
	public void fetchRemote(GitConfiguration gitConfig) throws Exception {
		// Öffne Arbeitsverzeichnis
		try {
			Git git = Git.open(new File(botConfig.getBotRefactoringDirectory() + gitConfig.getConfigurationId()));
			git.fetch().setRemote("upstream").call();
			git.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Konnte Remote 'upstream' nicht erfolgreich fetchen!");
		}
	}

	/**
	 * Diese Methode verwirft alle Änderungen seit dem letzten Commit auf dem
	 * aktuellen Git-Branch.
	 * 
	 * @param gitConfig
	 * @throws Exception
	 */
	public void stashChanges(GitConfiguration gitConfig) throws Exception {
		// Öffne Arbeitsverzeichnis
		try {
			Git git = Git.open(new File(botConfig.getBotRefactoringDirectory() + gitConfig.getConfigurationId()));
			git.stashApply().call();
			git.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Konnte Änderungen nicht stashen!");
		}
	}

	/**
	 * Diese Methode klont das gewünschte Repository mittels einer URL.
	 * 
	 * @param repoURL
	 * @throws Exception
	 */
	public void cloneRepository(GitConfiguration gitConfig) throws Exception {
		try {
			// Klone Repo aus der Konfiguration in Arbeitsverzeichnis
			Git git = Git.cloneRepository().setURI(gitConfig.getForkGitLink())
					.setDirectory(new File(botConfig.getBotRefactoringDirectory() + gitConfig.getConfigurationId()))
					.call();
			git.close();
		} catch (Exception e) {
			throw new Exception(
					"Konnte Repository " + "'" + gitConfig.getForkGitLink() + "' nicht erfolgreich Klonen/Pullen!");
		}
	}

	/**
	 * Diese Methode Pullt das gewünschte Repository mittels einer URL.
	 * 
	 * @param repoURL
	 * @throws Exception
	 */
	public void pullGithubRepo(String repoURL) throws Exception {
		try {
			// Lösche zunächst den Arbeitsordner
			FileUtils.deleteDirectory(new File(botConfig.getBotRefactoringDirectory()));

			// Klone Repo aus der Konfiguration in Arbeitsverzeichnis
			Git git = Git.cloneRepository().setURI(repoURL)
					.setDirectory(new File(botConfig.getBotRefactoringDirectory())).call();
			git.close();
		} catch (Exception e) {
			throw new Exception("Konnte Repository " + "'" + repoURL + "' nicht erfolgreich Klonen/Pullen!");
		}
	}

	/**
	 * Diese Methode wechselt den Branch vom gepullten Repository welches aktuell im
	 * Arbeitsverzeichnis des Bots ist.
	 * 
	 * @param gitConfig
	 * @param branchName
	 * @param origin
	 * @param id
	 * @throws Exception
	 */
	public void createBranch(GitConfiguration gitConfig, String branchName, String newBranch) throws Exception {
		// Branchwechsel nur wenn nicht 'master' da beim Clonen master geholt wird
		try {
			// Öffne Arbeitsverzeichnis
			Git git = Git.open(new File(botConfig.getBotRefactoringDirectory() + gitConfig.getConfigurationId()));
			// Versuche Branch zu erstellen
			@SuppressWarnings("unused")
			Ref ref = git.checkout().setCreateBranch(true).setName(newBranch)
					.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
					.setStartPoint("upstream/" + branchName).call();
			// Pulle Daten
			git.pull();
			git.close();
			// Falls Branch existiert wechsle zu diesem
		} catch (RefAlreadyExistsException r) {
			switchBranch(gitConfig, newBranch);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Konnte den Branch mit dem Namen " + "'" + newBranch + "' nicht erstellen!");
		}
	}

	/**
	 * Diese Methode wechselt den Branch vom gepullten Repository welches aktuell im
	 * Arbeitsverzeichnis des Bots ist.
	 * 
	 * @param branchName
	 * @throws Exception
	 */

	  public void switchBranch(GitConfiguration gitConfig, String branchName) throws Exception { 
		  // Branchwechsel nur wenn nicht 'master' da beim Clonen master geholt wird 
try { 
	// Öffne Arbeitsverzeichnis 
	  Git git = Git.open(new File(botConfig.getBotRefactoringDirectory() + gitConfig.getConfigurationId())); 
	  
	  // Wechsle auf Branch des PullRequests
	  @SuppressWarnings("unused") 
	  Ref ref = git.checkout().setName(branchName).call(); // Pulle Daten git.pull();
	  git.close(); // Falls Branch nicht existiert, dann erstelle Branch 
	  } catch (Exception e) { 
	  e.printStackTrace(); 
	  throw new Exception("Konnte nicht zu Branch " + "'" + branchName + "' wechseln!"); } }
	 

	/**
	 * Diese Methode pusht alle Änderungen welche im Repository durchgeführt wurden.
	 * Dabei handelt es sich um das Repository welches aktuell im Arbeitsverzeichnis
	 * des Bots ist.
	 * 
	 * @throws Exception
	 */
	public void pushChanges(GitConfiguration gitConfig, String commitMessage) throws Exception {
		try {
			// Öffne Arbeitsverzeichnis
			Git git = Git.open(new File(botConfig.getBotRefactoringDirectory() + gitConfig.getConfigurationId()));
			// Führe 'git add .' aus
			git.add().addFilepattern(".").call();
			// Mache einen commit (Aktuell hardgecodete Nachricht)
			git.commit().setMessage(commitMessage).call();
			// Pushe mit Bot-Daten
			git.push()
					.setCredentialsProvider(
							new UsernamePasswordCredentialsProvider(gitConfig.getBotName(), gitConfig.getBotPassword()))
					.call();
			git.close();
		} catch (Exception e) {
			throw new Exception("Konnte nicht erfolgreich Git-Pushe ausführen!");
		}
	}
}
