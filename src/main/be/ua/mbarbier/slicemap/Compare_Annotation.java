/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import net.lingala.zip4j.exception.ZipException;

/**
 *
 * @author mbarbier
 */
public class Compare_Annotation implements PlugIn{

	/**
	 * 
	 */
	@Override
	public void run(String arg) {

		GenericDialogPlus gdp = new GenericDialogPlus("Compare Regions: error/overlap of region definitions");
		gdp.addHelp( "https://gitlab.com/mbarbie1/SliceMap" );
		gdp.addDirectoryField( "Computed ROIs folder", "d:/p_prog_output/slicemap_3/output/roi" );
		gdp.addDirectoryField( "Reference ROIs folder", "d:/p_prog_output/slicemap_3/input/reference_rois" );
		gdp.addDirectoryField( "Output folder", "d:/p_prog_output/slicemap_3/output/roiOverlap.csv" );

		gdp.showDialog();
		if ( gdp.wasCanceled() ) {
			return;
		}
		File roisFile = new File( gdp.getNextString() );
		File refRoisFile = new File( gdp.getNextString() );
		File outputFile = new File( gdp.getNextString() );
		//outputFile.mkdirs();

		LinkedHashMap< String, LinkedHashMap< String, Double > > vopList =  computeOverlapList( refRoisFile, roisFile, "", "roi_" );

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
	 * @return 
	 */
	public static LinkedHashMap< String, LinkedHashMap< String, Double > > computeOverlapList( File folder1, File folder2, String filter1, String filter2 ) {

		LinkedHashMap< String, LinkedHashMap< String, Double > > vopList = new LinkedHashMap<>();
		LinkedHashMap< String, File > fileRois1 = filesMap( folder1, filter1 );
		LinkedHashMap< String, File > fileRois2 = filesMap( folder2, filter2 );
		ArrayList< String > keys = getCommonKeys( fileRois1, fileRois2 );

		for ( String key : keys ) {
			try {
				File file1 = fileRois1.get(key);
				File file2 = fileRois2.get(key);
				LinkedHashMap< String, Roi > rois1 = loadRoiAlternative( file1 );
				LinkedHashMap< String, Roi > rois2 = loadRoiAlternative( file2 );
				LinkedHashMap< String, Double > vop = compareRois( rois1, rois2 );
				vopList.put( key, vop);
			} catch (ZipException ex) {
				Logger.getLogger(Compare_Annotation.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(Compare_Annotation.class.getName()).log(Level.SEVERE, null, ex);
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

		ArrayList< String > keys = getCommonKeys( rois1, rois2 );
		//LinkedHashMap< String, LinkedHashMap< String, Double > > vops = new LinkedHashMap<>();
        LinkedHashMap<String, Double> vop = new LinkedHashMap<>();

		for ( String key : keys ) {
			LinkedHashMap<String, Double> vops = new LinkedHashMap<>();
			vops = LibError.roiVOP( rois1.get(key), rois2.get(key) );
			vop.put( key, vops.get("si1") );
		}

		return vop;
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
        IJ.log("END RUN plugin");
	}

}
