package com.byluroid.eclipse.maven.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;

public class AndroidMavenBuildParticipant extends AbstractBuildParticipant {

	MojoExecution execution;

	public AndroidMavenBuildParticipant (MojoExecution execution) {
		this.execution = execution;
	}

	@Override
	public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
		if(IncrementalProjectBuilder.CLEAN_BUILD == kind || IncrementalProjectBuilder.FULL_BUILD == kind) {
			MavenPlugin plugin = MavenPlugin.getDefault();
			MavenProjectManager projectManager = plugin.getMavenProjectManager();
			IMaven maven = plugin.getMaven();
			IProject project = getMavenProjectFacade().getProject();
			IFile pom = project.getFile(new Path(IMavenConstants.POM_FILE_NAME));
			IMavenProjectFacade projectFacade = projectManager.create(pom, false, monitor);
			ResolverConfiguration resolverConfiguration = projectFacade.getResolverConfiguration();
			MavenExecutionRequest request = projectManager.createExecutionRequest(pom, resolverConfiguration, monitor);

			List<String> goals = new ArrayList<String>();
			goals.add("package");
			request.setGoals(goals);

			Properties properties = request.getUserProperties();
			properties.setProperty("maven.test.skip", "true");
			request.setUserProperties(properties);

			MavenExecutionResult executionResult = maven.execute(request, monitor);

			if (executionResult.hasExceptions()){
				List<Throwable> exceptions = executionResult.getExceptions();
				for (Throwable throwable : exceptions) {
					throwable.printStackTrace();
				}
			}else{
				Artifact apkArtifact = executionResult.getProject().getArtifact();
				if ("apk".equals(apkArtifact.getType())){
					File apkFile = apkArtifact.getFile();
					IJavaProject javaProject = JavaCore.create(project);
					IPath outputLocation = javaProject.getOutputLocation();
					File realOutputFolder = project.getWorkspace().getRoot().getFolder(outputLocation).getLocation().toFile();
					String newApkFilename = project.getName() + ".apk";
					File newApkFile = new File(realOutputFolder, newApkFilename);
					FileUtils.copyFile(apkFile, newApkFile);
				}
			}
		}
		return null;
	}

}
