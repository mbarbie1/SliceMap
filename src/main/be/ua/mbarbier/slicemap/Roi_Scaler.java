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
import ij.gui.Roi;
import ij.plugin.PlugIn;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import static main.be.ua.mbarbier.slicemap.lib.LibIO.findFiles;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.loadRoiAlternative;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.saveRoiAlternative;
import static main.be.ua.mbarbier.slicemap.lib.transform.TransformRoi.applyRoiScaleTransform;
import net.lingala.zip4j.exception.ZipException;

/**
 *
 * @author mbarbier
 */
public class Roi_Scaler implements PlugIn {

	/**
	 *	Run the scaling plugin (with a dialog)
	 * 
	 * @param string 
	 */
	@Override
	public void run( String string ) {

		GenericDialogPlus gdp = new GenericDialogPlus("SliceMap: ROI Scaler");
		gdp.addHelp( "https://github.com/mbarbie1/SliceMap" );

		String userPath = IJ.getDirectory("current");
		if (userPath == null) {
			userPath = "";
		}
		gdp.addDirectoryField( "ROIs folder", "" );
		gdp.addStringField("ROI file name filter", "");
		gdp.addDirectoryField( "Output folder", "" );
		gdp.addNumericField("Scale factor", 1, 0);
		gdp.addCheckbox( "Overwrite ROI-files", true );
		gdp.addStringField("Output name prefix", "normalized_");

		gdp.showDialog();
		if ( gdp.wasCanceled() ) {
			return;
		}
		
		IJ.log("------------------------------------------------------");
		IJ.log("            ROI scaler ");
		IJ.log("------------------------------------------------------");

		File roiFolder = new File( gdp.getNextString() );
		String roiFilter = gdp.getNextString();
		File outputFile = new File( gdp.getNextString() );
		double scaleFactor = gdp.getNextNumber();
		String outputNamePrefix = gdp.getNextString();
		boolean overwriteRois = gdp.getNextBoolean();
		processFolder( roiFolder, roiFilter, outputFile, outputNamePrefix, scaleFactor, overwriteRois );

		IJ.log( "Finished scaling of all ROIs" );
		IJ.log("------------------------------------------------------");
		IJ.log("------------------------------------------------------");
		IJ.log("------------------------------------------------------");

	}


	/**
	 *	Process (scale ROIs) of the ROI files in the roiFolder and save them to the output folder
	 * 
	 * @param roiFolder The input folder containing the zip-files of the ImageJ ROIs
	 * @param roiFilter A filter on the ROI file names
	 * @param outputFile The output folder
	 * @param outputNamePrefix An optional prefix to indicate these are adapted ROIs
	 * @param scaleFactor The ROIs are multiplied (scaled) by this factor 
	 * @param overwriteRois Whether ROI files should be overwritten
	 */
	public void processFolder( File roiFolder, String roiFilter, File outputFile, String outputNamePrefix, double scaleFactor, boolean overwriteRois ) {
		
		String ext = "zip";
		ArrayList<File> fileList = findFiles( roiFolder, roiFilter, "sl;dj;klsd" );
		for (File file : fileList) {
			String fileName = file.getName();
			if( !fileName.endsWith(ext) ) {
				continue;
			}
			String sample_id = fileName;
			if (fileName.contains(".")) {
				sample_id = fileName.substring(0,fileName.lastIndexOf("."));
			}
			String outputName = outputNamePrefix + sample_id + ".zip";
			String outputPath = outputFile.getAbsolutePath() + "/" + outputName;
			IJ.log("Scaling ROI from: " + fileName);
			process( file, scaleFactor, outputFile, outputName );
		}
		IJ.log( "Finished manual Annotation of all samples" );
	}

	/**
	 *	Load a ROI file, scales the ROIs and saves them to the output file
	 * 
	 * @param roiFile
	 * @param scale 
	 * @param outputFolder
	 * @param outputName 
	 */
	public void process( File roiFile, double scale, File outputFolder, String outputName ) {
		
		LinkedHashMap< String, Roi > roiMap = new LinkedHashMap<>();
		try {
			roiMap = loadRoiAlternative( roiFile );
			process( roiMap, scale, outputFolder, outputName );
		} catch (ZipException ex) {
			Logger.getLogger(Roi_Scaler.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(Roi_Scaler.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public void process( LinkedHashMap< String, Roi > roiMap, double scale, File outputFolder, String outputName ) {
		File roiFile = new File( outputFolder.getAbsolutePath() + File.separator + outputName );
		roiMap = applyRoiScaleTransform( roiMap, 0.0, 0.0, scale );
		saveRoiAlternative( roiFile, roiMap);
	}

	/**
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {

        Class<?> clazz = Roi_Scaler.class;

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
