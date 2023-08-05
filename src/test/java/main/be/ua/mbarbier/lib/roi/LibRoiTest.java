/*
 * The MIT License
 *
 * Copyright 2023 University of Antwerp.
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
package test.java.main.be.ua.mbarbier.lib.roi;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import java.awt.Color;
import java.io.File;
import java.util.LinkedHashMap;
import main.be.ua.mbarbier.slicemap.gui.Gui;
import main.be.ua.mbarbier.slicemap.lib.roi.LabelFusion;
import static main.be.ua.mbarbier.slicemap.lib.roi.LabelFusion.getInterpolationMap;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.getOverlayImage;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.loadRoiDefinitions;

/**
 *
 * @author mbarbier
 */
public class LibRoiTest {
    
    public static void testRoiFromMask() {
		
	IJ.log("STARTING TEST: roiFromMask");
	String inputFolder = "/Users/mbarbier/Desktop/slicemap_question_kenneth/input_cortex_downsized_inverted/";
	String testFolder = "/Users/mbarbier/Desktop/slicemap_question_kenneth/testMask/";
        File roiFile = new File(inputFolder + "reference_images/932_4G8_6_1_1_downsized_1.tif");
        File imageFile = new File(inputFolder + "reference_rois/932_4G8_6_1_1_downsized_1.zip");
        
	LinkedHashMap< String, ImagePlus > probMapSubset_reduced = new LinkedHashMap<>();
        File probFile2 = new File(testFolder + "prob_test2.tif");
        ImagePlus prob2 = IJ.openImage( probFile2.getAbsolutePath() );
        probMapSubset_reduced.put("test2", prob2);
        File probFile1 = new File(testFolder + "prob_test1.tif");
        ImagePlus prob1 = IJ.openImage( probFile1.getAbsolutePath() );
        probMapSubset_reduced.put("test1", prob1);
        File probFile3 = new File(testFolder + "prob_test3.tif");
        ImagePlus prob3 = IJ.openImage( probFile3.getAbsolutePath() );
        probMapSubset_reduced.put("test3", prob3);
        LinkedHashMap<String, Color> roiColor = new LinkedHashMap<>();
        roiColor.put("test1", Color.RED);
        roiColor.put("test2", Color.GREEN);
        roiColor.put("test3", Color.BLUE);
        // ImagePlus mask = maskFromThreshold( prob, 0.5 );
        // mask.show();
	// Roi roi = roiFromMask(mask);
        ImagePlus sample = prob1.duplicate();
                                
        LinkedHashMap< String, Roi > roiInterpolationMapSubset = getInterpolationMap( probMapSubset_reduced, LabelFusion.METHOD_LABELFUSION_THRESHOLD, true );
        ImagePlus impOverlaySubsetTest = getOverlayImage( roiInterpolationMapSubset, sample, roiColor ).duplicate();
        impOverlaySubsetTest.show();
        
	// ImageStatistics stats = ImageStatistics.getStatistics( mask.getProcessor(), ImageStatistics.MIN_MAX, new Calibration() );
        // LinkedHashMap< String, Roi > rois = new LinkedHashMap<>();
        // rois.put("probRoi", roi);
        // getOverlayImage( rois, mask ).show();
        
        // roiInterpolationMapSubset = getInterpolationMap( probMapSubset_reduced, LabelFusion.METHOD_LABELFUSION_THRESHOLD, true );

        // LinkedHashMap<String, Roi> roiMap = loadRoiAlternative(File roiFile);
        // Roi roi roiFromMask	IJ.log("END AUTOMATED SEGMENTATION");
	IJ.log("END TEST: roiFromMask");
    }	
        
    public static void testLoadRoiDefinitions() {
	IJ.log("STARTING TEST: loadRoiDefinitions");
	String inputFolder = "/Users/mbarbier/Desktop/slicemap_question_kenneth/input_cortex_downsized_inverted/";
        File roiFile = new File(inputFolder + "reference_images/932_4G8_6_1_1_downsized_1.tif");
        File imageFile = new File(inputFolder + "reference_rois/932_4G8_6_1_1_downsized_1.zip");
        File roiDefinitionsFile = new File(inputFolder + "roi_definitions.csv");
        LinkedHashMap<String, Color> colors = loadRoiDefinitions(roiDefinitionsFile);
	IJ.log("END TEST: loadRoiDefinitions");
    }
	
    public static void main(String[] args) {
		
        // float[] xPoints = new float[]{ 1.f,2.f,3.f,4.f };
        // float[] yPoints = new float[]{ 2.f,3.f,4.f,5.f };
        // int type = Roi.POLYGON;
        // PolygonRoi meanRoi = new PolygonRoi( xPoints, yPoints, type);
        // run_convertRoiMapZipToCsv();
        
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Gui.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        ImageJ imagej = new ImageJ();

		IJ.log("START RUN test");
		//testRoiFromMask();
		testLoadRoiDefinitions();
                //Gui gui = new Gui();
		IJ.log("END RUN test");

    }
}
