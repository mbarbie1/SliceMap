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
import ij.gui.Toolbar;
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
import main.be.ua.mbarbier.slicemap.lib.congealing.Congealing;
import main.be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.minusRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.saveRoiAlternative;
import net.lingala.zip4j.exception.ZipException;

/**
 *
 * @author mbarbier
 */
public class Manual_Annotation_Curation implements PlugIn {

	public static final String METHOD_KEY = "key";
	public static final String METHOD_BUTTON = "button";
	public final String METHOD_ANNOTATION = "button";
	public boolean DEBUG = false;
	LinkedHashMap< String, Integer > sig = new LinkedHashMap<>();

	ArrayList< String > roiNameList;

	public void setRoiNameList(ArrayList<String> roiNameList) {
		this.roiNameList = roiNameList;
	}

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


	public class ChoiceList2 extends Panel implements ActionListener {

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

		public ChoiceList2(ImagePlus imp, boolean useRois, LinkedHashMap<String, Roi> roiMapInput, ArrayList< String > roiNameList, File outputFile, File outputRoiFile, LinkedHashMap< String, Integer > sig ) {
			super();
			sig.put("isReady", 0);
			this.outputFile = outputFile;
			this.outputRoiFile = outputRoiFile;
			this.roiNameList = roiNameList;//new ArrayList<>();
			this.roiNameMap = new LinkedHashMap<>();
			for ( String roiName : roiNameList ) {
				this.roiNameMap.put( roiName, roiName );
			}
			
			this.imp = imp;
			this.setVisible(false);
			this.title = imp.getTitle();
			this.roiMap = new LinkedHashMap<>();
			this.roiMapNew = new LinkedHashMap<>();
			this.roiMapOld = new LinkedHashMap<>();
			this.roiButtonMap = new LinkedHashMap();
			this.roiActive = new LinkedHashMap();
			this.roiInterpolation = false;
			this.interpolationSmoothDistance = Math.ceil( 0.01 * imp.getWidth() );
			this.outputNamePrefix = "";
			this.overlayVisible = true;
			this.defaultRoiThickness = 2;
			this.selectedRoiThickness = this.defaultRoiThickness * 3;
			this.overlayOld = new Overlay();//this.imp.getOverlay().duplicate();
			if (useRois) {
				if ( roiMapInput != null ) {
					this.roiMap.putAll( roiMapInput );
					for ( String key : roiMap.keySet() ) {
						overlayOld.add( roiMap.get(key), key );
					}
				}
			}
			this.overlayCurrent = this.overlayOld.duplicate();
			this.imp.setOverlay(overlayCurrent);
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
			int nControlButtons = 2;
			this.regionPanel.setLayout( new GridLayout( nButtons, 1 ) );
			this.viewPanel = new Panel( new GridLayout( nViewButtons, 1 ) );
			this.controlPanel = new Panel( new GridLayout( nControlButtons, 1 ) );

			Button buttonSaveAndNext = new Button( "Save & Next" );
			buttonSaveAndNext.addActionListener(this);
			this.controlPanel.add( buttonSaveAndNext );

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
			//roiRemoveOverlap( this.roiMap );

			this.setVisible(true);
			this.validate();
			
			//imp.getWindow().setLayout( new GridLayout() );
			imp.getWindow().add(this);
			//imp.getWindow().add(this);
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
			redrawOverlayCurrent();

			switch( command ) {
				case "Log ROIs":
					logRoiMap();
					logRoiOverlay();
					logRoiOverlayNew();
					break;
				case "Save & Next":
					LinkedHashMap< String, Roi > tempRoiMap = this.roiMap;
					if ( tempRoiMap.size() > 0 ) {
						saveRoiAlternative( this.outputRoiFile, tempRoiMap );
						sig.put("noFinalRois", 0);
					} else {
						sig.put("noFinalRois", 1);
					}
					this.imp.close();
					sig.put("isReady", 1);
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

		public void removeOverlap() {
			roiRemoveOverlap( this.roiMap );
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
				IJ.log( key + " position: " + this.roiMap.get(key) );
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
				this.overlayCurrent.remove(roi);
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
			removeRoiFromOverlay( this.overlayNew );
		}

		/**
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
		 * Remove all ROIs from a certain slice-position
		 * 
		 * @param overlay
		 * @param roiPosition
		 */
		public void removeRoiFromOverlay( Overlay overlay ) {
			for (Roi roi : overlay.toArray()) {
				overlay.remove( roi );
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
				if ( roi1 != null ) {
					for ( String key2 : keyset ) {
						Roi roi2 = roiMap.get(key2);
						if ( roi2 != null ) {
							if ( !key1.equals(key2) ) {
								Color sc = roi2.getStrokeColor();
								float sw = roi2.getStrokeWidth();
								Roi[] rois2 = minusRoi( roi2, roi1).getRois();
								if ( rois2.length > 0 ) {
									roi2 = rois2[0];
									roi2.setName(key2);
									roi2.setStrokeColor(sc);
									roi2.setStrokeWidth(sw);
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

					removeRoiFromOverlay( this.overlayCurrent, key );
					Roi roi = this.roiMap.get(key);
					// The position is already set?
					this.overlayCurrent.add( roi );

					// If roi was in new overlay exchange it for the cropped one in the "new regions" overlay
					if ( getRoiFromOverlay( this.overlayNew, key ) != null ) {
						removeRoiFromOverlay( this.overlayNew, key );
						roi = this.roiMap.get(key);
						this.overlayNew.add( roi );
					}
				}
			}
			this.imp.deleteRoi();
			//this.imp.updateImage();
			this.imp.repaintWindow();

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

		public ChoiceList( ImagePlus imp, boolean useRois, LinkedHashMap<String, Roi> roiMapInput, ArrayList< String > roiNameList, String outputPath ) {
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
			// if there are previously defined ROIs
			if (useRois) {
				if ( roiMapInput != null ) {
					this.roiMap.putAll( roiMapInput );
					for ( String key : this.roiMap.keySet() ) {
						this.overlayCurrent.add( this.roiMap.get(key) );
					}
				}
			}

			this.imp.setOverlay(overlayCurrent);
			this.regionPanel = new Panel();
			this.controlPanel = new Panel();
			this.viewPanel = new Panel();
			int nButtons = this.roiNameList.size();
			int nViewButtons = 2;
			int nControlButtons = 1;
			int maxButtons = 6;
			int nButtonCols = (int) Math.ceil( ( (double) nButtons ) / ( (double) maxButtons ) );
			int nButtonRows = (int) Math.floor( ( (double) nButtons ) / ( (double) nButtonCols ) );
			this.regionPanel.setLayout( new GridLayout( nButtonRows, nButtonCols ) );
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
				Color roiColor = Main.getDefaultColorMap().get( roiKey );
				button.setBackground( roiColor );
				button.addActionListener(this);
				//this.roiActive.put( roiKey, false );
				this.roiButtonMap.put( roiKey, button );
				this.regionPanel.add( button );
			}
			this.setLayout( new GridLayout(1, 3, 5, 5) );
			this.add(this.regionPanel);
			this.add(this.viewPanel);
			this.viewPanel.validate();
			this.add(this.controlPanel);
			this.controlPanel.validate();
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
			//IJ.log("Action performed with label: " + label );
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
							// Is it allowed to make this a ShapeRoi?
							roi2 = minusRoi( roi2, roi1);//.getRois()[0];
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
				gdp.addDirectoryField( "Sample folder", "G:/triad_temp_data/demo/Curation_2/images/multiChannel" );
				gdp.addDirectoryField( "ROIs folder folder", "G:/triad_temp_data/demo/Curation_2/rois" );
				gdp.addDirectoryField( "Output folder", "G:/triad_temp_data/demo/Curation_2/output" );
			} else {
				gdp.addDirectoryField( "Sample folder", userPath );
				gdp.addDirectoryField( "ROIs folder folder", userPath );
				gdp.addDirectoryField( "Output folder", userPath );
			}
			//gdp.addStringField("Sample file extension", "");
			gdp.addStringField("Sample name contains", "");
			gdp.addStringField("Output file name prefix", "");
			gdp.addCheckbox( "Overwriting existing output ROIs", true );
			//gdp.addCheckbox( "Use existing ROIs", true );
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
			IJ.log("            Manual Annotation & Curation ");
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
				if ( overwriteRois ) {
					try {
						Files.deleteIfExists( new File(outputPath).toPath() );
					} catch (IOException ex) {
						Logger.getLogger(Manual_Annotation.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
				IJ.log("Starting manual annotation & curation: " + fileName);
				process( sampleFile, roiFile, useRois, outputNamePrefix, outputFile, fileName, roiFileName, roiNameList, outputName, this.sig);
				IJ.log("Finished manual annotation & curation: " + fileName);
			}
			IJ.log( "Finished manual Annotation of all samples" );
			IJ.log("------------------------------------------------------");
			IJ.log("------------------------------------------------------");
			IJ.log("------------------------------------------------------");
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
	
	public void process( File inputFolder, File roiFolder, boolean useRois, String outputPrefix, File outputFolder, String inputName, String roiName, ArrayList< String > roiNameList, String outputName, LinkedHashMap< String, Integer > sig ) {
	
		String inputPath = inputFolder.getAbsolutePath() + "/" + inputName;
		String roiPath = roiFolder.getAbsolutePath() + "/" + roiName;
		String outputPath = outputFolder.getAbsolutePath() + "/" + outputName;
		String outputRoiPath = outputFolder.getAbsolutePath() + "/" + outputName;

		// Get current ImagePlus
		ImagePlus imp = IJ.openImage( inputPath );
		if ( !imp.isVisible() ) {
			imp.show();
			//imp.updateAndRepaintWindow()
		}

		// Get the ROI if it exists
		LinkedHashMap<String, Roi> roiMapInput = null;
		if ( Files.exists( new File(roiPath).toPath() ) ) {
			try {
				roiMapInput = LibRoi.loadRoiAlternative( new File(roiPath) );
			} catch (ZipException ex) {
				Logger.getLogger(Manual_Annotation_Curation.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(Manual_Annotation_Curation.class.getName()).log(Level.SEVERE, null, ex);
			}
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
				
				ChoiceList2 cl = new ChoiceList2( imp, useRois, roiMapInput, roiNameList, new File(outputPath), new File(outputRoiPath), sig );
				//ChoiceList cl = new ChoiceList( imp, useRois, roiMapInput, roiNameList, outputPath );
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
			while ( this.sig.get("isReady").intValue() == 0 ) {
			//while ( ( !new File(outputRoiPath).exists() && (this.sig.get("noFinalRois").intValue() == 0) ) || ( this.sig.get("isReady").intValue() == 0 ) ) {
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

        Class<?> clazz = Manual_Annotation_Curation.class;

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
