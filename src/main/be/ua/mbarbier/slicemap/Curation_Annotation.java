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
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import java.awt.Button;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		gdp.addDirectoryField( "SliceMap output folder", "C:/Users/mbarbier/Desktop/slicemap_astrid/samples" );
		gdp.addDirectoryField( "SliceMap output folder", "C:/Users/mbarbier/Desktop/slicemap_astrid/output/roi" );
		gdp.addStringField("Adapted ROI file name prefix", "");
		gdp.addStringField("Stack file name", "");
		gdp.addStringField("Stack properties file name", "");
		gdp.addCheckbox( "Overwriting existing ROIs", true );

		gdp.showDialog();
		if ( gdp.wasCanceled() ) {
			return;
		}

		// EXTRACTION OF PARAMETERS FROM DIALOG
		File inputFile = new File( gdp.getNextString() );
		File outputFile = inputFile;
		String outputNamePrefix = gdp.getNextString();
		String tempStackFileName = gdp.getNextString();
		String tempStackPropsFileName = gdp.getNextString();
		boolean overwriteRois = gdp.getNextBoolean();
		//ArrayList< String > roiNameList = new ArrayList<>();

		File stackFile = new File( inputFile.getAbsolutePath() + File.pathSeparator + tempStackFileName + ".tif" );
		File stackPropsFile = new File( inputFile.getAbsolutePath() + File.pathSeparator + tempStackPropsFileName + ".csv" );
		File inputRoiFile = new File( inputFile.getAbsolutePath() + File.pathSeparator + "rois" );
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
		param.PATTERN_REF_FILES = "^(.*?)\\.tif";
		param.CONTAINS_REF_FILES = "tif";
		param.DOESNOTCONTAIN_REF_FILES = ".zip";
		param.CONGEALING_STACKBINNING = 16;
		param.CONGEALING_BINCONGEALING = 1;
		param.CONGEALING_SATURATED_PIXELS_PERCENTAGE = 0.05;
		param.FORMAT_OUTPUT_GRAY_IMAGES = ".tif";
		param.APP_FOLDER = new File( outputFolder.getAbsolutePath() + File.pathSeparator + "thisShouldNotExist_see_Curation_Annotation_processFolder function");
		param.APP_CONGEALING_FOLDER = new File( param.APP_FOLDER.getAbsolutePath() + File.pathSeparator + "thisShouldNotExist_see_Curation_Annotation_processFolder function");
		param.SAMPLE_FOLDER = new File( param.APP_FOLDER.getAbsolutePath() + File.pathSeparator + "thisShouldNotExist_see_Curation_Annotation_processFolder function");
		//param.INPUT_FOLDER = inputFolder;
		param.OUTPUT_FOLDER = outputFolder;
		param.OUTPUT_ROIS_FOLDER = new File( param.APP_FOLDER.getAbsolutePath() + File.pathSeparator + "thisShouldNotExist_see_Curation_Annotation_processFolder function");
		param.FILE_REFERENCE_STACK = stackFile;
		param.FILENAME_REFERENCE_STACK = stackFile.getName();
		param.FILE_STACKPROPS = stackPropsFile;
		//param.PATTERN_ROI_FILES;
		
		RefStack rs = new RefStack();
		//if ( doStackGenerate ) {
			IJ.log("START RUN refStack");
			rs.init( param, inputRoiFolder, inputImageFolder );
			rs.run();
			param.IS_STACK_SET = true;
			param.setRefStack( rs.getStack() );
			param.setStackProps( rs.getStackProps() );
			IJ.log("END RUN refStack");
			//rs.getStack().duplicate().show();
		//}

		
		
		
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
		
	public static LinkedHashMap< String, Roi > processSlice( ImagePlus imp, LinkedHashMap< String, Roi > roiMap ) {
	
		imp.setDisplayMode(IJ.COMPOSITE);
		//imp.setActiveChannels("1010");
		imp.setC(1);
		IJ.run(imp, "Enhance Contrast", "saturated=0.05");
		//imp.setC(3)
		//IJ.run(imp, "Enhance Contrast", "saturated=0.05");
		// //imageMessage(imp, inputName, 1)

		// Polygon point selection tool
		IJ.setTool("polygon");

		// TODO
		return roiMap;
	}

	
	public class ChoiceList extends NonBlockingGenericDialog implements MouseListener {

		String title;
		LinkedHashMap< String, String > roiNameMap;
		LinkedHashMap< String, Button > roiButtonMap;
		
		public ChoiceList(String title, LinkedHashMap< String, String > roiNameMap ) {
			super(title);
			this.setVisible(false);
			this.title = title;
			this.roiNameMap = roiNameMap; 
			this.roiButtonMap = new LinkedHashMap();
			for ( String roiKey : this.roiNameMap.keySet() ) {
				Button button = new Button( this.roiNameMap.get(roiKey) );
				this.roiButtonMap.put(roiKey, button );
				this.add( button );
			}
			this.setVisible(true);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public void mousePressed(MouseEvent e) {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public void mouseExited(MouseEvent e) {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}
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
