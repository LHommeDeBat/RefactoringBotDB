package de.BA.refactoringBot.controller;

import java.io.File;
import java.io.IOException;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
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
	 * @throws InvalidRemoteException
	 * @throws TransportException
	 * @throws GitAPIException
	 * @throws IOException 
	 */
	public void pullGithubRepo(String repoURL) throws InvalidRemoteException, TransportException, GitAPIException, IOException {
		// Lösche zunächst den Arbeitsordner
		FileUtils.deleteDirectory(new File(botConfig.getBotWorkingDirectory()));
		
		// Klone Repo aus der Konfiguration in Arbeitsverzeichnis
		Git git = Git.cloneRepository().setURI(repoURL).setDirectory(new File(botConfig.getBotWorkingDirectory()))
				.call();
		git.close();
	}

	/**
	 * Diese Methode wechselt den Branch vom gepullten Repository welches aktuell im
	 * Arbeitsverzeichnis des Bots ist.
	 * 
	 * @param branchName
	 * @throws IOException
	 * @throws RefAlreadyExistsException
	 * @throws RefNotFoundException
	 * @throws InvalidRefNameException
	 * @throws CheckoutConflictException
	 * @throws GitAPIException
	 */
	public void checkoutBranch(String branchName) throws IOException, RefAlreadyExistsException, RefNotFoundException,
			InvalidRefNameException, CheckoutConflictException, GitAPIException {
		// Öffne Arbeitsverzeichnis
		Git git = Git.open(new File(botConfig.getBotWorkingDirectory()));
		// Wechsle auf Branch des PullRequests
		@SuppressWarnings("unused")
		Ref ref = git.checkout().setCreateBranch(true).setName(branchName)
				.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).setStartPoint("origin/" + branchName)
				.call();
		// Pulle Daten
		git.pull();
		git.close();
	}

	/**
	 * Diese Methode pusht alle Änderungen welche im Repository durchgeführt wurden.
	 * Dabei handelt es sich um das Repository welches aktuell im Arbeitsverzeichnis
	 * des Bots ist.
	 * 
	 * @throws IOException
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws UnmergedPathsException
	 * @throws ConcurrentRefUpdateException
	 * @throws WrongRepositoryStateException
	 * @throws AbortedByHookException
	 * @throws GitAPIException
	 */
	public void pushChanges(GitConfiguration gitConfig) throws IOException, NoHeadException, NoMessageException, UnmergedPathsException,
			ConcurrentRefUpdateException, WrongRepositoryStateException, AbortedByHookException, GitAPIException {
		// Öffne Arbeitsverzeichnis
		Git git = Git.open(new File(botConfig.getBotWorkingDirectory()));
		// Führe 'git add .' aus
		git.add().addFilepattern(".").call();
		// Mache einen commit (Aktuell hardgecodete Nachricht)
		git.commit().setMessage("Bot hat eine Textdatei hinzugefügt.").call();
		// Pushe mit Bot-Daten
		git.push()
				.setCredentialsProvider(
						new UsernamePasswordCredentialsProvider(gitConfig.getBotName(), gitConfig.getBotPassword()))
				.call();
		git.close();
	}
}
