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
import ij.gui.MessageDialog;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.FloatPolygon;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import static main.be.ua.mbarbier.slicemap.StackProperties.saveStackProps;
import main.be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.minusRoi;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.be.ua.mbarbier.slicemap.lib.Lib;

/**
 *
 * @author mbarbier
 */
public class CurationAnnotationValidation implements PlugIn {

	public boolean DEBUG = false;
	ArrayList< String > roiNameList;

	public void setRoiNameList(ArrayList<String> roiNameList) {
		this.roiNameList = roiNameList;
	}

	
	@Override
	public void run(String arg) {

        ImageJ imagej = new ImageJ();

		GenericDialogPlus gdp = new GenericDialogPlus("SliceMap: Region curation tool");
		gdp.addHelp( "https://gitlab.com/mbarbie1/SliceMap" );
		gdp.addDirectoryField( "Screen folder (e.g. STAGING/B49)", "" );
		gdp.addStringField("region list (comma separated)", "hp;cx;cb;th;bs;mb" );
		gdp.addNumericField("Pixel size in um", 5.2, 3);

		gdp.showDialog();
		if ( gdp.wasCanceled() ) {
			imagej.quit();
			return;
		}
		File baseFile = new File( gdp.getNextString() );
		String regionsString = gdp.getNextString();
		double pixelSizeMicron = gdp.getNextNumber();

		IJ.log("START RUN Curation annotation [Janssen]");
		File inputImageFile = Paths.get( baseFile.getAbsolutePath(), "debug", "montage" ).toFile();
		File outputFile = Paths.get( baseFile.getAbsolutePath(), "debug" ).toFile();
		String tempStackFileName = "curation_stack";
		String tempStackPropsFileName = "curation_stack_props";
		boolean overwriteRois = true;
		File stackFile = new File( outputFile.getAbsolutePath(), File.separator + tempStackFileName + ".tif" );
		File stackPropsFile = new File( outputFile.getAbsolutePath(), File.separator + tempStackPropsFileName + ".csv" );
		File outputRoisFile = new File( outputFile.getAbsolutePath(), "roi" );
		File inputRoiFile = new File( outputRoisFile.getAbsolutePath() );
		String outputNamePrefix = "adapted_";
		CurationAnnotationValidation ca = new CurationAnnotationValidation();
		String[] nameListSplit = regionsString.split(";");
		ArrayList< String > roiNameList = new ArrayList<>();
		for ( String roiName : nameListSplit ) {
			roiNameList.add(roiName);
		}
		ca.setRoiNameList( roiNameList );
		ca.processFolder( inputImageFile, inputRoiFile, outputFile, stackFile, stackPropsFile, outputNamePrefix, overwriteRois, 0.0, imagej );
		IJ.log("END RUN Curation annotation [Janssen]");
		
	}

	public void runNoUI( File inputImageFile, File inputRoiFile, File outputRoisFile, File outputFile, String sampleFilter, String regionsString, String regionStringSeparator, double pixelSizeMicron, String outputNamePrefix, ImageJ imagej ) {


		/*
		String outputNamePrefix = "roi_";
			String tempStackFileName = "curation_stack";
			String tempStackPropsFileName = "curation_stack_props";
			boolean overwriteRois = true;
			
			IJ.log("START RUN Annotation Curation Janssen");
			String[] nameListSplit = regionNameListMap.get( selectedLibrary ).split(";");
			ArrayList< String > roiNameList = new ArrayList<>();
			for ( String roiName : nameListSplit ) {
				roiNameList.add(roiName);
			}
			stackFile = new File( appFile.getAbsolutePath() + File.separator + tempStackFileName + ".tif" );
			stackPropsFile = new File( appFile.getAbsolutePath() + File.separator + tempStackPropsFileName + ".csv" );
			File inputRoiFile = new File( outputRoisFile.getAbsolutePath() );
			CurationAnnotationJanssen ca = new CurationAnnotationJanssen();
			ca.setRoiNameList( roiNameList );
			//ca.processFolder( new File(inputFile.getAbsolutePath() + "/" + Main.CONSTANT_SUBDIR_MONTAGE), inputRoiFile, outputRoisFile, stackFile, stackPropsFile, outputNamePrefix, overwriteRois );
			ca.processFolder( sampleFile, inputRoiFile, outputRoisFile, stackFile, stackPropsFile, outputNamePrefix, overwriteRois, 0.0, imagej );
			IJ.log("END Annotation Curation Janssen");
		*/
		
		//ImageJ imagej = new ImageJ();

		IJ.log("START RUN Curation annotation");
		String tempStackFileName = "curation_stack";
		String tempStackPropsFileName = "curation_stack_props";
		boolean overwriteRois = true;
		File stackFile = new File( outputFile.getAbsolutePath(), File.separator + tempStackFileName + ".tif" );
		File stackPropsFile = new File( outputFile.getAbsolutePath(), File.separator + tempStackPropsFileName + ".csv" );
		CurationAnnotationValidation ca = new CurationAnnotationValidation();
		String[] nameListSplit = regionsString.split( regionStringSeparator );
		ArrayList< String > roiNameList = new ArrayList<>();
		for ( String roiName : nameListSplit ) {
			roiNameList.add(roiName);
		}
		ca.setRoiNameList( roiNameList );
		ca.processFolderNoUI( inputImageFile, inputRoiFile, sampleFilter, outputFile, stackFile, stackPropsFile, outputNamePrefix, overwriteRois, 0.0, imagej );
		IJ.log("END RUN Curation annotation");

	}
	
	public void processFolderNoUI( File inputImageFolder, File inputRoiFolder, String sampleFilter, File outputFolder, File stackFile, File stackPropsFile, String outputNamePrefix, boolean overwriteRois, double sigmaRatio, ImageJ imagej ) {

		// Find images in inputFolder and ROIs in inputRoiFolder
		Main param = new Main();
		param.PATTERN_REF_FILES = "^(.*?)\\.(tif|png|czi)";
		param.CONTAINS_REF_FILES = "";
		param.DOESNOTCONTAIN_REF_FILES = ".zip";
		param.CONGEALING_STACKBINNING = 2;
		param.CONGEALING_BINCONGEALING = 1;
		param.CONGEALING_SATURATED_PIXELS_PERCENTAGE = 0.05;
		param.FORMAT_OUTPUT_GRAY_IMAGES = ".tif";
		param.APP_FOLDER = new File( outputFolder.getAbsolutePath() + File.separator + "thisShouldNotExist_see_Curation_Annotation_processFolder function");
		param.APP_CONGEALING_FOLDER = new File( param.APP_FOLDER.getAbsolutePath() + File.separator + "thisShouldNotExist_see_Curation_Annotation_processFolder function");
		param.SAMPLE_FOLDER = new File( param.APP_FOLDER.getAbsolutePath() + File.separator + "thisShouldNotExist_see_Curation_Annotation_processFolder function");
		//param.INPUT_FOLDER = inputFolder;
		param.OUTPUT_FOLDER = outputFolder;
		param.OUTPUT_ROIS_FOLDER = new File( param.APP_FOLDER.getAbsolutePath() + File.separator + "thisShouldNotExist_see_Curation_Annotation_processFolder function");
		param.FILE_REFERENCE_STACK = stackFile;
		param.FILENAME_REFERENCE_STACK = stackFile.getName();
		param.FILE_STACKPROPS = stackPropsFile;
		param.SIGMA_RATIO = sigmaRatio;
	
		double pixelSize = 2 * 2.6;

		RefStack rs = new RefStack();
		IJ.log("START RUN refStack");
		rs.setRoiPattern_prefix( "roi_.*" );
		rs.init( param, inputImageFolder, inputRoiFolder );
		rs.refNameContains = sampleFilter;
		rs.run();
		param.IS_STACK_SET = true;
		param.setRefStack( rs.getStack() );
		param.setStackProps( rs.getStackProps() );
		IJ.log("END RUN refStack");
		ImagePlus uiStack = rs.getStack().duplicate();
		uiStack.show();
		int sliceIndex = 1;
		uiStack.setPosition( sliceIndex );
		uiStack.setSlice(sliceIndex); 
		LinkedHashMap< String, ImageProperties > propsMap = rs.getStackProps();
		String sliceKey = ImageProperties.selectIndexFromMap(propsMap, sliceIndex);
		ImageProperties props = propsMap.get( sliceKey );

		LinkedHashMap< String, String > roiNameMap = new LinkedHashMap<>();
		for ( String roiName : props.roiMap.keySet() ) {
			if ( !this.roiNameList.contains(roiName) ) {
				String warningStr = "The specified ROIs do not contain the ROI: " + roiName + " given in the annotated image, it will be added to the list";
				IJ.log(warningStr);
				//MessageDialog md = new MessageDialog( null, "SliceMap: Annotation curation", warningStr );
				this.roiNameList.add(roiName);
			}
			roiNameMap.put( roiName, roiName );
		}
		for ( String roiName : roiNameList ) {
			roiNameMap.put( roiName, roiName );
		}
		ChoiceList cl = new ChoiceList( uiStack, "Regions", roiNameMap, propsMap, new File(outputFolder.getAbsolutePath(), "roi" ), outputFolder, stackFile, stackPropsFile, outputNamePrefix, imagej, pixelSize );
	}
	
	public void processFolder( File inputImageFolder, File inputRoiFolder, File outputFolder, File stackFile, File stackPropsFile, String outputNamePrefix, boolean overwriteRois, double sigmaRatio, ImageJ imagej ) {

		// Find images in inputFolder and ROIs in inputRoiFolder
		Main param = new Main();
		param.PATTERN_REF_FILES = "^(.*?)\\.(tif|png|czi)";
		param.CONTAINS_REF_FILES = "";
		param.DOESNOTCONTAIN_REF_FILES = ".zip";
		param.CONGEALING_STACKBINNING = 2;
		param.CONGEALING_BINCONGEALING = 1;
		param.CONGEALING_SATURATED_PIXELS_PERCENTAGE = 0.05;
		param.FORMAT_OUTPUT_GRAY_IMAGES = ".tif";
		param.APP_FOLDER = new File( outputFolder.getAbsolutePath() + File.separator + "thisShouldNotExist_see_Curation_Annotation_processFolder function");
		param.APP_CONGEALING_FOLDER = new File( param.APP_FOLDER.getAbsolutePath() + File.separator + "thisShouldNotExist_see_Curation_Annotation_processFolder function");
		param.SAMPLE_FOLDER = new File( param.APP_FOLDER.getAbsolutePath() + File.separator + "thisShouldNotExist_see_Curation_Annotation_processFolder function");
		//param.INPUT_FOLDER = inputFolder;
		param.OUTPUT_FOLDER = outputFolder;
		param.OUTPUT_ROIS_FOLDER = new File( param.APP_FOLDER.getAbsolutePath() + File.separator + "thisShouldNotExist_see_Curation_Annotation_processFolder function");
		param.FILE_REFERENCE_STACK = stackFile;
		param.FILENAME_REFERENCE_STACK = stackFile.getName();
		param.FILE_STACKPROPS = stackPropsFile;
		param.SIGMA_RATIO = sigmaRatio;
	
		double pixelSize = 2 * 2.6;

		RefStack rs = new RefStack();
		IJ.log("START RUN refStack");
		rs.setRoiPattern_prefix( "roi_.*" );
		rs.init( param, inputImageFolder, inputRoiFolder );
		rs.run();
		param.IS_STACK_SET = true;
		param.setRefStack( rs.getStack() );
		param.setStackProps( rs.getStackProps() );
		IJ.log("END RUN refStack");
		ImagePlus uiStack = rs.getStack().duplicate();
		uiStack.show();
		int sliceIndex = 1;
		uiStack.setPosition( sliceIndex );
		uiStack.setSlice(sliceIndex); 
		LinkedHashMap< String, ImageProperties > propsMap = rs.getStackProps();
		String sliceKey = ImageProperties.selectIndexFromMap(propsMap, sliceIndex);
		ImageProperties props = propsMap.get( sliceKey );

		LinkedHashMap< String, String > roiNameMap = new LinkedHashMap<>();
		for ( String roiName : props.roiMap.keySet() ) {
			if ( !this.roiNameList.contains(roiName) ) {
				String warningStr = "The specified ROIs do not contain the ROI: " + roiName + " given in the annotated image, it will be added to the list";
				IJ.log(warningStr);
				MessageDialog md = new MessageDialog( null, "SliceMap: Annotation curation", warningStr );
				this.roiNameList.add(roiName);
			}
			roiNameMap.put( roiName, roiName );
		}
		for ( String roiName : roiNameList ) {
			roiNameMap.put( roiName, roiName );
		}
		ChoiceList cl = new ChoiceList( uiStack, "Regions", roiNameMap, propsMap, new File(outputFolder.getAbsolutePath(), "roi" ), outputFolder, stackFile, stackPropsFile, outputNamePrefix, imagej, pixelSize );
	}

	public class ChoiceList extends Panel implements ActionListener {

		public final String OVERLAY_NEW = "New regions";
		public final String OVERLAY_CURRENT = "Current regions";
		public final String OVERLAY_OLD = "Old regions";
		String title;
		double pixelSize;
		LinkedHashMap< String, LinkedHashMap< String, Roi > > roiMapsOri;
		LinkedHashMap< String, String > roiNameMap;
		LinkedHashMap< String, Button > roiButtonMap;
		LinkedHashMap< String, Roi > roiMap;
		LinkedHashMap< String, Roi > roiMapNew;
		LinkedHashMap< String, Roi > roiMapOld;
		LinkedHashMap< String, ImageProperties > propsMap;
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
		File outputFolder = null;
		File outputRoiFolder = null;
		int currentZ;
		String currentSliceKey;
		int lastZ;
		boolean roiInterpolation;
		double interpolationSmoothDistance;
		File stackFile;
		File stackPropsFile;
		boolean overlayVisible;
		double defaultRoiThickness;
		double selectedRoiThickness;
		Color buttonDefaultForeGroundColor;
		String outputNamePrefix = "";
		boolean done = false;

		Panel regionPanel;
		Panel controlPanel;
		Panel viewPanel;

		public ChoiceList(ImagePlus imp, String title, LinkedHashMap< String, String > roiNameMap, LinkedHashMap< String, ImageProperties > propsMap, File outputRoiFolderFile, File outputFolderFile, File stackFile, File stackPropsFile, String outputNamePrefix, ImageJ imagej, double pixelSize ) {
			super();
			
			this.pixelSize = pixelSize;
			this.imp = imp;

			this.currentZ = imp.getZ();
			this.lastZ = imp.getZ();
			//this.textMessage = new TextRoi( 0, 0, "" );
			//this.overlayTypeText = new TextRoi( 0, 0, "" ); 
			//this.imp.getOverlay().add( this.textMessage );
			this.setVisible(false);
			this.title = title;
			this.currentSliceKey = ImageProperties.selectIndexFromMap(propsMap, this.currentZ);
			ImageProperties props = propsMap.get( this.currentSliceKey );
			this.roiMap = props.roiMap;
			this.roiMapNew = new LinkedHashMap<>();
			this.roiMapOld = new LinkedHashMap<>();
			this.roiMapOld.putAll(roiMap);
			this.propsMap = propsMap;
			this.roiNameMap = roiNameMap;
			this.roiButtonMap = new LinkedHashMap();
			this.roiActive = new LinkedHashMap();
			this.outputFolder = outputFolderFile;
			this.outputRoiFolder = outputRoiFolderFile;
			this.roiInterpolation = false;
			this.interpolationSmoothDistance = Math.ceil( 0.01 * imp.getWidth() );
			this.stackFile = stackFile;
			this.stackPropsFile = stackPropsFile;
			this.outputNamePrefix = outputNamePrefix;
			this.overlayVisible = true;
			this.defaultRoiThickness = 2;
			this.selectedRoiThickness = this.defaultRoiThickness * 3;
			this.overlayOld = this.imp.getOverlay().duplicate();
			this.overlayCurrent = this.imp.getOverlay();
			this.overlaySelection = null;
			this.overlayNew = new Overlay();
			this.overlayType = this.OVERLAY_CURRENT;
			//this.overlayTypeTextField = new TextField( this.overlayType );
			//this.overlayTypeTextField.setEditable(false);
			this.regionPanel = new Panel();
			this.controlPanel = new Panel();
			this.viewPanel = new Panel();
			int nButtons = this.roiNameMap.keySet().size();
			int nViewButtons = 1;
			int nControlButtons = 3;
			this.regionPanel.setLayout( new GridLayout( nButtons, 1 ) );
			this.viewPanel = new Panel( new GridLayout( nViewButtons, 1 ) );
			this.controlPanel = new Panel( new GridLayout( nControlButtons, 1 ) );

			Button buttonLogRoi = new Button( "Log ROIs" );
			buttonLogRoi.addActionListener(this);
			//this.controlPanel.add( buttonLogRoi );

			Button buttonSaveStack = new Button( "Save stack" );
			buttonSaveStack.addActionListener(this);
			//this.controlPanel.add( buttonSaveStack );

			Button buttonCheckRegions = new Button( "Check regions" );
			buttonCheckRegions.addActionListener(this);
			this.controlPanel.add( buttonCheckRegions );

			Button buttonSave = new Button( "Save current slice" );
			buttonSave.addActionListener(this);
			this.controlPanel.add( buttonSave );

			Button buttonSaveImage = new Button( "Save all slices" );
			buttonSaveImage.addActionListener(this);
			this.controlPanel.add( buttonSaveImage );

			Button buttonApply = new Button( "Confirm region" );
			buttonApply.addActionListener(this);
			this.viewPanel.add( buttonApply );

			Button buttonRedo = new Button( "Re-annotate" );
			buttonRedo.addActionListener(this);
			//this.controlPanel.add( buttonRedo );

			Button buttonRemoveOverlap = new Button( "Remove overlap" );
			buttonRemoveOverlap.addActionListener(this);
			//this.viewPanel.add( buttonRemoveOverlap );

			Button buttonToggleOverlay = new Button( "Toggle overlay" );
			buttonToggleOverlay.addActionListener(this);
			//this.viewPanel.add( buttonToggleOverlay );

			Button buttonTypeOverlay = new Button( "Overlay type" );
			buttonTypeOverlay.addActionListener(this);
			//this.viewPanel.add( buttonTypeOverlay );
			// // Add the label for the current overlay type shown
			//this.viewPanel.add( this.overlayTypeTextField );

			for ( String roiKey : this.roiNameMap.keySet() ) {
				Button button = new Button( this.roiNameMap.get(roiKey) );
				Color roiColor = Main.getDefaultColorMap().get( roiKey );
				button.setBackground( roiColor );
				button.addActionListener(this);
				this.roiButtonMap.put( roiKey, button );
				this.regionPanel.add( button );
				this.roiActive.put( roiKey, false );
			}
			this.setLayout( new GridLayout(1, 3, 5, 5) );
			this.add(this.regionPanel);
			this.add(this.viewPanel);
			this.viewPanel.validate();
			this.add(this.controlPanel);
			this.controlPanel.validate();
			Dimension panelDims = this.getPreferredSize();
			//this.setPreferredSize( new Dimension( imp.getWindow().getWidth(), (int) Math.round( panelDims.getHeight() ) ) );

			// Remove any overlap between the ROIs (should be actually already tackled in SliceMap)
			for ( int z = 1; z <= this.imp.getNSlices(); z++ ) {
				String sliceKey = ImageProperties.selectIndexFromMap( propsMap, z );
				imp.setPosition(z);
				props = propsMap.get( sliceKey );
				this.roiMap = props.roiMap;
				roiRemoveOverlap( this.roiMap, z );
			}

			// Save the original roiMaps to use as reference ROIs
			this.roiMapsOri = new LinkedHashMap<>();
			for ( int z = 1; z <= this.imp.getNSlices(); z++ ) {
				String sliceKey = ImageProperties.selectIndexFromMap( propsMap, z );
				imp.setPosition(z);
				props = propsMap.get( sliceKey );
				this.roiMap = props.roiMap;
				LinkedHashMap< String, Roi > roiMapCopy = Lib.deepCopyRoiMap( this.roiMap );
				//roiMapCopy.putAll(this.roiMap);
				this.roiMapsOri.put( sliceKey, roiMapCopy );
			}

			imp.setPosition(1);
			String sliceKey = ImageProperties.selectIndexFromMap( propsMap, 1 );
			props = propsMap.get( sliceKey );
			this.roiMap = props.roiMap;
			this.setVisible(true);
			this.validate();

			//imp.getWindow().setLayout( new GridLayout() );
			imp.getWindow().add(this);
			//imp.getWindow().add(this);
			imp.getWindow().pack();
			this.validate();

			this.buttonDefaultForeGroundColor = buttonSaveStack.getForeground();
			
			// Wait for the ROIs to be written before continuing
			try {
				while ( !this.done ) {
					TimeUnit.SECONDS.sleep(1);
				}
			} catch (Exception ex) {
				Logger.getLogger(Manual_Annotation.class.getName()).log(Level.SEVERE, null, ex);
			}

		}

		@Override
	    public void actionPerformed(ActionEvent e) {
			String label = e.getActionCommand();
			IJ.log("Action performed with label: " + label );
			if (label==null)
				return;
			String command = label;
			this.lastZ = this.currentZ;
			this.currentZ = this.imp.getZ();
			this.currentSliceKey = ImageProperties.selectIndexFromMap( this.propsMap, this.currentZ );
			if ( this.lastZ != this.currentZ ) {
				deactivateAllRois();
				IJ.log("current position: " + this.currentZ );
				this.imp.setPosition( this.currentZ );
				this.imp.setSlice( this.currentZ );
				this.currentSliceKey = ImageProperties.selectIndexFromMap(this.propsMap, this.currentZ);
				ImageProperties props = this.propsMap.get( this.currentSliceKey );
				this.roiMap = props.roiMap;
				this.roiMapOld.putAll(roiMap);
			}
			redrawOverlayCurrent();

			switch( command ) {
				case "Log ROIs":
					logRoiMap();
					logRoiOverlay();
					logRoiOverlayNew();
					break;
				case "Save stack":
					saveStackDebug();
					break;
				case "Check regions":
					popupCheck();
					break;
				case "Save all slices":
					updateRoiImp();
					//IJ.save( this.imp, this.stackFile.getAbsolutePath() );
					saveStackProps( this.stackPropsFile, this.propsMap );
					saveAllRois( this.outputNamePrefix, "roiSmall_" );
					saveRoiOverlap( "curation_area.csv", "curation_stack" );
					// Save all the region properties en compute the overlap with the non-curated reference regions (if they exist)
					imp.close();
					this.done = true;
					//System.exit(0);
					break;
				case "Save current slice":
					//saveRois( "roi_", "roiSmall_" );
					saveRois( this.outputNamePrefix, "roiSmall_" );
					break;
				case "Re-annotate":
					redoRois();
					IJ.setTool(Toolbar.FREEROI);
					break;
				case "Toggle overlay":
					imp.setHideOverlay(!imp.getHideOverlay());
					break;
				case "Overlay type":
					switch( this.overlayType ) {
						case OVERLAY_CURRENT:
							this.overlayType = this.OVERLAY_NEW;
							imp.setOverlay(this.overlayNew);
							break;
						case OVERLAY_NEW:
							this.overlayType = this.OVERLAY_OLD;
							imp.setOverlay(this.overlayOld);
							break;
						case OVERLAY_OLD:
							this.overlayType = this.OVERLAY_CURRENT;
							imp.setOverlay(this.overlayCurrent);
							break;
						default:
							break;
					}
					//this.overlayTypeTextField.setText(this.overlayType);
					break;
				case "Remove overlap":
					removeOverlap();
					break;
				case "Confirm region":
					doneRoi( this.selectedRoi );
					break;
				default:
					//String[] roiNames = this.roiNameList.toArray(new String[]{""});
					for ( String roiName : this.roiNameMap.keySet() ) {
						if ( command.equals( roiName ) ) {
							//if ( this.imp.getRoi() != null && !this.selectedRoi.equals(roiName) ) {
							//	this.overlayCurrent.add( this.imp.getRoi() );
							//}
							//if ( roiActive.get(roiName) ) {
							//	doneRoi( roiName );
							//} else {
							selectNewRoi( roiName );
							//}
						}
					}
					break;
			}
		}

		public void popupCheck() {

			ArrayList< String > regionList = new ArrayList<>();
			for ( String roiKey : this.roiNameMap.keySet() ) {
				regionList.add(roiKey);
			}
			ResultsTable r = new ResultsTable();
			//r.addValue( java.lang.String column, java.lang.String value)			
			
			int nSlices = this.imp.getNSlices();
			int nRegions = regionList.size();
			LinkedHashMap< String, double[] > allAreaList = new LinkedHashMap<>();
			LinkedHashMap< String, Double > meanAreaList = new LinkedHashMap<>();
			for ( String roiKey : regionList ) {
				meanAreaList.put( roiKey, 0.0 );
			}
			
			for ( String roiKey : regionList ) {
				double[] areaList = new double[nSlices];
				for ( int z = 1; z <= this.imp.getNSlices(); z++ ) {
					String sliceKey = ImageProperties.selectIndexFromMap( this.propsMap, z );
					ImageProperties props = this.propsMap.get(sliceKey);
					LinkedHashMap< String, Roi > roiMap = props.roiMap;
					try {
						Roi roi = roiMap.get(roiKey);
						double area_mm = (double) roi.getStatistics().area * pixelSize * pixelSize / 1000000.0;
						areaList[z-1] = area_mm;
					} catch( Exception e ) {
						areaList[z-1] = 0.0;
					}
				}
				allAreaList.put( roiKey, areaList );
				meanAreaList.put( roiKey, Lib.median( areaList ) );
			}
			
			// Fill the results table
			for ( int z = 1; z <= this.imp.getNSlices(); z++ ) {
				String sliceKey = ImageProperties.selectIndexFromMap( this.propsMap, z );
				r.incrementCounter();
				r.addValue( "Slice ID", sliceKey );
				r.addValue( "Outlier",  "ok");
				String outlierString = "";
				for ( String roiKey : regionList ) {
					r.addValue( roiKey + " area (mm)", Double.toString( allAreaList.get(roiKey)[z-1] ) );
					if (allAreaList.get(roiKey)[z-1] < ( meanAreaList.get( roiKey) / 2.0 ) ) {
						outlierString = outlierString + " " + roiKey;
					}
				}
				r.addValue("Outlier", outlierString);
			}
			
			// Show the results table
			r.show("Check the areas of the regions (too small region are listed as outliers)");
			IJ.setTool(Toolbar.FREEROI);
		}
		
		public void removeOverlap() {
			roiRemoveOverlap( this.roiMap, this.currentZ );
		}

		public void deactivateAllRois() {
			for ( String key : this.roiButtonMap.keySet() ) {
				Button button = this.roiButtonMap.get(key);
				Font font = button.getFont();
				button.setFont(font.deriveFont(Font.PLAIN));
				//button.setForeground( this.buttonDefaultForeGroundColor );
				// TODO roiActive to true instead of false?
				roiActive.put( key, false);
			}
		}

		public void logRoiMap() {
			
			IJ.log( "logRoiMap: size = " + this.roiMap.size() );
			for ( String key : this.roiMap.keySet() ) {
				//IJ.log( key + " position: " + this.roiMap.get(key).getPosition() + " , " + this.roiMap.get(key).getProperties() );
				IJ.log( key + " position: " + this.roiMap.get(key).getPosition() );
			}
		}

		public void logRoiOverlay() {
			
			IJ.log( "logRoiOverlay: size = " + this.overlayCurrent.size() );
			for ( Roi roi : this.overlayCurrent.toArray() ) {
				IJ.log( roi.getName() + " position: " + roi.getPosition() );
			}
		}

		public void logRoiOverlayNew() {
			
			IJ.log( "logRoiOverlayNew: size = " + this.overlayNew.size() );
			for ( Roi roi : this.overlayNew.toArray() ) {
				IJ.log( roi.getName() + " position: " + roi.getPosition() );
			}
		}

		public void redrawOverlayCurrent() {
			//this.overlayCurrent.clear();
			//removeOverlap();
			for ( Roi roi : this.overlayCurrent.toArray() ) {
				if ( roi.getPosition() == this.currentZ ) {
					this.overlayCurrent.remove(roi);
				}
			}
			for ( String key : this.roiMap.keySet() ) {
				this.overlayCurrent.add(this.roiMap.get(key), key);
			}
		}

		public void selectNewRoi( String roiName ) {

			IJ.setTool(Toolbar.FREEROI);
			redrawOverlayCurrent();
			this.overlayCurrent.remove( this.roiMap.get(roiName) );
			this.overlaySelection = this.roiMap.get(roiName);
			this.selectedRoi = roiName;

			deactivateAllRois();

			Font font = this.roiButtonMap.get(roiName).getFont();
			this.roiButtonMap.get(roiName).setFont( font.deriveFont( Font.BOLD ) );
			roiActive.put( roiName, true );

			Roi oldRoi = this.roiMap.get(roiName);
			if ( oldRoi != null ) {
				oldRoi.setStrokeWidth( this.selectedRoiThickness );
				this.imp.setRoi( oldRoi );
			}
		}

		public void doneRoi( String roiName ) {

			this.selectedRoi = roiName;
			this.overlayCurrent.remove( this.roiMap.get(roiName) );
			this.overlayNew.remove( this.roiMap.get(roiName) );
			this.editRoi = this.imp.getRoi();

			if (this.editRoi != null) {
				if (roiInterpolation) {
					FloatPolygon fp = this.editRoi.getInterpolatedPolygon(this.interpolationSmoothDistance, false);
					this.editRoi = new PolygonRoi(fp, Roi.POLYGON);
				}
				this.editRoi.setPosition(this.currentZ);
				this.editRoi.setStrokeWidth( this.defaultRoiThickness );
				Color roiColor = Main.getDefaultColorMap().get( roiName );
				this.editRoi.setStrokeColor( roiColor );
			}
			this.roiMap.put(roiName, this.editRoi);
			this.overlayNew.add(this.editRoi);
			this.overlayCurrent.add(this.editRoi);
			this.imp.deleteRoi();
			this.imp.updateImage();
			if (this.editRoi != null) {
				roiMap.put(this.selectedRoi, this.editRoi);
			} else {
				roiMap.remove(this.selectedRoi);
			}
			removeOverlap();
		}

		public void redoRois() {
			//String[] roiNames = roiNameList.toArray(new String[]{""});
			for (String roiName : roiNameList) {
				this.roiActive.put( roiName, true );
				Font font = this.roiButtonMap.get(roiName).getFont();
				this.roiButtonMap.get(roiName).setFont( font.deriveFont( Font.BOLD ) );
				this.overlayCurrent.remove( this.roiMap.get(roiName) );
			}
			removeRoiFromOverlay( this.overlayNew, this.currentZ );
			for (Roi roi : this.overlayNew.toArray()) {
				if ( roi.getPosition() == this.currentZ ) {
					//this.overlayNew.remove( roi );
				}
			}
		}

		/**
		 *
		 * @param overlay
		 * @param roiName
		 * @param roiPosition
		 */
		public void removeRoiFromOverlay( Overlay overlay, String roiName, int roiPosition ) {
			for (Roi roi : overlay.toArray()) {
				if ( ( roi.getPosition() == roiPosition) && roiName.equals( roi.getName() ) ) {
					overlay.remove( roi );
				}
			}
		}
		/**
		 * Remove all ROIs from a certain slice-position
		 * 
		 * @param overlay
		 * @param roiPosition
		 */
		public void removeRoiFromOverlay( Overlay overlay, int roiPosition ) {
			for (Roi roi : overlay.toArray()) {
				if ( roi.getPosition() == roiPosition ) {
					overlay.remove( roi );
				}
			}
		}

		/**
		 * Return the ROI from an overlay with a given roi name and position (in the stack)
		 * 
		 * @param overlay
		 * @param roiName
		 * @param roiPosition
		 */
		public Roi getRoiFromOverlay( Overlay overlay, String roiName, int roiPosition ) {
			for (Roi roi : overlay.toArray()) {
				if ( ( roi.getPosition() == roiPosition) && roiName.equals( roi.getName() ) ) {
					return roi ;
				}
			}
			return null;
		}

		public void roiRemoveOverlap( LinkedHashMap< String, Roi > roiMap, int roiPosition ) {

			String[] keyset = roiMap.keySet().toArray( new String[]{""} );

			for ( String key1 : keyset ) {
				Roi roi1 = roiMap.get(key1);
				if ( roi1 != null ) {
					for ( String key2 : keyset ) {
						Roi roi2 = roiMap.get(key2);
						if ( roi2 != null ) {
							if ( roi2.getPosition() == roiPosition && !key1.equals(key2) ) {
								Color sc = roi2.getStrokeColor();
								float sw = roi2.getStrokeWidth();
								Roi[] rois2 = minusRoi( roi2, roi1).getRois();
								if ( rois2.length > 0 ) {
									roi2 = rois2[0];
									roi2.setName(key2);
									roi2.setStrokeColor(sc);
									roi2.setStrokeWidth(sw);
									roi2.setPosition(roiPosition);
									roiMap.put(key2, roi2);
								} else {
									roiMap.remove(key2);
								}
							}
						}
					}
				}
			}

			for ( String key : keyset ) {

				// If the roi exists in the roiMap exchange it for the cropped one in the "current regions" overlay
				if ( this.roiMap.get(key) != null ) {

					removeRoiFromOverlay( this.overlayCurrent, key, roiPosition );
					Roi roi = this.roiMap.get(key);
					// The position is already set?
					roi.setPosition( roiPosition );
					this.overlayCurrent.add( roi );

					// If roi was in new overlay exchange it for the cropped one in the "new regions" overlay
					if ( getRoiFromOverlay( this.overlayNew, key, roiPosition ) != null ) {
						removeRoiFromOverlay( this.overlayNew, key, roiPosition );
						roi = this.roiMap.get(key);
						roi.setPosition( roiPosition );
						this.overlayNew.add( roi );
					}
				}
			}
			this.imp.deleteRoi();
			//this.imp.updateImage();
			this.imp.repaintWindow();

		}


		public void saveStackDebug() {
			File outputFileNew = new File( this.outputRoiFolder.getAbsolutePath() + File.separator + "stackNew.tif" );
			File outputFileOld = new File( this.outputRoiFolder.getAbsolutePath() + File.separator + "stackOld.tif" );
			ImagePlus impOld = this.imp.duplicate();
			impOld.setOverlay(this.overlayOld);
			IJ.save( impOld, outputFileOld.getAbsolutePath() );
			ImagePlus impNew = this.imp.duplicate();
			impNew.setOverlay(this.overlayNew);
			IJ.save( impNew, outputFileNew.getAbsolutePath() );
		}

		public void updateRoiImp() {

			Overlay impOverlay = new Overlay();

			for ( int iZ = 1; iZ <= this.imp.getNSlices(); iZ ++ ) {
				imp.setSlice(iZ);
				String sliceKey = ImageProperties.selectIndexFromMap( this.propsMap, iZ );
				ImageProperties props = this.propsMap.get( sliceKey );
				for ( String key : props.roiMap.keySet() ) {
					impOverlay.add( props.roiMap.get(key), key );
				}
			}
			imp.setOverlay( impOverlay );
		}

		public void saveRoiOverlap( String outputCsvName, String outputCurationName ) {

			ArrayList< String > regionList = new ArrayList<>();
			for ( String roiKey : this.roiNameMap.keySet() ) {
				regionList.add(roiKey);
			}
			imp.deleteRoi();
			ImagePlus impShow = imp.duplicate();
			
			// Remove any overlap between the ROIs (should be actually already tackled in SliceMap)
			Overlay overlayAll = new Overlay();
			for ( int z = 1; z <= impShow.getNSlices(); z++ ) {
				String sliceKey = ImageProperties.selectIndexFromMap( propsMap, z );
				ImageProperties props = this.propsMap.get(sliceKey);
				LinkedHashMap< String, Roi > roiMap = props.roiMap;
				try {
					LinkedHashMap< String, Roi > roiMapOri = this.roiMapsOri.get(sliceKey);
					for ( String roiKey : regionList ) {
						try {
							Roi roi = roiMap.get(roiKey);
							roi.setPosition(z);
							try {
								Roi roi_ori = roiMapOri.get(roiKey);
								roi_ori.setPosition(z);
								RegionProperties regionProps = new RegionProperties( sliceKey, roiKey, regionList, roi, roi_ori, this.pixelSize );

								roi_ori.setStrokeColor( Color.GRAY );
								overlayAll.add(roi_ori);
								overlayAll.add(roi);

								// Add text ROIs
								TextRoi textRoi;
								Font font = new Font( null, Font.PLAIN, 40 );

								textRoi = new TextRoi( roiKey +
										"\nArea: " + String.format("%3.2f" , regionProps.area_mm ) +
										"\nDice: " + String.format("%3.2f" , regionProps.dice )
										, roi.getXBase() + roi.getFloatWidth()/2.0, roi.getYBase() + roi.getFloatHeight()/2.0, font );
								textRoi.setPosition(z);
								textRoi.setStrokeColor( roi.getStrokeColor() );
								overlayAll.add( textRoi );

								// Area of the new ROI
								//textRoi = new TextRoi( Double.toString( regionProps.area_mm ) + "mm2", roi.getXBase() + roi.getFloatWidth()/2.0, roi.getYBase() + roi.getFloatHeight()/2.0, font );
								//textRoi.setPosition(z);
								//overlayAll.add( textRoi );

								//textRoi = new TextRoi( roiKey, roi_ori.getXBase() + roi_ori.getFloatWidth()/2.0, roi_ori.getYBase() + roi_ori.getFloatHeight()/2.0, font );
								//textRoi.setPosition(z);
								//overlayAll.add( textRoi );

								LinkedHashMap< String, String > regionString = regionProps.toMap();
							} catch(Exception e) {
								overlayAll.add(roi);
								// Add text ROIs
								TextRoi textRoi;
								Font font = new Font( null, Font.PLAIN, 40 );

								RegionProperties regionProps = new RegionProperties( sliceKey, roiKey, regionList, roi, null, this.pixelSize );
								textRoi = new TextRoi( roiKey +
									"\nArea: " + String.format("%3.2f" , regionProps.area_mm )
									, roi.getXBase() + roi.getFloatWidth()/2.0, roi.getYBase() + roi.getFloatHeight()/2.0, font );
								textRoi.setPosition(z);
								textRoi.setStrokeColor( roi.getStrokeColor() );
								overlayAll.add( textRoi );

								LinkedHashMap< String, String > regionString = regionProps.toMap();
							}
						} catch(Exception e) {
						}
					}
				} catch (Exception e) {
					for ( String roiKey : regionList ) {
						try {
							Roi roi = roiMap.get(roiKey);
							roi.setPosition(z);
							overlayAll.add(roi);
							// Add text ROIs
							TextRoi textRoi;
							Font font = new Font( null, Font.PLAIN, 40 );

							RegionProperties regionProps = new RegionProperties( sliceKey, roiKey, regionList, roi, null, this.pixelSize );
							textRoi = new TextRoi( roiKey +
								"\nArea: " + String.format("%3.2f" , regionProps.area_mm )
								, roi.getXBase() + roi.getFloatWidth()/2.0, roi.getYBase() + roi.getFloatHeight()/2.0, font );
							textRoi.setPosition(z);
							textRoi.setStrokeColor( roi.getStrokeColor() );
							overlayAll.add( textRoi );

							LinkedHashMap< String, String > regionString = regionProps.toMap();
						} catch( Exception e2 ) {
							Logger.getLogger(CurationAnnotationValidation.class.getName()).log(Level.SEVERE, null, e2 );
							IJ.log("Failed file 1: " + e2.getMessage() );
						}
					}
				}
			}
			impShow.setOverlay(overlayAll);
			
			File outputFileShow = new File( this.outputFolder.getAbsolutePath(), outputCurationName + ".tif" );
			IJ.save( impShow, outputFileShow.getAbsolutePath() );
			//LinkedHashMap< String, LinkedHashMap< String, Double > > vopList = computeOverlapList( refRoisFile, roisFile, "roi_", outputNamePrefix, this.regionList, this.pixelSizeMicron, this.addArea );
			//outputTable( outputFile, vopList, true );
		}

		public void saveRois( String outputNamePrefix, String outputNamePrefixSmall ) {
			saveRoisSingle( this.currentSliceKey, outputNamePrefix, outputNamePrefixSmall );
		}

		public void saveAllRois( String outputNamePrefix, String outputNamePrefixSmall ) {
			for ( String sliceKey : this.propsMap.keySet() ) {
				saveRoisSingle( sliceKey, outputNamePrefix, outputNamePrefixSmall );
			}
		}

		public void saveRoisSingle( String sliceKey, String outputNamePrefix, String outputNamePrefixSmall ) {

			ImageProperties props = this.propsMap.get( sliceKey );
			this.roiMap = props.roiMap;
			LibRoi.saveRoiMapCuration( roiMap, props.binning, props, sliceKey, outputRoiFolder.getAbsolutePath(), outputNamePrefixSmall, outputNamePrefix, false );
			double scale = 2.0 / ( (double) props.binning_congealing );
			int sizeX = props.stackWidth;
			int sizeY = props.stackHeight;
			String sampleId = sliceKey;

			// Save ROIs to the registration_images of the slice folder
			// Remove the extra "_montage" postfix if any:
			String sliceSubFolder = sliceKey.replaceAll("_montage", "");
			Path outputCsvPath = Paths.get( this.outputFolder.getAbsolutePath(), sliceSubFolder, "registration_images", "regRoi_curated.csv" );
			File outputCsvFile = outputCsvPath.toFile();
			if ( !outputCsvFile.getParentFile().exists() ) {
				outputCsvFile.getParentFile().mkdirs();
			}
			LibRoi.saveRoiMapAsCsv( this.roiMap, outputCsvFile, sampleId, scale, sizeX, sizeY, props );

			// Save ROIs to the roi folder
			outputCsvPath = Paths.get( this.outputRoiFolder.getAbsolutePath(), "regRoi_" + sliceKey + "_curated.csv" );
			outputCsvFile = outputCsvPath.toFile();
			if ( !outputCsvFile.getParentFile().exists() ) {
				outputCsvFile.getParentFile().mkdirs();
			}
			LibRoi.saveRoiMapAsCsv( this.roiMap, outputCsvFile, sampleId, scale, sizeX, sizeY, props );
		}
	}

	
	public static Roi imageMessage( ImagePlus imp, String text, int line, int index) {
		boolean hideOverlay = imp.getHideOverlay();
		int tx = 10;
		int ty = line * 10;
		Font font = new Font( "Arial", Font.PLAIN, 12 );
		Color color = Color.red;
		TextRoi t = new TextRoi( tx, ty, text, font);
		if (index > 0) {
			t.setPosition( index );
		}
		t.setStrokeColor(color);
		t.setNonScalable( true );
		Overlay overlay = imp.getOverlay();
		overlay.add( t );
		imp.setOverlay(overlay);
		imp.setHideOverlay(hideOverlay);
		//imp.setOverlay( t, color, 1, Color.black );

		return t;
	}

	public static void test() {

		ImageJ imagej = new ImageJ();
		CurationAnnotationValidation cav = new CurationAnnotationValidation();
		File inputImageFile = new File("G:/data/data_astrid_B38/CZI-Beerse38-AT8_PT25_NeuN_DAPI");
		File inputRoiFile = new File("D:/michael_barbier/2018_03_26_MEETING_tau-analysis_herve_astrid_rony/output/roi");
		File outputRoisFile = new File("D:/michael_barbier/2018_03_26_MEETING_tau-analysis_herve_astrid_rony/output/roi");
		File outputFile = new File("D:/michael_barbier/2018_03_26_MEETING_tau-analysis_herve_astrid_rony/output");
		String sampleFilter = "66";
		String regionString = "hp,cx,th,mb,bs,cb";
		String regionStringSeparator = ",";
		double pixelSizeMicron = 0.35 * 8.0;
		String outputNamePrefix = "adapted_";
		cav.runNoUI( inputImageFile, inputRoiFile, outputRoisFile, outputFile, sampleFilter, regionString, regionStringSeparator, pixelSizeMicron, outputNamePrefix, imagej );
	}
	
	
	public static void main(String[] args) {

        Class<?> clazz = CurationAnnotationValidation.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

		IJ.log("START RUN Curation annotation [Janssen]");
		test();
		//IJ.runPlugIn(clazz.getName(), "");
		IJ.log("END RUN Curation annotation [Janssen]");
	}

}
