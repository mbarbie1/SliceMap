package main.be.ua.mbarbier.slicemap.lib.roi;

import main.be.ua.mbarbier.slicemap.ImageProperties;
import main.be.ua.mbarbier.slicemap.Main;
import main.be.ua.mbarbier.slicemap.lib.Lib;
import static main.be.ua.mbarbier.slicemap.lib.Lib.median;
import static main.be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.maskFromThreshold;
import main.be.ua.mbarbier.slicemap.lib.transform.TransformRoi;
import static main.be.ua.mbarbier.slicemap.lib.transform.TransformRoi.applyRoiScaleTransform;
import bunwarpj.Transformation;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Wand;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.measure.Calibration;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.Blitter;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import main.be.ua.mbarbier.slicemap.gui.Gui;
import static main.be.ua.mbarbier.slicemap.gui.Gui.test;
import main.be.ua.mbarbier.slicemap.lib.LibIO;
import main.be.ua.mbarbier.slicemap.lib.LibText;
import static main.be.ua.mbarbier.slicemap.lib.roi.LabelFusion.getInterpolationMap;
import static main.be.ua.mbarbier.slicemap.lib.roi.LabelFusion.majorityVoting;
import static main.be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.excludeOutlierRois;
import static main.be.ua.mbarbier.slicemap.lib.roi.RoiMap.mapInvert;
import static main.be.ua.mbarbier.slicemap.lib.transform.TransformRoi.applyRoiScaleTransformAlternative;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;

public class LibRoi {

    final static String EVOTEC_REGION_PREFIX = "EVT_Regions_";

    /**
     * Interpolates multiple ROIs by taking a (sum) z-projection of the stack of ROIs
     * 
     * @param rois
     * @param sizeX
     * @param sizeY
     * @return 
     */
    public static Roi interpolateRois( ArrayList<Roi> rois, int sizeX, int sizeY ) {

        Roi roi = null;

		// Sum all the ROI masks
        ImageProcessor mask = new FloatProcessor( sizeX, sizeY );
        for ( int i = 0; i < rois.size(); i++ ) {
            Roi roiCurrent = rois.get(i);
            mask.copyBits( roiCurrent.getMask().convertToFloat(), (int) Math.round(roiCurrent.getXBase()), (int) Math.round(roiCurrent.getYBase()), Blitter.ADD );
        }

		ImageStatistics stats = ImageStatistics.getStatistics( mask, ImageStatistics.MIN_MAX, new Calibration() );
		double maxHeight = stats.max;

		// Obtain main masks of iso-contours for each integer height
		ArrayList<Roi> isoRois = new ArrayList<>();
		for (int h = 0; h < maxHeight; h++) {
			ImageProcessor tmpMask = mask.duplicate();
			tmpMask.subtract((double) h);
			tmpMask.max(1);
			ImagePlus impTmpMask = new ImagePlus("tmpMask", tmpMask);
			IJ.run( impTmpMask, "Create Selection", "");
			roi = impTmpMask.getRoi();
			isoRois.add(roi);
		}

		// Interpolate between the main isolines

        // TODO 

        return roi;
    }


	/**
     * When having only the imagej zip-files of the ROI's, convert them to a csv-file
     * 
     * @param roiFile
     * @param outputFile
     * @param sampleId
	 * @param scale
	 * @param sizeX
	 * @param sizeY 
	 * @throws net.lingala.zip4j.exception.ZipException 
	 * @throws java.io.IOException 
     */
	public static void convertRoiMapZipToCsv( File roiFile, File outputFile, String sampleId, double scale, int sizeX, int sizeY ) throws ZipException, IOException {
		LinkedHashMap<String, Roi> roiMap = loadRoiAlternative( roiFile );
		ImageProperties fakeSampleProps = new ImageProperties();
		fakeSampleProps.xOffset = 0;
		fakeSampleProps.yOffset = 0;
		saveRoiMapAsCsv( roiMap, outputFile, sampleId, scale, sizeX, sizeY, fakeSampleProps );
	}

	public static void run_convertRoiMapZipToCsv() {
		
		String[] sampleIdList = new String[] {"B49-P-73_montage",
		"B49-P-68_montage",
		"B49-P-66_montage",
		"B49-P-77_montage",
		"B49-P-79_montage",
		"B49-P-81_montage",
		"B49-P-9_montage",
		"B49-N-1_montage",
		"B49-N-12_montage",
		"B49-N-27_montage",
		"B49-N-20_montage",
		"B49-N-29_montage",
		"B49-N-30_montage",
		"B49-N-35_montage",
		"B49-N-39_montage",
		"B49-N-41_montage",
		"B49-N-43_montage",
		"B49-N-46_montage",
		"B49-N-49_montage",
		"B49-N-51_montage",
		"B49-N-52_montage",
		"B49-N-56_montage",
		"B49-N-57_montage",
		"B49-N-59_montage",
		"B49-N-63_montage",
		"B49-N-67_montage",
		"B49-N-69_montage",
		"B49-N-70_montage",
		"B49-N-72_montage",
		"B49-N-71_montage",
		"B49-N-75_montage",
		"B49-N-76_montage",
		"B49-N-78_montage",
		"B49-N-80_montage",
		"B49-N-82_montage",
		"B49-P-11_montage",
		"B49-P-28_montage",
		"B49-P-19_montage",
		"B49-P-3_montage",
		"B49-P-31_montage",
		"B49-P-38_montage",
		"B49-P-36_montage",
		"B49-P-40_montage",
		"B49-P-44_montage",
		"B49-P-45_montage",
		"B49-P-47_montage",
		"B49-P-48_montage",
		"B49-P-50_montage",
		"B49-P-53_montage",
		"B49-P-54_montage",
		"B49-P-58_montage",
		"B49-P-60_montage"};

		for ( String sampleId : sampleIdList ) {
			String baseFolder = "G:/triad/registration/B49/curated_hm/repeat";
			String outputFolder = "G:/triad/registration/B49/curated_hm/repeat_csv";
			//String baseFolder = "C:/Users/mbarbier/Desktop/curated_mb/roi";
			//String outputFolder = "C:/Users/mbarbier/Desktop/curated_mb/csv";
			String roiFileName = "roi_" + sampleId + ".zip";
			String outputFileName = sampleId + "_regRoi_curated.csv";
			double scale = 1;
			int sizeX = 5100;
			int sizeY = 5100;
			File roiFile = new File( baseFolder, roiFileName );
			File outputFile = new File( outputFolder, outputFileName );

			try {
				convertRoiMapZipToCsv( roiFile, outputFile, sampleId, scale, sizeX, sizeY );
			} catch (ZipException ex) {
				Logger.getLogger(LibRoi.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(LibRoi.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
	
	/**
     * Interpolates multiple ROIs by taking a (sum) z-projection of the stack of ROIs
     * 
     * @param rois
     * @param sizeX
     * @param sizeY
     * @return 
     */
    public static void saveRoiMapAsCsv( LinkedHashMap<String, Roi> roiMap, File outputFile, String sampleId, double scale, int sizeX, int sizeY, ImageProperties sampleProps ) {

		String sliceName = sampleId;
		//LinkedHashMap<String, Roi> roiInterpolationMap, double scale, ImageProperties sampleProps, String sampleId, String outputFolder, String prefix
		LinkedHashMap<String, Roi> roiL = applyRoiScaleTransformAlternative( roiMap, 0.0, 0.0, scale );
        ArrayList<String> roiNames = new ArrayList<>();
        ArrayList<Roi> roiList = new ArrayList<>();
        for ( String key : roiL.keySet() ) {
            roiNames.add( key );
			roiList.add( roiL.get(key) );
        }

		int cropX = sampleProps.xOffset;
		int cropY = sampleProps.yOffset;
		// OK now for the ROIs
		LinkedHashMap<String, Roi> roiCrop = new LinkedHashMap<>();
		for (String key : roiL.keySet()) {
			Roi roi = roiL.get(key);
			double xx = roi.getXBase();
			double yy = roi.getYBase();
			roi.setLocation(xx - cropX, yy - cropY);
		}
		
		// Save transformed roi
        //int sizeX = this.source.getWidth() * (int) Math.round( scale );
        //int sizeY = this.source.getHeight() * (int) Math.round( scale );
        float xlt = 0;
        float ylt = 0;
        float xrb = (float) (sizeX);
        float yrb = (float) (sizeY);
        String regRois = LibEvotecRois.getEvotecRoisRow(xlt, ylt, xrb, yrb, sizeX, sizeY, Lib.RoiArrayListToRoiArray( roiList ));
        LibIO.writeRois( LibText.concatenateStringArray(roiNames.toArray(new String[]{""}), "	"), regRois, outputFile.getAbsolutePath() );
	}
	
	public static void saveRoiMap( LinkedHashMap<String, Roi> roiInterpolationMap, double scale, ImageProperties sampleProps, Roi sampleRoi, String sampleId, String outputFolder, String prefix ) {
	
		String sliceName = sampleId;
		// ---------------------------------------------------------------------
		// --- Scale all the ROIs to original size -----------------------------
		// ---------------------------------------------------------------------
		LinkedHashMap<String, Roi> roiL = applyRoiScaleTransform(roiInterpolationMap, 0.0, 0.0, scale );
		// Smooth the large ROIs
		boolean smooth = true;
		double interval = scale;
		LinkedHashMap<String, Roi> roiTemp = new LinkedHashMap<String, Roi>();
		for (String key : roiL.keySet()) {
			Roi roi = roiL.get(key);
			Roi smoothRoi = new PolygonRoi(roi.getInterpolatedPolygon(interval, smooth), Roi.POLYGON);
			roiTemp.put(key, smoothRoi);
		}
		roiL = roiTemp;

		// ---------------------------------------------------------------------
		// --- Crop the transformed ROIs to original width and height ----------
		// ---------------------------------------------------------------------
		int cropX = sampleProps.xOffset;
		int cropY = sampleProps.yOffset;
		// OK now for the ROIs
		LinkedHashMap<String, Roi> roiCrop = new LinkedHashMap<>();
		for (String key : roiL.keySet()) {
			Roi roi = roiL.get(key);
			double xx = roi.getXBase();
			double yy = roi.getYBase();
			roi.setLocation(xx - cropX, yy - cropY);
			Roi cropRoi;
			if (sampleRoi != null) {
				cropRoi = LibRoi.intersectRoi(sampleRoi, roi);
			} else {
				cropRoi = roi;
			}
			try {
				if (cropRoi != null) {
					roiCrop.put(key, cropRoi);
				}
			} catch (Exception e) {
			}
		}

		// ---------------------------------------------------------------------
		// --- Save all the ROIs -----------------------------------------------
		// ---------------------------------------------------------------------
		//
		// SAVE THE ROIs
		String roiFileNameSample = prefix + "roiSample_" + sliceName + ".zip";
		String roiFileNameL = prefix + "roi_" + sliceName + ".zip";
		String roiFileNameC = prefix + "roiCrop_" + sliceName + ".zip";
		String roiFileNameS = prefix + "roiSmall_" + sliceName + ".zip";
		String roiFilePathSample = outputFolder + "/" + roiFileNameSample;
		String roiFilePathS = outputFolder + "/" + roiFileNameS;
		String roiFilePathC = outputFolder + "/" + roiFileNameC;
		String roiFilePathL = outputFolder + "/" + roiFileNameL;
		File roiFileSample = new File(roiFilePathSample);
		LinkedHashMap<String, Roi> sampleRoiMap = new LinkedHashMap<>();
		if (sampleRoi != null) {
			sampleRoiMap.put("bg", sampleRoi);
			saveRoiAlternative(roiFileSample, sampleRoiMap);
			File roiFileC = new File(roiFilePathC);
			saveRoiAlternative(roiFileC, roiCrop);
		}
		File roiFileL = new File(roiFilePathL);
		saveRoiAlternative(roiFileL, roiL);
		File roiFileS = new File(roiFilePathS);
		saveRoiAlternative(roiFileS, roiInterpolationMap);
	}

	public static void saveRoiMapCuration( LinkedHashMap<String, Roi> roiInterpolationMap, double scale, ImageProperties sampleProps, String sampleId, String outputFolder, String prefixSmall, String prefix, boolean smoothLarge ) {
	
		String sliceName = sampleId;
		// ---------------------------------------------------------------------
		// --- Scale all the ROIs to original size -----------------------------
		// ---------------------------------------------------------------------
		LinkedHashMap<String, Roi> roiL = applyRoiScaleTransform(roiInterpolationMap, 0.0, 0.0, scale );
		// Smooth the large ROIs
		if ( smoothLarge ) {
			boolean smooth = true;
			double interval = scale;
			LinkedHashMap<String, Roi> roiTemp = new LinkedHashMap<>();
			for (String key : roiL.keySet()) {
				Roi roi = roiL.get(key);
				Roi smoothRoi = new PolygonRoi(roi.getInterpolatedPolygon(interval, smooth), Roi.POLYGON);
				roiTemp.put(key, smoothRoi);
			}
			roiL = roiTemp;
		}

		// ---------------------------------------------------------------------
		// --- Crop the transformed ROIs to original width and height ----------
		// ---------------------------------------------------------------------
		int cropX = sampleProps.xOffset;
		int cropY = sampleProps.yOffset;
		// OK now for the ROIs
		LinkedHashMap<String, Roi> roiCrop = new LinkedHashMap<>();
		for (String key : roiL.keySet()) {
			Roi roi = roiL.get(key);
			double xx = roi.getXBase();
			double yy = roi.getYBase();
			roi.setLocation(xx - cropX, yy - cropY);
			Roi cropRoi = roi;
			try {
				if (cropRoi != null) {
					roiCrop.put(key, cropRoi);
				}
			} catch (Exception e) {
			}
		}

		// ---------------------------------------------------------------------
		// --- Save all the ROIs -----------------------------------------------
		// ---------------------------------------------------------------------
		//
		// SAVE THE ROIs
		String roiFileNameL = prefix + sliceName + ".zip";
		String roiFileNameS = prefixSmall + sliceName + ".zip";
		String roiFilePathL = outputFolder + "/" + roiFileNameL;
		String roiFilePathS = outputFolder + "/" + roiFileNameS;
		File roiFileL = new File(roiFilePathL);
		saveRoiAlternative(roiFileL, roiL);
		File roiFileS = new File(roiFilePathS);
		saveRoiAlternative(roiFileS, roiInterpolationMap);
	}

	public static Roi intersectRoi(Roi roi1, Roi roi2) {
		
		ShapeRoi sroi1 = new ShapeRoi(roi1);
		ShapeRoi sroi2 = new ShapeRoi(roi2);
		sroi1.and(sroi2);
		Roi outRoi = sroi1.shapeToRoi();
		
		return outRoi;
	}

	public static ShapeRoi xorRoi(Roi roi1, Roi roi2) {
		
		ShapeRoi sroi1 = new ShapeRoi(roi1);
		ShapeRoi sroi2 = new ShapeRoi(roi2);
		ShapeRoi soutRoi = sroi1.xor(sroi2);
		//Roi outRoi = soutRoi.shapeToRoi();
		
		return soutRoi;
	}

	public static ShapeRoi minusRoi(Roi roi1, Roi roi2) {
		
		ShapeRoi sroi1 = new ShapeRoi(roi1);
		ShapeRoi sroi2 = new ShapeRoi(roi2);
		ShapeRoi soutRoi = sroi1.not(sroi2);
		//Roi outRoi = soutRoi.shapeToRoi();
		
		return soutRoi;
	}

	public static ShapeRoi unionRoi(Roi roi1, Roi roi2) {
		
		ShapeRoi sroi1 = new ShapeRoi(roi1);
		ShapeRoi sroi2 = new ShapeRoi(roi2);
		ShapeRoi soutRoi = sroi1.or(sroi2);
		//Roi outRoi = soutRoi.shapeToRoi();
		
		return soutRoi;
	}

	
	public static Roi maskRoi(Roi roi, ImagePlus mask) {
		
		ShapeRoi sroi = new ShapeRoi(roi);
		Roi roiMask = roiFromMask(mask);
		ShapeRoi sroiMask = new ShapeRoi(roiMask);
		sroi.and(sroiMask);
		Roi outRoi = sroi.shapeToRoi();
		
		return outRoi;
	}
	
		/**
	 * multiply pixels inside roi
	 * 
	 * @param imp
	 * @param roi
	 * @return 
	 */
	public static ImagePlus mulRoi(ImagePlus imp, Roi roi) {

		ImageProcessor ip = imp.getProcessor();
		FloatProcessor fp = new FloatProcessor(imp.getWidth(), imp.getHeight() );
		fp.setRoi(roi);
		fp.setValue(-1);
		fp.fill(roi.getMask());
		fp.setValue(1);
		fp.fillOutside(roi);
		ImagePlus maskIMP = new ImagePlus("mask", fp);
		ip.copyBits( fp, 0, 0, Blitter.MULTIPLY);
		//maskIMP.show();
		//imp.duplicate().show();

		return imp;
	}

	public static Roi roisFromThreshold( ImagePlus imp, double minValue ) {
		
		ImageProcessor ip = imp.getProcessor();
		ip.setThreshold(minValue, ip.maxValue(), 0);
		//ip.setB
		ThresholdToSelection ts = new ThresholdToSelection();
//		ts.run(imp);
		Roi roi = ts.convert( ip );
		//imp2.setRoi(roi);
		return roi;
	}

	/*
	public static ArrayList< Roi > roiMaxFromMask( ImagePlus mask ) {

		ImageStatistics stats = mask.getStatistics(ImageStatistics.CENTER_OF_MASS);

		Roi sroi = roisFromThreshold( mask, minValue );
		Roi roi = roiMaxRegion( mask, sroi );
		
		//IJ.log("x = " + x + ", y = " + y );
		Wand w = new Wand(mask.getProcessor());
		double mint = 1;
		w.autoOutline( (int)(Math.round(x)), (int)(Math.round(y)) );
		//mask.show();
		Roi roi = null;
		if ( w.npoints > 0) {
	        roi = new PolygonRoi( w.xpoints, w.ypoints, w.npoints, Roi.TRACED_ROI );
	        mask.setRoi(roi);
		}
	    return roi;
	}
	*/
	
	public static Roi roiFromMask(ImagePlus mask) {

		ImageStatistics stats = mask.getStatistics(ImageStatistics.CENTER_OF_MASS);
                // ImageStatistics stats2 = mask.getStatistics();
                //mask.show();
		double x = 1; //stats.xCenterOfMass;
		double y = stats.yCenterOfMass;
                int yi = (int)(Math.round(y));
                int[] data = new int[mask.getWidth()];
                // mask.show();
                mask.getProcessor().getRow(1, yi, data, mask.getWidth());
		//IJ.log("x = " + x + ", y = " + y );
		Wand w = new Wand(mask.getProcessor());
		double mint = 1;
		w.autoOutline( (int)(Math.round(x)), (int)(Math.round(y)) );
		//mask.show();
		Roi roi = null;
		if ( w.npoints > 0) {
                    roi = new PolygonRoi( w.xpoints, w.ypoints, w.npoints, Roi.TRACED_ROI );
                    mask.setRoi(roi);
		}
	    return roi;
	}

	/**
	 * Make a contour mask image (ImagePlus) from a ImageJ ROI
	 * 
	 * @param roi
	 * @param w
	 * @param h
	 * @return 
	 */
	public static ImagePlus roiToContour(Roi roi, int w, int h) {
		ImagePlus imp = IJ.createImage("contour", w, h, 1, 8);
		ImageProcessor ip = imp.getProcessor();
		ip.setLineWidth(1);
		ip.setColor(Color.WHITE);
		roi.drawPixels(ip);
		//imp.duplicate().show();

		return imp;
	}


	public static LinkedHashMap< String, ArrayList< Roi > > getMapOfLists( LinkedHashMap< String, LinkedHashMap<String, Roi > > roiMapList ) {
		// get ROI arrays
		LinkedHashMap<String, ArrayList< Roi>> roiListMap = new LinkedHashMap<String, ArrayList< Roi>>();
		for (String key : roiMapList.keySet()) {
			LinkedHashMap<String, Roi> roiMap = roiMapList.get(key);
			for (String roiName : roiMap.keySet()) {
				if (!roiListMap.containsKey(roiName)) {
					roiListMap.put(roiName, new ArrayList< Roi>());
				}
				roiListMap.get(roiName).add(roiMap.get(roiName));
			}
		}
		return roiListMap;
	}


    /**
     * 
     * @param rois
     * @param imp
     * @return 
     */
    public static ImagePlus getInterpolationOverlayImage( ArrayList< LinkedHashMap< String, Roi > > rois, LinkedHashMap< String, Roi > interpRois, ImagePlus imp ) {

        ImagePlus impRois = new ImagePlus("Image with rois", imp.getProcessor().duplicate().convertToRGB());
        Overlay overlay = new Overlay();
        for (int i = 0; i < rois.size(); i++) {

            LinkedHashMap< String, Roi > roiMap = rois.get(i);

            for ( String key : roiMap.keySet() ) {

                Roi roi = roiMap.get(key);
                roi.setStrokeWidth(3);
                String name = key;
                if (name != null && key.startsWith("EVT_Regions_")) {
                    name = key.substring( key.lastIndexOf("_") + 1, key.length());
                }
                Color color = roiColor().get(name);
                if (color != null) {
                } else if ( roi.getStrokeColor() != null) {
                    color = roi.getStrokeColor();
                } else {
                    color = Color.GRAY;
                }
                roi.setStrokeColor(color);
                overlay.add( roi, name);
            }
        }

        LinkedHashMap< String, Roi > roiMap = interpRois;

        for (String key : roiMap.keySet()) {

			try {// Verify whether ROI exists, if not just don't add it
				Roi roi = roiMap.get(key);
				roi.setStrokeWidth(6);
				String name = key;
				if (name != null && key.startsWith("EVT_Regions_")) {
					name = key.substring(key.lastIndexOf("_") + 1, key.length());
				}
				Color color = roiColor().get(name);
				if (color != null) {
				} else if (roi.getStrokeColor() != null) {
					color = roi.getStrokeColor();
				} else {
					color = Color.GRAY;
				}
				roi.setStrokeColor(color);
				overlay.add(roi, name);
			} catch(Exception e) {
			}
        }

        overlay.setLabelFont(new Font("fontName", Font.PLAIN, (int) Math.round(impRois.getWidth() / 50.0)));
        impRois.setOverlay(overlay);
        impRois.setHideOverlay(false);

        return impRois;
    }

    /**
     * 
     * @param rois
     * @param imp
     * @return 
     */
    public static ImagePlus getOverlayImage( LinkedHashMap< String, Roi > rois, ImagePlus imp ) {

		ContrastEnhancer ce = new ContrastEnhancer();
		ImageProcessor ip = imp.getProcessor();
		ce.stretchHistogram( imp, 0.1 );
		ip.multiply( 255. / ((double) imp.getStatistics().max) );
		imp.setProcessor(ip.convertToByte(false));

        ImagePlus impRois = new ImagePlus("Image with rois", imp.getProcessor().duplicate().convertToRGB());
        Overlay overlay = new Overlay();
        LinkedHashMap< String, Roi > roiMap = rois;

        for (String key : roiMap.keySet()) {

			try {// Verify whether ROI exists, if not just don't add it
				Roi roi = roiMap.get(key);
				roi.setStrokeWidth(Math.max( Math.ceil( impRois.getWidth() / 200.0 ), 2) );
				String name = key;
				if (name != null && key.startsWith("EVT_Regions_")) {
					name = key.substring(key.lastIndexOf("_") + 1, key.length());
				}
				Color color = roiColor().get(name);
				if (color != null) {
				} else if (roi.getStrokeColor() != null) {
					color = roi.getStrokeColor();
				} else {
					color = Color.GRAY;
				}
				roi.setStrokeColor(color);
				overlay.add(roi, name);
			} catch(Exception e) {
			}
        }

        overlay.setLabelFont(new Font("fontName", Font.PLAIN, (int) Math.round(impRois.getWidth() / 50.0)));
        impRois.setOverlay(overlay);
        impRois.setHideOverlay(false);
		impRois = impRois.flatten();

        return impRois;
    }

    /**
     * 
     * @param rois
     * @param imp
     * @return 
     */
    public static ImagePlus getOverlayImageRGB( LinkedHashMap< String, Roi > rois, ImagePlus imp ) {

        ImagePlus impRois = new ImagePlus("Image with rois", imp.getProcessor().duplicate().convertToRGB());
        Overlay overlay = new Overlay();
        LinkedHashMap< String, Roi > roiMap = rois;

        for (String key : roiMap.keySet()) {

			try {// Verify whether ROI exists, if not just don't add it
				Roi roi = roiMap.get(key);
				roi.setStrokeWidth(Math.max( Math.ceil( impRois.getWidth() / 200.0 ), 2) );
				String name = key;
				if (name != null && key.startsWith("EVT_Regions_")) {
					name = key.substring(key.lastIndexOf("_") + 1, key.length());
				}
				Color color = roiColor().get(name);
				if (color != null) {
				} else if (roi.getStrokeColor() != null) {
					color = roi.getStrokeColor();
				} else {
					color = Color.GRAY;
				}
				roi.setStrokeColor(color);
				overlay.add(roi, name);
			} catch(Exception e) {
			}
        }

        overlay.setLabelFont(new Font("fontName", Font.PLAIN, (int) Math.round(impRois.getWidth() / 50.0)));
        impRois.setOverlay(overlay);
        impRois.setHideOverlay(false);
		impRois.flatten();

        return impRois;
    }



    /**
     * 
     * @param rois
     * @param imp
     * @return 
     */
    public static ImagePlus getConfidenceBandOverlayImage( LinkedHashMap< String, LinkedHashMap< String, Roi > > ciMap, ImagePlus imp ) {
	
		ImagePlus overlayImage = imp.duplicate();
		ImageProcessor ip = overlayImage.getProcessor();
		Overlay overlay = new Overlay();

		for ( String key : ciMap.keySet() ) {

                    LinkedHashMap< String, Roi > ci = ciMap.get(key);
                    Roi roi = ci.get("mean");
                    Roi roi1 = ci.get("inner");
                    Roi roi2 = ci.get("outer");
                    ShapeRoi roiOr = LibRoi.xorRoi( roi1, roi2);
                    roiOr.setFillColor(LibRoi.roiColor().get(key));
                    // If there is no difference between the inner and outer ROIs (approx. zero error) then we show the mean ROI
                    if (roiOr.getLength() == 0) {
                        overlay.add(roi);
                    } else {
                        overlay.add(roiOr);
                        overlay.setFillColor(LibRoi.roiColor().get(key));
                    }
			// roiMapBand.put( key + "_XOR", roiOr );
		}
		overlayImage.setOverlay( overlay );
		overlayImage.setHideOverlay(false);
		overlayImage = overlayImage.flatten();
		//overlayImage.show();
		
		return overlayImage;
	}
	 
    /**
     * 
     * @param rois
     * @param imp
     * @return 
     */
    public static ImagePlus getConfidenceOverlayImage( LinkedHashMap< String, LinkedHashMap< String, Roi > > ciMap, ImagePlus imp ) {

        ImagePlus impRois = new ImagePlus("Image with rois", imp.getProcessor().duplicate().convertToRGB());
        Overlay overlay = new Overlay();
        for ( String key : ciMap.keySet() ) {

			LinkedHashMap< String, Roi > ci = ciMap.get(key);
            Roi roi = ci.get("mean");
            Roi innerRoi = ci.get("inner");
            Roi outerRoi = ci.get("outer");
            roi.setStrokeWidth(3);
            innerRoi.setStrokeWidth(1);
            outerRoi.setStrokeWidth(1);
            String name = key;
            if (name != null && key.startsWith("EVT_Regions_")) {
                name = key.substring( key.lastIndexOf("_") + 1, key.length());
            }
            Color color = roiColor().get(name);
            if (color != null) {
            } else if ( roi.getStrokeColor() != null) {
                color = roi.getStrokeColor();
            } else {
                color = Color.GRAY;
            }
            roi.setStrokeColor(color);
            overlay.add( roi, name);
            overlay.add( innerRoi, name+"_inner");
            overlay.add( outerRoi, name+"_outer");
        }

        overlay.setLabelFont(new Font("fontName", Font.PLAIN, (int) Math.round(impRois.getWidth() / 50.0)));
        impRois.setOverlay(overlay);
        impRois.setHideOverlay(false);

        return impRois;
    }
	
    /**
     * 
     * @param rois
     * @param imp
     * @return 
    public static ImagePlus getConfidenceOverlayImage( LinkedHashMap< String, Roi > roiMap, LinkedHashMap< String, ImagePlus > ciMaskMap, ImagePlus imp ) {

        ImagePlus impRois = new ImagePlus("Image with rois", imp.getProcessor().duplicate().convertToRGB());
        Overlay overlay = new Overlay();
        for ( String key : ciMaskMap.keySet() ) {

			ImagePlus mask = ciMaskMap.get(key);
            Roi roi = roiMap.get(key);
            roi.setStrokeWidth(3);
            innerRoi.setStrokeWidth(1);
            outerRoi.setStrokeWidth(1);
            String name = key;
            if (name != null && key.startsWith("EVT_Regions_")) {
                name = key.substring( key.lastIndexOf("_") + 1, key.length());
            }
            Color color = roiColor().get(name);
            if (color != null) {
            } else if ( roi.getStrokeColor() != null) {
                color = roi.getStrokeColor();
            } else {
                color = Color.GRAY;
            }
            roi.setStrokeColor(color);
            overlay.add( roi, name);
            overlay.add( innerRoi, name+"_inner");
            overlay.add( outerRoi, name+"_outer");
        }

        overlay.setLabelFont(new Font("fontName", Font.PLAIN, (int) Math.round(impRois.getWidth() / 50.0)));
        impRois.setOverlay(overlay);
        impRois.setHideOverlay(false);

        return impRois;
    }
     */
	
    /**
     * 
     * @param rois
     * @param imp
     * @return 
     */
    public static ImagePlus getOverlayImage( Roi[] rois, ImagePlus imp ) {
        ImagePlus impRois = new ImagePlus("Image with rois", imp.getProcessor().duplicate().convertToRGB());
        Overlay overlay = new Overlay();
        for (int i = 0; i < rois.length; i++) {
            Roi currentRoi = rois[i];
            // TODO
            currentRoi.setStrokeWidth(3);
            String name = currentRoi.getName();
            if (name != null && currentRoi.getName().startsWith("EVT_Regions_")) {
                name = currentRoi.getName().substring(currentRoi.getName().lastIndexOf("_") + 1, currentRoi.getName().length());
            }
            Color color = roiColor().get(name);
            if (color != null) {
            } else if (currentRoi.getStrokeColor() != null) {
                color = currentRoi.getStrokeColor();
            } else {
                color = Color.GRAY;
            }
            currentRoi.setStrokeColor(color);
            overlay.add(currentRoi, name);// currentRoi.getName() );
//			overlay.setLabelColor( color );
//			overlay.setStrokeColor( color ); 
        }
        overlay.setLabelFont(new Font("fontName", Font.PLAIN, (int) Math.round(impRois.getWidth() / 50.0)));
        //overlay.drawNames( true );
        //overlay.drawLabels( true );
        impRois.setOverlay(overlay);
        impRois.setHideOverlay(false);

        return impRois;
    }
	
    /**
     * Translate the points in an imagej Roi
     */
    public static Roi translateRoi( Roi roi, double dx, double dy ) {
        ArrayList<Float> xt = new ArrayList<Float>();
        ArrayList<Float> yt = new ArrayList<Float>();
        // Get ROI points
        FloatPolygon polygon = roi.getFloatPolygon();
        int n_points = polygon.npoints;
        float[] x = polygon.xpoints;
        float[] y = polygon.ypoints;
        for (int i = 0; i < n_points; i++) {
            xt.add( (float) ( (double) x[i] + dx ) );
            yt.add( (float) ( (double )y[i] + dy ) );
        }
        Roi transfoRoi = new PolygonRoi(Lib.FloatArrayTofloatArray(xt), Lib.FloatArrayTofloatArray(yt), xt.size(), PolygonRoi.FREEROI);
        transfoRoi.setName(roi.getName());
        transfoRoi.setStrokeColor(roi.getStrokeColor());

        return transfoRoi;
    }

    
    /**
     * Transform the points in an imagej Roi using a bunwarpj registration
     * transformation object (note to self: remember the jarray java jython
     * array)
     */
    public static Roi transformRoi(Transformation transfo, Roi roi) {
        ArrayList<Float> xt = new ArrayList<Float>();
        ArrayList<Float> yt = new ArrayList<Float>();
        // Get ROI points
        FloatPolygon polygon = roi.getFloatPolygon();
        int n_points = polygon.npoints;
        float[] x = polygon.xpoints;
        float[] y = polygon.ypoints;
        double[] xyF = {30.0, 40.0};
        for (int i = 0; i < n_points; i++) {
            // TODO: Check, is it inverse or direct transformation???
            transfo.transform((double) (x[i]), (double) (y[i]), xyF, true);
            xt.add((float) xyF[0]);
            yt.add((float) xyF[1]);
        }
        Roi transfoRoi = new PolygonRoi(Lib.FloatArrayTofloatArray(xt), Lib.FloatArrayTofloatArray(yt), xt.size(), PolygonRoi.FREEROI);
        transfoRoi.setName(roi.getName());
        transfoRoi.setStrokeColor(roi.getStrokeColor());

        return transfoRoi;
    }

    /**
     * Transform the points in an imagej Roi using a bunwarpj registration
     * transformation object (note to self: remember the jarray java jython
     * array)
     */
    public static Roi transformRoiInverse(Transformation transfo, Roi roi) {
        ArrayList<Float> xt = new ArrayList<Float>();
        ArrayList<Float> yt = new ArrayList<Float>();
        // Get ROI points
        FloatPolygon polygon = roi.getFloatPolygon();
        int n_points = polygon.npoints;
        float[] x = polygon.xpoints;
        float[] y = polygon.ypoints;
        double[] xyF = {30.0, 40.0};
        for (int i = 0; i < n_points; i++) {
            // TODO: Check, is it inverse or direct transformation???
            transfo.transform((double) (x[i]), (double) (y[i]), xyF, false);
            xt.add((float) xyF[0]);
            yt.add((float) xyF[1]);
        }
        Roi transfoRoi = new PolygonRoi(Lib.FloatArrayTofloatArray(xt), Lib.FloatArrayTofloatArray(yt), xt.size(), PolygonRoi.FREEROI);
        transfoRoi.setName(roi.getName());
        transfoRoi.setStrokeColor(roi.getStrokeColor());

        return transfoRoi;
    }

    public static ArrayList<Roi> getTransformedRoiImage(ArrayList<Roi> rois, Transformation transfo) {
        ArrayList<Roi> trois = new ArrayList<Roi>();
        for (Roi roi : rois) {
            trois.add(transformRoi(transfo, roi));
        }
        return trois;
    }

	public static LinkedHashMap< String, Roi > getTransformedRoiImage( LinkedHashMap<String,Roi> roiMap, Transformation transfo) {
        LinkedHashMap< String, Roi > troiMap = new LinkedHashMap<>();
        for (String key : roiMap.keySet() ) {
			Roi roi = roiMap.get(key);
            troiMap.put( key, transformRoi(transfo, roi) );
        }
        return troiMap;
    }

    public static ArrayList<Roi> getInverseTransformedRoiImage(ArrayList<Roi> rois, Transformation transfo) {
        ArrayList<Roi> trois = new ArrayList<Roi>();
        for (Roi roi : rois) {
            trois.add(transformRoiInverse(transfo, roi));
        }
        return trois;
    }

	public static LinkedHashMap< String, Roi > getInverseTransformedRoiImage( LinkedHashMap<String,Roi> roiMap, Transformation transfo) {
        LinkedHashMap< String, Roi > troiMap = new LinkedHashMap<>();
        for (String key : roiMap.keySet() ) {
			Roi roi = roiMap.get(key);
            troiMap.put( key, transformRoiInverse(transfo, roi) );
        }
        return troiMap;
    }

    public static double polygonArea( float[] x, float[] y, int n) {

        double A = 0;
        int j = n-1;

        for (int i = 0; i < n; i++ ) {
            A = A + (x[j]+x[i]) * (y[j]-y[i]); 
            j = i;
        }

        return A/2.0;
    }
    
    public static LinkedHashMap<String, Color> roiColor() {
        LinkedHashMap<String, Color> colors = new LinkedHashMap<String, Color>();
        colors.put("Bg", Color.gray);
        colors.put("Cx", new Color(0, 128, 0));
        colors.put("Hip", Color.green);
        colors.put("Th", Color.red);
        colors.put("Bs", Color.pink);
        colors.put("Mb", Color.magenta);
        colors.put("Cb", Color.yellow);
        colors.put("Bg", Color.gray);
        // No Capitals
        colors.put("cx", new Color(0, 128, 0));
        colors.put("hp", Color.green);
        colors.put("th", Color.red);
        colors.put("bs", Color.pink);
        colors.put("mb", Color.magenta);
        colors.put("cb", Color.yellow);

        return colors;
    }

    public static String labelToEvotecLabel(String label) {
        return EVOTEC_REGION_PREFIX + label;
    }

    public static String evotecLabelToLabel(String evotecLabel) {
        return evotecLabel.substring(EVOTEC_REGION_PREFIX.length());
    }

	public static PointRoi loadPointRoi(File roiFile) throws ZipException, IOException {

		PointRoi proi = null;
        RoiDecoder rd = new RoiDecoder(roiFile.getAbsolutePath());
        try {
            ZipFile zipFile = new ZipFile(roiFile);
            List<FileHeader> hs = zipFile.getFileHeaders();
            for (FileHeader h : hs) {
                ZipInputStream z = zipFile.getInputStream(h);
                ArrayList<Byte> bytesa = new ArrayList<Byte>();
                int bi = z.read();
                byte b;
                while (bi > -1) {
                    b = (byte) bi;
                    bytesa.add(b);
                    bi = z.read();
                }
                byte[] bytes = new byte[bytesa.size()];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = bytesa.get(i);
                }
                //bytesa.toArray(be);
                Roi roi = RoiDecoder.openFromByteArray(bytes);
				String roiName = "NA";
				if (roi.getName() != null) {
					roiName = roi.getName();
				} else {
					String roiFileName = h.getFileName();
					roiName = roiFileName.substring(0, roiFileName.indexOf("."));
					IJ.log("roiFileName " + roiFileName);
				}
				proi = (PointRoi) roi;
			}
        } catch (ZipException | IOException e) {
            throw e;
        } finally {
        }
        return proi;
    }



	public static LinkedHashMap<String, Roi> loadRoiAlternative(File roiFile) throws ZipException, IOException {

        LinkedHashMap<String, Roi> roiM = new LinkedHashMap<String, Roi>();

        RoiDecoder rd = new RoiDecoder(roiFile.getAbsolutePath());
        try {
            ZipFile zipFile = new ZipFile(roiFile);
            List<FileHeader> hs = zipFile.getFileHeaders();
            for (FileHeader h : hs) {
                ZipInputStream z = zipFile.getInputStream(h);
                ArrayList<Byte> bytesa = new ArrayList<Byte>();
                int bi = z.read();
                byte b;
                while (bi > -1) {
                    b = (byte) bi;
                    bytesa.add(b);
                    bi = z.read();
                }
                //byte[] be = new byte[]{b};
                byte[] bytes = new byte[bytesa.size()];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = bytesa.get(i);
                }
                //bytesa.toArray(be);
                Roi roi = RoiDecoder.openFromByteArray(bytes);
				String roiName = "NA";
				if (roi.getName() != null) {
					roiName = roi.getName();
				} else {
					String roiFileName = h.getFileName();
					roiName = roiFileName.substring(0, roiFileName.indexOf("."));
					IJ.log("roiFileName " + roiFileName);
					//String[] parts = roiFileName.split(".");
					//roiName = parts[0];
				}
				roiM.put( roiName, roi);
				
				//IJ.log("ROI load " + roi.getName());
            }
        } catch (ZipException | IOException e) {
            //e.printStackTrace();
            throw e;
        } finally {
        }
        //zipFile.extractAll( roiFile.getParent() + "/ziptest" );

        //Lib.sortRois(roiM);

        return roiM;
    }
	
	public static void saveRoiAlternative(File roiFile, LinkedHashMap<String, Roi> roiM) {
        
        try {
            ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( roiFile.getAbsolutePath() ) );
            RoiEncoder re = new RoiEncoder( zos );
            for (String key : roiM.keySet()) {
                try {
                    ZipEntry ze = new ZipEntry( key + ".roi" );
                    zos.putNextEntry( ze );
                    Roi roi = roiM.get(key);
					if (roi != null) {
	                    re.write(roi);
					}
                } catch (IOException ex) {
                    Logger.getLogger(TransformRoi.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            zos.finish();
            zos.close();
        } catch (IOException ex) {
            Logger.getLogger(TransformRoi.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

        
    public static void test() {
		
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
        // ImagePlus mask = maskFromThreshold( prob, 0.5 );
        // mask.show();
	// Roi roi = roiFromMask(mask);
        ImagePlus sample = prob1.duplicate();
                                
        LinkedHashMap< String, Roi > roiInterpolationMapSubset = getInterpolationMap( probMapSubset_reduced, LabelFusion.METHOD_LABELFUSION_THRESHOLD, true );
        ImagePlus impOverlaySubsetTest = getOverlayImage( roiInterpolationMapSubset, sample ).duplicate();
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
		test();
		//Gui gui = new Gui();
		IJ.log("END RUN test");

    }
}
