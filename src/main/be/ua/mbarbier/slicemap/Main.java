/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap;

import main.be.ua.mbarbier.slicemap.lib.BiMap;
import static main.be.ua.mbarbier.slicemap.lib.Lib.prefixMapHeaders;
import main.be.ua.mbarbier.slicemap.lib.registration.BunwarpjParam;
import main.be.ua.mbarbier.slicemap.lib.registration.HarrisParam;
import main.be.ua.mbarbier.slicemap.lib.registration.SiftParam;
import ij.ImagePlus;
import java.awt.Color;
import java.io.File;
import java.util.LinkedHashMap;

/**
 *
 * @author mbarbier
 */
public class Main {

	// NAMES OF PARAMETERS
	/*
	public final static String PARAM_OUTPUT_FOLDER = "param_output_folder";
	public final static String PARAM_SAMPLE_FOLDER = "param_sample_folder";
	public final static String PARAM_APP_FOLDER = "param_app_folder";
    public final static String PARAM_INPUT_FOLDER = "param_input_folder";
	public final static String PARAM_PATTERN_ROI_FILES = "param_input_pattern_roi_files";
	public final static String PARAM_PATTERN_REF_FILES = "param_input_pattern_ref_files";
	public final static String PARAM_CONTAINS_REF_FILES = "param_input_contains_ref_files";
	public final static String PARAM_DOESNOTCONTAIN_REF_FILES = "param_input_doesnotcontain_ref_files";
    public final static String PARAM_FILE_REFERENCE_STACK = "param_input_file_reference_stack";
    public final static String PARAM_FILE_ALIGNED_REFERENCE_STACK = "param_input_file_aligned_reference_stack";
	public final static String PARAM_CONGEALING_NITERATIONS = "param_congealing_nIterations";
	public final static String PARAM_CONGEALING_NREFERENCES = "param_congealing_nReferences";
	public final static String PARAM_CONGEALING_BINCONGEALING = "param_congealing_binCongealing";
	public final static String PARAM_CONGEALING_NPOINTS = "param_congealing_nPoints";
	public final static String PARAM_CONGEALING_STACKBINNING = "param_congealing_stackbinning";
	public final static String PARAM_CONGEALING_SATURATED_PIXELS_PERCENTAGE = "param_congealing_saturated_pixels_percentage";
    public final static String PARAM_FILE_TRANSFORMVEC = "param_input_file_transformVec";
    public final static String PARAM_FILE_PRETRANSFORMVEC = "param_input_file_preTransformVec";
    public final static String PARAM_FILE_TRANSFORMREALVEC = "param_input_file_transformRealVec";
	public final static String PARAM_FILE_STACKPROPS = "param_input_file_stackprops";
    public final static String PARAM_FILE_ALIGNED_STACKPROPS = "param_input_file_aligned_stackprops";
    public final static String PARAM_LOAD_ALIGNED_STACK = "param_load_aligned_stack";
	*/

	// CONSTANT VARIABLES
	public static final Color[] CONSTANT_COLOR_LIST = {Color.green, new Color(0, 128, 0), Color.yellow, Color.red, Color.pink, Color.magenta, new Color(0, 128, 128), new Color(128, 128, 0), new Color(128, 0, 128), new Color(255, 128, 0), new Color(0, 128, 255), new Color(128, 255, 0), new Color(128, 0, 255), Color.gray, Color.green, new Color(0, 128, 0), Color.yellow, Color.red, Color.pink, Color.magenta, new Color(0, 128, 128), new Color(128, 128, 0), new Color(128, 0, 128), new Color(255, 128, 0), new Color(0, 128, 255), new Color(128, 255, 0), new Color(128, 0, 255), Color.gray, Color.green, new Color(0, 128, 0), Color.yellow, Color.red, Color.pink, Color.magenta, new Color(0, 128, 128), new Color(128, 128, 0), new Color(128, 0, 128), new Color(255, 128, 0), new Color(0, 128, 255), new Color(128, 255, 0), new Color(128, 0, 255), Color.gray, Color.green, new Color(0, 128, 0), Color.yellow, Color.red, Color.pink, Color.magenta, new Color(0, 128, 128), new Color(128, 128, 0), new Color(128, 0, 128), new Color(255, 128, 0), new Color(0, 128, 255), new Color(128, 255, 0), new Color(128, 0, 255), Color.gray};
	public static final String[] CONSTANT_SAMPLE_EXTENSIONS =		{"tif","tiff","dcm","fits","pgm","jpg","gif","bmp","png"};
	public static final String[] CONSTANT_REFERENCE_EXTENSIONS =	{"tif","tiff","dcm","fits","pgm","jpg","gif","bmp","png"};
    public final static String CONSTANT_SAMPLE_ID_LABEL = "sample_id";
    public final static String CONSTANT_REFERENCE_ID_LABEL = "ref_id";
	public final static String CONSTANT_NAME_REFERENCE_STACK = "reference_stack.tif";
	public final static String CONSTANT_NAME_TEMPLATE_POINTROI_IMAGE = "templatePointRoi.tif";
	public final static String CONSTANT_NAME_TEMPLATE_POINTROI_ROI = "templatePointRoi.zip";
    public final static String CONSTANT_SUBDIR_MONTAGE = "reference_images";
    public final static String CONSTANT_SUBDIR_ROI = "reference_rois";
    public final static String CONSTANT_SUBDIR_POINTROI = "reference_point_rois";
    public final static String CONSTANT_SUBDIR_REFERENCE_STACK = "reference_stack";
    public final static String CONSTANT_TRANSFORMVEC_LABEL = "transformVec";
    public final static String CONSTANT_PRETRANSFORMVEC_LABEL = "preTransformVec";
    public final static String CONSTANT_TRANSFORMREALVEC_LABEL = "transformRealVec";
    public final static String CONSTANT_STACKPROPS_LABEL = "stackProps";
    public final static String CONSTANT_ALIGNEDSTACK_LABEL = "alignedStack";
    public final static String CONSTANT_SUBDIR_ROI_CONGEALING = "congealing";
    public final static String CONSTANT_SUBDIR_ROI_ELASTIC = "elastic";
	public final static String CONSTANT_FILE_NAME_LOG = "javaLog.txt";
	public final static String CONSTANT_FILE_NAME_LOG_REGISTRATION = "registration.csv";
	public final static String CONSTANT_FILE_NAME_PREFIX_LOG_REGISTRATION = "registration_";
	public final static String CONSTANT_FILE_NAME_OUTPUT_OVERLAY = "overlayAnnotation.tif";
	public final static String CONSTANT_NAME_OUTPUT_TABLE_HEADER_SAMPLE_ID = "sample_id";
	public final static String CONSTANT_FILE_NAME_PREFIX_OUTPUT_TABLE = "outputFolderStructure_";
	public final static String CONSTANT_NAME_OUTPUT_TABLE_HEADER_REGION_CSV = "roi_file";
	public final static String CONSTANT_FILE_NAME_ROI_CSV = "regRoi_interpolation.csv";
	public final static int CONSTANT_MAX_PIXELS_FOR_PREPROCESSING = 10000;
	public final static double CONSTANT_SIGMA_RATIO = 0.005;// * ( (double) this.stackBinning ) / 16.0;
	public final static double CONSTANT_SATURATED_PIXELS_RATIO = 0.05;// * ( (double) this.stackBinning ) / 16.0;
	

	// REAL VARIABLES
	public boolean HEADLESS;
	public File OUTPUT_FOLDER;
	public File OUTPUT_ROIS_FOLDER;
	public File OUTPUT_ROIS_PATH_PROVIDED;
	public File SAMPLE_FOLDER;
	public File APP_FOLDER;
	public File APP_ELASTIC_FOLDER;
	public File APP_CONGEALING_FOLDER;
    public File INPUT_FOLDER;
	public String PATTERN_ROI_FILES;
	public String PATTERN_REF_FILES;
	public String CONTAINS_REF_FILES;
	public String DOESNOTCONTAIN_REF_FILES;
    public File FILE_REFERENCE_STACK;
    public String FILENAME_REFERENCE_STACK;
    public File FILE_ALIGNED_REFERENCE_STACK;
    public String FILENAME_ALIGNED_REFERENCE_STACK;
	public String FILENAME_PREFIX_REGISTERED_IMAGE;
	public String FILENAME_PREFIX_REGISTERED_COMPOSITE_IMAGE;
	public File FILE_SAMPLE;
	public String ID_SAMPLE;
	public String FILTER_FILE_NAME_SAMPLE;
	public String FILTER_FILE_NAME_REF;
	public String FORMAT_OUTPUT_GRAY_IMAGES;
	public int GENERAL_BINNING;
	// REFSTACK
	public double SIGMA_RATIO = Main.CONSTANT_SIGMA_RATIO;
	public double SATURATED_PIXELS_RATIO = Main.CONSTANT_SATURATED_PIXELS_RATIO;
	// PREWARPING
	public String PREWARPING_METHOD;
	// CONGEALING
	public int CONGEALING_NITERATIONS;
	public int CONGEALING_NREFERENCES;
	public int CONGEALING_NIMAGES;
	public int CONGEALING_NMIRRORSAMPLES;
	public int CONGEALING_BINCONGEALING;
	public int CONGEALING_NPOINTS;
	public int CONGEALING_STACKBINNING;
	public int CONGEALING_NPARAMETERS;
	public int CONGEALING_TRANSFORM;
	public String REGISTRATION_FEATURE_METHOD;
	// LABEL FUSION
	public String LABELFUSION_METHOD;
	//
	public double CONGEALING_SATURATED_PIXELS_PERCENTAGE;
    public File FILE_TRANSFORMVEC;
    public File FILE_PRETRANSFORMVEC;
    public File FILE_TRANSFORMREALVEC;
	public File FILE_STACKPROPS;
    public File FILE_ALIGNED_STACKPROPS;
    public boolean DO_LOAD_ALIGNED_STACK;

    public boolean DO_REGENERATE_REFSTACK;
	public boolean IS_STACK_SET = false;
	public boolean DO_SMOOTH_ROIS = false;

	// Large specific variables (should these be global?)
	double pixelSizeSample = 1.0;
	double pixelSizeRef = 1.0;
	LinkedHashMap< String, ImageProperties > stackProps;
	ImagePlus refStack;
	BiMap< String, Integer > idMap;
	SiftParam siftParam;
	HarrisParam harrisParam;
	BunwarpjParam bunwarpjParam;

	public BiMap<String, Integer> getIdMap() {
		return idMap;
	}

	public void setIdMap(BiMap<String, Integer> idMap) {
		this.idMap = idMap;
	}

	public SiftParam getSiftParam() {
		return siftParam;
	}

	public void setSiftParam(SiftParam siftParam) {
		this.siftParam = siftParam;
	}

	public HarrisParam getHarrisParam() {
		return harrisParam;
	}

	public void setHarrisParam(HarrisParam harrisParam) {
		this.harrisParam = harrisParam;
	}

	public BunwarpjParam getBunwarpjParam() {
		return bunwarpjParam;
	}

	public void setBunwarpjParam(BunwarpjParam bunwarpjParam) {
		this.bunwarpjParam = bunwarpjParam;
	}

	public LinkedHashMap<String, ImageProperties> getStackProps() {
		return stackProps;
	}

	public void setStackProps(LinkedHashMap<String, ImageProperties> stackProps) {
		this.stackProps = stackProps;
	}

	public ImagePlus getRefStack() {
		return refStack;
	}

	public void setRefStack(ImagePlus refStack) {
		this.refStack = refStack;
	}

	public static LinkedHashMap<String, Color> getDefaultColorMap() {

		LinkedHashMap<String, Color> colorMap = new LinkedHashMap<>();
		colorMap.put( "hp", Color.green);
		colorMap.put( "cx", new Color(0, 128, 0) );
		colorMap.put( "cb", Color.yellow);
		colorMap.put( "th", Color.red);
		colorMap.put( "bs", Color.pink);
		colorMap.put( "mb", Color.magenta);
		colorMap.put( "bg", Color.gray);
		
		return colorMap;
	}

	
	public LinkedHashMap< String, String > logParameters() {
		
		LinkedHashMap< String, String > map = new LinkedHashMap<>();
		map.put( Main.CONSTANT_SAMPLE_ID_LABEL, ID_SAMPLE );
		map.put( "file_sample", this.FILE_SAMPLE.getName() );
		map.put( "folder_sample", this.SAMPLE_FOLDER.getAbsolutePath() );
		map.put( "prewarping_nLandmarks", Integer.toString( this.CONGEALING_NPOINTS ) );
		map.put( "subset_nReferences", Integer.toString( this.CONGEALING_NREFERENCES ) );
		map.put( "binning", Integer.toString( this.CONGEALING_STACKBINNING ) );
		map.putAll( prefixMapHeaders( bunwarpjParam.getBunwarpjParamString(), "bunwarpj_" ) );
		if (this.REGISTRATION_FEATURE_METHOD == ElasticRegistration.METHOD_FEATURES_SIFT) { 
			map.putAll( prefixMapHeaders( siftParam.getSiftParamString(), "landmarks_sift_" ) );
		}
		if (this.REGISTRATION_FEATURE_METHOD == ElasticRegistration.METHOD_FEATURES_HARRIS) { 
			map.putAll( prefixMapHeaders( harrisParam.getHarrisParamString(), "landmarks_harris_" ) );
		}

		return map;
	}

	public Main readParameters( LinkedHashMap< String, String > mParam ) {
	
		Main param = new Main();
		param.CONGEALING_NREFERENCES = Integer.parseInt( mParam.get("param_congealing_nReferences") );
		param.CONGEALING_NITERATIONS = Integer.parseInt( mParam.get("param_congealing_nIterations") );
		//param.
		//param.CONGEALING_NIMAGES = ;???
		param.CONGEALING_BINCONGEALING = Integer.parseInt( mParam.get( "param_congealing_binCongealing" ) );
		
		return param;
	/*
		mParam.get("input_dir") = 										in_inputFolder,
		mParam.get("output_dir"=										FolderMap.output.registration,
		mParam.get("in_sourceId"=									in_sourceId,
		mParam.get("in_sourceName"=								sourceName,
		mParam.get("in_annotatorId"= 								in_annotatorId, 
		mParam.get("in_sourceRoiName"= 						in_sourceRoiName, 
		mParam.get("in_sourcePath"=								sourcePath, 
		mParam.get("in_sourceDir"=									outputFolder, 
		mParam.get("in_binning"=										in_Binning, 
		mParam.get("in_scale"= 										1.0 / in_Binning, 
		mParam.get("in_scaleRoi"=									0.125, 
		mParam.get("out_logPath"=									outputLogPath, 
		mParam.get("out_regRoiPath"=								outputRegRoiPath, 
		mParam.get("out_transfoDirectPath"= 					outputDirectTransfoPath, 
		mParam.get("out_transfoInversePath"=					outputInverseTransfoPath, 
		mParam.get("out_regPath"=									outputImageRegPath, 
			mParam.get("out_sourceOverlayPath"= 					outputSourceOverlayPath, 
			mParam.get("out_sourceOverlayAtlasPath"=			outputSourceAtlasOverlayPath, 
			mParam.get("out_compositePath"=						outputImageCompositePath,
			mParam.get("do_addBackground"=						"true", 
			mParam.get("do_sourceRoi"=								"false",
			mParam.get("do_sourceFromMontageDir"=			"false",
			param.CONGEALING_NREFERENCES = mParam.get("param_congealing_nReferences");
			mParam.get("param_congealing_nIterations"=		in_registration_congealing_nIterations,
			mParam.get("param_congealing_binCongealing"=	in_registration_congealing_binCongealing,
			mParam.get("param_bunwarpj_divWeight"=			param_bunwarpj_divWeight,
			mParam.get("param_bunwarpj_curlWeight"=			param_bunwarpj_curlWeight,
			mParam.get("param_bunwarpj_imageWeight"=		param_bunwarpj_imageWeight,
			mParam.get("param_bunwarpj_landmarkWeight"=	param_bunwarpj_landmarkWeight,
			mParam.get("param_bunwarpj_consistencyWeight"=param_bunwarpj_consistencyWeight,
			mParam.get("param_sift_initialSigma"=					param_sift_initialSigma,
			mParam.get("param_sift_steps"=							param_sift_steps,
			mParam.get("param_sift_fdBins"=							param_sift_fdBins,
			mParam.get("param_sift_fdSize"=							param_sift_fdSize,
			mParam.get("param_sift_rod"=								param_sift_rod,
			mParam.get("param_sift_maxEpsilon"=					param_sift_maxEpsilon,
			mParam.get("param_sift_minInlierRatio"=				param_sift_minInlierRatio,
			mParam.get("param_sift_modelIndex"=					param_sift_modelIndex
					
					
							this.param = new Main();
		param.PATTERN_REF_FILES = "^(.*?)\\.(tif|png)";
		param.CONTAINS_REF_FILES = "";
		param.DOESNOTCONTAIN_REF_FILES = ".zip";
		param.CONGEALING_STACKBINNING = 16;
		param.CONGEALING_NITERATIONS = 10;
		param.CONGEALING_BINCONGEALING = 1;
		param.CONGEALING_NPOINTS = 8;
		param.CONGEALING_SATURATED_PIXELS_PERCENTAGE = 0.05;
		param.FORMAT_OUTPUT_GRAY_IMAGES = ".tif";
		param.FILENAME_PREFIX_REGISTERED_IMAGE = "registered_";
		param.FILENAME_PREFIX_REGISTERED_COMPOSITE_IMAGE = "registered_composite_";
		param.REGISTRATION_FEATURE_METHOD = ElasticRegistration.METHOD_FEATURES_HARRIS;
		param.PREWARPING_METHOD = AffineAlign.PREWARPING_LINE;

		BunwarpjParam bunwarpjParam = new BunwarpjParam();
		SiftParam siftParam = new SiftParam();
		HarrisParam harrisParam = new HarrisParam();

		param.setBunwarpjParam( new BunwarpjParam() );
		param.setSiftParam( new SiftParam() );
		param.setHarrisParam( new HarrisParam() );

		File sampleFile = new File( mParam );
		File inputFile = new File( gdp.getNextString() );
		File outputFile = new File( gdp.getNextString() );
		File outputRoisFile = new File( outputFile.getAbsolutePath() + "/" + "roi" );
		File appFile = new File( outputFile.getAbsolutePath() + "/" + "debug" );
		File appFileCongealing = new File( appFile.getAbsolutePath() + "/" + "congealing" );
		File appFileElastic = new File( appFile.getAbsolutePath() + "/" + "elastic" );

		
		param.APP_FOLDER = appFile;
		param.APP_CONGEALING_FOLDER = appFileCongealing;
		param.APP_ELASTIC_FOLDER = appFileElastic;
		param.SAMPLE_FOLDER = sampleFile;
		param.INPUT_FOLDER = inputFile;
		param.OUTPUT_FOLDER = outputFile;
		param.OUTPUT_ROIS_FOLDER = outputRoisFile;
		param.FILE_REFERENCE_STACK = stackFile;
		param.FILENAME_REFERENCE_STACK = stackFile.getName();
		param.FILTER_FILE_NAME_SAMPLE = sampleFilter;
		param.DO_LOAD_ALIGNED_STACK = doStackAlign;
		param.DO_REGENERATE_REFSTACK = regenerateStack;
		File stackPropsFile = new File( param.INPUT_FOLDER.getAbsolutePath() + "/" + Main.CONSTANT_SUBDIR_REFERENCE_STACK + "/" + Main.CONSTANT_STACKPROPS_LABEL + "_" + Main.CONSTANT_NAME_REFERENCE_STACK + ".csv");
		param.FILE_STACKPROPS = stackPropsFile;
		param.FILE_TRANSFORMVEC = new File( param.APP_FOLDER.getAbsolutePath() + "/" + Main.CONSTANT_TRANSFORMVEC_LABEL + "_" +  Main.CONSTANT_NAME_REFERENCE_STACK + ".csv");
		param.FILE_PRETRANSFORMVEC = new File( param.APP_FOLDER.getAbsolutePath() + "/" + Main.CONSTANT_PRETRANSFORMVEC_LABEL + "_" + Main.CONSTANT_NAME_REFERENCE_STACK + ".csv");
		param.FILE_TRANSFORMREALVEC = new File( param.APP_FOLDER.getAbsolutePath() + "/" + Main.CONSTANT_TRANSFORMREALVEC_LABEL + "_" + Main.CONSTANT_NAME_REFERENCE_STACK + ".csv");
		File alignedStackFile = new File( param.APP_FOLDER.getAbsolutePath() + "/" + Main.CONSTANT_ALIGNEDSTACK_LABEL + "_" + Main.CONSTANT_NAME_REFERENCE_STACK );
		param.FILE_ALIGNED_REFERENCE_STACK = alignedStackFile;

		
				// ---------------------------------------------------------------------
		// Congealing
		// ---------------------------------------------------------------------
		param.CONGEALING_STACKBINNING = Integer.parseInt( gdp.getNextRadioButton() );
		param.CONGEALING_NPOINTS = (int) gdp.getNextNumber();
		param.CONGEALING_NITERATIONS = (int) gdp.getNextNumber();
		param.CONGEALING_NREFERENCES = (int) gdp.getNextNumber();
		//param.CONGEALING_BINCONGEALING = Integer.parseInt( gdp.getNextRadioButton() );
		// Landmarks for elastic registration
		param.REGISTRATION_FEATURE_METHOD = gdp.getNextChoice();
		// ---------------------------------------------------------------------
		
		// ---------------------------------------------------------------------
		// Elastic registration
		// ---------------------------------------------------------------------
		bunwarpjParam.setAccuracy_mode( gdp.getNextChoiceIndex() );
		//bunwarpjParam.setImg_subsamp_fact( (int) gdp.getNextNumber() );
		bunwarpjParam.setMin_scale_deformation( gdp.getNextChoiceIndex() );
		bunwarpjParam.setMax_scale_deformation( gdp.getNextChoiceIndex() );
		bunwarpjParam.setDivWeight( gdp.getNextNumber() );
		bunwarpjParam.setCurlWeight( gdp.getNextNumber() );
		bunwarpjParam.setLandmarkWeight( gdp.getNextNumber() );
		bunwarpjParam.setImageWeight( gdp.getNextNumber() );
		bunwarpjParam.setConsistencyWeight( gdp.getNextNumber() );
		bunwarpjParam.setStopThreshold( gdp.getNextNumber() );
		// ---------------------------------------------------------------------

		param.setBunwarpjParam( bunwarpjParam );
		param.setSiftParam( siftParam );
		param.setHarrisParam( harrisParam );
*/

					
	}
}
