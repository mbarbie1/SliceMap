/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ua.mbarbier.slicemap;

//import be.ua.mbarbier.slicemap.lib.MBLog;
import be.ua.mbarbier.slicemap.lib.congealing.Congealing;
import be.ua.mbarbier.slicemap.lib.image.LibImage;
import static be.ua.mbarbier.slicemap.lib.image.LibImage.binSample;
import static be.ua.mbarbier.slicemap.lib.image.LibImage.binStack;
import static be.ua.mbarbier.slicemap.lib.image.LibImage.subtractBackground;
import static be.ua.mbarbier.slicemap.lib.roi.LibRoi.roiFromMask;
import static be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.maskFromThreshold;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 *
 * @author mbarbier
 */
public class Align {

	Main param;
	File sampleFile;
	String sliceName;
	ImagePlus ref;
	ImagePlus stack;
	ImagePlus alignedStack;
	ImagePlus alignedReferenceStack;
	ImagePlus sampleCandidateStack;
	ImagePlus sample;
	String outputFolder;
	String inputFolder;
	File outputFolderFile;
	File inputFolderFile;

    public ArrayList<double[]> transformVec;
    public ArrayList<double[]> preTransformVec;
    public ArrayList<double[]> transformRealVec;

    //MBLog log;
	String logFileName;
	int averageSamplePixels;
	Roi sampleRoi;
	int sampleIndex;
	int bestSampleIndex;
	int bestSampleIndexEntropy;
	Congealing congealing;

	public boolean SUBTRACT_BACKGROUND;
	public boolean PREWARPING;
	public String PREWARPING_METHOD = "";
	public String PREWARPING_LINE = "prewarping_line";
	public String PREWARPING_POINTS = "prewarping_points";
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

	public Align() {
	}

	public ImagePlus getAlignedStack() {
		return alignedStack;
	}

	public ImagePlus getStack() {
		return stack;
	}

	public ArrayList<double[]> getTransformVec() {
		return transformVec;
	}

	public ArrayList<double[]> getPreTransformVec() {
		return preTransformVec;
	}

	public ArrayList<double[]> getTransformRealVec() {
		return transformRealVec;
	}

	public Roi getSampleRoi() {
		return sampleRoi;
	}
	
	public int getSampleIndex() {
		return sampleIndex;
	}

	public Congealing getCongealing() {
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

		this.logFileName = Main.CONSTANT_FILE_NAME_LOG;
		File logFile = new File( this.outputFolder + "/" + this.logFileName );
		//this.log = new MBLog(logFile.getAbsolutePath());
	}

	public void addSample( Congealing congealing, ImagePlus refStack ) {
	
		String inputSamplePath = this.sampleFile.getAbsolutePath();
		this.sample = IJ.openImage(inputSamplePath);
		//this.sample.show();
		this.averageSamplePixels = (int) Math.ceil( Math.sqrt( this.sample.getWidth() * this.sample.getHeight() ) );
		
	// BackgroundSubtraction sample (1e round: before congealing)
		if (SUBTRACT_BACKGROUND) {
			//this.log.log("Subtracting background sample");
			this.sample.setProcessor( LibImage.subtractBackground(this.sample.getProcessor(), 5) );
			String outputSamplePath = outputFolder + "/" + "sample_removeBackground_"+ this.sliceName +".tif";
			//this.log.log("Saving sample image after background subtraction: " + outputSamplePath);
			IJ.saveAsTiff(this.sample, outputSamplePath);
		}

	// Main object (slice) segmentation
		ImagePlus imp = this.sample.duplicate();
		imp.getProcessor().findEdges();
		int varianceRadius = 10;//(int) Math.round( this.varianceRadiusRatio * this.averageSamplePixels );
        IJ.run(imp, "Variance...", "radius="+varianceRadius);
		imp.setProcessor(imp.getProcessor().convertToByteProcessor());
		ImagePlus mask = maskFromThreshold(imp, 128);
		mask.getProcessor().multiply(255.0);
		IJ.run(mask, "Minimum...", "radius="+varianceRadius);
		mask.getProcessor().invert();
		IJ.run(mask, "Fill Holes", "");
		Roi sampleRoi = roiFromMask(mask);
		this.sampleRoi = new PolygonRoi( sampleRoi.getInterpolatedPolygon( 2*varianceRadius, true), Roi.POLYGON );
		imp.setRoi(sampleRoi);

		//  2) sample resized to original size of reference stack
		ImagePlus sampleOri = sample.duplicate();//IJ.openImage( sampleFile.getAbsolutePath() );
		// sample resizing to original size of reference stack
		//this.log.log("Create larger empty image");
		this.sample = IJ.createImage( this.sampleFile.getName(), congealing.refWidth, congealing.refHeight, sampleOri.getNSlices(), sampleOri.getBitDepth() );
		//this.log.log("Copy sample into image");
		sample.getProcessor().copyBits(sampleOri.getProcessor(), (int) Math.round((congealing.refWidth - sampleOri.getWidth()) / 2.0), (int) Math.round((congealing.refHeight - sampleOri.getHeight()) / 2.0), Blitter.COPY);

		//String inputDir = "D:/d_data/astrid/montage";//D:/p_prog_output/tissue_registration/congealing/input";
		int nPoints = param.CONGEALING_NPOINTS;
		ArrayList< LinkedHashMap<String, String>> out = new ArrayList< LinkedHashMap<String, String>>();

		sample.setProcessor(subtractBackground(sample.getProcessor(), 5));
		//sample.duplicate().show();
		ImagePlus sampleBinned = binSample(sample, congealing.binPreWarp, congealing.scalePreWarp, 1.0, congealing.refWidth, congealing.refHeight, congealing.saturatedPixelsPercentage );
		//double maxValue = sampleBinned.getProcessor().maxValue();
		//sampleBinned.setProcessor(sampleBinned.getProcessor().convertToFloatProcessor());
		//sampleBinned.getProcessor().multiply(1.0 / maxValue );

		refStack.getStack().addSlice(sampleBinned.getProcessor());
		refStack.getStack().setSliceLabel(sliceName, refStack.getNSlices());
		this.stack = refStack;
		int imageIndex = this.stack.getNSlices();
		String transformationType = congealing.getTRANSFORM();
		congealing.nParameters = congealing.MAP_TRANSFORMS_nPARAMETERS.get( congealing.getTRANSFORM() );
		congealing.initTransformVec( this.stack.getNSlices(), congealing.nParameters );
		double[] tvecTemp = new double[ congealing.nParameters ];

		ImagePlus impOri = this.stack.duplicate();
		this.sampleIndex = this.stack.getNSlices();

		//this.stack.show();
	}

	public void addMirroredSamples( Congealing congealing, ImagePlus refStack ) {

		ImagePlus sampleBinned_mirror_y;
		ImagePlus sampleBinned_mirror_x;
		ImagePlus sampleBinned_mirror_xy;
		
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
		refStack.getStack().addSlice(sampleBinned_mirror_x.getTitle(), sampleBinned_mirror_x.getProcessor());
		refStack.getStack().addSlice(sampleBinned_mirror_y.getTitle(), sampleBinned_mirror_y.getProcessor());
		refStack.getStack().addSlice(sampleBinned_mirror_xy.getTitle(), sampleBinned_mirror_xy.getProcessor());

		// Add tranformation vectors
		int nParams = congealing.preTransformVec.get(0).length;
		double[] tvecTemp = new double[nParams];
		double[] lastTransformVec = congealing.transformVec.get(congealing.nImages - 2).clone();
		double[] lastTransformRealVec = congealing.transformRealVec.get(congealing.nImages - 1).clone();
		// 1e mirror X
		Arrays.fill(tvecTemp, 0.0);
		tvecTemp[congealing.INDEX_MIRRORX] = 1.0 / congealing.methodParameters[congealing.INDEX_MIRRORX];
		congealing.preTransformVec.add(tvecTemp);
		congealing.transformVec.add(lastTransformVec.clone());
		congealing.transformRealVec.add(lastTransformRealVec.clone());
		// 2e mirror Y
		Arrays.fill(tvecTemp, 0.0);
		tvecTemp[congealing.INDEX_MIRRORY] = 1.0 / congealing.methodParameters[congealing.INDEX_MIRRORY];
		congealing.preTransformVec.add(tvecTemp);
		congealing.transformVec.add(lastTransformVec.clone());
		congealing.transformRealVec.add(lastTransformRealVec.clone());
		// 3e mirror XY
		Arrays.fill(tvecTemp, 0.0);
		tvecTemp[congealing.INDEX_MIRRORX] = 1.0 / congealing.methodParameters[congealing.INDEX_MIRRORX];
		tvecTemp[congealing.INDEX_MIRRORY] = 1.0 / congealing.methodParameters[congealing.INDEX_MIRRORY];
		congealing.preTransformVec.add(tvecTemp);
		congealing.transformVec.add(lastTransformVec.clone());
		congealing.transformRealVec.add(lastTransformRealVec.clone());

	}

	public void run() {

		this.congealing = new Congealing();
		//congealing.setLog(this.log);
		congealing.setTRANSFORM("RIGID_MIRROR_XY");
		int nParameters = 5;

		// LOAD REFERENCE STACK, PROPERTIES, SAMPLE ----------------------------
		//  1) reference stack which is pre-binned and smoothed
		ImagePlus imp = null;
		if (param.IS_STACK_SET) {
			//this.log.log("Congealing reference stack already computed, get stack and properties from Main param");
			imp = param.getRefStack();
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
		congealing.initTransformVec(imp.getNSlices(), nParameters);
		congealing.nImages = congealing.stackProps.size();
		//  add sample
		addSample( congealing, imp );
		// ---------------------------------------------------------------------

		// PRE-WARPING ---------------------------------------------------------
		//this.log.log("Prewarping sample");

		IJ.log(" --- PreWarping");
		if (this.PREWARPING_METHOD == this.PREWARPING_POINTS) {
			imp = congealing.preWarping(imp, nPoints);
		}
		if (this.PREWARPING_METHOD == this.PREWARPING_LINE) {
			imp = congealing.alignmentAngle(imp, 0.0, nPoints);
		}
		// ---------------------------------------------------------------------


		// ADDITION OF MIRRORED SAMPLES ----------------------------------------
		addMirroredSamples( congealing, imp );
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
		congealing.nParameters = 5;
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
		congealing.scaleTransform( (double) congealing.binCongealing );
		IJ.saveAsTiff( this.alignedStack, this.alignedStackFile.getAbsolutePath() );

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

		this.preTransformVec = congealing.preTransformVec;
		this.transformVec = congealing.transformVec;
		this.transformRealVec = congealing.transformRealVec;

		congealing.saveTransformVecs( preTransformVecFile, transformVecFile, transformRealVecFile );
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
