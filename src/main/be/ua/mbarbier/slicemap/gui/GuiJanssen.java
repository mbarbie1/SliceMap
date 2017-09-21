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
package main.be.ua.mbarbier.slicemap.gui;

import main.be.ua.mbarbier.slicemap.AffineAlign;
import main.be.ua.mbarbier.slicemap.AffineAnnotation;
import main.be.ua.mbarbier.slicemap.ElasticRegistration;
import main.be.ua.mbarbier.slicemap.ImageProperties;
import main.be.ua.mbarbier.slicemap.Main;
import main.be.ua.mbarbier.slicemap.RefStack;
import main.be.ua.mbarbier.slicemap.lib.BiMap;
import static main.be.ua.mbarbier.slicemap.lib.Lib.convertMapDoubleToString;
import static main.be.ua.mbarbier.slicemap.lib.Lib.getMeanMap;
import static main.be.ua.mbarbier.slicemap.lib.Lib.prefixMapHeaders;
import main.be.ua.mbarbier.slicemap.lib.LibIO;
import static main.be.ua.mbarbier.slicemap.lib.LibIO.writeCsv;
import main.be.ua.mbarbier.slicemap.lib.Timers;
import main.be.ua.mbarbier.slicemap.lib.registration.BunwarpjParam;
import main.be.ua.mbarbier.slicemap.lib.registration.HarrisParam;
import main.be.ua.mbarbier.slicemap.lib.registration.SiftParam;
import main.be.ua.mbarbier.slicemap.lib.roi.LabelFusion;
import static main.be.ua.mbarbier.slicemap.lib.roi.LabelFusion.getInterpolationMap;
import static main.be.ua.mbarbier.slicemap.lib.roi.LabelFusion.majorityVoting;
import main.be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.getConfidenceBandOverlayImage;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.getMapOfLists;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.getOverlayImage;
import main.be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation;
import static main.be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.excludeOutlierRois;
import static main.be.ua.mbarbier.slicemap.lib.roi.RoiMap.mapInvert;
import main.be.ua.mbarbier.slicemap.lib.transform.TransformCongealing;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import main.be.ua.mbarbier.slicemap.CurationAnnotationJanssen;
import main.be.ua.mbarbier.slicemap.Curation_Annotation;
import static main.be.ua.mbarbier.slicemap.lib.LibIO.readCsv;
import net.imglib2.realtransform.AffineTransform2D;

/**
 *
 * @author mbarbier
 */
public class GuiJanssen {

	Main param;
	private static final Logger logger = Logger.getLogger( Gui.class.getName() );
	boolean DEBUG = false;
	String platform = "columbus"; // platform = "columbus", "MB_lap", "MB_janssen"

	/**
	 * Constructor: defines the dialog of SliceMap
	 */
	public GuiJanssen() {

		DEBUG = true;

		// DEFAULT PARAMETERS
		this.param = new Main();
		this.param.HEADLESS = false;
		param.PATTERN_REF_FILES = "^(.*?)\\.(tif|png)";
		param.CONTAINS_REF_FILES = "";
		param.DOESNOTCONTAIN_REF_FILES = ".zip";
		param.CONGEALING_STACKBINNING = 16;
		param.CONGEALING_NITERATIONS = 10;
		param.CONGEALING_NREFERENCES = 5;
		param.CONGEALING_BINCONGEALING = 1;
		param.CONGEALING_NPOINTS = 8;
		param.CONGEALING_SATURATED_PIXELS_PERCENTAGE = 0.05;
		param.FORMAT_OUTPUT_GRAY_IMAGES = ".tif";
		param.FILENAME_PREFIX_REGISTERED_IMAGE = "registered_";
		param.FILENAME_PREFIX_REGISTERED_COMPOSITE_IMAGE = "registered_composite_";
		param.REGISTRATION_FEATURE_METHOD = ElasticRegistration.METHOD_FEATURES_HARRIS;
		param.PREWARPING_METHOD = AffineAlign.PREWARPING_LINE;
		param.setBunwarpjParam( new BunwarpjParam() );
		param.setSiftParam( new SiftParam() );
		param.setHarrisParam( new HarrisParam() );

		String filePath = "G:/triad_workflow/referenceLibraries.csv";
		if ( DEBUG ) {
			filePath = "G:/triad_workflow/referenceLibraries.csv";
		} else {
			filePath = "Y:/support_files/tau_analysis/referenceLibraries.csv";
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

		GenericDialogPlus gdp = new GenericDialogPlus("SliceMap: Automated annotation of fluorescent brain slices");
		gdp.addHelp( "https://gitlab.com/mbarbie1/SliceMap" );
		gdp.addChoice( "Select the reference library:", libraryLabelList, libraryLabelList[0] );
		
		// PARAMETER INPUT
		if (DEBUG) {
			gdp.addDirectoryField( "Sample folder", "G:/triad_workflow/samples" );
			gdp.addDirectoryField( "Output folder", "G:/triad_workflow/output" );
		} else {
			gdp.addDirectoryField( "Sample folder", "" );
			gdp.addDirectoryField( "Output folder", "" );
		}
		gdp.addStringField("sample name contains", "");

		gdp.showDialog();
		if ( gdp.wasCanceled() ) {
			return;
		}

		// EXTRACTION OF PARAMETERS FROM DIALOG
		String selectedLibrary = gdp.getNextChoice();
		String selectedLibraryPath = selectedLibrary;
		IJ.log("START RUN SliceMap Janssen");
		File inputFile = new File( libraryPathMap.get( selectedLibraryPath ) );

		File sampleFile = new File( gdp.getNextString() );
		File outputFile = new File( gdp.getNextString() );
		File appFile = new File( outputFile.getAbsolutePath() + "/" + "debug" );
		File outputRoisFile = new File( appFile.getAbsolutePath() + "/" + "roi" );
		File appFileCongealing = new File( appFile.getAbsolutePath() + "/" + "congealing" );
		File appFileElastic = new File( appFile.getAbsolutePath() + "/" + "elastic" );

		outputRoisFile.mkdirs();
		appFile.mkdirs();
		appFileCongealing.mkdirs();
		appFileElastic.mkdirs();
		String sampleFilter = gdp.getNextString();
		boolean regenerateStack = false;//gdp.getNextBoolean();
		// Check whether image file exists:
		File stackFile = new File( inputFile.getAbsolutePath() + "/" + Main.CONSTANT_SUBDIR_REFERENCE_STACK + "/" + Main.CONSTANT_NAME_REFERENCE_STACK);
		boolean doStackGenerate = false;
		boolean doStackAlign = false;
		if ( stackFile != null ) {
			if ( !stackFile.exists() ) {
				doStackGenerate = true;
			} else {
				doStackGenerate = false;
			}
		}

		param.APP_FOLDER = appFile;
		param.APP_CONGEALING_FOLDER = appFileCongealing;
		param.APP_ELASTIC_FOLDER = appFileElastic;
		param.SAMPLE_FOLDER = sampleFile;
		param.INPUT_FOLDER = inputFile;
		param.OUTPUT_FOLDER = appFile;// TODO This is a trick
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
		
		//try {
			run();
		//} catch(Exception e) {
		//	IJ.log( e.getMessage() );
		//}
		
		gdp = new GenericDialogPlus("SliceMap: Automated annotation of fluorescent brain slices");
		gdp.addHelp( "https://gitlab.com/mbarbie1/SliceMap" );
		gdp.addCheckbox( "Run Annotation Curation procedure?", true);
		gdp.showDialog();
		if ( gdp.getNextBoolean() ) {
			
			// constants
			String outputNamePrefix = "roi_";
			String tempStackFileName = "curation_stack";
			String tempStackPropsFileName = "curation_stack_props";
			boolean overwriteRois = true;
			
			IJ.log("START RUN Annotation Curation Janssen");
			String[] nameListSplit = regionNameListMap.get( selectedLibrary ).split(";");
			ArrayList< String > roiNameList = new ArrayList<>();
			for ( String roiName : nameListSplit ) {
				roiNameList.add(roiName);
			}
			stackFile = new File( appFile.getAbsolutePath() + File.separator + tempStackFileName + ".tif" );
			stackPropsFile = new File( appFile.getAbsolutePath() + File.separator + tempStackPropsFileName + ".csv" );
			File inputRoiFile = new File( outputRoisFile.getAbsolutePath() );
			CurationAnnotationJanssen ca = new CurationAnnotationJanssen();
			ca.setRoiNameList( roiNameList );
			ca.processFolder( sampleFile, inputRoiFile, outputRoisFile, stackFile, stackPropsFile, outputNamePrefix, overwriteRois );
			IJ.log("END Annotation Curation Janssen");
		}

		
	}

	/**
	 * The actual workhorse of the SliceMap plugin, runs the algorithm using provided parameters from the param variable
	 * 
	 */
	public void run() {

		IJ.log( "SliceMap version: 1.0.a" );

		boolean doStackGenerate = true;

		IJ.log("sampleFile = " + param.SAMPLE_FOLDER.getAbsolutePath() );
		IJ.log("inputFile = " + param.INPUT_FOLDER.getAbsolutePath() );
		IJ.log("appFile = " + param.APP_FOLDER.getAbsolutePath() );
		IJ.log("outputFile = " + param.OUTPUT_FOLDER.getAbsolutePath() );
		Timers timers = new Timers();

		RefStack rs = new RefStack();
		if ( doStackGenerate ) {
			IJ.log("START RUN refStack");
			rs.init( param );
			rs.run();
			param.IS_STACK_SET = true;
			param.setRefStack( rs.getStack() );
			param.setStackProps( rs.getStackProps() );
			IJ.log("END RUN refStack");
			//rs.getStack().duplicate().show();
		}
		int maxSizeX = rs.getMaxSizeX();
		int maxSizeY = rs.getMaxSizeY();

		BiMap<String, Integer> idMap = new BiMap();
		LinkedHashMap< String, ImageProperties > refStackProps = new LinkedHashMap< String, ImageProperties >();
		ArrayList< String > refList = new ArrayList<>();
		int indexTemp = 0;
		for ( String key : param.getStackProps().keySet() ) {
			ImageProperties props = param.getStackProps().get(key);
			indexTemp++;
			idMap.put( props.id, indexTemp );
			refStackProps.put( props.id, props);
			refList.add(props.id);
		}
		param.setIdMap(idMap);
		int nReferenceImages = param.getRefStack().getNSlices();
		int lastRefIndex = nReferenceImages;

		ArrayList<File> sampleFileList = LibIO.findFiles( param.SAMPLE_FOLDER );
		LinkedHashMap< String, File > sampleFileMap = new LinkedHashMap<>();
		for ( File file : sampleFileList ) {
			String fileName = file.getName();
			String sliceName;
			if (fileName.contains(".")) {
				sliceName = fileName.substring(0,fileName.lastIndexOf("."));
			} else {
				sliceName = fileName;
			}
			//for ( sliceName.endsWith(sliceName) )
			if ( sliceName.contains( param.FILTER_FILE_NAME_SAMPLE ) ) {
				sampleFileMap.put(sliceName, file);
			}
		}

		IJ.log("START RUN prepare output stack");
		int nChannels = 3; 
		int nSlices = sampleFileMap.keySet().size();
		int nFrames = 1;
		int width = param.getRefStack().getWidth();
		int height = param.getRefStack().getHeight();
        ImagePlus outputOverlayStack = IJ.createHyperStack( "Annotated samples", width, height, nChannels, nSlices, nFrames, 24 );
		int outputSampleIndex = 0;
		IJ.log("END RUN annotate");

		IJ.log("START RUN prepare log output");
		LinkedHashMap< String, LinkedHashMap< String, String > > summary_all = new LinkedHashMap<>();
		LinkedHashMap< String, LinkedHashMap< String, String > > summary_error = new LinkedHashMap<>();
		LinkedHashMap< String, LinkedHashMap< String, String > > summary_param = new LinkedHashMap<>();
		LinkedHashMap< String, LinkedHashMap< String, String > > summary_time = new LinkedHashMap<>();
		ArrayList< LinkedHashMap< String, String > > summary = new ArrayList<>();
		IJ.log("END RUN prepare log output");

		//ArrayList< String > refListOld = new ArrayList<>(refList);
		for (String key : sampleFileMap.keySet()) {

			
			try {
			
				timers.addTimer( "run_sample" );
				timers.addTimer( "refStack_generation" );
				if ( param.DO_REGENERATE_REFSTACK ) {
					// -----------------------------------------------------------------
					// One slice out approach: remove slice from:
					//		refStack
					//		idMap
					//		stackProps
					// -----------------------------------------------------------------
					rs = new RefStack();
					IJ.log("START RUN refStack");
					param.DOESNOTCONTAIN_REF_FILES = key;
					rs.init( param );
					rs.run( maxSizeX, maxSizeY );
					param.IS_STACK_SET = true;
					param.setRefStack( rs.getStack() );
					param.setStackProps( rs.getStackProps() );
					IJ.log("END RUN refStack");

					idMap = new BiMap();
					refStackProps = new LinkedHashMap< String, ImageProperties >();
					refList = new ArrayList<>();
					indexTemp = 0;
					for ( String key2 : param.getStackProps().keySet() ) {
						ImageProperties props = param.getStackProps().get(key2);
						indexTemp++;
						idMap.put( props.id, indexTemp );
						refStackProps.put( props.id, props);
						refList.add(props.id);
					}
					param.setIdMap(idMap);
					nReferenceImages = param.getRefStack().getNSlices();
					lastRefIndex = nReferenceImages;
					timers.getTimer( "refStack_generation" ).updateTime();
					// -----------------------------------------------------------------
					// -----------------------------------------------------------------
					// -----------------------------------------------------------------
				}

				IJ.log("---------------- START " + key + " START ---------------- ");
				outputSampleIndex++;
				//annotation.run( annotation.sampleFileMap.get(key), key );
				param.FILE_SAMPLE = sampleFileMap.get(key);
				param.ID_SAMPLE = key;
				IJ.log("START RUN align");
				timers.addTimer( "congealing_registration" );
				AffineAlign align = new AffineAlign();
				align.PREWARPING_METHOD = param.PREWARPING_METHOD;
				align.init(param);
				align.run();
				timers.getTimer( "congealing_registration" ).updateTime();
				IJ.log("END RUN align");

				IJ.log("START RUN annotate");
				timers.addTimer( "congealing_annotation" );
				int bestSampleIndex = align.getBestSampleIndex();
				LinkedHashMap< String, ImageProperties > stackProps = new LinkedHashMap< String, ImageProperties >();
				for ( ImageProperties props : align.getCongealing().stackProps ) {
					stackProps.put( props.id, props);
				}
				ImageProperties sampleProps = align.getCongealing().stackProps.get(bestSampleIndex);
				stackProps.put("sample", sampleProps);

				TransformCongealing transformCongealing = new TransformCongealing( align.getCongealing().preTransformMat, align.getCongealing().transformMat, bestSampleIndex, lastRefIndex, stackProps );
				LinkedHashMap< String, AffineTransform2D > preTransformVec = transformCongealing.getPreTransformVec();
				LinkedHashMap< String, AffineTransform2D > transformVec = transformCongealing.getTransformVec();
				LinkedHashMap< String, LinkedHashMap< String, Roi > > roiMapList = AffineAnnotation.annotate( align.getStack(), align.getCongealing().stackProps.get( bestSampleIndex-1 ).id, align.getSampleRoi(), transformCongealing, param);
				timers.getTimer( "congealing_annotation" ).updateTime();
				IJ.log("END RUN annotate");

				IJ.log("START RUN sorted annotate");
				timers.addTimer( "alignment_sorting" );
				int[] sortedIndices = AffineAnnotation.sortAnnotate( align.getStack(), align.getAlignedStack(), align.getCongealing().stackProps.get( bestSampleIndex-1 ).id, lastRefIndex, stackProps, roiMapList, param, idMap );// param.getIdMap() );// idMap );
				ArrayList<String> refListSorted = new ArrayList<>();
				for ( int i = lastRefIndex - param.CONGEALING_NREFERENCES; i < lastRefIndex; i++ ) {
					// lastRefIndex = one-based
					int index = sortedIndices[i]; // sortedIndices is zero-based? should be.
					refListSorted.add( idMap.getKey(index+1) );
				}
				timers.getTimer( "alignment_sorting" ).updateTime();
				IJ.log("END RUN sorted annotate");

				IJ.log("START RUN elastic registration");
				timers.addTimer( "elastic_registration" );
				//align.getStack().duplicate().show();
				IJ.save( align.getAlignedStack(), param.APP_CONGEALING_FOLDER + "/" + "alignedReferencesAndSampleMirrors" + "_" + param.ID_SAMPLE + ".tif" );
				ElasticRegistration elasticRegistration = new ElasticRegistration( param, align.getStack(), align.getAlignedStack(), sortedIndices, stackProps, idMap, align.getCongealing().stackProps.get( bestSampleIndex-1 ).id, nReferenceImages );
				elasticRegistration.setTransfosCongealing(preTransformVec, transformVec);
				elasticRegistration.runSorted(refListSorted);
				ImagePlus compositeElastic = elasticRegistration.getRefSampleComposite();
				IJ.save( compositeElastic, param.APP_ELASTIC_FOLDER + "/" + "elasticComposite" + "_" + param.ID_SAMPLE + ".tif" );
				timers.getTimer( "elastic_registration" ).updateTime();
				IJ.log("END RUN elastic registration");

				IJ.log("START RUN registration error");
				timers.addTimer( "registration_error" );
				LinkedHashMap< String, LinkedHashMap< String, Double> > errorMap = elasticRegistration.getErrorList();
				LinkedHashMap< String, Double> sample_errorMap = getMeanMap( errorMap );
				summary_error.put( param.ID_SAMPLE, convertMapDoubleToString( sample_errorMap ) );
				// Convert to arraylist<String,String> with ref names for writing to file
				timers.getTimer( "registration_error" ).updateTime();
				IJ.log("END RUN registration error");

				IJ.log("START RUN elastic annotation");
				timers.addTimer( "elastic_annotation" );
				// Map< roiImage, Map< roiName, Roi > >
				LinkedHashMap< String, LinkedHashMap< String, Roi > > roiMapList_elastic_sorted = elasticRegistration.annotate( refListSorted );
				timers.getTimer( "elastic_annotation" ).updateTime();
				IJ.log("END RUN elastic annotation");

				IJ.log("START RUN ROI interpolation");
				timers.addTimer( "region_label_fusion" );
				// Probability images (or other types of confidence intervals?)
				ImagePlus sample = new ImagePlus( "sample", align.getStack().getStack().getProcessor( align.getStack().getNSlices() ).duplicate() );
				// Obtain probability maps for a the regions
				LinkedHashMap< String, ArrayList<Roi> > roiMapArray_elastic = getMapOfLists( roiMapList_elastic_sorted );
				//
				// LinkedHashMap< String, ImagePlus > probMapSubset = getProbabilityMap( sample.getWidth(), sample.getHeight(), roiMapArray_elastic, true );
				// Map< roiName, Map< roiImage, Roi > >
				LinkedHashMap< String, LinkedHashMap< String, Roi > > roiMapList_elastic_sorted_inverse = mapInvert( roiMapList_elastic_sorted );
				double halfDist = 0.1;

				/*
				*	Temporary solution to find out whether we can have the simple probability the same as with the isolines:
				*		(1) Sum probability image
				*		(2) 0.5 probability ROI (interpolated ROI)
				*		(3) Exclude ROIs outside the interpolated ROI
				*		(4) Continue with the rest of the ROIs for the isolines
				*/
				// SIMPLE SUM OF THE MASKS: THE PROBABILITY
				LinkedHashMap< String, ImagePlus > probMapSubset = majorityVoting( sample.getWidth(), sample.getHeight(), roiMapList_elastic_sorted_inverse, LabelFusion.METHOD_PROBABILITY_SUM, halfDist, roiMapList_elastic_sorted.size(), false );
				// SIMPLE SUM OF THE MASKS: THE INTERPOLATED ROI
				LinkedHashMap<String, Roi > roiInterpolationMapSubset;
				roiInterpolationMapSubset = getInterpolationMap( probMapSubset, LabelFusion.METHOD_LABELFUSION_THRESHOLD, false );
				LinkedHashMap< String, LinkedHashMap< String, Roi > > roiMapList_elastic_sorted_inverse_reduced = new LinkedHashMap<>();
				double overlapPercentage = 0.5;
				for ( String roiName : roiInterpolationMapSubset.keySet() ) {
					Roi roiInterp = roiInterpolationMapSubset.get( roiName );
					LinkedHashMap< String, Roi > roiMap = roiMapList_elastic_sorted_inverse.get(roiName);
					LinkedHashMap< String, Roi > roisReduced = excludeOutlierRois( roiMap, roiInterp, overlapPercentage );
					//LinkedHashMap< String, Roi > roisReduced = excludeOutlierRois( roiMap, roiInterp );
					roiMapList_elastic_sorted_inverse_reduced.put( roiName, roisReduced );
				}
				LinkedHashMap< String, ImagePlus > probMapSubset_reduced = majorityVoting( sample.getWidth(), sample.getHeight(), roiMapList_elastic_sorted_inverse_reduced, LabelFusion.METHOD_PROBABILITY_SUM, halfDist, roiMapList_elastic_sorted_inverse_reduced.size(), true );
				roiInterpolationMapSubset = getInterpolationMap( probMapSubset_reduced, LabelFusion.METHOD_LABELFUSION_THRESHOLD, true );
				ImagePlus impOverlaySubset;
				impOverlaySubset = getOverlayImage( roiInterpolationMapSubset, sample ).duplicate();
				//impOverlaySubset.show();

				timers.getTimer( "region_label_fusion" ).updateTime();
				IJ.log("END RUN ROI interpolation");

				IJ.log("START RUN ROI confidence interval");
				timers.addTimer( "confidence_interval" );
				RoiInterpolation roiInterpolation = new RoiInterpolation();
				LinkedHashMap<String, Roi > roiCurveMap = new LinkedHashMap<>();
				LinkedHashMap<String, Roi > roiBandInnerMap = new LinkedHashMap<>();
				LinkedHashMap<String, Roi > roiBandOuterMap = new LinkedHashMap<>();
				LinkedHashMap<String, ImageProcessor > roiBandMaskMap = new LinkedHashMap<>();
				LinkedHashMap<String, LinkedHashMap<String, Roi > > ciMap = new LinkedHashMap<>();

				LinkedHashMap< String, ArrayList< Roi > > roiMapArray_elastic_copy = new LinkedHashMap<>();
				boolean debug = false;

				//LinkedHashMap< String, LinkedHashMap< String, Roi > > roiInterpolationMapSubset_2 = new LinkedHashMap<>();
				for ( String roiName : roiMapList_elastic_sorted_inverse_reduced.keySet() ) {

					ImagePlus empty = IJ.createImage("", sample.getWidth(), sample.getHeight(), 1, 8);

					// TODO: temporary fix for inconsistency in interpolating methods for confidence contour and ROI interpolation as resulting ROI 
					LinkedHashMap< String, Roi > rois = new LinkedHashMap<>();
					rois.putAll( roiMapList_elastic_sorted_inverse_reduced.get(roiName) );
					//ArrayList< Roi > rois = roiMapArray_elastic.get(roiName);
					ArrayList< Double > ws;

					ImagePlus prob = probMapSubset_reduced.get( roiName );
					// remove roi's which don't overlap with the mask?
					Roi roiInterp = roiInterpolationMapSubset.get( roiName );
					rois = excludeOutlierRois( rois, roiInterp, 0.5 );

					//roiMapArray_elastic_copy.put( roiName, rois );
					//ArrayList< RoiInterpolation.WeightRoi > wrois = new ArrayList<>();
					//for ( Roi roi : rois ) {
					//	RoiInterpolation.WeightRoi wr = new RoiInterpolation.WeightRoi( roi, 1 );
					//	wrois.add(wr);
					//}
					//roiInterpolation.roiConfidenceProbability( empty.duplicate(), wrois, 0.63 );
					roiInterpolation.roiConfidenceProbability( prob, 0.63 );
					if ( roiInterpolation.roiCurve != null ) {
						roiCurveMap.put( roiName, roiInterpolation.roiCurve );
						roiBandInnerMap.put( roiName, roiInterpolation.roiBandInner );
						roiBandOuterMap.put( roiName, roiInterpolation.roiBandOuter );
						LinkedHashMap<String, Roi > ciBand = new LinkedHashMap<>();
						ciBand.put( "mean", roiInterpolation.roiCurve );
						ciBand.put( "inner", roiInterpolation.roiBandInner );
						ciBand.put( "outer", roiInterpolation.roiBandOuter );
						ciMap.put( roiName, ciBand );
						double bgValue = 0.0;
						double fgValue = 1.0;
					}
				}

				IJ.log("START save ROIs as ImageJ zip of ROIs file:" + param.OUTPUT_ROIS_FOLDER.getAbsolutePath() );
				LibRoi.saveRoiMap( roiCurveMap, param.CONGEALING_STACKBINNING, stackProps.get("sample"), null, param.ID_SAMPLE, param.OUTPUT_ROIS_FOLDER.getAbsolutePath(), "" );
				IJ.log("END save ROIs as ImageJ zip of ROIs file");

				// -----------------------------------------------------------------
				// Check whether there is a destination for the output ROIs to go, 
				// This is a ugly way to incorporate the Columbus workflow
				// -----------------------------------------------------------------
				param.OUTPUT_ROIS_PATH_PROVIDED = null;
				String outputFolderStructure_csvPath = param.FILE_SAMPLE.getAbsolutePath();
				int dotIndex = outputFolderStructure_csvPath.lastIndexOf('.');
				if ( dotIndex >= 0 ) {
					outputFolderStructure_csvPath = outputFolderStructure_csvPath.substring(0,dotIndex) + ".csv";
				}
				File outputFolderStructure_csv = new File( outputFolderStructure_csvPath );
				if ( Files.exists( outputFolderStructure_csv.toPath() ) ) {
					ArrayList<LinkedHashMap<String, String>> outputFolderMapList = LibIO.readCsv( outputFolderStructure_csv.getAbsolutePath(), "", ",");
					LinkedHashMap<String, String> outputFolderMap = outputFolderMapList.get(0);
					param.OUTPUT_ROIS_PATH_PROVIDED = new File( param.OUTPUT_FOLDER + "/" + outputFolderMap.get( Main.CONSTANT_NAME_OUTPUT_TABLE_HEADER_REGION_CSV ) );
					if ( param.OUTPUT_ROIS_PATH_PROVIDED != null ) {
						// If the output folder does not exist, generate it
						param.OUTPUT_ROIS_PATH_PROVIDED.getAbsoluteFile().getParentFile().mkdirs();
						//File (param.SAMPLE_FOLDER + "/" + )
						//param.OUTPUT_ROIS_PATH_PROVIDED =  + "/" + Main.CONSTANT_FILE_NAME_ROI_CSV;
//						LibRoi.saveRoiMapAsCsv( roiCurveMap, new File( param.OUTPUT_ROIS_PATH_PROVIDED.getAbsolutePath() ), param.ID_SAMPLE, param.CONGEALING_STACKBINNING, param.CONGEALING_STACKBINNING * sample.getWidth(), param.CONGEALING_STACKBINNING * sample.getHeight() );
						IJ.log("START save ROIs as csv file:" + param.OUTPUT_ROIS_PATH_PROVIDED.getAbsolutePath());
						LibRoi.saveRoiMapAsCsv( roiCurveMap, new File( param.OUTPUT_ROIS_PATH_PROVIDED.getAbsolutePath() ), param.ID_SAMPLE, param.CONGEALING_STACKBINNING, param.CONGEALING_STACKBINNING * sample.getWidth(), param.CONGEALING_STACKBINNING * sample.getHeight(), stackProps.get("sample") );
						IJ.log("END save ROIs as csv file");
					} else {
					}
				}

				ImagePlus overlayConfidence = getConfidenceBandOverlayImage( ciMap, sample.duplicate() );
				//overlayConfidence.show();
				timers.getTimer( "confidence_interval" ).updateTime();
				IJ.log("END RUN ROI confidence interval");

				IJ.log("START RUN adding images to output image stack");
				nChannels = outputOverlayStack.getNChannels();
				outputOverlayStack.getStack().setProcessor( sample.getProcessor().convertToRGB(), nChannels * outputSampleIndex-2 );
				outputOverlayStack.getStack().setProcessor( impOverlaySubset.getProcessor().convertToRGB(), nChannels * outputSampleIndex-1 );
				outputOverlayStack.getStack().setProcessor( overlayConfidence.getProcessor().convertToRGB(), nChannels * outputSampleIndex );
				IJ.log("END RUN adding images to output image stack");

				IJ.log("START RUN output parameters/errors/log");
				ArrayList< LinkedHashMap< String, String > > logList = new ArrayList<>();
				for ( String refId : errorMap.keySet() ) {
					LinkedHashMap< String, String > logRow = new LinkedHashMap<>();
					LinkedHashMap< String, String > errorRow = convertMapDoubleToString( errorMap.get( refId ) );

					logRow.put( Main.CONSTANT_SAMPLE_ID_LABEL, param.ID_SAMPLE );
					logRow.put( Main.CONSTANT_REFERENCE_ID_LABEL, refId );
					//logRow.putAll( param );
					logRow.putAll( errorRow );
					// Add the total row to the list
					logList.add( logRow );
				}
				timers.getTimer( "run_sample" ).updateTime();
				String startTime = timers.getTimer( "run_sample" ).getTimeStartString();
				String stopTime = timers.getTimer( "run_sample" ).getTimeStopString();

				LinkedHashMap< String, String > timeMap = timers.getStringMap();
				timeMap.put( "stamp_start", startTime );
				timeMap.put( "stamp_end", stopTime );
				summary_time.put( key, timeMap );
				LinkedHashMap< String, String > logMap = new LinkedHashMap<>();
				LinkedHashMap< String, String > paramMap = new LinkedHashMap<>();
				paramMap = param.logParameters();
				logMap.putAll( prefixMapHeaders( paramMap, "param_" ) );
				logMap.putAll( prefixMapHeaders( summary_error.get(param.ID_SAMPLE), "error_" ) );
				logMap.putAll( prefixMapHeaders( timeMap, "time_" ) );
				summary_all.put(param.ID_SAMPLE, logMap);
				File outputLogFile = new File( param.APP_FOLDER.getAbsolutePath() + "/" + Main.CONSTANT_FILE_NAME_PREFIX_LOG_REGISTRATION + param.ID_SAMPLE + ".csv" );
				writeCsv(  logList, ",", outputLogFile.getAbsolutePath() );
				IJ.log("END RUN output parameters/errors/log");

				IJ.log("------------------ END " + key + " END ------------------ ");

			} catch( Exception e ) {
				IJ.log( "--------------------------------------------------------" );
				IJ.log( "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<" );
				IJ.log( "		COMPUTATION OF SAMPLE " + key + " FAILED!" );
				IJ.log( ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" );
				IJ.log( "--------------------------------------------------------" );
			}
		}

		IJ.log("START RUN save logs");
		LinkedHashMap< String, LinkedHashMap< String, String > > summaryMap = new LinkedHashMap<>();
		summaryMap.putAll( summary_all );
		for ( String key : summaryMap.keySet() ) {
			summary.add( summaryMap.get(key) );
		}
		writeCsv( summary, ",", new File(param.OUTPUT_FOLDER + "/" + Main.CONSTANT_FILE_NAME_LOG_REGISTRATION ).getAbsolutePath() );
		IJ.log("END RUN save logs");
		IJ.log("START RUN save overlay stack");
		//outputOverlayStack.show();
		IJ.saveAsTiff( outputOverlayStack, new File( param.OUTPUT_FOLDER + "/" + Main.CONSTANT_FILE_NAME_OUTPUT_OVERLAY ).getAbsolutePath() );
		IJ.log("END RUN save overlay stack");
	}

	/**
	 * For debugging the SliceMap plugin
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {

		// set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Gui.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        ImageJ imagej = new ImageJ();

		IJ.log("START RUN gui");
		Gui gui = new Gui();
		IJ.log("END RUN gui");
	}
}
