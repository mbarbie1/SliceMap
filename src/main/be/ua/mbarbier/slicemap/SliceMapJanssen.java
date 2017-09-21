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
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import static main.be.ua.mbarbier.slicemap.Compare_Annotation.computeOverlapList;
import static main.be.ua.mbarbier.slicemap.Compare_Annotation.outputTable;
import main.be.ua.mbarbier.slicemap.gui.GuiJanssen;
import static main.be.ua.mbarbier.slicemap.lib.LibIO.readCsv;

/**
 *
 * @author mbarbier
 */
public class SliceMapJanssen {
	
		final static boolean DEBUG = true;
	
		public static void main(String[] args) {

        ImageJ imagej = new ImageJ();

/*
		IJ.log("START RUN SliceMapMenu");
		// Load all paths for the reference libraries
		//		header: label,type,marker,name,path,dataset
		String filePath = "G:/triad_workflow/referenceLibraries.csv";
		if ( DEBUG ) {
			filePath = "G:/triad_workflow/referenceLibraries.csv";
		} else {
			filePath = "G:/triad_workflow/referenceLibraries.csv";
		}
		// Read the default file with path to the reference libraries 
		ArrayList<LinkedHashMap<String, String>> pathArrayList = readCsv( filePath, "", ",");
		String[] libraryLabelList = new String[ pathArrayList.size() ];
		LinkedHashMap< String, String > libraryPathMap = new LinkedHashMap<>();
		LinkedHashMap< String, String > regionNameListMap = new LinkedHashMap<>();
		for ( int i = 0; i < libraryLabelList.length; i ++ ) {
			libraryLabelList[i] = pathArrayList.get(i).get("label");
			libraryPathMap.put( pathArrayList.get(i).get("label"), pathArrayList.get(i).get("path") );
			regionNameListMap.put( pathArrayList.get(i).get("label"), pathArrayList.get(i).get("regions") );
		}

		GenericDialogPlus gdp = new GenericDialogPlus("[Janssen] SliceMap: Automated annotation of fluorescent brain slices");
		gdp.addHelp( "https://gitlab.com/mbarbie1/SliceMap" );
		gdp.addChoice( "Select the reference:", libraryLabelList, libraryLabelList[0] );
		gdp.showDialog();

		String selectedLibrary = gdp.getNextChoice();
		String selectedLibraryPath = selectedLibrary;
		File inputFile = new File( selectedLibraryPath );
*/
		IJ.log("START RUN SliceMap Janssen");
		new GuiJanssen();
		IJ.log("END SliceMap Janssen");

		
		/*
		gdp = new GenericDialogPlus("[Janssen] SliceMap: Automated annotation of fluorescent brain slices");
		gdp.addHelp( "https://gitlab.com/mbarbie1/SliceMap" );
		gdp.addCheckbox( "Run Annotation Curation procedure?", true);
		gdp.showDialog();
		if ( gdp.getNextBoolean() ) {
			IJ.log("START RUN Annotation Curation Janssen");
			String[] nameListSplit = regionNameListMap.get( selectedLibrary ).split(";");
			ArrayList< String > roiNameList = new ArrayList<>();
			for ( String roiName : nameListSplit ) {
				roiNameList.add(roiName);
			}
			File stackFile = new File( outputFile.getAbsolutePath() + File.separator + tempStackFileName + ".tif" );
			File stackPropsFile = new File( outputFile.getAbsolutePath() + File.separator + tempStackPropsFileName + ".csv" );
			File inputRoiFile = new File( roiFile.getAbsolutePath() );
			processFolder( inputFile, inputRoiFile, outputFile, stackFile, stackPropsFile, outputNamePrefix, overwriteRois );

			IJ.log("END Annotation Curation Janssen");
		}
		
		// nameList is a comma separated list of roiNames as a single String,
		// convert to ArrayList of roiNames
		String[] nameListSplit = nameList.split(",");
		*/
		IJ.log("END SliceMap Janssen");
		//imagej.exitWhenQuitting(true);
		//imagej.quit();
	}
}
