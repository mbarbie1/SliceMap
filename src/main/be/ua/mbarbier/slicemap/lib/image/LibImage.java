/**
 *
 */
package main.be.ua.mbarbier.slicemap.lib.image;

import FourierD.EllipticFD;
import main.be.ua.mbarbier.external.GLCM_Texture;
import main.be.ua.mbarbier.slicemap.lib.Lib;
import fiji.threshold.Auto_Threshold;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Wand;
import ij.measure.Measurements;
import ij.process.ImageProcessor;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.NumericType;
import ij.process.ShortBlitter;
import ij.process.ImageStatistics;
import ij.plugin.CanvasResizer;
import ij.plugin.Thresholder;
import ij.plugin.filter.EDM;
import ij.ImageJ;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.Toolbar;
import ij.plugin.Binner;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatBlitter;
import io.scif.config.SCIFIOConfig;
import io.scif.config.SCIFIOConfig.ImgMode;
import io.scif.img.ImgOpener;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.be.ua.mbarbier.slicemap.Main;
import main.be.ua.mbarbier.slicemap.lib.LibIO;
import static main.be.ua.mbarbier.slicemap.lib.LibIO.writeCsv;
import main.be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import mpicbg.ij.clahe.Flat;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.io.ImgIOException;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import net.lingala.zip4j.exception.ZipException;

/**
 * @author mbarbie1
 *
 */
public class LibImage {

    public static boolean debug = false;

	public static ImageProcessor convertSimilar( ImageProcessor ip, ImageProcessor similar ) {

		final int bitDepth = ip.getBitDepth();
		final int bitDepthSimilar = similar.getBitDepth();
		
		if (bitDepth != bitDepthSimilar) {
			switch( bitDepthSimilar ) {
				case 8:
					ip = ip.convertToByte(true);
					break;
				case 16:
					ip = ip.convertToShort(true);
					break;
				case 24:
					ip = ip.convertToRGB();
					break;
				case 32:
					ip = ip.convertToFloat();
					break;
			}
		}

		return ip;
	}
	
	public static ImagePlus convertContrastByte( ImagePlus in ) {
		ImagePlus imp = in.duplicate();
		ContrastEnhancer ce = new ContrastEnhancer();
		ImageProcessor ip = imp.getProcessor();
		ce.stretchHistogram( imp, 0.1 );
		ip.multiply( 255. / ((double) imp.getStatistics().max) );
		imp.setProcessor(ip.convertToByte(false));
		return imp;
	}

	/**
	* This is adapted from an example "Use of Gaussian Convolution on the Image
	* but convolve with a different outofboundsstrategy" from Stephan Preibisch and Stephan Saalfeld
	 * @param imp
	 * @param s
	 * @throws net.imglib2.io.ImgIOException
	 * @throws net.imglib2.exception.IncompatibleTypeException
	*/
    public static void gaussianBlur2( ImagePlus imp, double s ) throws ImgIOException, IncompatibleTypeException {
        // open with ImgOpener as a FloatType
        Img< UnsignedShortType > image = ImageJFunctions.wrap(imp);
//        Img< FloatType > image = new ImgOpener().openImg( "DrosophilaWing.tif",
//            new FloatType() );
 
        // perform gaussian convolution with float precision
        double[] sigma = new double[ image.numDimensions() ];
 
        for ( int d = 0; d < image.numDimensions(); ++d )
            sigma[ d ] = s;
 
        // first extend the image to infinity, zeropad
        RandomAccessible< UnsignedShortType > infiniteImg = Views.extendValue( image, new UnsignedShortType() );

        // now we convolve the whole image manually in-place
        // note that is is basically the same as the call above, just called in a more generic way
        //
        // sigma .. the sigma
        // infiniteImg ... the RandomAccessible that is the source for the convolution
        // image ... defines the RandomAccessibleInterval that is the target of the convolution
        Gauss3.gauss( sigma, infiniteImg, image );
 
        // show the in-place convolved image (note the different outofboundsstrategy at the edges)
        ImageJFunctions.show( image );
//		Img image = ImageJFunctions.wrap(imp);
    }

	/*	
    public static < T extends RealType< T > & NativeType< T > > void gaussianBlurBin( File file, double s, int binning ) throws ImgIOException, IncompatibleTypeException, io.scif.img.ImgIOException {
	
		String path = file.getAbsolutePath();
		ImgOpener imgOpener = new ImgOpener();
		// create the SCIFIOConfig. This gives us configuration control over how
	    // the ImgOpener will open its datasets.
        SCIFIOConfig config = new SCIFIOConfig();
 
        // If we know what type of Img we want, we can encourage their use through
        // an SCIFIOConfig instance. CellImgs dynamically load image regions and are
        // useful when an image won't fit in memory
        config.imgOpenerSetImgModes( ImgMode.CELL );
 
        // open with ImgOpener as a CellImg
        List< SCIFIOImgPlus< T > > imageCell = (Img< T >) imgOpener.openImgs(path, config);
 
        // display it via ImgLib using ImageJ. The Img type only affects how the
        // underlying data is accessed, so these images should look identical.
        ImageJFunctions.show( imageCell );
*/	

	public static ImagePlus binSlice( ImagePlus sample, int binning, double sigma ) {

		// if the binning is large or the original image is very large (side > CONSTANT_MAX_PIXELS_FOR_PREPROCESSING)
		if ( sample.getWidth() > Main.CONSTANT_MAX_PIXELS_FOR_PREPROCESSING || sample.getHeight() > Main.CONSTANT_MAX_PIXELS_FOR_PREPROCESSING || binning > 8 ) {
			if ( binning > 8 ) {
				int preBinning = 4;
				int afterBinning = binning / preBinning;

				sample = LibImage.binImageAlternative( sample, preBinning );
				GaussianBlur gb = new GaussianBlur();
				gb.blurGaussian( sample.getProcessor(), sigma / preBinning );
				sample = LibImage.binImageAlternative( sample, afterBinning );
				//gb.blurGaussian( sample.getProcessor(), sigma / binning );
			} else {
				if ( binning >= 2 ) {
					int preBinning = 2;
					int afterBinning = binning / preBinning;

					sample = LibImage.binImageAlternative( sample, preBinning );
					GaussianBlur gb = new GaussianBlur();
					gb.blurGaussian( sample.getProcessor(), sigma / preBinning );
					sample = LibImage.binImageAlternative( sample, afterBinning );
				} else {
					GaussianBlur gb = new GaussianBlur();
					gb.blurGaussian( sample.getProcessor(), sigma / binning );
				}
			}
		}
		
		
		return sample;
	}
	
	public static ImagePlus binSample( ImagePlus sample, int binning, double scale, double pixelSize, int refWidthBinned, int refHeightBinned, double saturatedPixelPercentage ) {
    
		// Bin the image (downscale)
		sample = binSlice( sample, binning, scale / pixelSize );

		// Resize to square images
		Toolbar.setBackgroundColor(Color.BLACK);
        CanvasResizer cr = new CanvasResizer();
        int newW = refWidthBinned;
        int newH = refHeightBinned;
        int w = sample.getWidth();
        int h = sample.getHeight();
        int newX = (int) (Math.round((newW - w) / 2.0));
        int newY = (int) (Math.round((newH - h) / 2.0));
        ImageProcessor ip = sample.getProcessor().duplicate();
        ImagePlus imp = new ImagePlus( "imp small Resized", cr.expandImage( ip, newW, newH, newX, newY) );

		// Subtract background signal
		imp.setProcessor( subtractBackground( imp.getProcessor(), 5 ) );

		// Enhance the contrast
		ContrastEnhancer ce = new ContrastEnhancer();
		ce.setNormalize(true);
		ce.stretchHistogram( imp, saturatedPixelPercentage );

        return imp;
    }
	
	public static ImagePlus binStack(ImagePlus stack, int binning) {

        ij.plugin.Binner binner = new Binner();
        return binner.shrink(stack, binning, binning, 1, Binner.AVERAGE);
    }
	
    public static ImagePlus binImage( ImagePlus imp, int binning ) {

        double scale = 1.0 / binning;
        ImageProcessor ip = imp.getProcessor().resize((int) Math.floor( imp.getWidth() * scale ) );
        ImagePlus imp_scaled = new ImagePlus("binning_" + binning, ip);
        
        return imp_scaled;
    }
    
	public static ImagePlus binImage( ImagePlus imp, double binning ) {

        double scale = 1.0 / binning;
        ImageProcessor ip = imp.getProcessor().resize((int) Math.floor( imp.getWidth() * scale ) );
        ImagePlus imp_scaled = new ImagePlus("binning_" + binning, ip);
        
        return imp_scaled;
    }
	
    public static ImagePlus binImageAlternative( ImagePlus imp, int binning ) {

        ij.plugin.Binner binner = new Binner();
        ImagePlus imp_scaled = binner.shrink(imp, binning, binning, 1, Binner.AVERAGE);
        
        return imp_scaled;
    }

    public static ImagesResized resizeImages(ImagePlus imp1, ImagePlus imp2) {

        int w1 = imp1.getWidth();
        int w2 = imp2.getWidth();
        int h1 = imp1.getHeight();
        int h2 = imp2.getHeight();
        int newW = Math.max(w1, w2);
        int newH = Math.max(h1, h2);
        int newX1 = 0;
        int newX2 = 0;
        int newY1 = 0;
        int newY2 = 0;
        if (newW > w1) {
            newX1 = (int) (Math.round((newW - w1) / 2.0));
            newX2 = 0;
        } else {
            newX2 = (int) (Math.round((newW - w2) / 2.0));
            newX1 = 0;
        }
        if (newH > h1) {
            newY1 = (int) (Math.round((newH - h1) / 2.0));
            newY2 = 0;
        } else {
            newY2 = (int) (Math.round((newH - h2) / 2.0));
            newY1 = 0;
        }

        CanvasResizer cr = new CanvasResizer();
        ImagePlus impR1 = new ImagePlus(imp1.getTitle(), cr.expandImage(imp1.getProcessor(), newW, newH, newX1, newY1));
        ImagePlus impR2 = new ImagePlus(imp2.getTitle(), cr.expandImage(imp2.getProcessor(), newW, newH, newX2, newY2));

        return new ImagesResized(impR1, impR2, newX1, newY1, newX2, newY2);
    }

    public ImagesResized resizeImagesBorder(ImagePlus imp1, ImagePlus imp2, int borderWidth) {

        int w1 = imp1.getWidth();
        int w2 = imp2.getWidth();
        int h1 = imp1.getHeight();
        int h2 = imp2.getHeight();
        int newW = Math.max(w1, w2) + borderWidth;
        int newH = Math.max(h1, h2) + borderWidth;
        int newX1 = 0;
        int newX2 = 0;
        int newY1 = 0;
        int newY2 = 0;
        newX1 = (int) (Math.round((newW - w1) / 2.0));
        newX2 = (int) (Math.round((newW - w2) / 2.0));
        newY1 = (int) (Math.round((newH - h1) / 2.0));
        newY2 = (int) (Math.round((newH - h2) / 2.0));

        CanvasResizer cr = new CanvasResizer();
        ImagePlus impR1 = new ImagePlus(imp1.getTitle(), cr.expandImage(imp1.getProcessor(), newW, newH, newX1, newY1));
        ImagePlus impR2 = new ImagePlus(imp2.getTitle(), cr.expandImage(imp2.getProcessor(), newW, newH, newX2, newY2));

        return new ImagesResized(impR1, impR2, newX1, newY1, newX2, newY2);
    }

    public static ImagePlus scaleImp(ImagePlus imp, int binning) {
        int newWidth = imp.getWidth() / binning;
        ImageProcessor ip = imp.getProcessor();
        ip.setInterpolationMethod(ImageProcessor.BILINEAR);
        ImageProcessor scaledSource = ip.resize(newWidth);
        ImagePlus scaledImp = new ImagePlus("scaled", scaledSource);

        return scaledImp;
    }

    public static void bgMask(ImagePlus imp) {

        double bandThicknessRatio = 0.01;
        int bandThickness = Math.max( (int) 2.0, (int) Math.round( bandThicknessRatio * ( imp.getHeight() + imp.getHeight() ) / 2.0 ) );
        IJ.log(" Thickness of the band to find background value: " + bandThickness);
        imp.killRoi();
        ImageProcessor ip = imp.getProcessor();
        ImagePlus bgImp = LibImage.mask( ip.duplicate(), 0, 1 );
        ImageProcessor bgIp = bgImp.getProcessor();
        bgIp = bgIp.convertToByteProcessor(false);
        bgImp.setProcessor(bgIp);
        //bgIp.threshold(0);
        //bgImp.show();
        bgIp.erode();
        bgIp.erode();
        bgIp.dilate();
        bgIp.dilate();
//        bgImp.close();
        //bgImp.updateAndRepaintWindow();
        
        Wand w = new Wand( bgIp );
        double lower = 0.5;
        double upper = 256;
        w.autoOutline( (int) (bgIp.getWidth()/2.0), (int) (bgIp.getHeight()/2.0), lower, upper, Wand.EIGHT_CONNECTED);
        Roi roibg = new Roi(0,0,0,0);
        if (w.npoints > 0) {
            roibg = new PolygonRoi( w.xpoints, w.ypoints, w.npoints, PolygonRoi.TRACED_ROI );
            IJ.log("ROI points = " + roibg.getPolygon().npoints);
        } else {
            IJ.log("Warning: bgMask:: Did not find roi at this threshold, skipped.");
        }

        ImageProcessor bIp = bgIp.duplicate();
        ImagePlus bImp = new ImagePlus( "", bIp );
        //bgImp.getProcessor().invert();
        
        EDM edm = new EDM();
        ImageProcessor edmIp = bIp.duplicate();
        edm.toEDM(edmIp);
        //new ImagePlus("bgMask function 184", edmIp).show();
        ImagePlus edmImp = new ImagePlus( "edm", edmIp );
        ImagePlus mask = thresholdMinMax( edmImp, 0.1, bandThickness );
        ImageProcessor maskIp = mask.getProcessor().convertToByteProcessor(false);
        mask.setProcessor( maskIp );
        mask.killRoi();
        //mask.show();
        
        w = new Wand( maskIp );
        lower = 0.5;
        upper = 5;
        w.autoOutline( (int) (mask.getWidth()/2.0), (int) (mask.getHeight()/2.0), lower, upper, Wand.EIGHT_CONNECTED);
        Roi roib = new Roi(0,0,0,0);
        if (w.npoints > 0) {
            roib = new PolygonRoi( w.xpoints, w.ypoints, w.npoints, PolygonRoi.TRACED_ROI );
            IJ.log("ROI points = " + roib.getPolygon().npoints);
        } else {
            IJ.log("Warning: bgMask:: Did not find roi at this threshold, skipped.");
        }
        
        ImagePlus testImp = new ImagePlus("test", ip.duplicate() );
        Overlay roiOverlay = new Overlay();
        roiOverlay.setStrokeColor(Color.yellow);
        roiOverlay.add(roib,"Band");
        roiOverlay.setStrokeColor(Color.red);
        roiOverlay.add(roibg,"BG");
        testImp.setOverlay(roiOverlay);
        //testImp.show();
        
        ShapeRoi roiShapeb = new ShapeRoi(roib);
        ShapeRoi roiShapebg = new ShapeRoi(roibg);
        roiShapeb = roiShapebg.not(roiShapeb);

        ImagePlus testImp2 = new ImagePlus("test2", ip.duplicate() );
        roiOverlay = new Overlay();
        roiOverlay.add(roiShapeb,"Band");
        testImp2.setOverlay(roiOverlay);
        //testImp2.show();
        

        imp.setRoi(roibg);
        ImageStatistics statsbg = imp.getStatistics(Measurements.MEAN | Measurements.MEDIAN | Measurements.AREA );
        imp.setRoi(roib);
        ImageStatistics statsb = imp.getStatistics(Measurements.MEAN | Measurements.MEDIAN | Measurements.AREA );
        imp.setRoi(roiShapeb);
        ImageStatistics statsband = imp.getStatistics(Measurements.MEAN | Measurements.MEDIAN | Measurements.AREA );
        IJ.log(" Value = " + Math.round( statsb.mean ) );
        IJ.log(" Value = " + Math.round( statsbg.mean ) );
        IJ.log(" Value = " + Math.round( statsband.mean ) );
        ip.setValue( Math.round( statsband.mean ) );
        ip.fillOutside( roibg );
        imp.killRoi();
        //new ImagePlus("ip",ip).show();
        //imp.show();
    }

    public static ImagePlus montageMask(ImagePlus imp, int binning, double sigma, String thresholdMethod) {

        // mask
        ImagePlus impt = smoothMask(imp, binning, sigma, thresholdMethod);
        impt.getProcessor().invert();

        // Retain largest object
        Wand w = new Wand(impt.getProcessor());
        double lower = 0.1;
        double upper = impt.getProcessor().maxValue();
        int mode = Wand.FOUR_CONNECTED;
        w.autoOutline(impt.getWidth() / 2, impt.getHeight() / 2, lower, upper, mode);
        if (w.npoints > 0) {
            imp.setRoi(new PolygonRoi(w.xpoints, w.ypoints, w.npoints, PolygonRoi.TRACED_ROI));
            // TODO headless test
            //IJ.run(imp, "Interpolate", "interval=1 smooth");
        } else {
            imp.setRoi(new Roi(new Rectangle(0, 0, imp.getWidth(), imp.getHeight())));
            IJ.log("Warning: montageMask:: Did not find mask, ROI set equal to whole image.");
        }
        //IJ.run(imp,"Fit Spline","");

        //ResultsTable rt = new ResultsTable();
        //int msrOpt = Measurements.AREA | Measurements.AREA_FRACTION | Measurements.FERET| Measurements.CIRCULARITY | Measurements.SHAPE_DESCRIPTORS | Measurements.SHAPE_DESCRIPTORS;
        //double minSize = 1.0;
        //double maxSize = impt.getWidth() * impt.getHeight();
        //double minCirc = 0.0;
        //double maxCirc = 1.0;
        //ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.INCLUDE_HOLES, msrOpt, rt, minSize, maxSize, minCirc, maxCirc);
        // Set Roi to mask area
        //imp.show();
        return imp;
    }

    /**
     * TODO Calculates an image mask based on the intensity variance in the
     * image, retains only the middle large image object.
     *
     * @param imp
     * @param binning
     * @param sigma
     * @param thresholdMethod
     * @return
     */
    public static ImagePlus varianceMask(ImagePlus imp, int binning, double sigma, String thresholdMethod) {

        ImageProcessor ip = imp.getProcessor().duplicate();

        IJ.run(imp, "Enhance Contrast", "saturated=0.35");
        imp.setProcessor(imp.getProcessor().convertToByteProcessor());
        IJ.run(imp, "Variance...", "radius=1");
        Auto_Threshold at = new Auto_Threshold();
        Object[] a = at.exec(imp.duplicate(), thresholdMethod, true, true, true, false, false, false);
        ImagePlus impa = (ImagePlus) a[1];
        impa.setTitle(thresholdMethod);

        impa.getProcessor().invert();
        //impa.show();

        //IJ.run( impt, "Enhance Contrast", "saturated=0.35");
        //IJ.run(impt,"Invert","");
        //impt.getProcessor().invert();
        //IJ.run(impt, "Variance...", "radius=1");
        // Retain largest object
        Wand w = new Wand(impa.getProcessor());
        w.autoOutline(impa.getWidth() / 2, impa.getHeight() / 2);
        imp.setRoi(new PolygonRoi(w.xpoints, w.ypoints, w.npoints, PolygonRoi.TRACED_ROI));

        return imp;
    }

    /**
     * Extracts ImageStatistics (ImageJ) from an image within a given Roi
     *
     * @param imp
     * @param roi
     * @return stats ImageStatistics object containing image features
     */
    public static ImageStatistics getRoiStatistics(ImagePlus imp, Roi roi) {

        imp.setRoi(roi);
        int statOptions = ImageStatistics.AREA | ImageStatistics.AREA_FRACTION | ImageStatistics.CENTER_OF_MASS | ImageStatistics.CENTROID | ImageStatistics.CIRCULARITY | ImageStatistics.ELLIPSE | ImageStatistics.FERET | ImageStatistics.INTEGRATED_DENSITY | ImageStatistics.KURTOSIS | ImageStatistics.MEAN | ImageStatistics.MEDIAN | ImageStatistics.MIN_MAX | ImageStatistics.PERIMETER | ImageStatistics.SHAPE_DESCRIPTORS | ImageStatistics.SKEWNESS | ImageStatistics.STD_DEV;
        ImageStatistics stats = imp.getStatistics(statOptions);

        return stats;
    }

    /**
     * TODO Gets the ROI from the main object in the image
     *
     * @param imp
     * @return roi ImageJ Roi
     */
    public static Roi mainObjectExtraction(ImagePlus imp) {

        imp.deleteRoi();
        ImagePlus mask = imp.duplicate();
        double lengthScale = ((double) ( imp.getWidth() + imp.getHeight() )) / 2.0;
        mask = montageMask(mask, 1, lengthScale / 100.0, "Percentile");
        Roi roi = mask.getRoi();

        return roi;
    }

    /**
     * TODO Extracts features from an image within a given Roi
     *
     * @param imp
     * @param roi
     * @return features LinkedHashMap<String, String>
     */
    public static LinkedHashMap<String, String> featureExtraction(ImagePlus imp, Roi roi) {

        imp.setRoi(roi);
        ImageStatistics stats = getRoiStatistics(imp, roi);
        LinkedHashMap<String, String> statsMap = statisticsMap(stats);
        LinkedHashMap<String, String> efdsMap = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> featuresMap = new LinkedHashMap<String, String>();

        double[] feret = roi.getFeretValues();
        LinkedHashMap<String, String> feretMap = new LinkedHashMap<String, String>();
        feretMap.put("shape_feretMax", Double.toString(feret[0]));
        feretMap.put("shape_feretAngle", Double.toString(feret[1]));
        feretMap.put("shape_feretMin", Double.toString(feret[2]));
        feretMap.put("shape_feretX", Double.toString(feret[3]));
        feretMap.put("shape_feretY", Double.toString(feret[4]));

        double perimeter = roi.getLength();
        Polygon convexPolygon = roi.getConvexHull();
        Roi convexRoi = new PolygonRoi(convexPolygon, PolygonRoi.POLYGON);
        // creating an image just for the calibration
        imp.deleteRoi();
        ImagePlus convexImp = imp.duplicate();

        convexImp.setRoi(convexRoi);
        convexImp.setOverlay(convexRoi, new Color(255), 2, null);
        //convexImp.show();
        ImageStatistics convexStats = convexImp.getStatistics();
        double convexPerimeter = convexRoi.getLength();
        double convexArea = convexStats.area;
        double solidity = stats.area / convexArea;
        double circularity = 4 * Math.PI * stats.area / Math.pow(perimeter, 2.0);
        double roundness = 4 * stats.area / (Math.PI * Math.pow(feret[0], 2.0));
        double compactness = Math.sqrt(4 * stats.area / Math.PI) / stats.major;
        double aspectRatioFeret = feret[0] / feret[2];
        double aspectRatio = stats.major / stats.minor;
        statsMap.put("shape_perimeter", Double.toString(perimeter));
        statsMap.put("shape_perimeterConvex", Double.toString(convexPerimeter));
        statsMap.put("shape_areaConvex", Double.toString(convexArea));
        statsMap.put("shape_solidity", Double.toString(solidity));
        statsMap.put("shape_circularity", Double.toString(circularity));
        statsMap.put("shape_roundness", Double.toString(roundness));
        statsMap.put("shape_compactness", Double.toString(compactness));
        statsMap.put("shape_aspectRatio", Double.toString(aspectRatio));
        statsMap.put("shape_aspectRatioFeret", Double.toString(aspectRatioFeret));

        // --- Elliptic Fourier Descriptors
        // TODO These are the ?moments? to 5th order? m_(i,j) with i + j < 6 --> [m00], [m10, m01], [m20, m11, m02],[m30, m21, m12, m03], ... 
        int n = 21;
        // TODO smooth the roi separately instead of during montageMask?
        // get the EFDs
        int[] xi = roi.getPolygon().xpoints;
        int[] yi = roi.getPolygon().ypoints;
        double[] xd = new double[roi.getPolygon().npoints];
        double[] yd = new double[roi.getPolygon().npoints];
        for (int i = 0; i < roi.getPolygon().npoints; i++) {
            xd[i] = (double) xi[i];
            yd[i] = (double) yi[i];
        }

        EllipticFD efd = new EllipticFD(xd, yd, n);
        double[] efds = efd.efd;
        // The first and second coeffs should be ignored: 1e coeff relates to centroid, 2e coeff always equals 2 in the case of elliptic features
        for (int i = 2; i < efds.length; i++) {
            efdsMap.put("EFD_" + Integer.toString(i + 1), Double.toString(efds[i]));
        }

        // GLCM texture features TODO we need more angles since texture isn't rotation invariant?
        GLCM_Texture glcm0 = new GLCM_Texture();
        ImageProcessor ip8 = imp.getProcessor().convertToByte(true);
        glcm0.compute(ip8, 1, GLCM_Texture.stepDirection0);
        GLCM_Texture glcm90 = new GLCM_Texture();
        glcm90.compute(ip8, 1, GLCM_Texture.stepDirection90);
        LinkedHashMap<String, String> haraMap = new LinkedHashMap<String, String>();
        haraMap.put("texture_0_entropy", Double.toString(glcm0.entropy));
        haraMap.put("texture_0_contrast", Double.toString(glcm0.contrast));
        haraMap.put("texture_0_correlation", Double.toString(glcm0.correlation));
        haraMap.put("texture_0_IDM", Double.toString(glcm0.IDM));
        haraMap.put("texture_0_ASM", Double.toString(glcm0.asm));
        haraMap.put("texture_90_entropy", Double.toString(glcm90.entropy));
        haraMap.put("texture_90_contrast", Double.toString(glcm90.contrast));
        haraMap.put("texture_90_correlation", Double.toString(glcm90.correlation));
        haraMap.put("texture_90_IDM", Double.toString(glcm90.IDM));
        haraMap.put("texture_90_ASM", Double.toString(glcm90.asm));

        featuresMap.putAll(statsMap);
        featuresMap.putAll(feretMap);
        featuresMap.putAll(efdsMap);
        featuresMap.putAll(haraMap);

        return featuresMap;
    }

    public static double computeFeatureDistance( LinkedHashMap<String, Double> refFeatures, LinkedHashMap<String, Double> features, LinkedHashMap<String, Double> featureWeights ) {
        
        double dist = 0;

        // TODO use external library to implement multiple distances, e.g. : WEKA, javaML, ...
        // Just For Now: Euclidian Distance
        for ( String k : refFeatures.keySet() ) {
            dist = dist + featureWeights.get(k) * Math.pow( refFeatures.get(k) - features.get(k), 2 );
        }
        dist = Math.sqrt( dist ) / ( (double) refFeatures.size() );
        
        return dist;
    }

    public static LinkedHashMap<String, Double> computeFeatureWeights( ArrayList< LinkedHashMap<String, Double> > featureList, ArrayList< LinkedHashMap<String, Double> > errorList ) {

        LinkedHashMap<String, Double> featureWeights = new LinkedHashMap<String, Double>( featureList.get(0) );
        for ( String k : featureWeights.keySet() ) {
            featureWeights.put(k, 1.0);
        }
        
        //errorList
        
        // TODO PCA to find the weights

        
        return featureWeights;
    }

    
    /**
     * TODO Converts ImageStatistics (ImageJ) to a (key,value) map of features
     *
     * @param stats
     * @return
     */
    public static LinkedHashMap<String, String> statisticsMap(ImageStatistics stats) {

        LinkedHashMap<String, String> statsMap = new LinkedHashMap<String, String>();
        statsMap.put("intensity_mean", Double.toString(stats.mean));
        statsMap.put("intensity_stddev", Double.toString(stats.stdDev));
        statsMap.put("intensity_min", Double.toString(stats.histMin));
        statsMap.put("intensity_max", Double.toString(stats.histMax));
        statsMap.put("intensity_median", Double.toString(stats.median));
        statsMap.put("intensity_kurtosis", Double.toString(stats.kurtosis));
        statsMap.put("intensity_skewness", Double.toString(stats.skewness));
        statsMap.put("intensity_mode", Double.toString(stats.mode));

        statsMap.put("shape_area", Double.toString(stats.area));
        statsMap.put("shape_major", Double.toString(stats.major));
        statsMap.put("shape_minor", Double.toString(stats.minor));
        statsMap.put("shape_angle", Double.toString(stats.angle));
        statsMap.put("shape_xCentroid", Double.toString(stats.xCentroid));
        statsMap.put("shape_yCentroid", Double.toString(stats.yCentroid));

        statsMap.put("shapeGray_xCenterOfMass", Double.toString(stats.xCenterOfMass));
        statsMap.put("shapeGray_yCenterOfMass", Double.toString(stats.yCenterOfMass));

        return statsMap;
    }

    /**
     * Calculates a mask from an image (multiple objects possible) by blurring,
     * thresholding and then dilating the image
     *
     * @param imp
     * @param binning
     * @param sigma
     * @param thresholdMethod
     * @return
     */
    public static ImagePlus smoothMask(ImagePlus imp, int binning, double sigma, String thresholdMethod) {

        // Smooth the image then threshold it using MinError threshold
        ImageProcessor ip = imp.getProcessor().duplicate();

        // Smoothing
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(ip, sigma);
        //new ImagePlus("test smooth", ip).show();

        // Thresholding
        Auto_Threshold at = new Auto_Threshold();
        Object[] a = at.exec(imp.duplicate(), thresholdMethod, true, true, true, false, false, false);
        ImagePlus impt = (ImagePlus) a[1];
        impt.setTitle(thresholdMethod);
        //impt.show();
        ImageProcessor ipt = impt.getProcessor();

        // Dilation
        RankFilters rf = new RankFilters();
        ImageProcessor ipt2 = ipt.duplicate();
        double radius = 1;
        rf.rank(ipt2, radius, RankFilters.MAX);

        ImagePlus impDM = LibImage.mask(ipt2, 0.1, 1);
        impDM.getProcessor().invert();
        //impDM.show();

        return impDM;
    }

    public static double maskArea(ImageProcessor ip) {
        if (debug) {
            new ImagePlus("maskArea function 518 before reset ROI").show();
        }
        ip = ip.duplicate();
        ip.resetRoi();
        if (debug) {
            new ImagePlus("maskArea function 518 after reset ROI");
        }
        ImageStatistics stats = ip.getStatistics();

        return stats.area * stats.mean / ip.getMax();
    }

    public static double maskAreaRoi(ImageProcessor ip) {
        ImageStatistics stats = ip.getStatistics();

        return stats.area;
    }

    public static double area(ImageProcessor ip) {
        ip = ip.duplicate();
        ip.resetRoi();
        ImageStatistics stats = ip.getStatistics();

        return stats.area;
    }

    /**
     * Threshold an image with min and max intensity {@link Iterable}.
     *
     * @param <T>
     * @param imp - An ImagePlus {@link ImagePlus}
     * @param mind - minimal value
     * @param maxd - maximal value
     * @return
     */
    public static < T extends Comparable< T> & NativeType< T> & NumericType< T>> ImagePlus thresholdMinMax(ImagePlus imp, double mind, double maxd) {

        Img img = ImageJFunctions.wrap(imp);
        Cursor<T> iterator = img.cursor();
        T t = iterator.next().createVariable();
        t.setOne();
        T min = t.copy();
        T max = t.copy();
        min.mul(mind);
        max.mul(maxd);
        while (iterator.hasNext()) {
            T type = iterator.next();
            if (type.compareTo(min) < 0 | type.compareTo(max) > 0) {
                type.setOne();
            } else {
                type.setZero();
            }
        }
        return ImageJFunctions.wrap(img, "Mask");
    }
    
    /**
     * Threshold an image with min and max intensity {@link Iterable}.
     *
     * @param <T>
     * @param imp - An ImagePlus {@link ImagePlus}
     * @param mind - minimal value
     * @param maxd - maximal value
     * @return
     */
    public static < T extends Comparable< T> & NativeType< T> & NumericType< T>> long[] findNonZeroPixel( ImagePlus imp, int nDimensions ) {

        long[] position = new long[nDimensions];
        Img img = ImageJFunctions.wrap(imp);
        Cursor<T> iterator = img.cursor();
        T zero = iterator.next().createVariable();
        zero.setZero();
        while (iterator.hasNext()) {
            T type = iterator.next();
            if ( type.compareTo(zero) > 0 ) {
                iterator.localize(position);
                return position;
            }
        }
        return position;
    }
    
    /**
     * Threshold an image with min and max intensity {@link Iterable}.
     *
     * @param <T>
     * @param imp - An ImagePlus {@link ImagePlus}
     * @param mind - minimal value
     * @param maxd - maximal value
     * @return
     */
    public static < T extends Comparable< T> & NativeType< T> & RealType< T>> long[] findMaxPixel( ImagePlus imp, int nDimensions ) {

        //double maxValue = imp.getProcessor().getMax();
        long[] position = new long[nDimensions];
        Img img = ImageJFunctions.wrap(imp);
        Cursor<T> iterator = img.cursor();
        T max = iterator.get().createVariable();
        max.setReal(1.0);
        //max.mul( maxValue );
        while (iterator.hasNext()) {
            iterator.fwd();
            T type = iterator.get();
            if ( type.compareTo( max ) == 0 ) {
                IJ.log( "Max pixel = " + type.toString() );
                iterator.localize(position);
                return position;
            }
        }
        return position;
    }

    // -------------------------------------
    // TODO 
    // This function doesn't work headless, so we removed its functionality: adapt it
    // -------------------------------------

    public static ImagePlus mask(ImageProcessor ip, double valueA, double valueB) {
        ip = ip.duplicate();
        ip.setThreshold(valueA, valueB, ImageProcessor.NO_LUT_UPDATE);
        //imp.getProcessor().setBinaryThreshold();
        //IJ.run(imp, "Convert to Mask", "");

        ImagePlus imp = new ImagePlus("Mask", ip);
        imp = thresholdMinMax(imp, valueA, valueB);
        //imp.show();

        return imp;
    }

    public static double sumOfSquares(ImageProcessor ip) {
        ip = ip.duplicate();
        ip.resetRoi();
        switch ( ip.getBitDepth() ) {
            case 8:
                ip = ip.convertToShort(false);
                break;
            case 16:
                ip = ip.convertToFloat();
                break;
            case 32:
                break;
        }
        ip.sqr();
        ImageStatistics stats = ip.getStatistics();

        return stats.area * stats.mean;
    }

    public static double sumOfProduct(ImageProcessor ip1, ImageProcessor ip2) {
        
        ImageStatistics stats = null;
        ImageProcessor ip1Temp = ip1.duplicate();
        ImageProcessor ip2Temp = ip2.duplicate();
        ip1Temp.resetRoi();
        ip2Temp.resetRoi();
        switch ( ip1.getBitDepth() ) {
            case 8:
                ip1Temp = ip1Temp.convertToShort(false);
                ip2Temp = ip2Temp.convertToShort(false);
                ip1Temp.copyBits(ip2Temp, 0, 0, ShortBlitter.MULTIPLY);
                stats = ip1Temp.getStatistics();
                break;
            case 16:
                ip1Temp.copyBits(ip2Temp, 0, 0, ShortBlitter.MULTIPLY);
                stats = ip1Temp.getStatistics();
                break;
            case 32:
                ip1Temp.copyBits(ip2Temp, 0, 0, FloatBlitter.MULTIPLY);
                stats = ip1Temp.getStatistics();
                break;
        }

        return stats.mean * stats.area;
    }

    public static ImageProcessor maskIntersection(ImageProcessor ip1, ImageProcessor ip2) {
        // TODO Should we convert to mask to be sure?
        ImageProcessor ip1Temp = ip1.duplicate().convertToShort(false);
        ImageProcessor ip2Temp = ip2.duplicate().convertToShort(false);
        // TODO correct/necessary for the copyBits ShortBlitter.AND?
        ip1Temp.max(1.0);
        ip2Temp.max(1.0);
        // TODO Do we need to reset the ROIs for copybits?
        ip1Temp.copyBits(ip2Temp, 0, 0, ShortBlitter.AND);

        return ip1Temp;
    }

    public static Roi maskIntersectionRoi(Roi roi1, Roi roi2) {

        ShapeRoi shapeRoi1 = new ShapeRoi(roi1);
        ShapeRoi shapeRoi2 = new ShapeRoi(roi2);
		ShapeRoi shapeRoiI = shapeRoi1.and(shapeRoi2);
		Roi[] rois = shapeRoiI.getRois();
		Roi roiI = shapeRoiI.shapeToRoi();
		if (rois.length > 1) {
			Roi maxAreaRoi = rois[0];
			double maxArea = 0;
			for ( Roi roi : rois ) {
				ImageStatistics stats = roi.getStatistics();
				if ( stats.area > maxArea ) {
					maxAreaRoi = roi;
				}
			}
			roiI = maxAreaRoi;
		}
        return roiI;
    }

    public static ImageProcessor maskUnion(ImageProcessor ip1, ImageProcessor ip2) {
        // TODO Should we convert to mask to be sure?
        ImageProcessor ip1Temp = ip1.duplicate().convertToShort(false);
        ImageProcessor ip2Temp = ip2.duplicate().convertToShort(false);
        // TODO Do we need to reset the ROIs for copybits?
        ip1Temp.copyBits(ip2Temp, 0, 0, ShortBlitter.OR);

        return ip1Temp;
    }

    public static ImageProcessor minus(ImageProcessor ip1, ImageProcessor ip2) {
        // TODO Should we convert to mask to be sure?
        ImageProcessor ip1Temp = ip1.duplicate().convertToShort(false);
        ImageProcessor ip2Temp = ip2.duplicate().convertToShort(false);
        // TODO Do we need to reset the ROIs for copybits?
        ip1Temp.copyBits(ip2Temp, 0, 0, ShortBlitter.SUBTRACT);

        return ip1Temp;
    }

    /**
     * Rescale intensities bin-wise of an image.
     *
     * @param <T>
     * @param imp - An ImagePlus {@link ImagePlus}
     * @param bins - original bins
     * @param bins_new - new bins
     * @return
     */
    public static < T extends Comparable< T> & NativeType< T> & NumericType< T>> ImagePlus RescaleBins(ImagePlus imp, double[] bins_ref, double[] bins) {

        Img img = ImageJFunctions.wrap(imp);
        Cursor<T> iterator = img.cursor();
        T t = iterator.get().createVariable();
        t.setOne();
        int nBins = bins.length;
        // Compute the intensity rescaling coeffs for each bin
        // Assume the following notation:
        //  I^R(i) written as Iri equals the intensity for the pixel fraction i in the reference histogram (the pixel fractions are defined in bins, the intensities are given in refIdx, because the indices correspond to the intensity values) 
        //  I^S(i) written as Isi equals the intensity for the pixel fraction i in the other histogram (the pixel fractions are defined in bins, the intensities are given in ipIdx, because the indices correspond to the intensity values) 
        //  J^S(i) written as Jsi equals the intensity values of the other image in the interval with pixel fractions [i-1, i] 
        //  The rescaled J^S(i) values are written as Jsi_new equal:
        //
        //      Js(i)_new = ( Js(i)- Is(i-1) ) * ( Ir(i) - Ir(i-1) ) / ( Is(i) - Is(i-1) ) + Ir(i-1)
        //
        //  If one denotes m(i) = ( Ir(i) - Ir(i-1) ) / ( Is(i) - Is(i-1) ), b(i) = Is(i-1), and c(i) = Ir(i-1)
        //
        //      --> Js(i)_new = m(i) * ( Js(i) - b(i) ) + c(i)
        //
        //  Since first pixels are equal to intensity 1: Ir(0) = 1, Is(0) = 1 
        // 
        double[] m = new double[nBins];
        double[] b = Arrays.copyOfRange(bins, 0, bins.length - 1);//new double[nBins];
        double[] c = Arrays.copyOfRange(bins_ref, 0, bins_ref.length - 1);//new double[nBins];
        for (int i = 1; i < nBins; i++) {
            m[i - 1] = (bins_ref[i] - bins_ref[i - 1]) / (bins[i] - bins[i - 1]);
        }
        while (iterator.hasNext()) {
            int found = 0;
            T type = iterator.next();
            type = iterator.get();
            for (int i = 1; i < nBins; i++) {
                T min = t.copy();
                T max = t.copy();
                min.mul(bins[i - 1]);
                max.mul(bins[i]);
                T bb = t.copy();
                T cc = t.copy();
                bb.mul(b[i - 1]);
                cc.mul(c[i - 1]);
                if ((type.compareTo(min) > 0) && (type.compareTo(max) <= 0) && (found == 0)) { //
                    //type.set( cc );
                    found = 1;
                    type.sub(bb);
                    type.mul(m[i - 1]);
                    type.add(cc);
                }
            }
        }
        return ImageJFunctions.wrap(img, "Mask");
    }

    /**
     * Standardize a histogram to the histogram of a reference image
     *
     * @param nBins
     * @param ref
     * @param ip
     * @return
     */
    public static <T extends NativeType< T> & NumericType< T> & Comparable<T>> ImageProcessor standardizeHistogram(ImageProcessor ref, ImageProcessor ip, int nBins) {

        // do we want to reset the fg/bg ROI's or not (reset them only for the calculation of the pixelfractions? but less general since we need a good initial ROI)
        ip.resetRoi();
        ref.resetRoi();

        // Obtain histograms
        ImageStatistics ipStats, refStats;
        refStats = ref.getStatistics();
        ipStats = ip.getStatistics();
        int[] ipH, refH;
        switch (ref.getBitDepth()) {
            case 16:
                ipH = ipStats.histogram16;
                refH = refStats.histogram16;
                break;
            case 8:
                ipH = ipStats.histogram;
                refH = refStats.histogram;
                break;
            default:
                ipH = ipStats.histogram;
                refH = refStats.histogram;
                break;
        }
        double refArea = refStats.area;
        double refZeroArea = refH[0];
        double refNonZeroArea = refArea - refZeroArea;
        double ipArea = ipStats.area;
        double ipZeroArea = ipH[0];
        double ipNonZeroArea = ipArea - ipZeroArea;

        // Remove zero intensity from histogram
        refH = Arrays.copyOfRange(refH, 1, refH.length);
        ipH = Arrays.copyOfRange(ipH, 1, ipH.length);

        // Normalize to number of nonzero pixels
        double[] refHd = new double[refH.length];
        for (int i = 0; i < refH.length; i++) {
            refHd[i] = (double) refH[i] / refNonZeroArea; // TODO *i ???
        }
        double[] ipHd = new double[ipH.length];
        for (int i = 0; i < ipH.length; i++) {
            ipHd[i] = (double) ipH[i] / ipNonZeroArea;
        }

        // Cumulative distributions
        double[] ipHcum = new double[ipH.length];
        ipHcum[0] = ipHd[0];
        for (int i = 1; i < ipH.length; i++) {
            ipHcum[i] = ipHcum[i - 1] + ipHd[i];
        }
        double[] refHcum = new double[refH.length];
        refHcum[0] = refHd[0];
        if (debug) {
            IJ.log("Area ref = " + refArea);
            IJ.log("Area ref non zero pixels = " + refNonZeroArea);
            IJ.log("Sum refHcum = " + Lib.sumDouble(refHd));
        }
        for (int i = 1; i < refH.length; i++) {
            refHcum[i] = refHcum[i - 1] + refHd[i];
        }

        // Compute pixelfractions
        double[] bins = Lib.linearSpacedSequence(0.0, 1.0, nBins);
        // Search indices of the bins in the ref
        int[] refIdx = findIndicesBins(refHcum, bins);
        double[] refIdxd = Lib.intArrayToDoubleArray(refIdx);
        // Convert the indices = 
        int[] ipIdx = findIndicesBins(ipHcum, bins);
        double[] ipIdxd = Lib.intArrayToDoubleArray(ipIdx);

        if (debug) {
            double[] x = Lib.intArrayToDoubleArray(Lib.intSequence(1, refHcum.length));
            IJ.log("N bins: " + x.length);
            Plot ph_ref = new Plot("reference cumulative histogram", "Intensity", "pixel number", x, refHd);
            Plot ph_ip = new Plot("sample cumulative histogram", "Intensity", "pixel number", x, ipHd);
            Plot pc_ref = new Plot("reference cumulative histogram", "Intensity", "Cumulative histogram", x, refHcum);
            Plot pc_ip = new Plot("sample cumulative histogram", "Intensity", "Cumulative histogram", x, ipHcum);
            Plot pi_ref = new Plot("reference indices bins", "index", "Cumulative histogram", bins, refIdxd);
            Plot pi_ip = new Plot("sample indices bins", "index", "Cumulative histogram", bins, ipIdxd);
            ph_ip.show();
            ph_ref.show();
            pc_ip.show();
            pc_ref.show();
            pi_ip.show();
            pi_ref.show();
        }

        // Compute the intensity rescaling coeffs for each bin
        // Assume the following notation:
        //  I^R(i) written as Iri equals the intensity for the pixel fraction i in the reference histogram (the pixel fractions are defined in bins, the intensities are given in refIdx, because the indices correspond to the intensity values) 
        //  I^S(i) written as Isi equals the intensity for the pixel fraction i in the other histogram (the pixel fractions are defined in bins, the intensities are given in ipIdx, because the indices correspond to the intensity values) 
        //  J^S(i) written as Jsi equals the intensity values of the other image in the interval with pixel fractions [i-1, i] 
        //  The rescaled J^S(i) values are written as Jsi_new equal:
        //
        //      Js(i)_new = ( Js(i)- Is(i-1) ) * ( Ir(i) - Ir(i-1) ) / ( Is(i) - Is(i-1) ) + Ir(i-1)
        //
        //  If one denotes m(i) = ( Ir(i) - Ir(i-1) ) / ( Is(i) - Is(i-1) ), b(i) = Is(i-1), and c(i) = Ir(i-1)
        //
        //      --> Js(i)_new = m(i) * ( Js(i) - b(i) ) + c(i)
        //
        //  Since first pixels are equal to intensity 1: Ir(0) = 1, Is(0) = 1 
        // 
        ImageProcessor ip_new = ip.duplicate();
        ImagePlus imp_new = new ImagePlus("rescaled image", ip_new);
        imp_new = RescaleBins(imp_new, refIdxd, ipIdxd);

        return ip_new;
    }


	/**
     * Extract the background intensity of an image using a percentile (minus exactly zero pixels)
     *
	 * @param perc
     * @param ip
     * @return the lowest percentage intensity 
     */
    public static double extractBackgroundIntensity(ImageProcessor ip, double perc) {

        ip.resetRoi();
        // Obtain histograms
        ImageStatistics ipStats;
        ipStats = ip.getStatistics();
        int[] ipH;
        switch (ip.getBitDepth()) {
            case 16:
                ipH = ipStats.histogram16;
                break;
            case 8:
                ipH = ipStats.histogram;
                break;
            default:
                ipH = ipStats.histogram;
                break;
        }
        double ipArea = ipStats.area;
        double ipZeroArea = ipH[0];
        double ipNonZeroArea = ipArea - ipZeroArea;

        // Remove zero intensity from histogram
        ipH = Arrays.copyOfRange(ipH, 1, ipH.length);

        // Normalize to number of nonzero pixels
        double[] ipHd = new double[ipH.length];
        for (int i = 0; i < ipH.length; i++) {
            ipHd[i] = (double) ipH[i] / ipNonZeroArea;
        }

        // Cumulative distributions
        double[] ipHcum = new double[ipH.length];
        ipHcum[0] = ipHd[0];
        for (int i = 1; i < ipH.length; i++) {
            ipHcum[i] = ipHcum[i - 1] + ipHd[i];
        }
		
        // Compute pixelfractions
        //double[] bins = Lib.linearSpacedSequence(0.0, 1.0, nBins);
		double[] bins = new double[]{perc/100.0};
        // Search indices of the bins in the ref
        int[] ipIdx = findIndicesBins(ipHcum, bins);
        double[] ipIdxd = Lib.intArrayToDoubleArray(ipIdx);

        if (debug) {
            double[] x = Lib.intArrayToDoubleArray(Lib.intSequence(1, ipHcum.length));
            IJ.log("N bins: " + x.length);
            Plot ph_ip = new Plot("sample cumulative histogram", "Intensity", "pixel number", x, ipHd);
            Plot pc_ip = new Plot("sample cumulative histogram", "Intensity", "Cumulative histogram", x, ipHcum);
            Plot pi_ip = new Plot("sample indices bins", "index", "Cumulative histogram", bins, ipIdxd);
            ph_ip.show();
            pc_ip.show();
            pi_ip.show();
        }

        return ipIdxd[0];
    }


	
	
	/**
     * Subtract the background intensity of an image using a percentile (minus exactly zero pixels)
     *
     * @param nBins
     * @param ref
     * @param ip
     * @return
     */
    public static ImageProcessor subtractBackground(ImageProcessor ip, double perc) {

        ip.resetRoi();
        // Obtain histograms
        ImageStatistics ipStats;
        ipStats = ip.getStatistics();
        int[] ipH;
        switch (ip.getBitDepth()) {
            case 16:
                ipH = ipStats.histogram16;
                break;
            case 8:
                ipH = ipStats.histogram;
                break;
            default:
                ipH = ipStats.histogram;
                break;
        }
        double ipArea = ipStats.area;
        double ipZeroArea = ipH[0];
        double ipNonZeroArea = ipArea - ipZeroArea;

        // Remove zero intensity from histogram
        ipH = Arrays.copyOfRange(ipH, 1, ipH.length);

        // Normalize to number of nonzero pixels
        double[] ipHd = new double[ipH.length];
        for (int i = 0; i < ipH.length; i++) {
            ipHd[i] = (double) ipH[i] / ipNonZeroArea;
        }

        // Cumulative distributions
        double[] ipHcum = new double[ipH.length];
        ipHcum[0] = ipHd[0];
        for (int i = 1; i < ipH.length; i++) {
            ipHcum[i] = ipHcum[i - 1] + ipHd[i];
        }
		
        // Compute pixelfractions
        //double[] bins = Lib.linearSpacedSequence(0.0, 1.0, nBins);
		double[] bins = new double[]{perc/100.0};
        // Search indices of the bins in the ref
        int[] ipIdx = findIndicesBins(ipHcum, bins);
        double[] ipIdxd = Lib.intArrayToDoubleArray(ipIdx);

        if (debug) {
            double[] x = Lib.intArrayToDoubleArray(Lib.intSequence(1, ipHcum.length));
            IJ.log("N bins: " + x.length);
            Plot ph_ip = new Plot("sample cumulative histogram", "Intensity", "pixel number", x, ipHd);
            Plot pc_ip = new Plot("sample cumulative histogram", "Intensity", "Cumulative histogram", x, ipHcum);
            Plot pi_ip = new Plot("sample indices bins", "index", "Cumulative histogram", bins, ipIdxd);
            ph_ip.show();
            pc_ip.show();
            pi_ip.show();
        }

        // Subtract intensity
        ImageProcessor ip_new = ip.duplicate();
		ip_new.subtract(ipIdxd[0]);
        ImagePlus imp_new = new ImagePlus("subtracted " + ipIdxd[0] + " image", ip_new);

        return ip_new;
    }

	/**
     * Divide by the background intensity of an image using a percentile (minus exactly zero pixels)
     *
	 * @param perc
     * @param ip
     * @return
     */
    public static ImageProcessor divideBackground(ImageProcessor ip, double perc) {

        ip.resetRoi();
        // Obtain histograms
        ImageStatistics ipStats;
        ipStats = ip.getStatistics();
        int[] ipH;
        switch (ip.getBitDepth()) {
            case 16:
                ipH = ipStats.histogram16;
                break;
            case 8:
                ipH = ipStats.histogram;
                break;
            default:
                ipH = ipStats.histogram;
                break;
        }
        double ipArea = ipStats.area;
        double ipZeroArea = ipH[0];
        double ipNonZeroArea = ipArea - ipZeroArea;

        // Remove zero intensity from histogram
        ipH = Arrays.copyOfRange(ipH, 1, ipH.length);

        // Normalize to number of nonzero pixels
        double[] ipHd = new double[ipH.length];
        for (int i = 0; i < ipH.length; i++) {
            ipHd[i] = (double) ipH[i] / ipNonZeroArea;
        }

        // Cumulative distributions
        double[] ipHcum = new double[ipH.length];
        ipHcum[0] = ipHd[0];
        for (int i = 1; i < ipH.length; i++) {
            ipHcum[i] = ipHcum[i - 1] + ipHd[i];
        }
		
        // Compute pixelfractions
        //double[] bins = Lib.linearSpacedSequence(0.0, 1.0, nBins);
		double[] bins = new double[]{perc/100.0};
        // Search indices of the bins in the ref
        int[] ipIdx = findIndicesBins(ipHcum, bins);
        double[] ipIdxd = Lib.intArrayToDoubleArray(ipIdx);

        if (debug) {
            double[] x = Lib.intArrayToDoubleArray(Lib.intSequence(1, ipHcum.length));
            IJ.log("N bins: " + x.length);
            Plot ph_ip = new Plot("sample cumulative histogram", "Intensity", "pixel number", x, ipHd);
            Plot pc_ip = new Plot("sample cumulative histogram", "Intensity", "Cumulative histogram", x, ipHcum);
            Plot pi_ip = new Plot("sample indices bins", "index", "Cumulative histogram", bins, ipIdxd);
            ph_ip.show();
            pc_ip.show();
            pi_ip.show();
        }

        // Subtract intensity
        ImageProcessor ip_new = ip.duplicate();
		ip_new = ip.convertToFloat();
		if (ipIdxd[0] > 0.0) {
			ip_new.multiply(1.0/ipIdxd[0]);
		}
        ImagePlus imp_new = new ImagePlus("divided by " + ipIdxd[0] + " image", ip_new);

        return ip_new;
    }
	
	
	
	
    /**
     * Find the index of the first value of an array of increasing values which
     * is larger than or equal to the threshold
     *
     * @param incValues
     * @param t
     * @return index as integer of threshold ( if not found, -1 is returned )
     */
    public static int findIndexThreshold(double[] incValues, double t) {

        int n = incValues.length;
        for (int i = 0; i < n; i++) {
            if (incValues[i] >= t) {
                return i;
            }
        }
        return -1;
    }

    public static int[] findIndicesBins(double[] cumValues, double[] bins) {

        int n = bins.length;
        int[] idx = new int[n];
        double eps = 0.00001;

        for (int i = 0; i < n; i++) {
            idx[i] = findIndexThreshold(cumValues, bins[i] - eps);
        }

        return idx;
    }

//	public static void test_clahe( ImagePlus imp, ) {
//
//		Flat.getInstance().run( imp, blockRadius, bins, slope, mask, composite );
//	}
	
    public static void main(String[] args) {

        
        new ImageJ();

        String MAIN_METHOD = "TEST_gaussianBlur2";
        switch (MAIN_METHOD) {
            
			case "TEST_gaussianBlur2":
                String folder = "D:/p_prog_output/slicemap_2/input/reference_images/";
                File srcFile = new File(folder + "ref-01.tif");
                ImagePlus imp = IJ.openImage( srcFile.getAbsolutePath() );
				double s = 8.0;
				try {
					gaussianBlur2( imp, s );
				} catch(Exception e) {
					IJ.log( e.getMessage() );
				}
				break;
			
			case "TEST_feature_extraction_per_roi":
				
				// Map< imageName, Map< featureKey, featureValue > >
				LinkedHashMap< String, LinkedHashMap<String, String > > featureMap = new LinkedHashMap<>();
				ArrayList< LinkedHashMap<String, String > > featureList = new ArrayList<>();
				File inputRoiFolder = new File("d:/p_prog_output/slicemap_3/input/reference_rois_for_features_computed");
				File imageFolder = new File("d:/p_prog_output/slicemap_3/input/reference_images_for_features");
				File outputFolder = new File("d:/p_prog_output/slicemap_3/output/features_per_roi_computed");
				outputFolder.mkdirs();
				String filter = "";

				ArrayList<File> imageFileList = LibIO.findFiles( imageFolder );
				LinkedHashMap< String, File > imageFileMap = new LinkedHashMap<>();
				LinkedHashMap< String, File > roiFileMap = new LinkedHashMap<>();
				for ( File file : imageFileList ) {
					String fileName = file.getName();
					String sliceName;
					if (fileName.contains(".")) {
						sliceName = fileName.substring(0,fileName.lastIndexOf("."));
					} else {
						sliceName = fileName;
					}
					if ( sliceName.contains( filter ) ) {
						File roiSimilarFile = LibIO.findSimilarFile( inputRoiFolder, ".*"+sliceName+".*" );
						if (roiSimilarFile != null) {
							imageFileMap.put(sliceName, file);
							roiFileMap.put( sliceName, roiSimilarFile );
						}
					}
				}

				
				for ( String imageName : imageFileMap.keySet() ) {
					File imageFile = imageFileMap.get( imageName );
					imp = IJ.openImage( imageFile.getAbsolutePath() );
					File roiFile = roiFileMap.get( imageName );
					LinkedHashMap< String, Roi > roiMap = new LinkedHashMap<>();
					try {
						roiMap.putAll( LibRoi.loadRoiAlternative( roiFile ) );
					} catch (ZipException ex) {
						Logger.getLogger(LibImage.class.getName()).log(Level.SEVERE, null, ex);
					} catch (IOException ex) {
						Logger.getLogger(LibImage.class.getName()).log(Level.SEVERE, null, ex);
					}

					for ( String roiName : roiMap.keySet() ) {
						LinkedHashMap<String, String> features = new LinkedHashMap<>();
						Roi roi = roiMap.get( roiName );
						features.put( "image_id", imageName );
						features.put("region_id", roiName);
						features.putAll( featureExtraction( imp, roi) );
						featureMap.put( imageName + "_" + roiName, features );
					}
				}

				IJ.log("START RUN save features");
				for	( String key : featureMap.keySet() ) {
					featureList.add( featureMap.get(key) );
				}
				writeCsv( featureList, ",", new File( outputFolder.getAbsolutePath() + "/" + "features_roi.csv" ).getAbsolutePath() );
				IJ.log("END RUN save features");

				break;

			case "TEST_feature_extraction":
				// Map< imageName, Map< featureKey, featureValue > >
				featureMap = new LinkedHashMap<>();
				// List< Map< featureKey, featureValue > >
				featureList = new ArrayList<>();
				inputRoiFolder = new File("");
				imageFolder = new File("d:/p_prog_output/slicemap_3/input/temp");
				outputFolder = new File("d:/p_prog_output/slicemap_3/output/features");
				outputFolder.mkdirs();
				filter = "";

				imageFileList = LibIO.findFiles( imageFolder );
				imageFileMap = new LinkedHashMap<>();
				for ( File file : imageFileList ) {
					String fileName = file.getName();
					String sliceName;
					if (fileName.contains(".")) {
						sliceName = fileName.substring(0,fileName.lastIndexOf("."));
					} else {
						sliceName = fileName;
					}
					if ( sliceName.contains( filter ) ) {
						imageFileMap.put(sliceName, file);
					}
				}

				for ( String imageName : imageFileMap.keySet() ) {
					File imageFile = imageFileMap.get( imageName );
					imp = IJ.openImage( imageFile.getAbsolutePath() );
					Roi roi = mainObjectExtraction( imp );
					LinkedHashMap<String, String> features = new LinkedHashMap<>();
					features.put( "image_id", imageName );
					features.putAll( featureExtraction( imp, roi) );
					featureMap.put( imageName, features );
				}
				IJ.log("START RUN save features");
				for	( String key : featureMap.keySet() ) {
					featureList.add( featureMap.get(key) );
				}
				writeCsv( featureList, ",", new File( outputFolder.getAbsolutePath() + "/" + "features.csv" ).getAbsolutePath() );
				IJ.log("END RUN save features");

				break;
			
            case "TEST_clahe":
				
                folder = "D:/p_prog_output/slicemap_2/input/reference_images/";
                srcFile = new File(folder + "ref-01.tif");
                imp = IJ.openImage( srcFile.getAbsolutePath() );
				imp.duplicate().show();
				int blockRadius = 5;
				int bins = 128;
				float slope = 2.5f;
				ByteProcessor mask = null;
				boolean composite = false;
				Flat.getInstance().run( imp, blockRadius, bins, slope, mask, composite );
				imp.duplicate().show();
				break;
				
			case "TEST_bgMask":

                folder = "D:/p_prog_output/tau_analysis/output_11/1-X/2016-06-20T101218Z/registration_images/debug/";
                srcFile = new File(folder + "sample.tif");
                File refFile = new File(folder + "ref.tif");
                IJ.log("Loading source: " + srcFile.getAbsolutePath() );
                ImagePlus source = IJ.openImage( srcFile.getAbsolutePath() );
                IJ.log("Loading ref: " + refFile.getAbsolutePath() );
                ImagePlus ref = IJ.openImage( refFile.getAbsolutePath() );
                bgMask(source);
                break;

            case "TEST_standardizeHistogram":

                String srcPath = "C:/Users/Michael/Desktop/tau_analysis/input/montage/B21-3_C2_binning_32.tif";
                srcFile = new File(srcPath);
                String refPath = "C:/Users/Michael/Desktop/tau_analysis/input/montage/B21-2_C2_binning_32.tif";
                refFile = new File(refPath);
                source = IJ.openImage(srcFile.getAbsolutePath());
                ImagePlus target = IJ.openImage(refFile.getAbsolutePath());
                source.show();
                target.show();

                // TODO TEST histogram standardization
                ImageProcessor refIp = target.getProcessor();
                ImageProcessor sourceIp = source.getProcessor();
                int nBins = 10;
                ImageProcessor ipNorm = standardizeHistogram(refIp, sourceIp, nBins);
                ImagePlus impNorm = new ImagePlus("Normalized image", ipNorm);
                impNorm.show();
                break;
				
			case "TEST_subtractBackground":
				srcPath = "D:/d_data/astrid/Beerse31_montage_8_binned/2-Y-Image Export-02_c3_ORG.png";
                srcFile = new File(srcPath);
                source = IJ.openImage(srcFile.getAbsolutePath());
                source.show();

                sourceIp = source.getProcessor();
                ipNorm = subtractBackground( sourceIp, 5);
                impNorm = new ImagePlus("Normalized (bg subtraction) image", ipNorm);
                impNorm.show();
				break;

			case "LOOP_subtractBackground":
				srcPath = "D:/d_data/astrid/Beerse21_stack_C2_binned_8.tif";
				String normPath = "D:/d_data/astrid/Beerse21_stack_C2_binned_8_removeBackground_5.tif";
                srcFile = new File(srcPath);
                source = IJ.openImage(srcFile.getAbsolutePath());
				ImageStack stack = source.getStack();
	
				int nSlices = stack.getSize();
				for (int i = 1; i < nSlices+1; i++) {
	                ImageProcessor ip = stack.getProcessor(i);
					ipNorm = subtractBackground( ip, 5);
					stack.setProcessor(ipNorm, i);
//		            impNorm = new ImagePlus("Normalized (bg subtraction) image", ipNorm);
				}
				IJ.saveAsTiff(source, normPath);

                //impNorm.show();
				break;
        }
    }
}
