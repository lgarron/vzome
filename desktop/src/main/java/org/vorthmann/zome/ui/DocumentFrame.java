package org.vorthmann.zome.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.FileDialog;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.ToolTipManager;

import org.vorthmann.j3d.J3dComponentFactory;
import org.vorthmann.j3d.Platform;
import org.vorthmann.ui.Controller;
import org.vorthmann.ui.DefaultController;
import org.vorthmann.ui.ExclusiveAction;

import com.vzome.core.render.Scene;
import com.vzome.desktop.controller.RenderingViewer;

public class DocumentFrame extends JFrame implements PropertyChangeListener, ControlActions
{
    private static final long serialVersionUID = 1L;

    protected final Controller mController, toolsController;

    private final ModelPanel modelPanel;
            
    private final JTabbedPane tabbedPane = new JTabbedPane();
    
    private final ExclusiveAction.Excluder mExcluder = new ExclusiveAction.Excluder( this );

    private boolean isEditor, fullPower, readerPreview, canSave;

    private CardLayout modelArticleCardLayout;

    private static final Logger logger = Logger.getLogger( "org.vorthmann.zome.ui" );

    private LessonPanel lessonPanel;
    
    private JFrame zomicFrame, zomodFrame, pythonFrame;

    private JButton snapshotButton;

    private JRadioButton articleButton, modelButton;

    private JPanel viewControl, modelArticleEditPanel;

    private JLabel statusText;

    private Controller cameraController;

    private Controller lessonController;
    
    private JDialog polytopesDialog, importScaleDialog;

	private final boolean developerExtras;
	
	private final ActionListener localActions;
	    
    private final FileDialog fileDialog = new FileDialog( this );
    
	private File mFile = null;
	
	private final Controller.ErrorChannel errors;

    private Snapshot2dFrame snapshot2dFrame;

	private ControllerFileAction saveAsAction;

	private PropertyChangeListener appUI;
    
    public ExclusiveAction.Excluder getExcluder()
    {
        return mExcluder;
    }
    
    private void createLessonPanel()
    {
    	if ( lessonPanel != null )
    		return;
        lessonPanel = new LessonPanel( lessonController );
        modelArticleEditPanel .add( lessonPanel, "article" );
    }
    
    void setAppUI( PropertyChangeListener appUI )
    {
    	this .appUI = appUI;
    }
        
    public DocumentFrame( final Controller controller, final J3dComponentFactory factory3d )
    {
        mController = controller;
        mController .addPropertyListener( this );
        toolsController = mController .getSubController( "tools" );
        
        // Keep the tool tip showing
        ToolTipManager.sharedInstance().setDismissDelay( 20000 );

        String path = mController .getProperty( "window.file" );
        if ( path != null )
            this .mFile = new File( path ); // this enables "save" in localActions

        // TODO: compute these booleans once here, and don't recompute in DocumentMenuBar

        this.readerPreview = mController .propertyIsTrue( "reader.preview" );

        this.isEditor = mController .userHasEntitlement( "model.edit" ) && ! readerPreview;

        this.canSave = mController .userHasEntitlement( "save.files" );

        this.fullPower = isEditor ;//&& controller .userHasEntitlement( "all.tools" );
        
        this.developerExtras = fullPower && mController .userHasEntitlement( "developer.extras" );
        
        errors = new Controller.ErrorChannel()
        {
			@Override
            public void reportError( String errorCode, Object[] arguments )
            {
                if ( Controller.USER_ERROR_CODE.equals( errorCode ) ) {
                    errorCode = ( (Exception) arguments[0] ).getMessage();
                    // don't want a stack trace for a user error
                    logger.log( Level.WARNING, errorCode );
                } else if ( Controller.UNKNOWN_ERROR_CODE.equals( errorCode ) ) {
                    errorCode = ( (Exception) arguments[0] ).getMessage();
                    logger.log( Level.WARNING, "internal error: " + errorCode, ( (Exception) arguments[0] ) );
                    errorCode = "internal error has been logged";
                } else {
                    logger.log( Level.WARNING, "reporting error: " + errorCode, arguments );
                    // TODO use resources
                }

                if ( statusText != null )
                    statusText .setText( errorCode );
                else
                    JOptionPane .showMessageDialog( DocumentFrame.this, errorCode, "Command Failure", JOptionPane .ERROR_MESSAGE );
            }

			@Override
            public void clearError()
            {
                if ( statusText != null )
                    statusText .setText( "" );
            }
        };
        mController .setErrorChannel( errors );

        // ---- catch-all ActionListener for locally-handled actions

        final String initSystem = mController .getProperty( "symmetry" );
        localActions = new ActionListener()
        {
            private String system = initSystem;

            private final Map<String, JDialog> shapesDialogs = new HashMap<>();

            private final Map<String, JDialog> directionsDialogs = new HashMap<>();

			@Override
            public void actionPerformed( ActionEvent e )
            {
				Controller delegate = mController;
                String cmd = e.getActionCommand();
                switch ( cmd ) {
                
                case "close":
                    closeWindow();
                    break;

//            	case "openURL":
//                    String url = JOptionPane .showInputDialog( DocumentFrame.this, "Enter the URL for an online .vZome file.", "Open URL",
//                            JOptionPane.PLAIN_MESSAGE );
//                    try {
//                        mController .doAction( cmd, new ActionEvent( DocumentFrame.this, ActionEvent.ACTION_PERFORMED, url ) );
//                    } catch ( Exception ex )
//                    {
//                        ex .printStackTrace();
//                        // TODO better error report
//                        errors .reportError( Controller.USER_ERROR_CODE, new Object[]{ ex } );
//                    }
//                    break;
//                    
                case "save":
                    if ( mFile == null ) {
                        saveAsAction .actionPerformed( e );
                    }
                    else {
                        mController .doFileAction( "save", mFile );
                    }
                    break;

                case "saveDefault":
                    // this is basically "save a copy...", with a hard-coded file path.
                    String fieldName = mController.getProperty( "field.name" );
                    String fieldLabel = mController.getProperty( "field.label" );
                    File prototype = new File( Platform.getPreferencesFolder(), "Prototypes/" + fieldName + ".vZome" );
                    try {
                        String path = prototype.getCanonicalPath();
                        int response = JOptionPane.showConfirmDialog(
                                DocumentFrame.this,
                                "Save as the template for new models in the " + fieldLabel + " field?"
                                + "\n\nTemplate file: " + path,
                                "Save Template?",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE );
                        
                        if ( response == JOptionPane.YES_OPTION ) {
                            logger.config("Saving default template to " + path);
                            mController .doFileAction( "save", prototype );
                        } 
                        else if ( prototype.exists() ) {
                            response = JOptionPane.showConfirmDialog(
                                DocumentFrame.this,
                                "Delete the existing template for new models in the " + fieldLabel + " field?"
                                + "\n\nTemplate file: " + path,
                                "Delete Template?",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE );

                            if ( response == JOptionPane.YES_OPTION ) {
                                logger.config("Deleting default template at " + path);
                                prototype.delete();
                            }
                        }
                    } catch (IOException ex) {
                        errors .reportError( Controller.USER_ERROR_CODE, new Object[]{ ex } );
                    }
                    break;
                    
                case "snapshot.2d":
                    if ( snapshot2dFrame == null ) {
                        snapshot2dFrame = new Snapshot2dFrame( mController.getSubController( "snapshot.2d" ), fileDialog );
                    }
                    snapshot2dFrame.setPanelSize( modelPanel .getRenderedSize() );
                    snapshot2dFrame.pack();
                    if ( ! snapshot2dFrame .isVisible() )
                        snapshot2dFrame.repaint();
                    snapshot2dFrame.setVisible( true );
                    break;

                case "showToolsPanel":
                    tabbedPane .setSelectedIndex( 1 );  // should be "tools" tab
                    break;

                case "export4dPolytope":
                    {
                        Controller polytopesController = mController .getSubController( "polytopes" );
                        ActionListener actionListener = new ControllerFileAction( fileDialog, false, cmd, "vef", polytopesController );
                        actionListener .actionPerformed( e );
                    }
                    break;

                case "showPolytopesDialog":
                    Controller polytopesController = mController .getSubController( "polytopes" );
                    if ( polytopesDialog == null )
                        polytopesDialog = new PolytopesDialog( DocumentFrame.this, polytopesController );
                    try {
                    	    polytopesController .actionPerformed( DocumentFrame.this, "setQuaternion" );
                    } catch ( Exception e1 ) {
                        errors .reportError( Controller.USER_ERROR_CODE, new Object[]{ e1 } );
                    }
                    polytopesDialog .setVisible( true );
                    break;
                
                case "showPythonWindow":
                    if ( pythonFrame == null ) {
                        pythonFrame = new JFrame( "Python Scripting" );
                        pythonFrame .setContentPane( new PythonConsolePanel( pythonFrame, mController ) );
                    }
                    pythonFrame .pack();
                    pythonFrame .setVisible( true );
                    break;
                    
                case "showZomicWindow":
                    if ( zomicFrame == null ) {
                        zomicFrame = new JFrame( "Zomic Scripting" );
                        zomicFrame .setContentPane( new ZomicEditorPanel( zomicFrame, mController, false ) );
                    }
                    zomicFrame .pack();
                    zomicFrame .setVisible( true );
                    break;

                case "showZomodWindow":
                    if ( zomodFrame == null ) {
                        zomodFrame = new JFrame( "Zomod Scripting" );
                        zomodFrame .setContentPane( new ZomicEditorPanel( zomicFrame, mController, true ) );
                    }
                    zomodFrame .pack();
                    zomodFrame .setVisible( true );
                    break;
                
                case "setItemColor":
                    Long intval = Long.decode( mController .getProperty( "lastObjectColor" ) );
                    int i = intval.intValue();
                    Color color = new Color((i >> 24) & 0xFF, (i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
                    color = JColorChooser.showDialog( DocumentFrame.this, "Choose Object Color", color );
                    if ( color == null )
                        return;
                    int rgb = color .getRGB() & 0xffffff;
                    int alpha = color .getAlpha() & 0xff;
                    String colorString = Integer.toHexString( ( rgb << 8 ) | alpha );
                    mController .setProperty( "lastObjectColor", "#"+colorString );
                    String command = "ColorManifestations/" + colorString;
                    mController .actionPerformed( e .getSource(), command );
                    break;
                    
                case "setBackgroundColor":
                    color = Color.decode( mController .getProperty( "backgroundColor" ) );
                    color = JColorChooser.showDialog( DocumentFrame.this, "Choose Background Color", color );
                    if ( color != null )
                        mController .setProperty( "backgroundColor", Integer.toHexString( color.getRGB() & 0xffffff ) );
                    break;
                
                case "usedOrbits":
                    mController .actionPerformed( e .getSource(), e .getActionCommand() ); // TO DO exclusive
                    break;
                
                case "rZomeOrbits":
                case "predefinedOrbits":
                case "setAllDirections":
                    delegate = mController .getSubController( "symmetry." + system );
                    delegate .actionPerformed( e .getSource(), e .getActionCommand() );
                    break;
                
                case "configureShapes":
                    JDialog shapesDialog = shapesDialogs.get( system );
                    if ( shapesDialog == null ) {
                    	delegate = mController .getSubController( "symmetry." + system );
                        shapesDialog = new ShapesDialog( DocumentFrame.this, delegate );
                        shapesDialogs .put( system, shapesDialog );
                    }
                    shapesDialog .setVisible( true );
                    break;
                
                case "configureDirections":
                    JDialog symmetryDialog = directionsDialogs.get( system );
                    if ( symmetryDialog == null ) {
                    	delegate = mController .getSubController( "symmetry." + system );
                        symmetryDialog = new SymmetryDialog( DocumentFrame.this, delegate );
                        directionsDialogs .put( system, symmetryDialog );
                    }
                    symmetryDialog .setVisible( true );
                    break;
                    
                case "addBookmark":
                    String numStr = toolsController .getProperty( "next.tool.number" );
                    int bookmarkNum = Integer .parseInt( numStr );
                    String bookmarkId = "bookmark." + bookmarkNum;
                    String bookmarkName = (String) JOptionPane .showInputDialog( (Component) e .getSource(),
                            "Name the new bookmark:", "New Selection Bookmark",
                            JOptionPane.PLAIN_MESSAGE, null, null, bookmarkId );
                    if ( ( bookmarkName == null ) || ( bookmarkName .length() == 0 ) ) {
                        return;
                    }
                    mController .actionPerformed( e .getSource(), "newTool/" + bookmarkId + "/" + bookmarkName );
                    break;

                default:
                    if ( cmd .startsWith( "LoadVEF/" )
                      || cmd .startsWith( "ImportSimpleMeshJson/" )
                      || cmd .startsWith( "ImportColoredMeshJson/" ) )
                    {
                        Controller importScaleController = mController .getSubController( "importScale" );
                        if ( importScaleDialog == null || importScaleDialog.getTitle() != cmd) {
                            importScaleDialog = new VefImportDialog( DocumentFrame.this, importScaleController, "Set Scale and Rotation",
                                new ControllerFileAction( fileDialog, true, cmd, "vef", controller ) );
                        }
                        importScaleDialog .setVisible( true );
                    }
                    else if ( cmd .startsWith( "setSymmetry." ) )
                    {
                        system = cmd .substring( "setSymmetry.".length() );
                        mController .actionPerformed( e .getSource(), e .getActionCommand() ); // TODO getExclusiveAction
                    }
                    else if ( cmd .startsWith( "execCommandLine/" ) )
                    {
                        if ( mFile == null ) {
                            JOptionPane .showMessageDialog( DocumentFrame.this, "You must save your model before you can run a shell command.",
                                    "Command Failure", JOptionPane .ERROR_MESSAGE );
                            return;
                        }
                        String cmdLine = cmd .substring( "execCommandLine/" .length() );
                        cmdLine = cmdLine .replace( "{}", mFile .getName() );
                        logger.log( Level.INFO, "executing command line: " + cmdLine );
                        try {
                            Runtime .getRuntime() .exec( cmdLine, null, mFile .getParentFile() );
                        } catch ( IOException ioe ) {
                            System .err .println( "Runtime.exec() failed on " + cmdLine );
                            ioe .printStackTrace();
                        }
                    }
                    else if ( cmd .startsWith( "showProperties-" ) )
                    {
                        String key = cmd .substring( "showProperties-" .length() );
                        Controller subc = mController .getSubController( key + "Picking" );
                        JOptionPane .showMessageDialog( DocumentFrame.this, subc .getProperty( "objectProperties" ), "Object Properties",
                                JOptionPane.PLAIN_MESSAGE );
                    }
                    else
                        mController .actionPerformed( e .getSource(), e .getActionCommand() );
                    break;
                }
            }
        };

        // -------------------------------------- create panels and tools

        cameraController = mController .getSubController( "camera" );
        lessonController = mController .getSubController( "lesson" );
        lessonController .addPropertyListener( this );

        ControllerActionListener actionListener = new ControllerActionListener( this .mController );
        
        // Now the component containment hierarchy
        
        JPanel outerPanel = new JPanel( new BorderLayout() );
        setContentPane( outerPanel );
        {
            JPanel leftCenterPanel = new JPanel( new BorderLayout() );
            {
                if ( this .isEditor )
                {
                    JPanel modeAndStatusPanel = new JPanel( new BorderLayout() );
                    JPanel articleButtonsPanel = new JPanel();
                    modeAndStatusPanel .add( articleButtonsPanel, BorderLayout.LINE_START );

                    ButtonGroup group = new ButtonGroup();
                    modelButton  = new JRadioButton( "Model" );
                    modelButton .setSelected( true );
                    articleButtonsPanel .add( modelButton );
                    group .add( modelButton );
                    modelButton .setActionCommand( "switchToModel" );
                    modelButton .addActionListener( actionListener );
                    snapshotButton = new JButton( "-> capture ->" );
                    //                String imgLocation = "/icons/snapshot.png";
                    //                URL imageURL = getClass().getResource( imgLocation );
                    //                if ( imageURL != null ) {
                    //                    Icon icon = new ImageIcon( imageURL, "capture model snapshot" );
                    //                    snapshotButton .setIcon( icon );
                    //                    Dimension dim = new Dimension( 50, 36 );
                    //                    snapshotButton .setPreferredSize( dim );
                    //                    snapshotButton .setMaximumSize( dim );
                    //                }
                    articleButtonsPanel .add( snapshotButton );
                    snapshotButton .setActionCommand( "takeSnapshot" );
                    snapshotButton .addActionListener( new ActionListener()
                    {
						@Override
                        public void actionPerformed( ActionEvent e )
                        {
                            mController .actionPerformed( e .getSource(), e .getActionCommand() ); // sends thumbnailChanged propertyChange, but no listener...
                            articleButton .doClick();  // switch to article mode, so now there's a listener
                            // now trigger the propertyChange again
                            lessonController .actionPerformed( DocumentFrame.this, "setView" );
                        }
                    } );
                    articleButton  = new JRadioButton( "Article" );
                    articleButton .setEnabled( lessonController .propertyIsTrue( "has.pages" ) );
                    articleButton .setSelected( false );
                    articleButtonsPanel .add( articleButton );
                    group .add( articleButton );
                    articleButton .setActionCommand( "switchToArticle" );
                    articleButton .addActionListener( actionListener );

                    JPanel statusPanel = new JPanel( new BorderLayout() );
                    statusPanel .setBorder( BorderFactory .createEmptyBorder( 4, 4, 4, 4 ) );
                    statusText = new JLabel( "", JLabel.CENTER );
                    statusText .setBorder( BorderFactory .createLineBorder( Color.LIGHT_GRAY ) );
                    statusText .setForeground( Color .RED );
                    statusPanel .add( statusText );
                    modeAndStatusPanel .add( statusPanel, BorderLayout.CENTER );

                    leftCenterPanel .add(  modeAndStatusPanel, BorderLayout.PAGE_START );
                }

                Scene scene = ((Scene.Provider) mController) .getScene();
                RenderingViewer viewer = factory3d .createRenderingViewer( scene );
                modelPanel = new ModelPanel( mController, viewer, (ControlActions) this, this .isEditor, fullPower );
                leftCenterPanel .add( modelPanel, BorderLayout.CENTER );
            }
            outerPanel.add( leftCenterPanel, BorderLayout.CENTER );

            // String mag = props .getProperty( "default.magnification" );
            // if ( mag != null ) {
            // float magnification = Float .parseFloat( mag );
            // // TODO this seems to work, but ought not to!
            // viewControl .zoomAdjusted( (int) magnification );
            // }

            JPanel rightPanel = new JPanel( new BorderLayout() );
            {
                Scene scene = ((Scene.Provider) cameraController) .getScene();
                RenderingViewer viewer = factory3d .createRenderingViewer( scene );
                viewControl = new CameraControlPanel( viewer, cameraController );
                // this is probably moot for reader mode
                rightPanel .add( viewControl, BorderLayout.PAGE_START );
                
                modelArticleEditPanel = new JPanel();
                modelArticleCardLayout = new CardLayout();
                modelArticleEditPanel .setLayout( modelArticleCardLayout );
                if ( this .isEditor )
                {
                    Controller sbController = controller .getSubController( "strutBuilder" );
                    JPanel buildPanel = new StrutBuilderPanel( DocumentFrame.this, mController .getProperty( "symmetry" ), sbController, this );
                    if ( this .fullPower )
                    {
                        tabbedPane .addTab( "build", buildPanel );
                        if ( mController .propertyIsTrue( "original.tools" ) )
                        {
                            ToolsPanel toolsPanel = new ToolsPanel( DocumentFrame.this, toolsController );
                            tabbedPane .addTab( "tools", toolsPanel );
                        }
                        
                        JPanel bomPanel = new PartsPanel( mController .getSubController( "parts" ) );
                        tabbedPane .addTab( "parts", bomPanel );
                        
                        JPanel measurePanel = new MeasurePanel( mController .getSubController( "measure" ) );
                        tabbedPane .addTab( "measure", measurePanel );
                        
                        modelArticleEditPanel .add( tabbedPane, "model" );
                    }
                    else
                        modelArticleEditPanel .add( buildPanel, "model" );
                }

                if ( this .isEditor )
                {
                    modelArticleCardLayout .show( modelArticleEditPanel, "model" );
                    modelArticleEditPanel .setMinimumSize( new Dimension( 300, 500 ) );
                    modelArticleEditPanel .setPreferredSize( new Dimension( 300, 800 ) );
                }
                else
                {
                	createLessonPanel();
                    modelArticleCardLayout .show( modelArticleEditPanel, "article" );
                    modelArticleEditPanel .setMinimumSize( new Dimension( 400, 500 ) );
                    modelArticleEditPanel .setPreferredSize( new Dimension( 400, 800 ) );
                }
                rightPanel .add( modelArticleEditPanel, BorderLayout.CENTER );
            }
            outerPanel.add( rightPanel, BorderLayout.LINE_END );
        }

        JPopupMenu.setDefaultLightWeightPopupEnabled( false );
        ToolTipManager ttm = ToolTipManager.sharedInstance();
        ttm .setLightWeightPopupEnabled( false );

        Controller saveAsController = new DefaultController()
        {
            @Override
            public void doFileAction( String command, final File file )
            {
                mController .doFileAction( command, file );
                mFile = file;
                String newTitle = file .getAbsolutePath();
                mController .setProperty( "name", newTitle );
                setTitle( newTitle );
            }
        };
        mController .addSubController( "saveAs", saveAsController ); // need this so property flow works
		this .saveAsAction = new ControllerFileAction( fileDialog, false, "save", "vZome", saveAsController );

        this .setJMenuBar( new DocumentMenuBar( mController, this ) );

        this.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
        this.addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowClosing( WindowEvent we )
            {
                closeWindow();
            }
        } );

		// Find the screen with the largest area if this is a multi-monitor system.
		// Set the frame size to just a bit smaller than the screen
		//	so the frame will fit on the screen if the user un-maximizes it.
		// Default to opening the window as maximized on the selected (or default) monitor.
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		if(gs.length > 0) {
			int bestIndex = 0;
			GraphicsDevice bestDevice = gs[bestIndex];
			DisplayMode bestMode = bestDevice.getDisplayMode();
			int bestArea = bestMode.getHeight() * bestMode.getWidth();
			for (int i = bestIndex+1; i < gs.length; i++) {
				GraphicsDevice testDevice = gs[i];
				DisplayMode testMode = testDevice.getDisplayMode();
				int testArea = testMode.getHeight() * testMode.getWidth();
				if(bestArea < testArea) {
					bestArea = testArea;					
					bestMode = testMode;					
					bestDevice = testDevice;
				}
			}
			Rectangle bounds = bestDevice.getDefaultConfiguration().getBounds();
			this.setLocation(bounds.x, bounds.y);
			int n = 15, d = n + 1; // set size to 15/16 of full screen size then maximize it
			this.setSize(bestMode.getWidth() * n/d, bestMode.getHeight() * n/d);
		}
		this.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);

        this.pack();
        this.setVisible( true );
        this.setFocusable( true );


        new ExclusiveAction( this .getExcluder() )
        {
			@Override
            protected void doAction( ActionEvent e ) throws Exception
            {
                mController .actionPerformed( this, "finish.load" );

                String title = mController .getProperty( "window.title" );
                boolean migrated = mController .propertyIsTrue( "migrated" );
                
                boolean asTemplate = mController .propertyIsTrue( "as.template" );
                URL url = null; // TODO

                if ( ! mController .userHasEntitlement( "model.edit" ) )
                {
                    mController .actionPerformed( e .getSource(), "switchToArticle" );
                    if ( url != null )
                        title = url .toExternalForm();
                    migrated = false;
                }

                if ( ! asTemplate && migrated ) { // a migration
                    final String NL = System .getProperty( "line.separator" );
                    if ( mController .propertyIsTrue( "autoFormatConversion" ) )
                    {
                        if ( mController .propertyIsTrue( "formatIsSupported" ) )
                            JOptionPane .showMessageDialog( DocumentFrame.this,
                                    "This document was created by an older version." + NL + 
                                    "If you save it now, it will be converted automatically" + NL +
                                    "to the current format.  It will no longer open using" + NL +
                                    "the older version.",
                                    "Automatic Conversion", JOptionPane.INFORMATION_MESSAGE );
                        else
                        {
                            title = null;
                            DocumentFrame.this .makeUnnamed();
                            JOptionPane .showMessageDialog( DocumentFrame.this,
                                    "You have \"autoFormatConversion\" turned on," + NL + 
                                    "but the behavior is disabled until this version of vZome" + NL +
                                    "is stable.  This converted document is being opened as" + NL +
                                    "a new document.",
                                    "Automatic Conversion Disabled", JOptionPane.INFORMATION_MESSAGE );
                        }
                    }
                    else
                    {
                        title = null;
                        DocumentFrame.this .makeUnnamed();
                        JOptionPane .showMessageDialog( DocumentFrame.this,
                                "This document was created by an older version." + NL + 
                                "It is being opened as a new document, so you can" + NL +
                                "still open the original using the older version.",
                                "Outdated Format", JOptionPane.INFORMATION_MESSAGE );
                    }
                }

                if ( title == null )
                    title = mController .getProperty( "untitled.title" );
                
                DocumentFrame.this .setTitle( title );
            }

			@Override
            protected void showError( Exception e )
            {
                JOptionPane .showMessageDialog( DocumentFrame.this,
                        e .getLocalizedMessage(),
                        "Error Loading Document", JOptionPane.ERROR_MESSAGE );
                // setting "visible" to FALSE will remove this document from the application controller's 
                // document collection so its document count is correct and it cleans up correctly 
                mController .setProperty( "visible", Boolean.FALSE );
                DocumentFrame.this .dispose();
            }
            
        } .actionPerformed( null );
    }

    private ExclusiveAction getExclusiveAction( final String action, final Controller controller )
    {
        return new ExclusiveAction( getExcluder() )
        {
			@Override
            protected void doAction( ActionEvent e ) throws Exception
            {
				controller .actionPerformed( e .getSource(), action );
            }

			@Override
            protected void showError( Exception e )
            {
                JOptionPane.showMessageDialog( DocumentFrame.this, e.getMessage(), "Command failure", JOptionPane.ERROR_MESSAGE );
            }
        };
    }
    
    @Override
    public AbstractButton setButtonAction( String command, Controller controller, AbstractButton control )
    {
        control .setActionCommand( command );
        boolean enable = true;
        switch ( command ) {

        // TODO: find a better way to disable these... they are found in OrbitPanel.java
        case "predefinedOrbits":
        case "usedOrbits":
        case "setAllDirections":
            enable = fullPower;
            break;

        case "save":
        case "saveAs":
        case "saveDefault":
            enable = this .canSave;
            break;

        default:
            if ( command .startsWith( "export." ) ) {
                enable = this .canSave;
            }
        }
        control .setEnabled( enable );
        if ( control .isEnabled() )
        {
            ActionListener actionListener = new ControllerActionListener(controller);
            switch ( command ) {

            // these can fall through to the ApplicationController
            case "quit":
            case "new":
            case "new-rootTwo":
            case "new-rootThree":
            case "new-heptagon":
            case "new-snubDodec":
            case "openURL":
            case "showAbout":

                // these will be handled by the DocumentController
            case "toggleWireframe":
            case "toggleOrbitViews":
            case "toggleStrutScales":
            case "toggleFrameLabels":
            case "toggleNormals":
            case "toggleOutlines":
                break;

            case "open":
            case "newFromTemplate":
            case "openDeferringRedo":
                actionListener = new ControllerFileAction( fileDialog, true, command, "vZome", controller );
                break;

            case "saveAs":
                actionListener = saveAsAction;
                break;

            case "save":
            case "saveDefault":
            case "close":
            case "snapshot.2d":
            case "showToolsPanel":
            case "setItemColor":
            case "setBackgroundColor":
            case "showPolytopesDialog":
            case "showZomicWindow":
            case "showZomodWindow":
            case "showPythonWindow":
            case "rZomeOrbits":
            case "predefinedOrbits":
            case "usedOrbits":
            case "setAllDirections":
            case "configureShapes":
            case "configureDirections":
            case "addBookmark":
            case "export4dPolytope":
                actionListener = this .localActions;
                break;

            case "capture-animation":
                actionListener = new ControllerFileAction( fileDialog, false, command, "png", controller );
                break;

            default:
                if ( command .startsWith( "openResource-" ) ) {
                }
                else if ( command .startsWith( "LoadVEF/" ) ) {
                    actionListener = this .localActions;
                }
                else if ( command .startsWith( "ImportSimpleMeshJson/" ) ) {
                    actionListener = this .localActions;
                }
                else if ( command .startsWith( "ImportColoredMeshJson/" ) ) {
                    actionListener = this .localActions;
                }
                else if ( command .startsWith( "setSymmetry." ) ) {
                    actionListener = this .localActions;
                }
                else if ( command .startsWith( "execCommandLine/" ) ) {
                    actionListener = this .localActions;
                }
                else if ( command .startsWith( "showProperties-" ) ) {
                    actionListener = this .localActions;
                }
                else if ( command .startsWith( "capture." ) ) {
                    String ext = command .substring( "capture." .length() );
                    actionListener = new ControllerFileAction( fileDialog, false, command, ext, controller );
                }
                else if ( command .startsWith( "export2d." ) ) {
                    String ext = command .substring( "export2d." .length() );
                    actionListener = new ControllerFileAction( fileDialog, false, command, ext, controller );
                }
                else if ( command .startsWith( "export." ) ) {
                    String ext = command .substring( "export." .length() );
                    ext = controller .getProperty( "exportExtension." + ext );
                    switch ( ext ) {
                    case "vrml": ext = "wrl"; break;
                    case "size": ext = "txt"; break;
                    case "partslist": ext = "txt"; break;
                    case "partgeom": ext = "vef"; break;
                    default:
                        break;
                    }
                    actionListener = new ControllerFileAction( fileDialog, false, command, ext, controller );
                }
                else {
                    actionListener = getExclusiveAction( command, controller );
                    this .mExcluder .addExcludable( control );
                }
                break;
            }
            control .addActionListener( actionListener );
        }
        return control;
    }
    
    @Override
    public JMenuItem setMenuAction( String command, Controller controller, JMenuItem menuItem )
    {
        return (JMenuItem) this .setButtonAction( command, controller, menuItem );
    }

	@Override
    public void propertyChange( PropertyChangeEvent e )
    {
		switch ( e .getPropertyName() ) {

		case "command.status":
            if ( statusText != null && ! this .developerExtras )
                statusText .setText( (String) e .getNewValue() );
			break;

		case "current.edit.xml":
			if ( statusText != null && this .developerExtras )
				statusText .setText( (String) e .getNewValue() );
			break;

		case "editor.mode":
            String mode = (String) e .getNewValue();
            int width = 0;
            if ( "article" .equals( mode ) )
            {
                width = 400;
                if ( snapshotButton != null ) // this gets called just once for the Reader
                    snapshotButton .setEnabled( false );
//                viewControl .setVisible( false );
                mExcluder .grab();
            }
            else
            {                   
                width = 300;
                snapshotButton .setEnabled( true );
//                viewControl .setVisible( true );
                mExcluder .release();
            }
            createLessonPanel();
            modelArticleCardLayout .show( modelArticleEditPanel, mode );
            modelArticleEditPanel .setMinimumSize( new Dimension( width, 500 ) );
            modelArticleEditPanel .setPreferredSize( new Dimension( width, 800 ) );
			break;

		case "has.pages":
		    // lessonPanel is necessary for thumbnails to render when loading a file
		    createLessonPanel();
            if ( articleButton != null )
            {
                boolean enable = e .getNewValue() .toString() .equals( "true" );
                articleButton .setEnabled( enable );
                if ( ! enable )
                    modelButton .doClick();
            }
			break;
			
		case "window.title":
            this .setTitle( e .getNewValue() .toString() );
            break;
			
		case "visible":
			if ( Boolean.TRUE .equals( e .getNewValue() ) ) {
				this .appUI .propertyChange( e ); // remove this window from the UI's collection
				this .setVisible( true );
			}
            break;
		}
    }

	// called from ApplicationUI on quit
	//
	boolean closeWindow()
	{
	    if ( "true".equals( mController.getProperty( "edited" ) ) && ( isEditor && canSave ) )
	    {
	        // TODO replace showConfirmDialog() with use of EscapeDialog, or something similar...
	        //   see  http://java.sun.com/docs/books/tutorial/uiswing/components/dialog.html
	        int response = JOptionPane.showConfirmDialog( DocumentFrame.this, "Do you want to save your changes?",
	                "file is changed", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE );

	        if ( response == JOptionPane.CANCEL_OPTION )
	            return false;
	        if ( response == JOptionPane.YES_OPTION )
	            try {
	                localActions .actionPerformed( new ActionEvent( DocumentFrame.this, ActionEvent.ACTION_PERFORMED, "save" ) );
	                return false;
	            } catch ( RuntimeException re ) {
	                logger.log( Level.WARNING, "did not save due to error", re );
	                return false;
	            }
	    }
	    dispose();
	    mController .setProperty( "visible", Boolean.FALSE );
	    return true;
	}

	public void makeUnnamed()
	{
		this .mFile = null;
	}
}
