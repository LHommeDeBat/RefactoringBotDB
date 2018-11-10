package de.BA.refactoringBot.controller.main;

import java.io.File;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
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
	 * Diese Methode Pullt das gewünschte Repository mittels einer URL.
	 * 
	 * @param repoURL
	 * @throws Exception
	 */
	public void pullGithubRepo(String repoURL) throws Exception {
		try {
			// Lösche zunächst den Arbeitsordner
			FileUtils.deleteDirectory(new File(botConfig.getBotWorkingDirectory()));

			// Klone Repo aus der Konfiguration in Arbeitsverzeichnis
			Git git = Git.cloneRepository().setURI(repoURL).setDirectory(new File(botConfig.getBotWorkingDirectory()))
					.call();
			git.close();
		} catch (Exception e) {
			throw new Exception("Konnte Repository " + "'" + repoURL + "' nicht erfolgreich Klonen/Pullen!");
		}
	}

	/**
	 * Diese Methode wechselt den Branch vom gepullten Repository welches aktuell im
	 * Arbeitsverzeichnis des Bots ist.
	 * 
	 * @param branchName
	 * @throws Exception
	 */
	public void checkoutBranch(String branchName) throws Exception {
		// Branchwechsel nur wenn nicht 'master' da beim Clonen master geholt wird
		if (branchName != "master") {
			try {
				// Öffne Arbeitsverzeichnis
				Git git = Git.open(new File(botConfig.getBotWorkingDirectory()));
				// Wechsle auf Branch des PullRequests
				@SuppressWarnings("unused")
				Ref ref = git.checkout().setCreateBranch(true).setName(branchName)
						.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
						.setStartPoint("origin/" + branchName).call();
				// Pulle Daten
				git.pull();
				git.close();
			} catch (Exception e) {
				throw new Exception("Konnte kein Checkout vom Branch " + "'" + branchName + "' durchführen!");
			}
		}
	}

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
			Git git = Git.open(new File(botConfig.getBotWorkingDirectory()));
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
