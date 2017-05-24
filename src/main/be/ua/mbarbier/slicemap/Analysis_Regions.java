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
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.be.ua.mbarbier.slicemap.lib.LibIO;
import static main.be.ua.mbarbier.slicemap.lib.LibIO.writeCsv;
import main.be.ua.mbarbier.slicemap.lib.image.LibImage;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.divideBackground;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.featureExtraction;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.subtractBackground;
import main.be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import net.lingala.zip4j.exception.ZipException;

/**
 *
 * @author mbarbier
 */
public class Analysis_Regions  implements PlugIn {

	@Override
	public void run(String arg) {

		// PARAMETER INPUT
		GenericDialogPlus gdp = new GenericDialogPlus("SliceMap: Analysis regions");
		gdp.addHelp( "https://github.com/mbarbie1/SliceMap" );
		gdp.addDirectoryField( "Images folder", "G:/triad_temp_data/EVT/AT8_scale" );
		gdp.addDirectoryField( "ROIs folder", "G:/triad_temp_data/EVT/output/rois" );
		gdp.addDirectoryField( "Output folder", "G:/triad_temp_data/EVT/output/output_analysis" );
//		gdp.addDirectoryField( "Images folder", "C:/Users/mbarbier/Desktop/test_samples" );
//		gdp.addDirectoryField( "ROIs folder", "C:/Users/mbarbier/Desktop/test_roi" );
//		gdp.addDirectoryField( "Output folder", "C:/Users/mbarbier/Desktop/test_output" );
		gdp.addCheckbox( "Normalize signal to background", true );
		gdp.addStringField("Output name prefix", "normalized_");
		gdp.addStringField("Output table name", "analysis");

		gdp.showDialog();
		if ( gdp.wasCanceled() ) {
			return;
		}

		// EXTRACTION OF PARAMETERS FROM DIALOG
		File inputFile = new File( gdp.getNextString() );
		File inputRoiFile = new File( gdp.getNextString() );
		File outputFile = new File( gdp.getNextString() );
		outputFile.mkdirs();
		boolean normalizeImage = gdp.getNextBoolean();
		String outputImagePrefix = gdp.getNextString();
		String outputName = gdp.getNextString();
		
		processFolder( inputFile, inputRoiFile, outputFile, outputImagePrefix, outputName, normalizeImage );
	}

	public static void processFolder( File imageFolder, File inputRoiFolder, File outputFolder, String outputImagePrefix, String outputName, boolean normalizeImage ) {

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

			for (String roiName : roiMap.keySet()) {
				LinkedHashMap<String, String> features = new LinkedHashMap<>();
				Roi roi = roiMap.get(roiName);
				features.put("image_id", imageName);
				features.put("region_id", roiName);
				features.putAll( featureExtraction(imp, roi) );
				featureMap.put(imageName + "_" + roiName, features);
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
