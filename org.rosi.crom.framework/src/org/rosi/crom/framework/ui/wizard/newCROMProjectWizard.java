package org.rosi.crom.framework.ui.wizard;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class newCROMProjectWizard extends Wizard implements INewWizard, IExecutableExtension {

	private WizardNewProjectCreationPage wizardPage;

	private IConfigurationElement config;

	private IWorkbench workbench;

	private IStructuredSelection selection;

	private IProject project;

	private static final String PAGE_NAME = "New CROM Project";
	private static final String PAGE_DESCRIPTION = "Create a new CROM Project.";
	private static final String PAGE_TITLE = "New CROM Project";
	private static final String WIZARD_NAME = "New CROM Project";

	/**
	 * Constructor
	 */
	public newCROMProjectWizard() {
		super();
		setWindowTitle(WIZARD_NAME);
	}

	// 设置新建页内容
	public void addPages() {

		wizardPage = new WizardNewProjectCreationPage(PAGE_NAME);
		wizardPage.setDescription(PAGE_DESCRIPTION);
		wizardPage.setTitle(PAGE_TITLE);
		addPage(wizardPage);
	}

	@Override
	public boolean performFinish() {

		if (project != null) {
			return true;
		}

		final IProject projectHandle = wizardPage.getProjectHandle();

		URI projectURI = (!wizardPage.useDefaults()) ? wizardPage.getLocationURI() : null;

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		final IProjectDescription desc = workspace.newProjectDescription(projectHandle.getName());

		final ICommand jbuilder = desc.newCommand();
		jbuilder.setBuilderName("org.eclipse.jdt.core.javabuilder");
		final ICommand manifestbuilder = desc.newCommand();
		manifestbuilder.setBuilderName("org.eclipse.pde.ManifestBuilder");
		final ICommand schemabuilder = desc.newCommand();
		schemabuilder.setBuilderName("org.eclipse.pde.SchemaBuilder");
		desc.setBuildSpec(new ICommand[] { jbuilder,manifestbuilder,schemabuilder });

		desc.setNatureIds(new String[] { "org.eclipse.jdt.core.javanature","org.eclipse.pde.PluginNature" });
		desc.setLocationURI(projectURI);
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			protected void execute(IProgressMonitor monitor) throws CoreException {
				createProject(desc, projectHandle, monitor);
			}
		};

		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}

		project = projectHandle;

		if (project == null) {
			return false;
		}

		BasicNewProjectResourceWizard.updatePerspective(config);
		BasicNewProjectResourceWizard.selectAndReveal(project, workbench.getActiveWorkbenchWindow());

		return true;
	}

	/**
	 * This creates the project in the workspace.
	 * 
	 * @param description
	 * @param projectHandle
	 * @param monitor
	 * @throws CoreException
	 * @throws OperationCanceledException
	 */
	void createProject(IProjectDescription description, IProject proj, IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		try {

			monitor.beginTask("", 2000);

			proj.create(description, new SubProgressMonitor(monitor, 1000));

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			proj.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));

			/*
			 * Okay, now we have the project and we can do more things with it before
			 * updating the perspective.
			 */
			IContainer container = (IContainer) proj;

			/* Add a file */
			// addFileToProject(container, new
			// Path("index.html"),NewXHTMLFileWizard.openContentStream("Welcome to "+
			// proj.getName()), monitor);

			/* Add a folder */
			final IFolder modelFolder = container.getFolder(new Path("model"));
			modelFolder.create(true, true, monitor);
			final IFolder srcFolder = container.getFolder(new Path("src"));
			srcFolder.create(true, true, monitor);
			final IFolder binFolder = container.getFolder(new Path("bin"));
			binFolder.create(true, true, monitor);
			final IFolder jsonFolder = container.getFolder(new Path("instance"));
			jsonFolder.create(true, true, monitor);
			final IFolder libFolder = container.getFolder(new Path("lib"));
			libFolder.create(true, true, monitor);
			final IFolder METAFolder = container.getFolder(new Path("META-INF"));
			METAFolder.create(true, true, monitor);
			
			InputStream classpath = this.getClass().getResourceAsStream("templates/classpath.resource");
			addFileToProject(container, new Path(".classpath"), classpath, monitor);

			try {
				classpath.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			InputStream build = this.getClass().getResourceAsStream("templates/build.properties.resource");
			addFileToProject(container, new Path("build.properties"), build, monitor);

			try {
				build.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			InputStream manifest = this.getClass().getResourceAsStream("templates/MANIFEST.MF.resource");
			addFileToProject(container, new Path("META-INF/MANIFEST.MF"), manifest, monitor);

			try {
				manifest.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			

			InputStream gsonJar = this.getClass().getResourceAsStream("templates/gson-2.6.2.jar");
			addFileToProject(container, new Path("lib/gson-2.6.2.jar"), gsonJar, monitor);
			try {
				gsonJar.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			/*
			 * Add the images folder, which is an official Exmample.com standard for static
			 * web projects.
			 */
			/*
			 * IFolder imageFolder = container.getFolder(new Path("images"));
			 * imageFolder.create(true, true, monitor); } catch (IOException ioe) { IStatus
			 * status = new Status(IStatus.ERROR, "NewFileWizard", IStatus.OK,
			 * ioe.getLocalizedMessage(), null); throw new CoreException(status);
			 **/
		} finally {
			monitor.done();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
	 * org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
		this.workbench = workbench;
	}

	/**
	 * Sets the initialization data for the wizard.
	 */
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
		this.config = config;
	}

	/**
	 * Adds a new file to the project.
	 * 
	 * @param container
	 * @param path
	 * @param contentStream
	 * @param monitor
	 * @throws CoreException
	 */
	private void addFileToProject(IContainer container, Path path, InputStream contentStream, IProgressMonitor monitor)
			throws CoreException {
		final IFile file = container.getFile(path);

		if (file.exists()) {
			file.setContents(contentStream, true, true, monitor);
		} else {
			file.create(contentStream, true, monitor);
		}
	}

}