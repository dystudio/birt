/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.designer.ui.internal.rcp.wizards;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.birt.report.designer.internal.ui.util.ExceptionHandler;
import org.eclipse.birt.report.designer.internal.ui.wizards.WizardReportSettingPage;
import org.eclipse.birt.report.designer.internal.ui.wizards.WizardTemplateChoicePage;
import org.eclipse.birt.report.designer.nls.Messages;
import org.eclipse.birt.report.designer.ui.ReportPlugin;
import org.eclipse.birt.report.designer.ui.editors.RCPReportEditor;
import org.eclipse.birt.report.designer.ui.editors.ReportEditorInput;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;

/**
 * An implementation of <code>INewWizard</code>. Creates a new blank report
 * file.
 */

public class NewReportWizard extends Wizard implements
		INewWizard,
		IExecutableExtension
{

	private static final String NEW = Messages.getString( "NewReportWizard.title.New" ); //$NON-NLS-1$

	private static final String REPORT = Messages.getString( "NewReportWizard.title.Report" ); //$NON-NLS-1$

	private static final String WIZARDPAGE = Messages.getString( "NewReportWizard.title.WizardPage" ); //$NON-NLS-1$

	private static final String TEMPLATECHOICEPAGE = Messages.getString( "NewReportWizard.title.Template" ); //$NON-NLS-1$

	private static final String CREATE_A_NEW_REPORT = Messages.getString( "NewReportWizard.text.CreateReport" ); //$NON-NLS-1$

	private static final String SELECT_A_REPORT_TEMPLATE = Messages.getString( "NewReportWizard.text.SelectTemplate" ); //$NON-NLS-1$

	private static final String CREATING = Messages.getString( "NewReportWizard.text.Creating" ); //$NON-NLS-1$

	private static final String OPENING_FILE_FOR_EDITING = Messages.getString( "NewReportWizard.text.OpenFileForEditing" ); //$NON-NLS-1$

	private static final String NEW_REPORT_FILE_NAME_PREFIX = Messages.getString( "NewReportWizard.displayName.NewReportFileNamePrefix" ); //$NON-NLS-1$

	private static final String NEW_REPORT_FILE_EXTENSION = ".rptdesign"; //$NON-NLS-1$

	private WizardNewReportCreationPage newReportFileWizardPage;

	private WizardTemplateChoicePage templateChoicePage;

	private WizardReportSettingPage settingPage;

	public NewReportWizard( )
	{
		setWindowTitle( NEW );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#getDefaultPageImage()
	 */
	public Image getDefaultPageImage( )
	{
		return ReportPlugin.getImage( "/icons/wizban/create_report_wizard.gif" ); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages( )
	{
		newReportFileWizardPage = new WizardNewReportCreationPage( WIZARDPAGE );

		addPage( newReportFileWizardPage );

		templateChoicePage = new WizardTemplateChoicePage( TEMPLATECHOICEPAGE );
		addPage( templateChoicePage );

		// set titles
		newReportFileWizardPage.setTitle( REPORT );
		newReportFileWizardPage.setDescription( CREATE_A_NEW_REPORT );
		templateChoicePage.setTitle( REPORT );
		templateChoicePage.setDescription( SELECT_A_REPORT_TEMPLATE );

		settingPage = new WizardReportSettingPage( null );
		settingPage.setTitle( Messages.getFormattedString( "SaveReportAsWizard.SettingPage.title",//$NON-NLS-1$
				new Object[]{
					Messages.getString( "NewReportWizard.wizardPageTitle.report" )} ) );//$NON-NLS-1$

		addPage( settingPage );

		// initialize new report file page.
		newReportFileWizardPage.setInitialFileName( getNewFileFullName( NEW_REPORT_FILE_NAME_PREFIX ) );
		newReportFileWizardPage.setInitialFileLocation( getDefaultLocation( ) );
	}

	/**
	 * Get the defualt location for the provided name.
	 * 
	 * @return the location
	 */
	private String getDefaultLocation( )
	{
		IPath defaultPath = Platform.getLocation( );
		return defaultPath.toOSString( );
	}

	private String getNewFileFullName( String defaultName )
	{
		String extension = NEW_REPORT_FILE_EXTENSION;
		String path = getDefaultLocation( );
		String name = defaultName + extension;

		int count = 0;

		File file;

		file = new File( path, name );

		while ( file.exists( ) )
		{
			count++;
			name = defaultName + "_" + count + extension; //$NON-NLS-1$
			file = null;
			file = new File( path, name );
		}

		file = null;

		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish( )
	{
		final IPath locPath = newReportFileWizardPage.getFileLocationFullPath( );
		String fn = newReportFileWizardPage.getFileName( );

		final String fileName;
		if ( !fn.endsWith( ".rptdesign" ) ) //$NON-NLS-1$
		{
			fileName = fn + ".rptdesign"; //$NON-NLS-1$
		}
		else
		{
			fileName = fn;
		}
		InputStream inputData = null;
		String cheatSheetIdFromPage = "";//$NON-NLS-1$
		boolean showCheatSheetFromPage = false;

		URL url = Platform.find( Platform.getBundle( ReportPlugin.REPORT_UI ),
				new Path( templateChoicePage.getTemplate( ).getReportPath( ) ) );
		if ( url != null )
		{
			try
			{
				inputData = url.openStream( );
			}
			catch ( IOException e1 )
			{
				return false;
			}
		}
		else
		{
			File file = new File( templateChoicePage.getTemplate( )
					.getReportPath( ) );
			if ( file.exists( ) )
			{
				try
				{
					inputData = new FileInputStream( file );
				}
				catch ( FileNotFoundException e )
				{
					return false;
				}
			}
		}

		cheatSheetIdFromPage = templateChoicePage.getTemplate( )
				.getCheatSheetId( );
		showCheatSheetFromPage = templateChoicePage.getShowCheatSheet( );
		final InputStream stream = inputData;
		final String cheatSheetId = cheatSheetIdFromPage;
		final boolean showCheatSheet = showCheatSheetFromPage;
		IRunnableWithProgress op = new IRunnableWithProgress( ) {

			public void run( IProgressMonitor monitor )
			{
				try
				{
					doFinish( locPath,
							fileName,
							stream,
							cheatSheetId,
							showCheatSheet,
							monitor );
				}
				finally
				{
					monitor.done( );
				}
			}
		};
		try
		{
			getContainer( ).run( true, false, op );
		}
		catch ( InterruptedException e )
		{
			return false;
		}
		catch ( InvocationTargetException e )
		{
			Throwable realException = e.getTargetException( );
			ExceptionHandler.handle( realException );
			return false;
		}
		return true;
	}

	/**
	 * The worker method. It will find the container, create the file if missing
	 * or just replace its contents, and open the editor on the newly created
	 * file.
	 * 
	 * @param cheatSheetId
	 * 
	 * @param locationPath
	 * @param fileName
	 * @param showCheatSheet
	 * @param monitor
	 */

	private void doFinish( IPath locationPath, String fileName,
			InputStream stream, final String cheatSheetId,
			final boolean showCheatSheet, IProgressMonitor monitor )
	{
		// create a sample file
		monitor.beginTask( CREATING + fileName, 2 );

		final File file = new File( locationPath.toString( ), fileName );
		try
		{
			File container = new File( locationPath.toString( ) );
			if ( !container.exists( ) )
			{
				container.mkdirs( );
			}
			FileOutputStream out = new FileOutputStream( file );
			byte[] buff = new byte[stream.available( )];
			stream.read( buff );
			out.write( buff );
			out.close( );
			stream.close( );
		}
		catch ( IOException e )
		{
			ExceptionHandler.handle( e );
		}

		monitor.worked( 1 );
		monitor.setTaskName( OPENING_FILE_FOR_EDITING );
		getShell( ).getDisplay( ).asyncExec( new Runnable( ) {

			public void run( )
			{
				IWorkbench workbench = PlatformUI.getWorkbench( );
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow( );

				IWorkbenchPage page = window.getActivePage( );
				try
				{
					// sanity checks
					if ( page == null )
					{
						throw new IllegalArgumentException( );
					}

					// open the editor on the file
					IEditorPart editorPart = page.openEditor( new ReportEditorInput( file ),
							ReportEditorInput.REPORT_EDITOR_ID,
							true );
					setReportSettings( ( (RCPReportEditor) editorPart ).getModel( ) );
					editorPart.doSave( null );

					if ( showCheatSheet && !cheatSheetId.equals( "" ) ) //$NON-NLS-1$
					{
						OpenCheatSheetAction action = new OpenCheatSheetAction( cheatSheetId );
						action.run( );
					}
				}
				catch ( Exception e )
				{
					ExceptionHandler.handle( e );
				}
			}
		} );

		monitor.worked( 1 );

	} /*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.wizard.IWizard#canFinish()
		 */

	public boolean canFinish( )
	{
		return templateChoicePage.isPageComplete( )
				&& newReportFileWizardPage.isPageComplete( );
	}

	public void init( IWorkbench workbench, IStructuredSelection selection )
	{
		// TODO Auto-generated method stub

	}

	public void setInitializationData( IConfigurationElement config,
			String propertyName, Object data ) throws CoreException
	{
		// TODO Auto-generated method stub

	}

	/**
	 * Set report basic settings.
	 * 
	 * @param model
	 * @throws IOException
	 */
	void setReportSettings( Object model ) throws IOException
	{
		ReportDesignHandle handle = (ReportDesignHandle) model;
		try
		{
			handle.setDisplayName( settingPage.getDisplayName( ) );
			handle.setDescription( settingPage.getDescription( ) );
			handle.setIconFile( settingPage.getPreviewImagePath( ) );
		}
		catch ( SemanticException e )
		{
		}
		handle.save( );
	}
}