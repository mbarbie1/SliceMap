/*
 * The MIT License
 *
 * Copyright 2017 mbarbier.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package main.be.ua.mbarbier.slicemap;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.io.File;
import ij.plugin.frame.RoiManager;
import ij.gui.ImageCanvas;
import ij.gui.MessageDialog;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.process.FloatPolygon;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static main.be.ua.mbarbier.slicemap.lib.LibIO.findFiles;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.minusRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.saveRoiAlternative;

/**
 *
 * @author mbarbier
 */
public class Manual_Annotation implements PlugIn {

	public static final String METHOD_KEY = "key";
	public static final String METHOD_BUTTON = "button";
	public final String METHOD_ANNOTATION = "button";
	public boolean DEBUG = false;

	/**
	 * The Tap class implements the interaction with the user interface with 
	 * keys where the ROI names are presented one by one and the user uses "g" 
	 * to go to the next region name and "z" to save all roi of the slice.
	 */
	public class Tap extends KeyAdapter {
	
		RoiManager rm;
		ImagePlus imp;
		ArrayList< String > roiNameList;
		String outputPath;

		public Tap( RoiManager rm, ImagePlus imp, ArrayList< String > roiNameList, String outputPath ) {
			this.rm = rm;
			this.imp = imp;
			this.roiNameList = roiNameList;
			this.outputPath = outputPath;
		}
	
		public void keyPressed( KeyEvent event ) {
			
			int keyCode = event.getKeyCode();
			if (KeyEvent.VK_G == keyCode) {
				if ( this.roiNameList.size() < this.rm.getCount()+1 ) {
					IJ.log("All ROIs are already defined, it not possible to create new ones");
					event.consume(); // AVOID propagation to other key listeners
					return;
				}
				this.rm.addRoi( this.imp.getRoi() );
				int roiIndex = this.rm.getCount()-1;
				this.rm.select(roiIndex);
				String roiName = this.roiNameList.get(roiIndex);
				this.rm.runCommand("Rename", roiName);
				this.rm.runCommand("Show All");
				IJ.log("ROI added: " + roiName);
				if ( this.roiNameList.size() > roiIndex+1 ) {
					String roiNameNext = this.roiNameList.get(roiIndex+1);
					imageMessage(this.imp, "Next ROI = " + roiNameNext, 2);
				} else {
					IJ.log("All ROIs defined!");
					imageMessage(this.imp, "All ROIs defined!", 2);
				}
				event.consume(); // AVOID propagation to other key listeners
			}
			
			if (KeyEvent.VK_Z == keyCode) {
				IJ.log("Saving to " + this.outputPath);
				this.rm.runCommand("Save", this.outputPath);
				this.imp.close();
				this.rm.close();
				event.consume(); // AVOID propagation to other key listeners
			}
		}
	}
	
	/**
	 * The ChoiceList class implements the user interface where the user has to
	 * use the buttons at the bottom of the image with the ROI names and next
	 * button.
	 */
	public class ChoiceList extends Panel implements ActionListener {

		LinkedHashMap< String, String > roiNameMap;
		ArrayList< String > roiNameList;
		LinkedHashMap< String, Button > roiButtonMap;
		LinkedHashMap< String, Roi > roiMap;
		Overlay overlayCurrent;
		String selectedRoi = null;
		Roi editRoi = null;
		ImagePlus imp = null;
		File outputFile;
		boolean roiInterpolation;
		double interpolationSmoothDistance;
		boolean overlayVisible;
		double defaultRoiThickness;
		double selectedRoiThickness;

		Panel regionPanel;
		Panel controlPanel;
		Panel viewPanel;

		public ChoiceList( ImagePlus imp, ArrayList< String > roiNameList, String outputPath ) {
			super();
			this.imp = imp;
			this.outputFile = new File(outputPath);
			this.setVisible(false);
			this.roiMap = new LinkedHashMap<>();
			this.roiNameList = roiNameList;
			this.roiNameMap = new LinkedHashMap<>();
			for ( String roiName : roiNameList ) {
				this.roiNameMap.put( roiName, roiName );
			}
			this.roiButtonMap = new LinkedHashMap();
			this.roiInterpolation = false;
			this.interpolationSmoothDistance = Math.ceil( 0.01 * imp.getWidth() );
			this.overlayVisible = true;
			this.defaultRoiThickness = 2;
			this.selectedRoiThickness = this.defaultRoiThickness * 3;
			this.overlayCurrent = new Overlay();
			this.imp.setOverlay(overlayCurrent);
			this.regionPanel = new Panel();
			this.controlPanel = new Panel();
			this.viewPanel = new Panel();
			int nButtons = this.roiNameList.size();
			int nViewButtons = 3;
			int nControlButtons = 4;
			this.regionPanel.setLayout( new GridLayout( nButtons, 1 ) );
			this.viewPanel = new Panel( new GridLayout( nViewButtons, 1 ) );
			this.controlPanel = new Panel( new GridLayout( nControlButtons, 1 ) );

			Button buttonSave = new Button( "Save & Next" );
			buttonSave.addActionListener(this);
			this.controlPanel.add( buttonSave );

			Button buttonRemoveOverlap = new Button( "Remove overlap" );
			buttonRemoveOverlap.addActionListener(this);
			this.viewPanel.add( buttonRemoveOverlap );

			Button buttonToggleOverlay = new Button( "Toggle overlay" );
			buttonToggleOverlay.addActionListener(this);
			this.viewPanel.add( buttonToggleOverlay );

			for ( String roiKey : this.roiNameMap.keySet() ) {
				Button button = new Button( this.roiNameMap.get(roiKey) );
				button.addActionListener(this);
				this.roiButtonMap.put( roiKey, button );
				this.regionPanel.add( button );
			}
			this.setLayout( new GridLayout(1, 3, 5, 5) );
			this.add(this.controlPanel);
			this.controlPanel.validate();
			this.add(this.viewPanel);
			this.viewPanel.validate();
			this.add(this.regionPanel);
			Dimension panelDims = this.getPreferredSize();
			//this.setPreferredSize( new Dimension( imp.getWindow().getWidth(), (int) Math.round( panelDims.getHeight() ) ) );
			this.setVisible(true);
			this.validate();
			
			imp.getWindow().add(this);
			imp.getWindow().pack();
			this.validate();

		}

		@Override
	    public void actionPerformed(ActionEvent e) {
			String label = e.getActionCommand();
			IJ.log("Action performed with label: " + label );
			if (label==null)
				return;
			String command = label;
			for ( String roiName : roiMap.keySet() ) {
				this.roiButtonMap.get( roiName ).setForeground(Color.black);
			}

			switch( command ) {
				case "Save & Next":
					LinkedHashMap< String, Roi > tempRoiMap = this.roiMap;
					File outputRoiFile = outputFile;
					saveRoiAlternative( outputRoiFile, tempRoiMap );
					this.imp.close();
					break;
				case "Toggle overlay":
					imp.setHideOverlay(!imp.getHideOverlay());
					break;
				case "Remove overlap":
					removeOverlap();
					break;
				default:
					for ( String roiName : this.roiNameList ) {
						if ( command.equals( roiName ) ) {
							doneRoi( roiName );
						}
					}
					break;
			}
		}

		public void removeOverlap() {
			roiRemoveOverlap( this.roiMap );
		}

		public void doneRoi( String roiName ) {
			
			this.selectedRoi = roiName;
			this.overlayCurrent.remove( this.roiMap.get(roiName) );
			this.editRoi = this.imp.getRoi();
			Color roiColor = Main.CONSTANT_COLOR_LIST[ this.roiNameList.indexOf(roiName) % Main.CONSTANT_COLOR_LIST.length ];
			
			if (this.editRoi != null) {
				if (roiInterpolation) {
					FloatPolygon fp = this.editRoi.getInterpolatedPolygon(this.interpolationSmoothDistance, false);
					this.editRoi = new PolygonRoi(fp, Roi.POLYGON);
				}
				this.editRoi.setStrokeWidth( this.defaultRoiThickness );
				this.editRoi.setStrokeColor( roiColor );
			}
			this.roiMap.put(roiName, this.editRoi);
			this.overlayCurrent.add(this.editRoi);

			this.imp.deleteRoi();
			this.imp.updateImage();
			this.roiButtonMap.get(roiName).setForeground(Color.red);
			if (this.editRoi != null) {
				roiMap.put(this.selectedRoi, this.editRoi);
			} else {
				roiMap.remove(this.selectedRoi);
			}
		}
		
		/**
		 * Remove the ROI from an overlay with a given roi name
		 *
		 * @param overlay
		 * @param roiName
		 * @param roiPosition
		 */
		public void removeRoiFromOverlay( Overlay overlay, String roiName ) {
			for (Roi roi : overlay.toArray()) {
				if ( roiName.equals( roi.getName() ) ) {
					overlay.remove( roi );
				}
			}
		}
		
		/**
		 * Return the ROI from an overlay with a given roi name
		 * 
		 * @param overlay
		 * @param roiName
		 */
		public Roi getRoiFromOverlay( Overlay overlay, String roiName ) {
			for (Roi roi : overlay.toArray()) {
				if ( roiName.equals( roi.getName() ) ) {
					return roi ;
				}
			}
			return null;
		}
		
		public void roiRemoveOverlap( LinkedHashMap< String, Roi > roiMap ) {

			String[] keyset = roiMap.keySet().toArray( new String[]{""} );
			
			for ( String key1 : keyset ) {
				Roi roi1 = roiMap.get(key1);
				for ( String key2 : keyset ) {
					Roi roi2 = roiMap.get(key2);
					if ( !key1.equals(key2) ) {
						if (roi2 != null) {
							Color sc = roi2.getStrokeColor();
							float sw = roi2.getStrokeWidth();
							roi2.setName(key2);
							roi2 = minusRoi( roi2, roi1).getRois()[0];
							roi2.setStrokeColor(sc);
							roi2.setStrokeWidth(sw);
							roiMap.put(key2, roi2);
						}
					}
				}
			}
			for ( String key : keyset ) {
				// If the roi exists in the roiMap: exchange it for the cropped one in the "current regions" overlay
				if (!this.roiMap.get(key).equals(null)) {
					removeRoiFromOverlay( this.overlayCurrent, key );
					this.overlayCurrent.add( this.roiMap.get(key) );
				}
			}
			this.imp.repaintWindow();
		}

	}

	@Override
	public void run(String arg) {

		try {
			this.DEBUG = false;

			// PARAMETER INPUT
			GenericDialogPlus gdp = new GenericDialogPlus("SliceMap: Manual Annotation");
			gdp.addHelp( "https://github.com/mbarbie1/SliceMap" );
			// Get the last used folder
			String userPath = IJ.getDirectory("current");
			if (userPath == null) {
				userPath = "";
			}
			if (this.DEBUG) {
				gdp.addDirectoryField( "Sample folder", "G:/triad_temp_data/demo/Manual/images/oneChannel" );
				gdp.addDirectoryField( "Output folder", "G:/triad_temp_data/demo/Manual/output" );
			} else {
				gdp.addDirectoryField( "Sample folder", userPath );
				gdp.addDirectoryField( "Output folder", userPath );
			}
			gdp.addStringField("Sample file extension", "");
			gdp.addStringField("Sample name contains", "");
			gdp.addStringField("Output file name prefix", "");
			gdp.addCheckbox( "Overwriting existing ROIs", true );
			gdp.addStringField("List of ROI-names (comma separated)", "hp,cx,cb,th,bs,mb");

			gdp.showDialog();
			if ( gdp.wasCanceled() ) {
				return;
			}

			// EXTRACTION OF PARAMETERS FROM DIALOG
			File sampleFile = new File( gdp.getNextString() );
			if (!sampleFile.exists()) {
				String warningStr = "(Exiting) Error: Given sample folder does not exist: " + sampleFile;
				IJ.log(warningStr);
				MessageDialog md = new MessageDialog( null, "SliceMap: Manual annotation", warningStr );
				return;
			}

			File outputFile = new File( gdp.getNextString() );
			outputFile.mkdirs();
			String ext = gdp.getNextString();
			String sampleFilter = gdp.getNextString();
			String outputNamePrefix = gdp.getNextString();
			boolean overwriteRois = gdp.getNextBoolean();
			String nameList = gdp.getNextString();
			ArrayList< String > roiNameList = new ArrayList<>();
			// nameList is a comma separated list of roiNames as a single String,
			// convert to ArrayList of roiNames
			String[] nameListSplit = nameList.split(",");
			for ( String roiName : nameListSplit ) {
				roiNameList.add(roiName);
			}

			ArrayList< File > fileList = findFiles( sampleFile, sampleFilter, "sl;dj;klsd" );
			for (File file : fileList) {
				// Check for file extension
				String fileName = file.getName();
				if( !fileName.endsWith(ext) )
					continue;
				String sample_id;
				if (fileName.contains(".")) {
					sample_id = fileName.substring(0,fileName.lastIndexOf("."));
				} else {
					sample_id = fileName;
				}
				String outputName = outputNamePrefix + sample_id + ".zip";
				// check for existing output files
				String outputPath = outputFile.getAbsolutePath() + "/" + outputName;
				if ( overwriteRois ) {
					try {
						Files.deleteIfExists( new File(outputPath).toPath() );
					} catch (IOException ex) {
						Logger.getLogger(Manual_Annotation.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
				IJ.log("Starting manual annotation: " + sampleFile);
				process( sampleFile, outputFile, fileName, roiNameList, outputName);
				IJ.log("Finished manual annotation: " + sampleFile);
			}
			IJ.log( "Finished manual Annotation of all samples" );
			MessageDialog md = new MessageDialog(null, "SliceMap: Manual annotation", "Manual annotation finished.\n" + "Output folder: " + outputFile );
		} catch( Exception e ) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String stackTraceString = errors.toString();
			String warningStr = "(Exiting) Error: An unknown error occurred.\n\n"+
					"Please contact Michael Barbier if the error persists:\n\n\t michael(dot)barbier(at)gmail(dot)com\n\n"+
					"with the following error:\n\n" + stackTraceString + "\n";
			IJ.log(warningStr);
			MessageDialog md = new MessageDialog( null, "SliceMap: Manual annotation", warningStr );
			return;
			//throw new RuntimeException(Macro.MACRO_CANCELED);
		}
		//if ( !md.isVisible() ) {
		//	md.setVisible(true);
		//}
	}
	
	public void process( File inputFolder, File outputFolder, String inputName, ArrayList< String > roiNameList, String outputName ) {
	
		String inputPath = inputFolder.getAbsolutePath() + "/" + inputName;
		String outputPath = outputFolder.getAbsolutePath() + "/" + outputName;

		// Get current ImagePlus
		ImagePlus imp = IJ.openImage( inputPath );
		if ( !imp.isVisible() ) {
			imp.show();
			//imp.updateAndRepaintWindow()
		}

		imp.setDisplayMode(IJ.COMPOSITE);
		//imp.setActiveChannels("1010");
		imp.setC(1);
		IJ.run(imp, "Enhance Contrast", "saturated=0.05");
		//imp.setC(3)
		//IJ.run(imp, "Enhance Contrast", "saturated=0.05");
		// //imageMessage(imp, inputName, 1)

		// Polygon point selection tool
		IJ.setTool("freehand");

		// Select ROIs
		switch ( this.METHOD_ANNOTATION ) {

			// User interface with buttons
			case Manual_Annotation.METHOD_BUTTON:
				ChoiceList cl = new ChoiceList( imp, roiNameList, outputPath );
				break;

			// User interface using short keys
			case Manual_Annotation.METHOD_KEY:
				// RoiManager
				RoiManager rm = RoiManager.getInstance();
				if (rm == null) {
					rm = new RoiManager();
					rm.reset();
				}
				selectROI(imp, rm, roiNameList, outputPath );
				break;
		}

		// Wait for the ROIs to be written before continuing
		try {
			while ( !new File(outputPath).exists() ) {
					TimeUnit.SECONDS.sleep(1);
			}
		} catch (Exception ex) {
			Logger.getLogger(Manual_Annotation.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void imageMessage( ImagePlus imp, String text, int line) {
		int tx = 10;
		int ty = line * 10;
		Font font = new Font( "Arial", Font.PLAIN, 12 );
		Color color = Color.red;
		TextRoi t = new TextRoi( tx, ty, text, font);
		t.setNonScalable( true );
		imp.setOverlay( t, color, 1, Color.black );
	}
	
	public void selectROI( ImagePlus imp, RoiManager rm, ArrayList< String > roiNameList, String outputPath ) {
	
		// Remove keyListeners
		ImageCanvas canvas = imp.getWindow().getCanvas();
		KeyListener[] kls = canvas.getKeyListeners();
		for (KeyListener kl : kls) {
			canvas.removeKeyListener( kl );
		}

		// Add custom keylisteners
		KeyListener listener = new Tap( rm, imp, roiNameList, outputPath );
		canvas.addKeyListener( listener );

		// Re-add existing key listeners
		for (KeyListener kl : kls) {
			canvas.addKeyListener( kl );
		}
		
		imageMessage( imp, "Next ROI = " + roiNameList.get(0), 2);
	}

	
	public static void main(String[] args) {

        Class<?> clazz = Manual_Annotation.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        ImageJ imagej = new ImageJ();

		IJ.log("START RUN Manual annotation");
		IJ.runPlugIn(clazz.getName(), "");
		IJ.log("END RUN Manual annotation");
	}

}
