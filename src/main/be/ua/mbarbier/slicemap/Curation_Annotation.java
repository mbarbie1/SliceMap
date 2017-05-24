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
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
//import javax.swing.BoxLayout;
//import javax.swing.JFrame;
import main.be.ua.mbarbier.slicemap.lib.congealing.Congealing;
import main.be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.minusRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.saveRoiAlternative;

/**
 *
 * @author mbarbier
 */
public class Curation_Annotation implements PlugIn {

	@Override
	public void run(String arg) {

		// PARAMETER INPUT
		GenericDialogPlus gdp = new GenericDialogPlus("SliceMap: Annotation curation");
		gdp.addHelp( "https://github.com/mbarbie1/SliceMap" );
		gdp.addDirectoryField( "Images folder", "G:/triad_temp_data/demo/Curation/images" );
		gdp.addDirectoryField( "ROIs folder", "G:/triad_temp_data/demo/Curation/rois" );
		gdp.addDirectoryField( "Output folder", "G:/triad_temp_data/demo/Curation/output" );
//		gdp.addDirectoryField( "Images folder", "C:/Users/mbarbier/Desktop/test_samples" );
//		gdp.addDirectoryField( "ROIs folder", "C:/Users/mbarbier/Desktop/test_roi" );
//		gdp.addDirectoryField( "Output folder", "C:/Users/mbarbier/Desktop/test_output" );
		gdp.addStringField("Adapted ROI file name prefix", "adapted_");
		gdp.addStringField("Stack file name", "tempStack");
		gdp.addStringField("Stack properties file name", "tempStackProps");
		gdp.addCheckbox( "Overwriting existing ROIs", true );
		gdp.addStringField("List of ROI-names (comma separated)", "hp,cx,cb,th,bs,mb");

		gdp.showDialog();
		if ( gdp.wasCanceled() ) {
			return;
		}

		// EXTRACTION OF PARAMETERS FROM DIALOG
		File inputFile = new File( gdp.getNextString() );
		File roiFile = new File( gdp.getNextString() );
		File outputFile = new File( gdp.getNextString() );
		outputFile.mkdirs();
		String outputNamePrefix = gdp.getNextString();
		String tempStackFileName = gdp.getNextString();
		String tempStackPropsFileName = gdp.getNextString();
		boolean overwriteRois = gdp.getNextBoolean();
		String nameList = gdp.getNextString();
		ArrayList< String > roiNameList = new ArrayList<>();
		// nameList is a comma separated list of roiNames as a single String,
		// convert to ArrayList of roiNames
		String[] nameListSplit = nameList.split(",");
		for ( String roiName : nameListSplit ) {
			roiNameList.add(roiName);
		}
		//ArrayList< String > roiNameList = new ArrayList<>();

		File stackFile = new File( outputFile.getAbsolutePath() + File.separator + tempStackFileName + ".tif" );
		File stackPropsFile = new File( outputFile.getAbsolutePath() + File.separator + tempStackPropsFileName + ".csv" );
		File inputRoiFile = new File( roiFile.getAbsolutePath() );
		processFolder( inputFile, inputRoiFile, outputFile, stackFile, stackPropsFile, outputNamePrefix, overwriteRois );

		//LinkedHashMap< String, String > roiNameMap = new LinkedHashMap<>();
		//roiNameMap.put( "key1", "label1");
		//roiNameMap.put( "key2", "label2");
		//roiNameMap.put( "key3", "label3");
		//ChoiceList cl = new ChoiceList( "Regions", roiNameMap );
	}

	public void processFolder( File inputImageFolder, File inputRoiFolder, File outputFolder, File stackFile, File stackPropsFile, String outputNamePrefix, boolean overwriteRois ) {

		// Find images in inputFolder and ROIs in inputRoiFolder
		Main param = new Main();
		param.PATTERN_REF_FILES = "^(.*?)\\.(tif|png)";
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
		//param.PATTERN_ROI_FILES;
		
		RefStack rs = new RefStack();
		//if ( doStackGenerate ) {
		IJ.log("START RUN refStack");
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
			roiNameMap.put( roiName, roiName );
		}
		ChoiceList cl = new ChoiceList( uiStack, "Regions", roiNameMap, propsMap, outputFolder, outputFolder, stackFile, stackPropsFile );

		// TODO
		//String inputName = ;
		//String inputRoiName = ;
		//String outputName = ;
		//process( inputFolder, inputRoiFolder, outputFolder, inputName, inputRoiName, outputName );
		
		
	}
	
	public void process( File inputFolder, File inputRoiFolder, File outputFolder, String inputName, String inputRoiName, String outputName ) {
	
		String inputPath = inputFolder.getAbsolutePath() + "/" + inputName;
		String inputRoiPath = inputRoiFolder.getAbsolutePath() + "/" + inputRoiName;
		String outputPath = outputFolder.getAbsolutePath() + "/" + outputName;

		// Get current ImagePlus
		ImagePlus imp = IJ.openImage( inputPath );
		if ( !imp.isVisible() ) {
			imp.show();
			//imp.updateAndRepaintWindow()
		}

		// RoiManager
		RoiManager rm = RoiManager.getInstance();
		if (rm == null) {
			rm = new RoiManager();
			rm.reset();
		}
		
	}

	public static LinkedHashMap< String, Roi > processSlice( ImagePlus imp, LinkedHashMap< String, Roi > roiMap, Boolean[] activeChannels ) {

		String activeChannelsString = "1";
		imp.setDisplayMode(IJ.COMPOSITE);
		if (activeChannels.length > 1) {
			imp.setActiveChannels( activeChannelsString );
			for ( int i = 1; i < (activeChannels.length+1); i++ ) {
				imp.setC( i );
				IJ.run( imp, "Enhance Contrast", "saturated=0.05" );
			}
		} else {
			imp.setC(1);
			IJ.run(imp, "Enhance Contrast", "saturated=0.05");
		}
		//imp.setC(3)
		//IJ.run(imp, "Enhance Contrast", "saturated=0.05");
		// //imageMessage(imp, inputName, 1)

		// Polygon point selection tool
		IJ.setTool(Toolbar.FREEROI);

		// TODO
		return roiMap;
	}

	public class ChoiceList extends Panel implements ActionListener {

		public final String OVERLAY_NEW = "New regions";
		public final String OVERLAY_CURRENT = "Current regions";
		public final String OVERLAY_OLD = "Old regions";
		String title;
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
		Roi textMessage = null;
		Roi overlayTypeText = null;
		TextField overlayTypeTextField;
		ImagePlus imp = null;
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

		Panel regionPanel;
		Panel controlPanel;
		Panel viewPanel;

		public ChoiceList(ImagePlus imp, String title, LinkedHashMap< String, String > roiNameMap, LinkedHashMap< String, ImageProperties > propsMap, File outputRoiFolderFile, File outputFolderFile, File stackFile, File stackPropsFile ) {
			super();
			this.imp = imp;
			this.currentZ = imp.getZ();
			this.lastZ = imp.getZ();
			this.textMessage = new TextRoi( 0, 0, "" );
			this.overlayTypeText = new TextRoi( 0, 0, "" ); 
			this.imp.getOverlay().add( this.textMessage );
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
			this.outputRoiFolder = outputRoiFolderFile;
			this.roiInterpolation = false;
			this.interpolationSmoothDistance = Math.ceil( 0.01 * imp.getWidth() );
			this.stackFile = stackFile;
			this.stackPropsFile = stackPropsFile;
			this.overlayVisible = true;
			this.defaultRoiThickness = 2;
			this.selectedRoiThickness = this.defaultRoiThickness * 3;
			this.overlayOld = this.imp.getOverlay().duplicate();
			this.overlayCurrent = this.imp.getOverlay();
			this.overlayNew = new Overlay();
			this.overlayType = this.OVERLAY_CURRENT;
			this.overlayTypeTextField = new TextField( this.overlayType );
			this.overlayTypeTextField.setEditable(false);
			this.regionPanel = new Panel();
			this.controlPanel = new Panel();
			this.viewPanel = new Panel();
			int nButtons = this.roiNameMap.keySet().size();
			int nViewButtons = 3;
			int nControlButtons = 5;
			this.regionPanel.setLayout( new GridLayout( nButtons, 1 ) );
			this.viewPanel = new Panel( new GridLayout( nViewButtons, 1 ) );
			this.controlPanel = new Panel( new GridLayout( nControlButtons, 1 ) );
			
			Button buttonSaveStack = new Button( "Save stack" );
			buttonSaveStack.addActionListener(this);
			this.controlPanel.add( buttonSaveStack );

			Button buttonSaveImage = new Button( "Save all" );
			buttonSaveImage.addActionListener(this);
			this.controlPanel.add( buttonSaveImage );

			Button buttonSave = new Button( "Save" );
			buttonSave.addActionListener(this);
			this.controlPanel.add( buttonSave );

			Button buttonRedo = new Button( "Re-annotate" );
			buttonRedo.addActionListener(this);
			this.controlPanel.add( buttonRedo );

			Button buttonRemoveOverlap = new Button( "Remove overlap" );
			buttonRemoveOverlap.addActionListener(this);
			this.viewPanel.add( buttonRemoveOverlap );

			Button buttonToggleOverlay = new Button( "Toggle overlay" );
			buttonToggleOverlay.addActionListener(this);
			this.viewPanel.add( buttonToggleOverlay );

			Button buttonTypeOverlay = new Button( "Overlay type" );
			buttonTypeOverlay.addActionListener(this);
			this.viewPanel.add( buttonTypeOverlay );
			// Add the label for the current overlay type shown
			this.viewPanel.add( this.overlayTypeTextField );

			for ( String roiKey : this.roiNameMap.keySet() ) {
				Button button = new Button( this.roiNameMap.get(roiKey) );
				button.addActionListener(this);
				this.roiButtonMap.put( roiKey, button );
				this.regionPanel.add( button );
				this.roiActive.put( roiKey, false );
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
			
			//imp.getWindow().setLayout( new GridLayout() );
			imp.getWindow().add(this);
			//imp.getWindow().add(this);
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
			this.lastZ = this.currentZ;
			this.currentZ = this.imp.getZ();
			if ( this.lastZ != this.currentZ ) {
				for ( String roiName : roiMap.keySet() ) {
					this.roiActive.put( roiName, false );
					this.roiButtonMap.get( roiName ).setForeground(Color.black);
				}
			}
			IJ.log("current position: " + this.currentZ );
			this.imp.setPosition( this.currentZ );
			this.imp.setSlice( this.currentZ );
			this.currentSliceKey = ImageProperties.selectIndexFromMap(this.propsMap, this.currentZ);
			ImageProperties props = this.propsMap.get( this.currentSliceKey );
			this.roiMap = props.roiMap;
			this.roiMapOld.putAll(roiMap);

			switch( command ) {
				case "Save stack":
					saveStackDebug();
					break;
				case "Save all":
					IJ.save( this.imp, this.stackFile.getAbsolutePath() );
					Congealing.saveStackProps( this.stackPropsFile, this.propsMap );
					saveAllRois();
					break;
				case "Save":
					saveRois();
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
					this.overlayTypeTextField.setText(this.overlayType);
					break;
				case "Remove overlap":
					removeOverlap();
					break;
				default:
					String[] roiNames = roiMap.keySet().toArray(new String[]{""});
					for ( String roiName : roiNames ) {
						if ( command.equals( roiName ) ) {
							if ( roiActive.get(roiName) ) {
								doneRoi( roiName );
							} else {
								selectNewRoi( roiName );
							}
						}
					}
					break;
			}
		}

		public void removeOverlap() {
			roiRemoveOverlap( this.roiMap, this.currentZ );
		}
		
		public void selectNewRoi( String roiName ) {

			IJ.setTool(Toolbar.FREEROI);
			this.selectedRoi = roiName;
			this.roiButtonMap.get(roiName).setForeground(Color.blue);
			roiActive.put(roiName, true);
			//this.overlayCurrent.remove(this.textMessage);
			//this.textMessage = imageMessage( this.imp, "Region = " + roiName + " CHANGING", 2, this.currentZ );
			this.overlayCurrent.remove(this.roiMap.get(roiName));
			//this.overlayOld.remove(this.roiMap.get(roiName));
			
			Roi oldRoi = this.roiMap.get(roiName);
			oldRoi.setStrokeWidth( selectedRoiThickness );
			this.imp.setRoi( oldRoi );

			//Roi dashedRoi = this.roiMap.get(roiName);
			//BasicStroke s = dashedRoi.getStroke();
			//dashedRoi.setStroke(new BasicStroke(s.getLineWidth(), s.getEndCap(), s.getLineJoin(), s.getMiterLimit(), new float[]{0.0f, 5.0f, 10.0f}, 20.0f));
			//this.imp.getOverlay().add(dashedRoi);
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
				this.editRoi.setStrokeWidth( this.roiMap.get(roiName).getStrokeWidth() );
				this.editRoi.setStrokeColor( this.roiMap.get(roiName).getStrokeColor() );
			}
			this.roiMap.put(roiName, this.editRoi);
			this.overlayNew.add(this.editRoi);
			this.overlayCurrent.add(this.editRoi);
			//roiOntoOverlay( this.roiMap, this.editRoi, this.currentZ );
			//roiOntoOverlay( this.overlayNew, this.editRoi, this.currentZ );
			//roiOntoOverlay( this.overlayCurrent, this.editRoi, this.currentZ );

			this.imp.deleteRoi();
			this.imp.updateImage();
			//this.overlayCurrent.remove(this.textMessage);
			//this.textMessage = imageMessage(this.imp, "Region " + roiName + " DONE", 2, this.currentZ);
			this.roiButtonMap.get(roiName).setForeground(Color.red);
			//this.roiButtonMap.get( roiName ).setEnabled(false);
			roiActive.put(roiName, false);
			if (this.editRoi != null) {
				roiMap.put(this.selectedRoi, this.editRoi);
				//roiOntoOverlay( this.roiMap, this.editRoi, this.currentZ );
			} else {
				roiMap.remove(this.selectedRoi);
			}
		}

		public void redoRois() {
			String[] roiNames = roiMap.keySet().toArray(new String[]{""});
			for (String roiName : roiNames) {
				this.roiActive.put( roiName, true );
				this.roiButtonMap.get(roiName).setForeground(Color.blue);
				this.overlayCurrent.remove( this.roiMap.get(roiName) );
			}
			for (Roi roi : this.overlayNew.toArray()) {
				if (roi.getPosition() == this.currentZ ) {
					this.overlayNew.remove( roi );
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
		
		public void roiOntoOverlay( Overlay overlay, Roi newRoi, int roiPosition ) {

			for (Roi roi : overlay.toArray()) {
				if ( roi.getPosition() == roiPosition ) {
					if (roi != null) {
						roi = getRoiFromOverlay( overlay, roi.getName(), roiPosition );
						overlay.remove(roi);
						Color sc = roi.getStrokeColor();
						float sw = roi.getStrokeWidth();
						roi = minusRoi( roi, newRoi).getRois()[0];
						roi.setStrokeColor(sc);
						roi.setStrokeWidth(sw);
						overlay.add(roi);
					}
				}
			}
			newRoi.setPosition(roiPosition);
			overlay.add(newRoi);
		}
		public void roiOntoOverlay( LinkedHashMap< String, Roi > roiMap, Roi newRoi, int roiPosition ) {

			String[] keyset = roiMap.keySet().toArray( new String[]{""} );
			
			for ( String key : keyset ) {
				Roi roi = roiMap.get(key);
				if ( roi.getPosition() == roiPosition ) {
					if (roi != null) {
						Color sc = roi.getStrokeColor();
						float sw = roi.getStrokeWidth();
						roi = minusRoi( roi, newRoi).getRois()[0];
						roi.setStrokeColor(sc);
						roi.setStrokeWidth(sw);
						roiMap.put(key, roi);
					}
				}
			}
			newRoi.setPosition(roiPosition);
			roiMap.put( newRoi.getName(), newRoi );
		}

		public void roiRemoveOverlap( LinkedHashMap< String, Roi > roiMap, int roiPosition ) {

			String[] keyset = roiMap.keySet().toArray( new String[]{""} );
			
			for ( String key1 : keyset ) {
				Roi roi1 = roiMap.get(key1);
				for ( String key2 : keyset ) {
					Roi roi2 = roiMap.get(key2);
					if ( roi2.getPosition() == roiPosition && !key1.equals(key2) ) {
						if (roi2 != null) {
							Color sc = roi2.getStrokeColor();
							float sw = roi2.getStrokeWidth();
							roi2 = minusRoi( roi2, roi1).getRois()[0];
							roi2.setStrokeColor(sc);
							roi2.setStrokeWidth(sw);
							roi2.setPosition(roiPosition);
							roiMap.put(key2, roi2);
						}
					}
				}
			}
			for ( String key : keyset ) {
				// If the roi exists in the roiMap exchange it for the cropped one in the "current regions" overlay
				if (!this.roiMap.get(key).equals(null)) {
					removeRoiFromOverlay( this.overlayCurrent, key, roiPosition );
					this.overlayCurrent.add( this.roiMap.get(key) );
					// If roi was in new overlay exchange it for the cropped one in the "new regions" overlay
					if ( getRoiFromOverlay( this.overlayNew, key, roiPosition ) != null ) {
						removeRoiFromOverlay( this.overlayNew, key, roiPosition );
						this.overlayNew.add( this.roiMap.get(key) );
					}
				}
			}
		}
//		public void roiOntoOverlay( LinkedHashMap< String, Roi > roiMap, Roi newRoi, int roiPosition ) {
//
//			for (Roi roi : overlay.toArray()) {
//				if ( roi.getPosition() == roiPosition ) {
//					if (roi != null) {
//						roi = minusRoi( roi, newRoi).getRois()[0];
//					}
//				}
//			}
//			newRoi.setPosition(roiPosition);
//			overlay.add(newRoi);
//		}

		public void saveRois() {
			//File outputRoiFile = new File( outputRoiFolder.getAbsolutePath() + File.separator + "roiSmall_" + this.currentSliceKey + ".zip" );
			//saveRoiAlternative( outputRoiFile, roiMap );
			ImageProperties props = propsMap.get(this.currentSliceKey);
			LibRoi.saveRoiMap( roiMap, props.binning, props, null, this.currentSliceKey, outputRoiFolder.getAbsolutePath(), "" );
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

		public void saveAllRois() {
			for ( String sliceKey : this.propsMap.keySet() ) {
				LinkedHashMap< String, Roi > tempRoiMap = this.propsMap.get(sliceKey).roiMap;
				File outputRoiFile = new File( outputRoiFolder.getAbsolutePath() + File.separator + sliceKey + ".zip" );
				saveRoiAlternative( outputRoiFile, tempRoiMap );
			}
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
	
	public static void main(String[] args) {

        Class<?> clazz = Curation_Annotation.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        ImageJ imagej = new ImageJ();

		IJ.log("START RUN Curation annotation");
		IJ.runPlugIn(clazz.getName(), "");
		IJ.log("END RUN Curation annotation");
	}

}
