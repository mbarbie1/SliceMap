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

/**
 *
 * @author mbarbier
 */
public class SliceMapMenu {
	
	static final String[] TOOL_LIST = new String[]{ "SliceMap", "Manual Annotation & Curation", "Comparing annotations", "Region analysis" };
			
	public static void main(String[] args) {

        ImageJ imagej = new ImageJ();

		IJ.log("START RUN SliceMapMenu");
		GenericDialogPlus gdp = new GenericDialogPlus("[Tools Menu] SliceMap: Automated annotation of fluorescent brain slices");
		gdp.addHelp( "https://gitlab.com/mbarbie1/SliceMap" );
		gdp.addChoice( "Select the tool:", TOOL_LIST, TOOL_LIST[0] );
		gdp.showDialog();
		if ( gdp.wasCanceled() ) {
			return;
		}
		
		String toolSelectedUI = gdp.getNextChoice();
		switch ( toolSelectedUI ) {
			
			case "SliceMap":
				IJ.log("START RUN SliceMap");
				new SliceMap_().run("");
				IJ.log("END RUN SliceMap");
				break;
				
			case "Manual Annotation & Curation":
				//IJ.log("START RUN Manual annotation");
				new Manual_Annotation_Curation().run("");
				//IJ.log("END RUN Manual annotation");
				break;
				
			case "Comparing annotations":
				IJ.log("START RUN Compare annotation");
				new Compare_Annotation().run("");
				IJ.log("END RUN Compare annotation");
				break;
				
//			case "Annotation curation":
//				IJ.log("START RUN Annotation curation");
//				new Curation_Annotation().run("");
//				IJ.log("END RUN Annotation curation");
//				break;
				
			case "Region analysis":
				IJ.log("START RUN Region analysis");
				new Analysis_Regions().run("");
				IJ.log("END RUN Region analysis");
				break;
		}
		IJ.log("END RUN SliceMapMenu");
		imagej.exitWhenQuitting(true);
		imagej.quit();
		// alternative exit
//        if (!debug) {
        //System.exit(0);
//        }
	}
}
