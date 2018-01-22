/*
 * The MIT License
 *
 * Copyright 2017 University of Antwerp.
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
import ij.gui.ImageCanvas;
import ij.gui.MessageDialog;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.io.File;
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
import main.be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.minusRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.saveRoiAlternative;
import net.lingala.zip4j.exception.ZipException;

/**
 *
 * @author mbarbier
 */
public class Manual_Add_Region implements PlugIn {
	
	boolean DEBUG = true;


	public class ChoiceList extends Panel implements ActionListener {

		public final String OVERLAY_NEW = "New regions";
		public final String OVERLAY_CURRENT = "Current regions";
		public final String OVERLAY_OLD = "Old regions";
		String title;
		LinkedHashMap< String, String > roiNameMap;
		ArrayList<String> roiNameList;
		LinkedHashMap< String, Button > roiButtonMap;
		LinkedHashMap< String, Roi > roiMap;
		LinkedHashMap< String, Roi > roiMapNew;
		LinkedHashMap< String, Roi > roiMapOld;
		LinkedHashMap< String, Boolean > roiActive;
		Overlay overlayOld;
		Overlay overlayCurrent;
		Overlay overlayNew;
		Overlay overlaySlice;
		String overlayType = "";
		String selectedRoi = null;
		Roi editRoi = null;
		Roi overlaySelection = null;
		//Roi textMessage = null;
		Roi overlayTypeText = null;
		//TextField overlayTypeTextField;
		ImagePlus imp = null;
		File outputRoiFolder = null;
		boolean roiInterpolation;
		double interpolationSmoothDistance;
		boolean overlayVisible;
		double defaultRoiThickness;
		double selectedRoiThickness;
		Color buttonDefaultForeGroundColor;
		String outputNamePrefix = "";
		File outputFile;
		File outputRoiFile;

		Panel regionPanel;
		Panel controlPanel;
		Panel viewPanel;

		public ChoiceList(ImagePlus imp, String roiName, File outputFile, File outputRoiFile ) {

			super();
			this.outputFile = outputFile;
			this.outputRoiFile = outputRoiFile;
			this.imp = imp;
			this.setVisible(false);
			this.title = imp.getTitle();
			this.outputNamePrefix = "new_";
			this.overlayVisible = true;
			this.defaultRoiThickness = 2;
			
			this.controlPanel = new Panel( new FlowLayout() );
			Button buttonSaveAndNext = new Button( "Save & Next" );
			buttonSaveAndNext.addActionListener(this);
			this.controlPanel.add( buttonSaveAndNext );

			this.setLayout( new FlowLayout() );
			this.add(this.controlPanel);
			this.controlPanel.validate();

			this.setVisible(true);
			this.validate();
			
			imp.getWindow().add(this);
			imp.getWindow().pack();
			this.validate();
			
			this.buttonDefaultForeGroundColor = buttonSaveAndNext.getForeground();
		}

		@Override
	    public void actionPerformed(ActionEvent e) {
			String label = e.getActionCommand();
			//IJ.log("Action performed with label: " + label );
			if (label==null)
				return;
			String command = label;

			switch( command ) {
				case "Save & Next":
					doneRoi( this.selectedRoi );
					LinkedHashMap< String, Roi > tempRoiMap = this.roiMap;
					if ( tempRoiMap.size() > 0 ) {
						saveRoiAlternative( this.outputRoiFile, tempRoiMap );
					} else {
					}
					this.imp.close();
					break;
				default:
					break;
			}
		}

		public void doneRoi( String roiName ) {

			this.selectedRoi = roiName;
			this.editRoi = this.imp.getRoi();

			if (this.editRoi != null) {
				if (roiInterpolation) {
					FloatPolygon fp = this.editRoi.getInterpolatedPolygon(this.interpolationSmoothDistance, false);
					this.editRoi = new PolygonRoi(fp, Roi.POLYGON);
				}
				this.editRoi.setStrokeWidth( this.defaultRoiThickness );
				Color roiColor = Main.getDefaultColorMap().get( roiName );
				this.editRoi.setStrokeColor( roiColor );
			}
			this.roiMap.put(roiName, this.editRoi);
		}

	}


	public void process( File inputFolder, String outputPrefix, File outputFolder, String inputName, String roiName ) {
	
		String inputPath = inputFolder.getAbsolutePath() + "/" + inputName;
		String outputRoiPath = outputFolder.getAbsolutePath() + "/" + outputPrefix + inputName;

		// Get current ImagePlus
		ImagePlus imp = IJ.openImage( inputPath );
		if ( !imp.isVisible() ) {
			imp.show();
		}

		imp.setDisplayMode(IJ.COMPOSITE);
		imp.setC(1);
		IJ.run(imp, "Enhance Contrast", "saturated=0.05");
		IJ.setTool("freehand");

		// Select ROIs
		ChoiceList cl = new ChoiceList( imp, roiName, outputFolder, new File(outputRoiPath) );

		// Wait for the ROIs to be written before continuing
		try {
			while ( !new File(outputRoiPath).exists() ) {
				TimeUnit.SECONDS.sleep(1);
			}
		} catch (Exception ex) {
			Logger.getLogger(Manual_Annotation.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void run(String arg) {

		try {
			this.DEBUG = true;

			// PARAMETER INPUT
			GenericDialogPlus gdp = new GenericDialogPlus("SliceMap: Manual Annotation & Curation");
			gdp.addHelp( "https://github.com/mbarbie1/SliceMap" );
			// Get the last used folder
			String userPath = IJ.getDirectory("current");
			if (userPath == null) {
				userPath = "";
			}
			if (this.DEBUG) {
				gdp.addDirectoryField( "Sample folder", "G:/data/Stephan Missault images for Winnok/crops_large_reelin" );
				gdp.addDirectoryField( "ROIs folder folder", "G:/data/Stephan Missault images for Winnok/rois_reelin" );
				gdp.addDirectoryField( "Output folder", "G:/data/Stephan Missault images for Winnok/rois_reelin_cc" );
			} else {
				gdp.addDirectoryField( "Sample folder", userPath );
				gdp.addDirectoryField( "ROIs folder folder", userPath );
				gdp.addDirectoryField( "Output folder", userPath );
			}
			gdp.addStringField("Sample name contains", "");
			gdp.addStringField("Output file name prefix", "");
			gdp.addCheckbox( "Overwriting existing output ROIs", true );
			gdp.addStringField("List of ROI-names (comma separated)", "cc");

			gdp.showDialog();
			if ( gdp.wasCanceled() ) {
				return;
			}

			// EXTRACTION OF PARAMETERS FROM DIALOG
			File sampleFile = new File( gdp.getNextString() );
			if (!sampleFile.exists()) {
				String warningStr = "(Exiting) Error: Given sample folder does not exist: " + sampleFile;
				IJ.log(warningStr);
				MessageDialog md = new MessageDialog( null, "SliceMap: Manual Add Region", warningStr );
				return;
			}

			File roiFile = new File( gdp.getNextString() );
			File outputFile = new File( gdp.getNextString() );
			outputFile.mkdirs();
			String ext = gdp.getNextString();
			String sampleFilter = "";//gdp.getNextString();
			String outputNamePrefix = gdp.getNextString();
			boolean overwriteRois = gdp.getNextBoolean();
			boolean useRois = true;//gdp.getNextBoolean();
			String nameList = gdp.getNextString();
			ArrayList< String > roiNameList = new ArrayList<>();
			// nameList is a comma separated list of roiNames as a single String,
			// convert to ArrayList of roiNames
			String[] nameListSplit = nameList.split(",");
			for ( String roiName : nameListSplit ) {
				roiNameList.add(roiName);
			}

			ArrayList< File > fileList = findFiles( sampleFile, sampleFilter, "sl;dj;klsd" );
			IJ.log("------------------------------------------------------");
			IJ.log("            Manual Add Region ");
			IJ.log("------------------------------------------------------");
			IJ.log("List of images:");
			for (File file : fileList) {
				String fileName = file.getName();
				IJ.log(fileName);
			}
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
				
				String roiFileName = sample_id + ".zip";
				String roiPath = roiFile.getAbsolutePath() + "/" + roiFileName;

				String outputName = outputNamePrefix + sample_id + ".zip";
				// check for existing output files
				String outputPath = outputFile.getAbsolutePath() + "/" + outputName;
				IJ.log("Starting Manual Add Region: " + fileName);
				//process( sampleFile, outputNamePrefix, outputFile, fileName, roiFileName, outputName);
				IJ.log("Finished Manual Add Region: " + fileName);
			}
			IJ.log( "Finished manual adding region of all samples" );
			IJ.log("------------------------------------------------------");
			IJ.log("------------------------------------------------------");
			IJ.log("------------------------------------------------------");
			MessageDialog md = new MessageDialog(null, "SliceMap: Manual Add Region", "Manual Add Region finished.\n" + "Output folder: " + outputFile );
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
	
	public static void main(String[] args) {

        Class<?> clazz = Manual_Add_Region.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        ImageJ imagej = new ImageJ();

		//IJ.log("START RUN Manual annotation");
		IJ.runPlugIn(clazz.getName(), "");
		//IJ.log("END RUN Manual annotation");
	}

}
