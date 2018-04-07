/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap;

//import be.ua.mbarbier.slicemap.lib.MBLog;
import main.be.ua.mbarbier.slicemap.lib.BiMap;
import main.be.ua.mbarbier.slicemap.lib.congealing.AffineCongealing;
import main.be.ua.mbarbier.slicemap.lib.congealing.LandmarksRegistration;
import main.be.ua.mbarbier.slicemap.lib.image.LibImage;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.binSample;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.binStack;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.subtractBackground;
import main.be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.loadRoiAlternative;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.roiFromMask;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.translateRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.maskFromThreshold;
import main.be.ua.mbarbier.slicemap.lib.transform.Transform2D;
import static main.be.ua.mbarbier.slicemap.lib.transform.TransformRoi.applyRoiScaleTransform;
import static main.be.ua.mbarbier.slicemap.lib.transform.TransformRoi.applyRoiScaleTransformAlternative;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.GaussianBlur;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import loci.formats.FormatException;
import static main.be.ua.mbarbier.slicemap.lib.ImageBF.getSeriesMetadata;
import static main.be.ua.mbarbier.slicemap.lib.ImageBF.getSeriesMetadataNoEx;
import static main.be.ua.mbarbier.slicemap.lib.ImageBF.openSeriesNoEx;
//import loci.common.services.DependencyException;
//import loci.formats.FormatException;
import net.imglib2.realtransform.AffineTransform2D;
import net.lingala.zip4j.exception.ZipException;
import static main.be.ua.mbarbier.slicemap.lib.Lib.log;
import main.be.ua.mbarbier.slicemap.lib.Meta;

/**
 *
 * @author mbarbier
 */
public class AffineAlign {

	Main param;
	File sampleFile;
	String sliceName;
	ImagePlus ref;
	ImagePlus stack;
	ImagePlus alignedStack;
	ImagePlus alignedReferenceStack;
	ImagePlus sampleCandidateStack;
	ImagePlus sample;
	ImagePlus bestSample;
	String outputFolder;
	String inputFolder;
	File outputFolderFile;
	File inputFolderFile;
	
	LinkedHashMap< String, Roi > templatePointRoi;
	LinkedHashMap< String, Roi > templatePointRoiOri;
	ImagePlus templateImageOri;
	ImagePlus templateImage;
	String templateRefId;
	
    //public ArrayList<double[]> transformVec;
    //public ArrayList<double[]> preTransformVec;
    //public ArrayList<double[]> transformRealVec;

    //MBLog log;
	String logFileName;
	public static final Logger LOGGER = Logger.getLogger( AffineAlign.class.getName());
	int averageSamplePixels;
	Roi sampleRoi;
	LinkedHashMap< String, Roi > samplePointRoi;
	int sampleIndex;
	int bestSampleIndex;
	int bestSampleIndexEntropy;
	BiMap< String, Integer > idMap;
	AffineCongealing congealing;

	public boolean SUBTRACT_BACKGROUND;
	public boolean SCALE_SAMPLES_TO_REFS;
	public boolean PREWARPING;
	public String PREWARPING_METHOD = "";
	public static final String PREWARPING_LINE = "prewarping_line";
	public static final String PREWARPING_POINTS = "prewarping_points";
	public static final String PREWARPING_MANUAL_3_POINTS = "manual_3_points";
	public boolean CONGEALING;
	public boolean SUBSET_SELECTION;
	public boolean ELASTIC;
	public boolean MIRROR_SAMPLES;

	File transformVecFile;
	File preTransformVecFile;
	File transformRealVecFile;
	File stackPropsFile;
	File stackFile;
	File alignedStackFile;

	static String METHOD_FEATURE_DETECTION = "SIFT";

	public AffineAlign() {
	}

	public ImagePlus getAlignedStack() {
		return alignedStack;
	}

	public int getBestSampleIndex() {
		return bestSampleIndex;
	}

	public int getBestSampleIndexEntropy() {
		return bestSampleIndexEntropy;
	}

	public ImagePlus getStack() {
		return stack;
	}

	//public ArrayList<double[]> getTransformVec() {
	//	return transformVec;
	//}

	//public ArrayList<double[]> getPreTransformVec() {
	//	return preTransformVec;
	//}

	//public ArrayList<double[]> getTransformRealVec() {
	//	return transformRealVec;
	//}

	public Roi getSampleRoi() {
		return sampleRoi;
	}
	
	public int getSampleIndex() {
		return sampleIndex;
	}

	public AffineCongealing getCongealing() {
		return congealing;
	}
	
	public void init( Main param ) {
		this.param = param;
		//this.appFolder = param.get(Main.PARAM_APP_FOLDER);
		this.inputFolder = param.INPUT_FOLDER.getAbsolutePath();
		this.inputFolderFile = param.INPUT_FOLDER;
		this.outputFolder = param.OUTPUT_FOLDER.getAbsolutePath();
		this.transformVecFile = param.FILE_TRANSFORMVEC;
		this.preTransformVecFile = param.FILE_PRETRANSFORMVEC;
		this.transformRealVecFile = param.FILE_TRANSFORMREALVEC;
		//this.stackPropsOriFile = new File( this.inputFolder + "/" + Main.INPUT_SUBDIR_REFERENCE_STACK + "/" + Main.INPUT_STACKPROPS_LABEL + "_" + Main.INPUT_FILE_REFERENCE_STACK + ".csv");
		//this.stackPropsFile = new File( this.inputFolder + "/" + Main.INPUT_SUBDIR_REFERENCE_STACK + "/" + Main.INPUT_STACKPROPS_LABEL + "_" + Main.INPUT_ALIGNEDSTACK_LABEL + "_" + INPUT_FILE_REFERENCE_STACK + ".csv");
		this.alignedStackFile = param.FILE_ALIGNED_REFERENCE_STACK;
		this.stackFile = param.FILE_REFERENCE_STACK;
		this.stackPropsFile = param.FILE_STACKPROPS;
		this.sampleFile = param.FILE_SAMPLE;
		this.sliceName = param.ID_SAMPLE;
		this.idMap = param.getIdMap();

		this.logFileName = Main.CONSTANT_FILE_NAME_LOG;
		File logFile = new File( this.outputFolder + "/" + "debugLogFile.txt");//this.logFileName );
		FileHandler logFileHandler;
		try {
			logFileHandler = new FileHandler( logFile.getAbsolutePath() );
			SimpleFormatter simpleFormatter = new SimpleFormatter();
			logFileHandler.setFormatter(simpleFormatter);
			Logger.getLogger( AffineAlign.class.getName()).addHandler( logFileHandler );
            logFileHandler.setLevel(Level.ALL);
            LOGGER.setLevel(Level.ALL);

		} catch (IOException ex) {
			Logger.getLogger(AffineAlign.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SecurityException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		
		
		//logFileHandler.

		// CONSTANTS WHICH ARE FIXED
		this.SCALE_SAMPLES_TO_REFS = true;
	}

	/**
	 * 
	 * @param congealing this parameter is only used for the width/height of the original merged ref stack, we should try to get those elsewhere, so we can remove this dependency
	 */
	public void generatePointRoi(AffineCongealing congealing) {

		try {
			this.templatePointRoiOri = loadRoiAlternative( new File( this.inputFolder + "/" + Main.CONSTANT_NAME_TEMPLATE_POINTROI_ROI ) );
			this.templatePointRoi = applyRoiScaleTransform( templatePointRoiOri, 0.0, 0.0, 1./congealing.binPreWarp );
			//props.roiMap = applyRoiScaleTransformAlternative( props.roiMapOri, xOffset, yOffset, 1.0 / ((double) props.binning) );

			this.templateImageOri = IJ.openImage( this.inputFolder + "/" + Main.CONSTANT_NAME_TEMPLATE_POINTROI_IMAGE );
			this.templateRefId = templateImageOri.getTitle();
			int stackWidthSmall = param.refStack.getWidth();
			int stackHeightSmall = param.refStack.getHeight();
			int stackWidthLarge = param.stackProps.get( param.stackProps.keySet().iterator().next() ).stackWidth;
			int stackHeightLarge = param.stackProps.get( param.stackProps.keySet().iterator().next() ).stackHeight;
			
			this.templateImage = IJ.createImage( this.templateImageOri.getTitle(), stackWidthSmall, stackHeightSmall, this.templateImageOri.getNSlices(), this.templateImageOri.getBitDepth() );
			ImagePlus tempSmall = LibImage.binImage( this.templateImageOri, param.CONGEALING_STACKBINNING );
			this.templateImage.getProcessor().copyBits( tempSmall.getProcessor(), (int) Math.round((stackWidthSmall - tempSmall.getWidth()) / 2.0), (int) Math.round((stackHeightSmall - tempSmall.getHeight()) / 2.0), Blitter.COPY);
			for (String key : this.templatePointRoi.keySet() ) {
				this.templatePointRoi.put( key, translateRoi(this.templatePointRoi.get(key), (int) Math.round((stackWidthSmall - tempSmall.getWidth()) / 2.0), (int) Math.round((stackHeightSmall - tempSmall.getHeight()) / 2.0) ) );
			}
			this.templateImage.show();
		} catch (ZipException ex) {
			Logger.getLogger(AffineAlign.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(AffineAlign.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	public void addSample( AffineCongealing congealing, ImagePlus refStack ) {

		// TODO The part where we derive the pixel size should go outside the addSample function (put it before), so we can change the behavior like e.g. having a user input pixel size 
		String inputSamplePath = this.sampleFile.getAbsolutePath();
		
		// We will want to scale the samples to the references, here we find the binning fraction (this should be a power of 2? )
		double binFraction = this.param.pixelSizeRef / this.param.pixelSizeSample;
		IJ.log( "binning fraction = " + binFraction );
		int sampleOriWidth = 1;
		int sampleOriHeight = 1;
		String sampleOriTitle = "notitle?";
		
		// if we have a sample that is in a pyramidal format we want to use this to avoid loading the whole image and downscaling

		if ( this.param.PYRAMID_IMAGE ) {
			int sampleBinning = (int) Math.round( binFraction );
			int seriesIndex = log( sampleBinning, 2 );
			int channelIndex = this.param.channelNuclei;
			IJ.log("--------------------------------------------------------------------------------");
			IJ.log(" seriesIndex = " + seriesIndex);
			IJ.log("--------------------------------------------------------------------------------");
			this.sample = openSeriesNoEx( inputSamplePath, seriesIndex, channelIndex );
			sampleOriWidth = this.sample.getWidth();
			sampleOriHeight = this.sample.getHeight();
			sampleOriTitle = this.sample.getTitle();
			Logger.getLogger(AffineAlign.class.getName()).log(Level.INFO, "addSample::sampleOriWidth = " + sampleOriWidth );
			Logger.getLogger(AffineAlign.class.getName()).log(Level.INFO, "addSample::sampleOriHeight = " + sampleOriHeight );
			//this.sample = IJ.openImage(inputSamplePath);
			//}
		} else {

			this.sample = IJ.openImage(inputSamplePath);
			sampleOriWidth = this.sample.getWidth();
			sampleOriHeight = this.sample.getHeight();
			sampleOriTitle = this.sample.getTitle();
			Logger.getLogger(AffineAlign.class.getName()).log(Level.INFO, "addSample::sampleOriWidth = " + sampleOriWidth );
			Logger.getLogger(AffineAlign.class.getName()).log(Level.INFO, "addSample::sampleOriHeight = " + sampleOriHeight );

			if (SCALE_SAMPLES_TO_REFS) {

				LOGGER.log(Level.INFO, "addSample::binFraction = " + binFraction );

				if (binFraction < 0.99 ) {
					this.sample = LibImage.binImage(sample, binFraction);
				} else {
					if (binFraction > 1.01) {
						this.sample = LibImage.binImage(sample, binFraction);
					}
				}
			}
		}
		int maxSize = Math.max(sampleOriWidth, sampleOriHeight);

		// downscale slice image
		congealing.scalePreWarp =  (int) Math.round((double) maxSize * Main.CONSTANT_SIGMA_RATIO);
		ImagePlus sampleBinned = binSample( sample, congealing.binPreWarp, congealing.scalePreWarp, 1.0, congealing.refWidthBinned, congealing.refHeightBinned, congealing.saturatedPixelsPercentage);
		LOGGER.log(Level.INFO, "addSample::binSample function: binSample( ImagePlus sample, int binning, double scale, double pixelSize, int refWidthBinned, int refHeightBinned, double saturatedPixelPercentage )"  );
		LOGGER.log(Level.INFO, "addSample::binSample function called with parameters: ( sample, congealing.binPrewarp = " + congealing.binPreWarp + ", congealing.scalePreWarp = " + congealing.scalePreWarp + ", pixelSize = 1.0, refWidthBinned = " + congealing.refWidthBinned + ", congealing.refHeightBinned = " + congealing.refHeightBinned + ", congealing.saturatedPixelsPercentage = " + congealing.saturatedPixelsPercentage );
		LOGGER.log(Level.INFO, "addSample::sampleBinned width (just after binning) = " + sampleBinned.getWidth() );
		LOGGER.log(Level.INFO, "addSample::sampleBinned height (just after binning) = " + sampleBinned.getHeight() );
		
		//this.sample.show();
		this.averageSamplePixels = (int) Math.ceil(Math.sqrt(this.sample.getWidth() * this.sample.getHeight()));

		// BackgroundSubtraction sample (1e round: before congealing)
		if (SUBTRACT_BACKGROUND) {
			sampleBinned.setProcessor( LibImage.subtractBackground( sampleBinned.getProcessor(), 5) );
		}

		// Main object (slice) segmentation
		ImagePlus imp = sampleBinned.duplicate();
		imp.getProcessor().findEdges();
		int varianceRadius = 1;//(int) Math.round( this.varianceRadiusRatio * this.averageSamplePixels );
		IJ.run(imp, "Variance...", "radius=" + varianceRadius);
		imp.setProcessor(imp.getProcessor().convertToByteProcessor());
		ImagePlus mask = maskFromThreshold(imp, 128);
		mask.getProcessor().multiply(255.0);
		IJ.run(mask, "Minimum...", "radius=" + varianceRadius);
		mask.getProcessor().invert();
		IJ.run(mask, "Fill Holes", "");
		Roi sampleRoi = roiFromMask(mask);
		this.sampleRoi = new PolygonRoi(sampleRoi.getInterpolatedPolygon(2 * varianceRadius, true), Roi.POLYGON);
		imp.setRoi(sampleRoi);

/*
		//  2) sample resized to original size of reference stack
		ImagePlus sampleOri = sample.duplicate();//IJ.openImage( sampleFile.getAbsolutePath() );
		sampleOri.setTitle("sample");
		// sample resizing to original size of reference stack
		//this.log.log("Create larger empty image");
		this.sample = IJ.createImage(this.sampleFile.getName(), congealing.refWidth, congealing.refHeight, sampleOri.getNSlices(), sampleOri.getBitDepth());

		//this.log.log("Copy sample into image");
		sample.getProcessor().copyBits(sampleOri.getProcessor(), (int) Math.round((congealing.refWidth - sampleOri.getWidth()) / 2.0), (int) Math.round((congealing.refHeight - sampleOri.getHeight()) / 2.0), Blitter.COPY);

		//String inputDir = "D:/d_data/astrid/montage";//D:/p_prog_output/tissue_registration/congealing/input";
		int nPoints = param.CONGEALING_NPOINTS;
		ArrayList< LinkedHashMap<String, String>> out = new ArrayList< LinkedHashMap<String, String>>();

		sample.setProcessor(subtractBackground(sample.getProcessor(), 5));
		//sample.duplicate().show();

		//ImagePlus sampleBinned = binSample(sample, congealing.binPreWarp, congealing.scalePreWarp, 1.0, congealing.refWidth, congealing.refHeight, congealing.saturatedPixelsPercentage);
*/

		LOGGER.log(Level.INFO, "addSample::sampleBinned width (just before addSlice) = " + sampleBinned.getWidth() );
		LOGGER.log(Level.INFO, "addSample::sampleBinned height (just before addSlice) = " + sampleBinned.getHeight() );
		LOGGER.log(Level.INFO, "addSample::refStack width (just before addSlice) = " + refStack.getStack().getWidth() );
		LOGGER.log(Level.INFO, "addSample::refStack height (just before addSlice) = " + refStack.getStack().getHeight() );
		String saveDebugSampleBinnedFilePath = new File( param.OUTPUT_FOLDER + "/" + "debug_sampleBinnedFile.tif" ).getAbsolutePath();
		LOGGER.log(Level.INFO, "Saving sampleBinned " + saveDebugSampleBinnedFilePath );
		IJ.saveAsTiff( sampleBinned, saveDebugSampleBinnedFilePath );	
		refStack.getStack().addSlice(sampleBinned.getProcessor());
		refStack.getStack().setSliceLabel(sliceName, refStack.getNSlices());
		this.stack = refStack;

		this.sampleIndex = this.stack.getNSlices();
		//this.congealing.stackProps.
		ImageProperties tempProps = this.congealing.stackProps.get(0).copy();
		tempProps.pointRoiOri = null;
		tempProps.pointRoiOriFile = null;
		tempProps.pointRoi = this.samplePointRoi;
		congealing.addImageProps(tempProps, this.sampleIndex, sampleOriTitle, sampleOriWidth, sampleOriHeight );
		congealing.nImages = this.congealing.stackProps.size();
		double[] tTempVec = new double[congealing.nParameters];
		for (double el : tTempVec) {
			el = 0.;
		}

		congealing.preTransformVec.put( sampleOriTitle, tTempVec);
		congealing.transformVec.put(sampleOriTitle, tTempVec);

	}
	
	public void addMirroredSamples( AffineCongealing congealing, ImagePlus refStack ) {

		ImagePlus sampleBinned_mirror_y;
		ImagePlus sampleBinned_mirror_x;
		ImagePlus sampleBinned_mirror_xy;
		double centerX = refStack.getWidth()/2.;
		double centerY = refStack.getHeight()/2.;
		
		ImageProcessor ip = refStack.getStack().getProcessor(this.sampleIndex).duplicate();
		ImageProcessor ip_x = ip.duplicate();
		ip_x.flipHorizontal();
		ImageProcessor ip_y = ip.duplicate();
		ip_y.flipVertical();
		ImageProcessor ip_xy = ip_x.duplicate();
		ip_xy.flipVertical();

		// Generate mirror images to the stack
		sampleBinned_mirror_x = new ImagePlus("mirror_x", ip_x);
		sampleBinned_mirror_y = new ImagePlus("mirror_y", ip_y);
		sampleBinned_mirror_xy = new ImagePlus("mirror_xy", ip_xy);
		// Add mirror images to the stack
		refStack.getStack().addSlice( sampleBinned_mirror_x.getTitle(), sampleBinned_mirror_x.getProcessor() );
		refStack.getStack().addSlice( sampleBinned_mirror_y.getTitle(), sampleBinned_mirror_y.getProcessor() );
		refStack.getStack().addSlice( sampleBinned_mirror_xy.getTitle(), sampleBinned_mirror_xy.getProcessor() );
		
		// Is this correct???
		this.alignedStack = refStack;
		congealing.addImageProps( this.congealing.stackProps.get(this.sampleIndex-1), this.sampleIndex+1, sampleBinned_mirror_x.getTitle(), this.congealing.stackProps.get(this.sampleIndex-1).width, this.congealing.stackProps.get(this.sampleIndex-1).height );
		congealing.addImageProps( this.congealing.stackProps.get(this.sampleIndex-1), this.sampleIndex+2, sampleBinned_mirror_y.getTitle(), this.congealing.stackProps.get(this.sampleIndex-1).width, this.congealing.stackProps.get(this.sampleIndex-1).height );
		congealing.addImageProps( this.congealing.stackProps.get(this.sampleIndex-1), this.sampleIndex+3, sampleBinned_mirror_xy.getTitle(), this.congealing.stackProps.get(this.sampleIndex-1).width, this.congealing.stackProps.get(this.sampleIndex-1).height );

		// Add tranformation vectors
		//int nParams = congealing.preTransformVec.get(0).length;
		//double[] tvecTemp = new double[nParams];
		//Transform2D lastTransformMat = new Transform2D();
		String lastId = this.congealing.stackProps.get(this.sampleIndex-1).id;

		AffineTransform2D lastPreTransformMat = congealing.preTransformMat.get( lastId ).copy();
		double[] lastPreTransformVec = congealing.preTransformVec.get( lastId ).clone();
		double[] lastTransformVec = congealing.transformVec.get( lastId ).clone();

		//AffineTransform2D lastTransformMat = congealing.transformMat.get( lastId ).copy();
		// 1e mirror X
		// TODO MB: how to change the pretransform/transform (initialized to unity?)
		double[] tTempVec;
		congealing.preTransformMat.put( sampleBinned_mirror_x.getTitle(), Transform2D.mirror( lastPreTransformMat.copy(), true, false, centerX, centerY ) );
		tTempVec = lastPreTransformVec.clone();
		tTempVec[congealing.INDEX_MIRROR_X] = 1.;
		congealing.preTransformVec.put( sampleBinned_mirror_x.getTitle(), tTempVec.clone() );
		congealing.transformVec.put( sampleBinned_mirror_x.getTitle(), tTempVec.clone() );
		//congealing.transformMat.put( sampleBinned_mirror_x.getTitle(), Transform2D.unitMatrix() );
		// 2e mirror Y
		congealing.preTransformMat.put( sampleBinned_mirror_y.getTitle(), Transform2D.mirror( lastPreTransformMat.copy(), false, true, centerX, centerY ) );
		tTempVec = lastPreTransformVec.clone();
		tTempVec[congealing.INDEX_MIRROR_Y] = 1.;
		congealing.preTransformVec.put( sampleBinned_mirror_y.getTitle(), tTempVec.clone() );
		congealing.transformVec.put( sampleBinned_mirror_y.getTitle(), tTempVec.clone() );
		//congealing.transformMat.put( sampleBinned_mirror_x.getTitle(), Transform2D.mirror( lastTransformMat.copy(), false, true, centerX, centerY ) );
		// 3e mirror XY
		congealing.preTransformMat.put( sampleBinned_mirror_xy.getTitle(), Transform2D.mirror( lastPreTransformMat.copy(), true, true, centerX, centerY ) );
		tTempVec = lastPreTransformVec.clone();
		tTempVec[congealing.INDEX_MIRROR_XY] = 1.;
		congealing.preTransformVec.put( sampleBinned_mirror_xy.getTitle(), tTempVec.clone() );
		congealing.transformVec.put( sampleBinned_mirror_xy.getTitle(), tTempVec.clone() );
		//congealing.transformMat.put( sampleBinned_mirror_x.getTitle(), Transform2D.mirror( lastTransformMat.copy(), true, true, centerX, centerY ) );

		// TODO MB: do we need the transformVec also? (since iterTransform needs it as input???)
	}

	public void run() {

		this.congealing = new AffineCongealing();
		//congealing.setLog(this.log);
		congealing.setTRANSFORM("AFFINE");
		this.congealing.nParameters = congealing.methodParameters.length;

		// LOAD REFERENCE STACK, PROPERTIES, SAMPLE ----------------------------
		//  1) reference stack which is pre-binned and smoothed
		ImagePlus imp = null;
		if (param.IS_STACK_SET) {
			//this.log.log("Congealing reference stack already computed, get stack and properties from Main param");
			imp = param.getRefStack().duplicate();
			congealing.setStackProps( param.getStackProps() );
		} else {
			//this.log.log("Load congealing reference stack: " + stackFile.getAbsolutePath());
			imp = IJ.openImage(stackFile.getAbsolutePath());
			//this.log.log("Load stack properties: " + stackPropsFile.getAbsolutePath());
			congealing.loadStackProps( this.stackPropsFile );
		}
		congealing.setNImages(imp.getNSlices());
		int nPoints = param.CONGEALING_NPOINTS;
		//  init transformvecs
		ArrayList<String> imageIdList = new ArrayList<>();
		for (ImageProperties prop : congealing.stackProps ) {
			imageIdList.add( prop.id );
		}

		congealing.initTransformMat( imageIdList );
		congealing.initTransformVec( imageIdList, congealing.nParameters );
		congealing.nImages = congealing.stackProps.size();
		//  add sample
		addSample( congealing, imp );

		// ---------------------------------------------------------------------

		// PRE-WARPING ---------------------------------------------------------
		//this.log.log("Prewarping sample");
		ImagePlus impBefore = imp.duplicate();
		impBefore.setTitle("before pre-warping");
		//impBefore.show();

		IJ.log(" --- PreWarping");
		switch( this.PREWARPING_METHOD ) {
			case PREWARPING_POINTS:
				imp = congealing.preWarping(imp, nPoints);
				break;
			case PREWARPING_LINE:
				imp = congealing.alignmentAngle(imp, 0.0, nPoints);
				break;
			case PREWARPING_MANUAL_3_POINTS:
				LinkedHashMap< String, LinkedHashMap< String, Roi > > roiList = new LinkedHashMap<>();
				for ( ImageProperties props : congealing.stackProps ) {
					roiList.put( props.id, props.pointRoi );
				}
				generatePointRoi(this.congealing);
				LinkedHashMap< String, Roi > roi0 = this.templatePointRoi;
				ImagePlus imp0 = this.templateImage;
				imp = LandmarksRegistration.alignmentPoints( imp, roiList, idMap, roi0, imp0 );
				break;
		}

		ImagePlus impAfter = imp.duplicate();
		impAfter.setTitle("after pre-warping");
		//impAfter.show();
		// ---------------------------------------------------------------------


		// ADDITION OF MIRRORED SAMPLES ----------------------------------------
		// TODO readd the mirror samples in the proper way
		addMirroredSamples( congealing, imp );
		// Add the backprojected mirrored samples for overlay purposes
		ImageProcessor sampleBinned = this.stack.getStack().getProcessor( this.stack.getNSlices() );
		this.stack.getStack().addSlice( "mirror_x", sampleBinned.duplicate() );
		this.stack.getStack().addSlice( "mirror_y", sampleBinned.duplicate() );
		this.stack.getStack().addSlice( "mirror_xy", sampleBinned.duplicate() );

		// congealing.saveStackProps( new File(this.outputFolder + "/" + "stackprops_new.csv") );
		// ---------------------------------------------------------------------


		// CONGEALING ----------------------------------------------------------
		int nReferences = param.CONGEALING_NREFERENCES;
		int nIterations = param.CONGEALING_NITERATIONS;
		int binCongealing = param.CONGEALING_BINCONGEALING;
		int nImages = imp.getNSlices();
		param.CONGEALING_NIMAGES = nImages;

		//this.log.log("START actual congealing with: nImages = " + nImages + ", nIterations = " + nIterations + ", binning congealing = " + binCongealing);
		congealing.binCongealing = binCongealing;
		IJ.log(" --- Binning for congealing = " + congealing.binCongealing);
		imp = binStack(imp, binCongealing);

		IJ.log(" --- Normalizing the stack [0..1]");
		congealing.nImages = nImages;
		congealing.nParameters = 8;
		ImagePlus impNorm = IJ.createHyperStack("Stack 32 bit", imp.getWidth(), imp.getHeight(), 1, nImages, 1, 32);
		for (int i = 1; i < nImages + 1; i++) {
			ImageProcessor ipNormi = imp.getStack().getProcessor(i).convertToFloatProcessor();
			ipNormi.multiply(1.0 / imp.getProcessor().maxValue() );
			impNorm.getStack().setProcessor(ipNormi, i);
			impNorm.getStack().setSliceLabel( imp.getStack().getSliceLabel(i), i);
		}

		IJ.log(" --- Run the actual congealing algorithm with #iterations = " + nIterations);
		congealing.runCongealing( impNorm, nIterations, congealing.TRANSFORM, congealing.methodParameters );
		bestSampleIndex = sampleIndex;
		bestSampleIndexEntropy = sampleIndex;
		double minp = congealing.pearsonImpact[sampleIndex-1];
		double minpe = congealing.entropyImpact[sampleIndex-1];
		for ( int i = sampleIndex-1; i < nImages; i++ ) {
			double p = congealing.pearsonImpact[i];
			if (p < minp) {
				minp = p;
				bestSampleIndex = i;
			}
			double pe = congealing.entropyImpact[i];
			if (pe < minpe) {
				minpe = pe;
				bestSampleIndexEntropy = i;
			}
		}

		this.alignedStack = congealing.getAlignedStack();
		for (int i = 1; i < nImages + 1; i++) {
			this.alignedStack.getStack().setSliceLabel( imp.getStack().getSliceLabel(i), i);
		}

		// Scale up the image stack again to binCongealing factor
		IJ.run(this.alignedStack, "Size...", "width=" + this.stack.getWidth() + " height=" + this.stack.getHeight() + " depth=" + this.alignedStack.getNSlices() + " constrain average interpolation=Bilinear"); 

		// ---------------------------------------------------------------------
		// TODO MB !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// Were going to have problems with the centerX,Y vs the scaling
		// ---------------------------------------------------------------------
		//congealing.scaleTransform( (double) congealing.binCongealing );
		//IJ.saveAsTiff( this.alignedStack, this.alignedStackFile.getAbsolutePath() );

		// Construct separate stacks for the samples and the refs (can we split them)
		this.alignedReferenceStack = IJ.createHyperStack("Stack 32 bit", this.alignedStack.getWidth(), this.alignedStack.getHeight(), 1, sampleIndex-1, 1, this.alignedStack.getBitDepth());
		this.sampleCandidateStack = IJ.createHyperStack("Stack 32 bit", this.alignedStack.getWidth(), this.alignedStack.getHeight(), 1, nImages-sampleIndex+1, 1, this.alignedStack.getBitDepth());
		for ( int i = 0; i < this.alignedReferenceStack.getNSlices(); i++ ) {
			this.alignedReferenceStack.getStack().setProcessor( this.alignedStack.getStack().getProcessor(i+1) , i+1 );
			this.alignedReferenceStack.getStack().setSliceLabel( this.alignedStack.getStack().getSliceLabel(i+1) , i+1 );
		}
		for ( int i = 0; i < this.sampleCandidateStack.getNSlices(); i++ ) {
			this.sampleCandidateStack.getStack().setProcessor( this.alignedStack.getStack().getProcessor( sampleIndex+i ) , i+1 );
			this.sampleCandidateStack.getStack().setSliceLabel( this.alignedStack.getStack().getSliceLabel( sampleIndex+i ) , i+1 );
		}
		//this.alignedReferenceStack.show();
		//this.sampleCandidateStack.show();

		
		
		// ---------------------------------------------------------------------
		// TODO MB
		// ---------------------------------------------------------------------
		// this.preTransformVec = congealing.preTransformVec;
		// this.transformVec = congealing.transformVec;
		//
		// congealing.saveTransformVecs( preTransformVecFile, transformVecFile, transformRealVecFile );
		LOGGER.exiting( this.getClass().getName(), "run");
	}

	/**
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {

		// set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Align.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        new ImageJ();

		Align align = new Align();
		align.PREWARPING_METHOD = align.PREWARPING_LINE;

		Main param = new Main();
		param.INPUT_FOLDER = new File("d:/p_prog_output/slicemap/input");
		param.OUTPUT_FOLDER = new File("d:/p_prog_output/slicemap/output_ref_align");
		param.CONGEALING_NITERATIONS = 20;
		param.CONGEALING_NREFERENCES = 5;
		param.CONGEALING_BINCONGEALING = 2;
		param.CONGEALING_NPOINTS = 8;
		param.FILE_REFERENCE_STACK = new File("Beerse21.tif");
		align.init( param );

        IJ.log("START RUN plugin");
		align.run();
        IJ.log("END RUN plugin");
	}
}
