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
	public static final Color[] CONSTANT_COLOR_LIST = {Color.green, new Color(0, 128, 0), Color.yellow, Color.red, Color.pink, Color.magenta, Color.gray};
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

	// REAL VARIABLES
	public File OUTPUT_FOLDER;
	public File OUTPUT_ROIS_FOLDER;
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
}
