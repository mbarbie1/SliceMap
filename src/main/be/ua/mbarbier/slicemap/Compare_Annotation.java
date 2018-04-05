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

import static main.be.ua.mbarbier.slicemap.lib.Lib.convertMapDoubleToString;
import static main.be.ua.mbarbier.slicemap.lib.Lib.getCommonKeys;
import main.be.ua.mbarbier.slicemap.lib.LibIO;
import static main.be.ua.mbarbier.slicemap.lib.LibIO.writeCsv;
import main.be.ua.mbarbier.slicemap.lib.error.LibError;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.loadRoiAlternative;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static main.be.ua.mbarbier.slicemap.lib.Lib.getFirstKeys;
import static main.be.ua.mbarbier.slicemap.lib.Lib.readCommaSeparatedList;
import static main.be.ua.mbarbier.slicemap.lib.Lib.writeCommaSeparatedList;
import net.lingala.zip4j.exception.ZipException;

/**
 *
 * @author mbarbier
 */
public class Compare_Annotation implements PlugIn{

	String regionString = "";
	ArrayList< String > regionList = new ArrayList<>();
	double pixelSizeMicron = 1.0;
	boolean addArea = false;

	/**
	 * 
	 */
	@Override
	public void run(String arg) {

		GenericDialogPlus gdp = new GenericDialogPlus("Compare Regions: error/overlap of region definitions");
		gdp.addHelp( "https://gitlab.com/mbarbie1/SliceMap" );

		String userPath = IJ.getDirectory("current");
		if (userPath == null) {
			userPath = "";
		}
		userPath = "C:/Users/mbarbier/Desktop";
		gdp.addDirectoryField( "Computed ROIs folder", userPath + "/curated_mb" );
		gdp.addDirectoryField( "Reference ROIs folder", userPath + "/curated_not" );
		gdp.addFileField( "Output file path", userPath + "/curated_comparisons/" + "roiOverlap.csv");//addDirectoryField( "Output file name", "roiOverlap.csv" );
		gdp.addStringField("region list (komma separated)", "");
		gdp.addNumericField("Pixel size in um", 2.6, 3);
		gdp.addCheckbox( "Add area values in table", true );

		//gdp.addDirectoryField( "Output folder", userPath );

		gdp.showDialog();
		if ( gdp.wasCanceled() ) {
			return;
		}
		File roisFile = new File( gdp.getNextString() );
		File refRoisFile = new File( gdp.getNextString() );
		File outputFile = new File( gdp.getNextString() );
		this.regionString = gdp.getNextString();
		if ( this.regionString.length() > 0 ) {
			this.regionList = readCommaSeparatedList( this.regionString );
		}
		this.pixelSizeMicron = gdp.getNextNumber();
		this.addArea = gdp.getNextBoolean();

		LinkedHashMap< String, LinkedHashMap< String, Double > > vopList = computeOverlapList( refRoisFile, roisFile, "roi_", "roi_", this.regionList, this.pixelSizeMicron, this.addArea );

		outputTable( outputFile, vopList, true );
	}
	
	/**
	 * Write double LinkedHashMap< String, LinkedHashMap< String, Double > > to a file as a table
	 * 
	 * @param file the file where the table is written to
	 * @param tableMap the map of maps describing the table values (Double)
	 * @param extended if true adds id of the row to the table with sample_id in the header
	 */
	public static void outputTable( File file, LinkedHashMap< String, LinkedHashMap< String, Double > > tableMap, boolean extended ) {

		ArrayList< LinkedHashMap< String, String > > tableList = new ArrayList<>();

		Set< String > roiNames = tableMap.get( tableMap.keySet().iterator().next() ).keySet();
		for ( String key : tableMap.keySet() ) {
			LinkedHashMap< String, String > tableRowExtended = new LinkedHashMap<>();
			LinkedHashMap< String, String > tableRow = convertMapDoubleToString( tableMap.get( key ) );
			LinkedHashMap< String, String > tableRowOrder = new LinkedHashMap<>();
			for ( String roiName : roiNames ) {
				tableRowOrder.put(roiName, tableRow.get(roiName) );
			}
			tableRowExtended.put( Main.CONSTANT_SAMPLE_ID_LABEL, key );
			tableRowExtended.putAll( tableRowOrder );
			// Add the total row to the list
			if ( extended ) {
				tableList.add( tableRowExtended );
			} else {
				tableList.add( tableRowOrder );
			}
		}
		writeCsv( tableList, ",", file.getAbsolutePath() );
	}

	/**
	 * Search for files inside a folder (optional filter, string which should be contained within the file name)
	 * and puts their file names without the extension as keys, and the Files as the values
	 * 
	 * @param folder
	 * @param filter Only files with a file name containing this filter as substring are accepted
	 * @return Map of files as a LinkedHashMap< String, File >
	 */
	public static LinkedHashMap< String, File > filesMap( File folder, String filter ) {

		ArrayList<File> fileList = LibIO.findFiles( folder );
		LinkedHashMap< String, File > fileMap = new LinkedHashMap<>();
		for ( File file : fileList ) {
			String fileName = file.getName();
			String sliceName = fileName.substring( 0, fileName.lastIndexOf(".") );
			if ( sliceName.contains( filter ) ) {
				sliceName = sliceName.substring( filter.length(), sliceName.length() );
				fileMap.put(sliceName, file);
			}
		}

		return fileMap;
	}

	/**
	 * Accepts two folders containing corresponding ROIs (similar file names).
	 * Then computes the overlap between corresponding ROIs, and returns it as a map.
	 * 
	 * @param folder1
	 * @param folder2
	 * @param filter1
	 * @param filter2
	 * @param regionList
	 * @param pixelSize
	 * @param addArea
	 * @return 
	 */
	/*
	public static LinkedHashMap< String, LinkedHashMap< String, String > > computeProblemRegionList( File folder1, File folder2, String filter1, String filter2, ArrayList< String > regionList, double pixelSize, boolean addArea ) {

		LinkedHashMap< String, String > out = new LinkedHashMap<>();
		LinkedHashMap< String, File > fileRois1 = filesMap( folder1, filter1 );
		LinkedHashMap< String, File > fileRois2 = filesMap( folder2, filter2 );
		ArrayList< String > keys = getCommonKeys( fileRois1, fileRois2 );
		//ArrayList< String > keys = getFirstKeys( fileRois1, fileRois2 );

		for ( String key : keys ) {
			try {
				File file1 = fileRois1.get(key);
				File file2 = fileRois2.get(key);
				LinkedHashMap< String, Roi > rois1 = loadRoiAlternative( file1 );
				LinkedHashMap< String, Roi > rois2 = loadRoiAlternative( file2 );
				if (addArea) {
					String problemRegions = "";
					ArrayList< String > problemRegionList = problemRois( rois1, rois2, regionList, pixelSize );
					if ( problemRegionList.size() > 0 ) {
						problemRegions = writeCommaSeparatedList( problemRegionList );
					}
					out.put( key, problemRegions);
				}
			} catch (ZipException ex) {
				Logger.getLogger(Compare_Annotation.class.getName()).log(Level.SEVERE, null, ex);
				IJ.log("Failed file 1: " + fileRois1.get(key).getAbsolutePath());
				IJ.log("Failed file 2: " + fileRois2.get(key).getAbsolutePath());
//				LinkedHashMap< String, Double > vop = failedVop();
//				LinkedHashMap< String, Double > areas = failedAreaRois();
				//vop.putAll(areas);
				//vopList.put( key, vop);
			} catch (IOException ex) {
				Logger.getLogger(Compare_Annotation.class.getName()).log(Level.SEVERE, null, ex);
				IJ.log("Failed file 1: " + fileRois1.get(key).getAbsolutePath());
				IJ.log("Failed file 2: " + fileRois2.get(key).getAbsolutePath());
			}
		}

		return vopList;
	}
*/
	
	/**
	 * Accepts two folders containing corresponding ROIs (similar file names).
	 * Then computes the overlap between corresponding ROIs, and returns it as a map.
	 * 
	 * @param folder1
	 * @param folder2
	 * @param filter1
	 * @param filter2
	 * @param regionList
	 * @param pixelSize
	 * @param addArea
	 * @return 
	 */
	public static LinkedHashMap< String, LinkedHashMap< String, Double > > computeOverlapList( File folder1, File folder2, String filter1, String filter2, ArrayList< String > regionList, double pixelSize, boolean addArea ) {

		LinkedHashMap< String, LinkedHashMap< String, Double > > vopList = new LinkedHashMap<>();
		LinkedHashMap< String, File > fileRois1 = filesMap( folder1, filter1 );
		LinkedHashMap< String, File > fileRois2 = filesMap( folder2, filter2 );
		ArrayList< String > keys = getCommonKeys( fileRois1, fileRois2 );
		//ArrayList< String > keys = getFirstKeys( fileRois1, fileRois2 );

		for ( String key : keys ) {
			try {
				File file1 = fileRois1.get(key);
				File file2 = fileRois2.get(key);
				LinkedHashMap< String, Roi > rois1 = loadRoiAlternative( file1 );
				LinkedHashMap< String, Roi > rois2 = loadRoiAlternative( file2 );
				LinkedHashMap< String, Double > vop = compareRois( rois1, rois2 );
				if (addArea) {
					LinkedHashMap< String, Double > areas = areaRois( rois1, rois2, regionList, pixelSize );
					String problemRegions = "";
					ArrayList< String > problemRegionList = problemRois( rois1, rois2, regionList, pixelSize );
					if ( problemRegionList.size() > 0 ) {
						problemRegions = writeCommaSeparatedList( problemRegionList );
					}
					vop.putAll(areas);
					//vop.put( "problem_regions", problemRegions );
				}
				vopList.put( key, vop);
			} catch (ZipException ex) {
				Logger.getLogger(Compare_Annotation.class.getName()).log(Level.SEVERE, null, ex);
				IJ.log("Failed file 1: " + fileRois1.get(key).getAbsolutePath());
				IJ.log("Failed file 2: " + fileRois2.get(key).getAbsolutePath());
//				LinkedHashMap< String, Double > vop = failedVop();
//				LinkedHashMap< String, Double > areas = failedAreaRois();
				//vop.putAll(areas);
				//vopList.put( key, vop);
			} catch (IOException ex) {
				Logger.getLogger(Compare_Annotation.class.getName()).log(Level.SEVERE, null, ex);
				IJ.log("Failed file 1: " + fileRois1.get(key).getAbsolutePath());
				IJ.log("Failed file 2: " + fileRois2.get(key).getAbsolutePath());
			}
		}

		return vopList;
	}


	/**
	 * Compute the overlap between the regions of two RoiSets
	 * 
	 * @param rois1
	 * @param rois2
	 * @return 
	 */
	public static LinkedHashMap< String, Double > compareRois( LinkedHashMap< String, Roi > rois1, LinkedHashMap< String, Roi > rois2 ) {

		ArrayList< String > keys = getFirstKeys( rois1, rois2 );
		//LinkedHashMap< String, LinkedHashMap< String, Double > > vops = new LinkedHashMap<>();
        LinkedHashMap<String, Double> vop = new LinkedHashMap<>();

		for ( String key : keys ) {
			LinkedHashMap<String, Double> vops = new LinkedHashMap<>();
			try {
				vops = LibError.roiVOP( rois1.get(key), rois2.get(key) );
				vop.put( key, vops.get("si") );
			} catch( Exception e ) {
				vop.put( key, 0.0 );
			}
		}

		return vop;
	}

	/**
	 * Compute the overlap between the regions of two RoiSets
	 * 
	 * @param rois1
	 * @param rois2
	 * @return 
	 */
	public static LinkedHashMap< String, Double > areaRois( LinkedHashMap< String, Roi > rois1, LinkedHashMap< String, Roi > rois2, ArrayList< String > regionList, double pixelSize ) {

		ArrayList< String > keys = new ArrayList<String>();
		if ( regionList.size() > 0 ) {
			keys.addAll( regionList );
		} else {
			keys = getFirstKeys( rois1, rois2 );
		}
		//LinkedHashMap< String, LinkedHashMap< String, Double > > vops = new LinkedHashMap<>();
        LinkedHashMap<String, Double> area = new LinkedHashMap<>();

		for ( String key : keys ) {
			try {
				double area1 = (double) rois1.get(key).getStatistics().area * pixelSize * pixelSize / 1000000.0;
				area.put( key + "_area_1", area1 );
			} catch( Exception e ) {
				area.put( key + "_area_1", -1.0 );
			}
			try {
				double area2 = (double) rois2.get(key).getStatistics().area * pixelSize * pixelSize / 1000000.0;
				area.put( key + "_area_2", area2 );
			} catch( Exception e ) {
				area.put( key + "_area_2", -1.0 );
			}
		}

		return area;
	}
	
	/**
	 * Compute the overlap between the regions of two RoiSets
	 * 
	 * @param rois1
	 * @param rois2
	 * @return 
	 */
	public static ArrayList< String > problemRois( LinkedHashMap< String, Roi > rois1, LinkedHashMap< String, Roi > rois2, ArrayList< String > regionList, double pixelSize ) {

		String problemRegions = "";
		ArrayList< String > problemRegionList = new ArrayList<String>();
		ArrayList< String > keys = new ArrayList<String>();
		if ( regionList.size() > 0 ) {
			keys.addAll( regionList );
		} else {
			keys = getFirstKeys( rois1, rois2 );
		}

		for ( String key : keys ) {
			double area1 = -1.0;
			double area2 = -1.0;
			try {
				area1 = (double) rois1.get(key).getStatistics().area * pixelSize * pixelSize / 1000000.0;
			} catch( Exception e ) {
			}
			try {
				area2 = (double) rois2.get(key).getStatistics().area * pixelSize * pixelSize / 1000000.0;
			} catch( Exception e ) {
			}
			if ( (area1 <= 0.0) | (area2 <= 0.0) ) {
				problemRegionList.add(key);
			}
		}
		return problemRegionList;
	}
	
	public static void main(String[] args) {

// set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Compare_Annotation.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        ImageJ imagej = new ImageJ();

		IJ.log("START RUN Compare annotation");
		IJ.runPlugIn(clazz.getName(), "");
		IJ.log("END RUN Compare annotation");
		//imagej.exitWhenQuitting(true);
		//imagej.quit();
		// alternative exit
//        if (!debug) {
//            System.exit(0);
//        }
	}

}
