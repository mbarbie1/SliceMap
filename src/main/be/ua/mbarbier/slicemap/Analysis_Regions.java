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

import fiji.threshold.Auto_Threshold;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.MessageDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.be.ua.mbarbier.slicemap.lib.LibIO;
import static main.be.ua.mbarbier.slicemap.lib.LibIO.writeCsv;
import main.be.ua.mbarbier.slicemap.lib.image.LibImage;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.divideBackground;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.featureExtraction;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.getRoiStatistics;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.statisticsMap;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.subtractBackground;
import main.be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.roisFromThreshold;
import net.lingala.zip4j.exception.ZipException;

/**
 *
 * @author mbarbier
 */
public class Analysis_Regions  implements PlugIn {

	public boolean DEBUG = false;
	public static final String ANALYSIS_METHOD_SPOTS = "intensity and spots";
	public static final String ANALYSIS_METHOD_INTENSITY = "intensity";
	public static final String[] METHODS = new String[]{ ANALYSIS_METHOD_INTENSITY, ANALYSIS_METHOD_SPOTS };

	@Override
	public void run(String arg) {

		try {
			this.DEBUG = true;

			// PARAMETER INPUT
			GenericDialogPlus gdp = new GenericDialogPlus("SliceMap: Analysis regions");
			gdp.addHelp( "https://github.com/mbarbie1/SliceMap" );

			String userPath = IJ.getDirectory("current");
			if (userPath == null) {
				userPath = "";
			}
			if ( this.DEBUG ) {
				gdp.addDirectoryField( "Images folder", "G:/triad_temp_data/demo/Analysis/images/channel_tau" );
				gdp.addDirectoryField( "ROIs folder", "G:/triad_temp_data/demo/Analysis/rois" );
				gdp.addDirectoryField( "Output folder", "G:/triad_temp_data/demo/Analysis/output" );
			} else {
				gdp.addDirectoryField( "Images folder", userPath );
				gdp.addDirectoryField( "ROIs folder", userPath );
				gdp.addDirectoryField( "Output folder", userPath );
			}
			gdp.addChoice( "Analysis method", this.METHODS, this.METHODS[0] );
			gdp.addCheckbox( "Normalize signal to background", true );
			gdp.addStringField("Output name prefix", "normalized_");
			gdp.addStringField("Output table name", "analysis");

			gdp.showDialog();
			if ( gdp.wasCanceled() ) {
				return;
			}

			// EXTRACTION OF PARAMETERS FROM DIALOG
			File inputFile = new File( gdp.getNextString() );
			if (!inputFile.exists()) {
				String warningStr = "(Exiting) Error: Given input folder does not exist: " + inputFile;
				IJ.log(warningStr);
				MessageDialog md = new MessageDialog( null, "SliceMap: Region analysis", warningStr );
				return;
			}
			File inputRoiFile = new File( gdp.getNextString() );
			if (!inputRoiFile.exists()) {
				String warningStr = "(Exiting) Error: Given ROI's folder does not exist: " + inputRoiFile;
				IJ.log(warningStr);
				MessageDialog md = new MessageDialog( null, "SliceMap: Region analysis", warningStr );
				return;
			}
			File outputFile = new File( gdp.getNextString() );
			outputFile.mkdirs();
			String analysisMethod = gdp.getNextChoice();
			boolean normalizeImage = gdp.getNextBoolean();
			String outputImagePrefix = gdp.getNextString();
			String outputName = gdp.getNextString();

			processFolder( inputFile, inputRoiFile, outputFile, outputImagePrefix, outputName, normalizeImage, analysisMethod );
		} catch( Exception e ) {
			StringWriter errors = new StringWriter();
			e.printStackTrace();
			e.printStackTrace(new PrintWriter(errors));
			String stackTraceString = errors.toString();
			String warningStr = "(Exiting) Error: An unknown error occurred.\n\n"+
					"Please contact Michael Barbier if the error persists:\n\n\t michael(dot)barbier(at)gmail(dot)com\n\n"+
					"with the following error:\n\n" + stackTraceString + "\n";
			IJ.log(warningStr);
			MessageDialog md = new MessageDialog( null, "SliceMap: Region analysis", warningStr );
			return;
			//throw new RuntimeException(Macro.MACRO_CANCELED);
		}
	}

	public static void processFolder( File imageFolder, File inputRoiFolder, File outputFolder, String outputImagePrefix, String outputName, boolean normalizeImage, String analysisMethod ) {

		LinkedHashMap< String, LinkedHashMap<String, String > > featureMap = new LinkedHashMap<>();
		ArrayList< LinkedHashMap<String, String > > featureList = new ArrayList<>();
		String filter = "";
		ArrayList<File> imageFileList = LibIO.findFiles(imageFolder);
		LinkedHashMap< String, File> imageFileMap = new LinkedHashMap<>();
		LinkedHashMap< String, File> roiFileMap = new LinkedHashMap<>();
		for (File file : imageFileList) {
			String fileName = file.getName();
			String sliceName;
			if (fileName.contains(".")) {
				sliceName = fileName.substring(0, fileName.lastIndexOf("."));
			} else {
				sliceName = fileName;
			}
			if (sliceName.contains(filter)) {
				File roiSimilarFile = LibIO.findSimilarFile(inputRoiFolder, ".*" + sliceName + ".*");
				if (roiSimilarFile != null) {
					imageFileMap.put(sliceName, file);
					roiFileMap.put(sliceName, roiSimilarFile);
				}
			}
		}

		for (String imageName : imageFileMap.keySet()) {
			File imageFile = imageFileMap.get(imageName);
			ImagePlus imp = IJ.openImage(imageFile.getAbsolutePath());

			// Preprocessing image: normalization of the intensity
			double saturationPercentage = 0.05;
			imp = normalizeIntensity( imp, saturationPercentage, 0.5 );
			//impt = LibImage.
			//imp.show();

			File roiFile = roiFileMap.get(imageName);
			LinkedHashMap< String, Roi> roiMap = new LinkedHashMap<>();
			try {
				roiMap.putAll(LibRoi.loadRoiAlternative(roiFile));
			} catch (ZipException ex) {
				Logger.getLogger(LibImage.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(LibImage.class.getName()).log(Level.SEVERE, null, ex);
			}

			double minSpotAreaPixels = 4.0;
			String thresholdMethod = "Otsu";

			switch ( analysisMethod ) {

				case Analysis_Regions.ANALYSIS_METHOD_INTENSITY :

					for (String roiName : roiMap.keySet()) {
						LinkedHashMap<String, String> features = new LinkedHashMap<>();
						Roi roi = roiMap.get(roiName);
						features.put("image_id", imageName);
						features.put("region_id", roiName);
						// region general features
						features.putAll( featureExtraction(imp, roi) );
						featureMap.put(imageName + "_" + roiName, features);
					}
					break;

				case Analysis_Regions.ANALYSIS_METHOD_SPOTS :

					Roi spotsRoi = extractSpotsRoi( imp, thresholdMethod );
					ShapeRoi sroi = new ShapeRoi( spotsRoi );
					Roi[] rois = sroi.getRois();
					LinkedHashMap< String, String > spotsMap = new LinkedHashMap<>();
					Overlay overlay = new Overlay();
					overlay.add(spotsRoi);
					ImagePlus impShow = imp.duplicate();
					impShow.setOverlay(overlay);
					impShow.deleteRoi();
					impShow.show();
					for (String roiName : roiMap.keySet()) {

						Roi roi = roiMap.get(roiName);
						// spot detection (features)
						ArrayList< LinkedHashMap< String, String > > spotFeaturesList = new ArrayList<>();
						spotFeaturesList = spotsExtraction( imp, new ShapeRoi(spotsRoi), roi, minSpotAreaPixels );
						for ( LinkedHashMap< String, String > spotFeatures : spotFeaturesList ) {
							//spotFeatures.put("channel_id", channelIndexString );
							spotFeatures.put("image_id", imageName);
							spotFeatures.put("region_id", roiName);
						}
						writeCsv(spotFeaturesList, ",", new File( outputFolder.getAbsolutePath() + "/" + "spotFeatures_"+ imageName + "_" + roiName + ".csv").getAbsolutePath());
					}
					break;
			}

		}

		IJ.log("START RUN save features");
		for (String key : featureMap.keySet()) {
			featureList.add(featureMap.get(key));
		}
		writeCsv(featureList, ",", new File(outputFolder.getAbsolutePath() + "/" + "features_roi_g0.5.csv").getAbsolutePath());
		IJ.log("END RUN save features");
	}

	/**
	 * 
	 */
	public static ArrayList< LinkedHashMap< String, String > > spotsExtraction( ImagePlus imp, ShapeRoi spotsRoi, Roi regionRoi, double minSpotAreaPixel ) {

		ArrayList< LinkedHashMap< String, String > > spotFeaturesList = new ArrayList<>();
		ShapeRoi sroi = new ShapeRoi( regionRoi );
		ShapeRoi regionSpotsRoi = spotsRoi.and(sroi);
		Roi[] rois = regionSpotsRoi.getRois();
		for ( Roi roi : rois ) {
			LinkedHashMap< String, String > spotFeatures = new LinkedHashMap<>();
			ImageStatistics stats = getRoiStatistics(imp, roi);
			if (stats.area > minSpotAreaPixel) {
				spotFeatures.putAll( statisticsMap(stats) );
				spotFeaturesList.add(spotFeatures);
			}
		}

		return spotFeaturesList;
	}

	
	/**
	 * 
	 */
	public static Roi extractSpotsRoi( ImagePlus imp, String thresholdMethod ) {

		Auto_Threshold at = new Auto_Threshold();
		Object[] a = at.exec( imp.duplicate(), thresholdMethod, true, true, true, false, false, false);
		ImagePlus impTemp = (ImagePlus) a[1];
		double t = ((Integer) a[0]).intValue() + 0.0;
		Roi sroi = roisFromThreshold( impTemp, t );
		//impShow2.setRoi(sroi);
		//impShow2.show();
		//imp.show();
		//IJ.setAutoThreshold(imp, "Otsu dark");

		return sroi;
	}

	/**
	 *
	 * 
	 * @param imp
	 * @param saturationPercentage Value in range of [ 0.0, 1.0 ]
	 * @return
	 */
	public static ImagePlus normalizeIntensity( ImagePlus imp, double saturationPercentage, double gamma ) {
		
		ImageProcessor ip = imp.getProcessor();
		ip.gamma( gamma );
		// Need ROI of non-zero part
		//ip = subtractBackground( ip, 5 );
		ip = divideBackground( ip, 100.0 * saturationPercentage );
		//ImageStatistics stats = ip.getStatistics();
		//double mean = stats.mean;
		//ip = ip.convertToFloat();
		//if (mean > 0.0) {
		//	ip.multiply( 1.0/mean );
		//}
		imp.setProcessor("normalized", ip);
		
		return imp;
	}
	
	public static void main(String[] args) {

        Class<?> clazz = Analysis_Regions.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        ImageJ imagej = new ImageJ();

		IJ.log("START RUN Analysis regions");
		IJ.runPlugIn(clazz.getName(), "");
		IJ.log("END RUN Analysis regions");
	}	
}
