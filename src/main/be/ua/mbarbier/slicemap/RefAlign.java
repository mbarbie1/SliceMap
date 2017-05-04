/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap;

//import be.ua.mbarbier.slicemap.lib.MBLog;
import main.be.ua.mbarbier.slicemap.lib.congealing.Congealing;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.binStack;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 *
 * @author mbarbier
 */
public class RefAlign {

	Main param;
	ImagePlus ref;
	ImagePlus stack;
	ImagePlus alignedStack;
	String outputFolder;
	String inputFolder;
	File outputFolderFile;
	File inputFolderFile;

    public ArrayList<double[]> transformVec;
    public ArrayList<double[]> preTransformVec;
    public ArrayList<double[]> transformRealVec;

    //MBLog log;
	String logFileName;

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

		this.logFileName = Main.CONSTANT_FILE_NAME_LOG;
		File logFile = new File( this.outputFolder + "/" + this.logFileName );
		//this.log = new MBLog(logFile.getAbsolutePath());
	}

	public void run() {

	// Congealing

        Congealing congealing = new Congealing();
        //congealing.setLog(this.log);
        congealing.setTRANSFORM("RIGID_MIRROR_XY");
        int nParameters = 5;

		//  1) reference stack which is pre-binned and smoothed
        //this.log.log("Load congealing reference stack: " + stackFile.getAbsolutePath());
        ImagePlus imp = IJ.openImage(stackFile.getAbsolutePath());
		//imp.show();

		congealing.setNImages(imp.getNSlices());
		//congealing.initRefRois(inputRoiFolder, inputRefFolder);
		int nPoints = param.CONGEALING_NPOINTS;
		congealing.initTransformVec(imp.getNSlices(), nParameters);

		// PRE-WARPING ---------------------------------------------------------
		//this.log.log("Prewarping sample");

		IJ.log(" --- PreWarping");
		if (this.PREWARPING_METHOD == this.PREWARPING_POINTS) {
			imp = congealing.preWarping(imp, nPoints);
		}
		if (this.PREWARPING_METHOD == this.PREWARPING_LINE) {
			imp = congealing.alignmentAngle(imp, 0.0, nPoints);
		}
		//imp.duplicate().show();

		// CONGEALING ----------------------------------------------------------

		int nReferences = param.CONGEALING_NREFERENCES;
		int nIterations = param.CONGEALING_NITERATIONS;
		int binCongealing = param.CONGEALING_BINCONGEALING;

		//this.log.log("START actual congealing with: nReferences = " + nReferences + ", nIterations = " + nIterations + ", binning congealing = " + binCongealing);
		congealing.binCongealing = binCongealing;
		IJ.log(" --- Binning for congealing = " + congealing.binCongealing);
		imp = binStack(imp, binCongealing);
		//imp.duplicate().show();

		IJ.log(" --- Normalizing the stack [0..1]");
		int nImages = imp.getNSlices();
		congealing.nImages = nImages;
		congealing.nParameters = 5;
		ImagePlus impNorm = IJ.createHyperStack("Stack 32 bit", imp.getWidth(), imp.getHeight(), 1, nImages, 1, 32);
		for (int i = 1; i < nImages + 1; i++) {
			ImageProcessor ipNormi = imp.getStack().getProcessor(i).convertToFloatProcessor();
			ipNormi.multiply( 1.0 / imp.getProcessor().maxValue() );
			impNorm.getStack().setProcessor(ipNormi, i);
			impNorm.getStack().setSliceLabel( imp.getStack().getSliceLabel(i), i);
		}
		// TODO
		//impOri.show();
		//impNorm.show();

		//IJ.log(" --- Transfomation vector of the 1e image");
		//double[] tvec = congealing.transformVec.get(0);
		//IJ.log("NORMALISED tmat(j) = tx: " + tvec[congealing.INDEX_TRANSLATION_X] + ", ty: " + tvec[congealing.INDEX_TRANSLATION_Y] + ", angle: " + tvec[congealing.INDEX_ROTATION] + ", mirror: " + tvec[congealing.INDEX_MIRRORY]);

		IJ.log(" --- Run the actual congealing algorithm with #iterations = " + nIterations);
		congealing.runCongealing(impNorm, nIterations, congealing.TRANSFORM, congealing.methodParameters);
		this.alignedStack = congealing.getAlignedStack();
		for (int i = 1; i < nImages + 1; i++) {
			this.alignedStack.getStack().setSliceLabel( imp.getStack().getSliceLabel(i), i);
		}
		//this.alignedStack.duplicate().show();
		//IJ.saveAsTiff( this.alignedStack, this.alignedStackFile.getAbsolutePath() );

		this.preTransformVec = congealing.preTransformVec;
		this.transformVec = congealing.transformVec;
		this.transformRealVec = congealing.transformRealVec;

		congealing.saveTransformVecs( preTransformVecFile, transformVecFile, transformRealVecFile );
		//congealing.saveStackProps( stackPropsFile );
		//Congealing.saveStackProps(File stackPropsFile, LinkedHashMap< String, Congealing.ImageProperties > stackProps)
		// congealing.
		// writeCsv( ArrayList<LinkedHashMap<String, String>> al, String separator, String filePath )
	}

	/**
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {

		// set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = RefAlign.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        new ImageJ();

		RefAlign refAlign = new RefAlign();
		refAlign.PREWARPING_METHOD = refAlign.PREWARPING_LINE;

		Main param = new Main();
		param.INPUT_FOLDER = new File("d:/p_prog_output/slicemap/input");
		param.OUTPUT_FOLDER = new File("d:/p_prog_output/slicemap/output_ref_align");
		param.CONGEALING_NITERATIONS = 20;
		param.CONGEALING_NREFERENCES = 5;
		param.CONGEALING_BINCONGEALING = 2;
		param.CONGEALING_NPOINTS = 8;
		param.FILE_REFERENCE_STACK = new File("Beerse21.tif");
		refAlign.init( param );

        IJ.log("START RUN plugin");
		refAlign.run();
        IJ.log("END RUN plugin");
	}
}
