/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ua.mbarbier.slicemap.gui;

import be.ua.mbarbier.slicemap.AffineAlign;
import be.ua.mbarbier.slicemap.AffineAnnotation;
import be.ua.mbarbier.slicemap.Align;
import be.ua.mbarbier.slicemap.ElasticRegistration;
import be.ua.mbarbier.slicemap.ImageProperties;
import be.ua.mbarbier.slicemap.Main;
import be.ua.mbarbier.slicemap.RefAlign;
import be.ua.mbarbier.slicemap.RefStack;
import be.ua.mbarbier.slicemap.lib.BiMap;
import static be.ua.mbarbier.slicemap.lib.Lib.convertMapDoubleToString;
import static be.ua.mbarbier.slicemap.lib.Lib.getMeanMap;
import static be.ua.mbarbier.slicemap.lib.Lib.prefixMapHeaders;
import be.ua.mbarbier.slicemap.lib.LibIO;
import static be.ua.mbarbier.slicemap.lib.LibIO.writeCsv;
import be.ua.mbarbier.slicemap.lib.Timers;
import be.ua.mbarbier.slicemap.lib.congealing.Congealing;
import be.ua.mbarbier.slicemap.lib.error.LibError;
import static be.ua.mbarbier.slicemap.lib.image.LibImage.convertContrastByte;
import be.ua.mbarbier.slicemap.lib.registration.BunwarpjParam;
import be.ua.mbarbier.slicemap.lib.registration.HarrisParam;
import be.ua.mbarbier.slicemap.lib.registration.SiftParam;
import be.ua.mbarbier.slicemap.lib.roi.LabelFusion;
import static be.ua.mbarbier.slicemap.lib.roi.LabelFusion.getInterpolationMap;
import static be.ua.mbarbier.slicemap.lib.roi.LabelFusion.getProbabilityMap;
import static be.ua.mbarbier.slicemap.lib.roi.LabelFusion.majorityVoting;
import be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import static be.ua.mbarbier.slicemap.lib.roi.LibRoi.getConfidenceBandOverlayImage;
import static be.ua.mbarbier.slicemap.lib.roi.LibRoi.getConfidenceOverlayImage;
import static be.ua.mbarbier.slicemap.lib.roi.LibRoi.getInterpolationOverlayImage;
import static be.ua.mbarbier.slicemap.lib.roi.LibRoi.getMapOfLists;
import static be.ua.mbarbier.slicemap.lib.roi.LibRoi.getOverlayImage;
import static be.ua.mbarbier.slicemap.lib.roi.LibRoi.intersectRoi;
import static be.ua.mbarbier.slicemap.lib.roi.LibRoi.roiFromMask;
import be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation;
import static be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.excludeOutlierRois;
import static be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.fillRoiBand;
import static be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.isolineFromProbabilityImage;
import static be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.maskFromThreshold;
import static be.ua.mbarbier.slicemap.lib.roi.RoiMap.mapInvert;
import be.ua.mbarbier.slicemap.lib.transform.TransformCongealing;
import bunwarpj.Transformation;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import net.imglib2.realtransform.AffineTransform2D;
import static org.apache.commons.collections.MapUtils.invertMap;

/**
 *
 * @author mbarbier
 */
public class Gui {

	Main param;
	private static final Logger logger = Logger.getLogger( Gui.class.getName() );

	public Gui() {

		// DEFAULT PARAMETERS
		this.param = new Main();
		param.PATTERN_REF_FILES = "^(.*?)\\.tif";
		param.CONTAINS_REF_FILES = "tif";
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

		// PARAMETER INPUT
		GenericDialogPlus gdp = new GenericDialogPlus("SliceMap: Automated annotation of fluorescent brain slices");
		gdp.addHelp( "https://gitlab.com/mbarbie1/SliceMap" );
//		gdp.addDirectoryField( "sample folder", "C:/Users/mbarbier/Desktop/slicemap_astrid/samples" );
//		gdp.addDirectoryField( "Input folder", "C:/Users/mbarbier/Desktop/slicemap_astrid/input" );
//		gdp.addDirectoryField( "Output folder", "C:/Users/mbarbier/Desktop/slicemap_astrid/output" );
		gdp.addDirectoryField( "sample folder", "d:/p_prog_output/slicemap_3/samples" );
		gdp.addDirectoryField( "Input folder", "d:/p_prog_output/slicemap_3/input" );
		gdp.addDirectoryField( "Output folder", "d:/p_prog_output/slicemap_3/output" );
		gdp.addStringField("sample name contains", "");
		gdp.addCheckbox( "Force regeneration downscaled aligned reference stack", true );
		// ADVANCED PARAMETERS INPUT
		gdp.addButton( "Advanced options", new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				popupOptions();
			} 
		} );

		gdp.showDialog();
		if ( gdp.wasCanceled() ) {
			return;
		}
		// EXTRACTION OF PARAMETERS FROM DIALOG
		File sampleFile = new File( gdp.getNextString() );
		File inputFile = new File( gdp.getNextString() );
		File outputFile = new File( gdp.getNextString() );
		File outputRoisFile = new File( outputFile.getAbsolutePath() + "/" + "roi" );
		File appFile = new File( outputFile.getAbsolutePath() + "/" + "debug" );
		File appFileCongealing = new File( appFile.getAbsolutePath() + "/" + "congealing" );
		File appFileElastic = new File( appFile.getAbsolutePath() + "/" + "elastic" );
		outputRoisFile.mkdirs();
		appFile.mkdirs();
		appFileCongealing.mkdirs();
		appFileElastic.mkdirs();
		String sampleFilter = gdp.getNextString();
		boolean regenerateStack = gdp.getNextBoolean();
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
		
		run();

		/*
		try {
			FileHandler fh;
			fh = new FileHandler( param.OUTPUT_FOLDER + "/" + Main.CONSTANT_FILE_NAME_LOG );
			//logger.setUseParentHandlers(false);
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
		} catch (IOException ex) {
			Logger.getLogger(Gui.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SecurityException ex) {
			Logger.getLogger(Gui.class.getName()).log(Level.SEVERE, null, ex);
		}
		*/
	}

	public void popupOptions() {

		GenericDialogPlus gdp = new GenericDialogPlus("Annotation algorithm advanced options");

		int CONGEALING_STACKBINNING = 16;
		int CONGEALING_NITERATIONS = 10;
		int CONGEALING_NREFERENCES = 5;
		int CONGEALING_BINCONGEALING = 1;
		int CONGEALING_NPOINTS = 8;
		String ELASTIC_FEATURES_METHOD = ElasticRegistration.METHOD_FEATURES_SIFT;
		BunwarpjParam bunwarpjParam = new BunwarpjParam();
		SiftParam siftParam = new SiftParam();
		HarrisParam harrisParam = new HarrisParam();

		// General
		gdp.addMessage( "GENERAL :" );
		//gdp.addNumericField( "Binning of the stacks for : ", CONGEALING_STACKBINNING, 16);
		String[] binningChoiceList = new String[]{"1","2","4","8","16","32","64","128"};
		gdp.addRadioButtonGroup( "Binning of the stacks : ", new String[]{"1","2","4","8","16","32","64","128"}, 1, 8, "8");

		// For the prewarping
		gdp.addNumericField( "Nr of feature points for initial horizontal alignment: ", CONGEALING_NPOINTS, 0);
		// For the congealing
		gdp.addNumericField( "Nr of iterations congealing: ", CONGEALING_NITERATIONS, 0);
		//gdp.addNumericField( "Nr of best matching references kept: ", CONGEALING_NREFERENCES, 0);
		gdp.addSlider( "Nr of best matching references kept: ", 1, 10, 5 );
		//gdp.addNumericField( "Extra binning during congealing: ", CONGEALING_BINCONGEALING, 0);
		//gdp.addRadioButtonGroup( "Extra binning during congealing: ", new String[]{"1","2","4","8"}, 1, 4, "1");
		gdp.addMessage( "AUTOMATED LANDMARKS :" );
		gdp.addChoice( "Method of feature points", new String[]{ ElasticRegistration.METHOD_FEATURES_SIFT, ElasticRegistration.METHOD_FEATURES_HARRIS}, ELASTIC_FEATURES_METHOD );

		gdp.addMessage( "REGISTRATION (BunwarpJ parameters) :" );
		String[] sRegistrationModes = { "Fast", "Accurate", "Mono" };
		gdp.addChoice("Registration Mode", sRegistrationModes, sRegistrationModes[1]);
		//gdp.addSlider("Image_Subsample_Factor", 0, 7, 0);
		String[] sMinScaleDeformationChoices = { "Very Coarse", "Coarse", "Fine", "Very Fine" };
		gdp.addChoice("Initial_Deformation :", sMinScaleDeformationChoices, sMinScaleDeformationChoices[bunwarpjParam.getMin_scale_deformation()]);
		String[] sMaxScaleDeformationChoices = { "Very Coarse", "Coarse", "Fine", "Very Fine", "Super Fine" };
		gdp.addChoice("Final_Deformation :", sMaxScaleDeformationChoices, sMaxScaleDeformationChoices[bunwarpjParam.getMax_scale_deformation()]);
		gdp.addNumericField("Divergence_Weight :", bunwarpjParam.getDivWeight(), 1);
		gdp.addNumericField("Curl_Weight :", bunwarpjParam.getCurlWeight(), 1);
		gdp.addNumericField("Landmark_Weight :", bunwarpjParam.getLandmarkWeight(), 1);
		gdp.addNumericField("Image_Weight :", bunwarpjParam.getImageWeight(), 1);
		gdp.addNumericField("Consistency_Weight :", bunwarpjParam.getConsistencyWeight(), 1);
		gdp.addNumericField("Stop_Threshold :", bunwarpjParam.getStopThreshold(), 1);

		gdp.showDialog();
		// Congealing
		param.CONGEALING_STACKBINNING = Integer.parseInt( gdp.getNextRadioButton() );
		param.CONGEALING_NPOINTS = (int) gdp.getNextNumber();
		param.CONGEALING_NITERATIONS = (int) gdp.getNextNumber();
		param.CONGEALING_NREFERENCES = (int) gdp.getNextNumber();
		//param.CONGEALING_BINCONGEALING = Integer.parseInt( gdp.getNextRadioButton() );
		// Landmarks for elastic registration
		param.REGISTRATION_FEATURE_METHOD = gdp.getNextChoice();
		// Elastic registration
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

		param.setBunwarpjParam( bunwarpjParam );
		param.setSiftParam( siftParam );
		param.setHarrisParam( harrisParam );

	}

	
	public void run() {

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
			String sliceName = fileName.substring(0,fileName.lastIndexOf("."));
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
				}

				/*
				ImagePlus rsCopy = rs.getStack().duplicate();
				LinkedHashMap< String, ImageProperties > stackPropsCopy = new LinkedHashMap<>();
				stackPropsCopy.putAll( rs.getStackProps() );
				BiMap< String, Integer > idMapCopy = idMap.copy();
				refList = new ArrayList<>(refListOld);
				// remove the reference = sample
				stackPropsCopy.remove(key);
				rsCopy.getStack().deleteSlice( idMap.get(key) );
				int indexRemoved = idMap.get(key);
				idMapCopy.remove(key);
				for ( String keyId : idMapCopy.getKeys() ) {
					if (idMapCopy.get(keyId) > indexRemoved ) {
						idMapCopy.put( keyId, idMapCopy.get(keyId) - 1 );
					}
				}
				nReferenceImages = param.getRefStack().getNSlices();
				lastRefIndex = nReferenceImages;
				refList.remove(key);
				// set the new variables to the param
				param.setStackProps( stackPropsCopy );
				param.setRefStack( rsCopy );
				param.setIdMap(idMapCopy);
				*/
				// -----------------------------------------------------------------
				// -----------------------------------------------------------------
				// -----------------------------------------------------------------

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
				// TODO
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
				IJ.save( align.getAlignedStack(), param.APP_CONGEALING_FOLDER + "/" + param.FILENAME_ALIGNED_REFERENCE_STACK + "_" + param.ID_SAMPLE + ".tif" );
				ElasticRegistration elasticRegistration = new ElasticRegistration( param, align.getStack(), align.getAlignedStack(), sortedIndices, stackProps, idMap, align.getCongealing().stackProps.get( bestSampleIndex-1 ).id, nReferenceImages );
				elasticRegistration.setTransfosCongealing(preTransformVec, transformVec);
				//elasticRegistration.registerOne( idMap.getKey(bestSampleIndex), idMap.getKey(1) );
				elasticRegistration.runSorted(refListSorted);
				ImagePlus compositeElastic = elasticRegistration.getRefSampleComposite();
				IJ.save( compositeElastic, param.APP_CONGEALING_FOLDER + "/" + "elasticComposite" + "_" + param.ID_SAMPLE + ".tif" );
				//ImagePlus compositeAligned = elasticRegistration.getRefSampleComposite( false );
				//IJ.save( compositeAligned, param.APP_CONGEALING_FOLDER + "/" + "alignedComposite" + "_" + param.ID_SAMPLE + ".tif" );
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
				//LinkedHashMap<String, Transformation> transfos = elasticRegistration.getTransfos();
				// LinkedHashMap< String, LinkedHashMap< String, Roi > > roiMapList_elastic = elasticRegistration.annotate( refList );
				//elasticRegistration.annotateShowComposite( roiMapList_elastic );
				// Map< roiImage, Map< roiName, Roi > >
				LinkedHashMap< String, LinkedHashMap< String, Roi > > roiMapList_elastic_sorted = elasticRegistration.annotate( refListSorted );
				//elasticRegistration.annotateShowComposite( roiMapList_elastic_sorted );
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

				//roiMSE( imp, rois );
				//this.mle.show();
				// TODO MB
				for ( String keyp : probMapSubset.keySet() ) {
					ImagePlus p = probMapSubset.get(keyp);
					p.setTitle("prob " + keyp);
					//p.duplicate().show();
				}
				for ( String keyp : probMapSubset_reduced.keySet() ) {
					ImagePlus p_red = probMapSubset_reduced.get(keyp);
					p_red.setTitle("reduced prob " + keyp);
					//p_red.duplicate().show();
				}

				// Interpolated ROIs from probability
				//sample = convertContrastByte( sample );

				//LinkedHashMap<String, Roi > roiInterpolationMapSubset = getInterpolationMap( probMapSubset );
				//roiInterpolationMapSubset = getInterpolationMap( probMapSubset, LabelFusion.METHOD_LABELFUSION_THRESHOLD, false );
				//impOverlaySubset = getOverlayImage( roiInterpolationMapSubset, sample ).duplicate();
				//impOverlaySubset.show();
				//roiInterpolationMapSubset = getInterpolationMap( probMapSubset, LabelFusion.METHOD_LABELFUSION_THRESHOLD, true );
				//impOverlaySubset = getOverlayImage( roiInterpolationMapSubset, sample ).duplicate();
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
				//ImagePlus overlayConfidence = sample.duplicate();

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

					//roiInterpolationMapSubset_2.put( roiName, roiIn )

					//ImagePlus mask = fillRoiBand( empty.duplicate(), roiInterpolation.roiBandInner, roiInterpolation.roiBandOuter, bgValue, fgValue );
					//roiBandMaskMap.put( roiName, mask.getProcessor() );
				}
				//ImagePlus impOverlaySubset_2 = getOverlayImage( roiCurveMap, sample );
				//impOverlaySubset_2.show();

				//ImagePlus overlayRoi = getOverlayImage( rois, sample );

				// TODO MB

				ArrayList< LinkedHashMap< String, Roi > > roisList = new ArrayList<>();
				ImagePlus overlayRoi = IJ.createImage("overlay", sample.getWidth(), sample.getHeight(), 1, 24);
				Overlay overlay = new Overlay();

				for ( String roiName : roiMapArray_elastic_copy.keySet() ) {
					//LinkedHashMap< String, Roi > imageRois = new LinkedHashMap<>();
					ArrayList< Roi > rois2 = roiMapArray_elastic_copy.get(roiName);
					for ( Roi roi : rois2 ) {
						overlay.add( roi );
						//roisList.add( imageRois );
					}
					//imageRois.put( roiName, rois );
				}
				overlayRoi.setOverlay(overlay);
				overlayRoi.setHideOverlay(false);
				//overlayRoi.show();

				//LinkedHashMap< String, Roi > interpRois = roiCurveMap;
				//ImagePlus overlayRoi = getInterpolationOverlayImage( roisList, interpRois, sample );

				//ImagePlus overlayConfidence = getOverlayRoiConfidence( roiMapArray_elastic, sample.duplicate() );
				//overlayConfidence.show();
				//ImagePlus overlayConfidence2 = getConfidenceOverlayImage( ciMap, sample.duplicate() );
				LibRoi.saveRoiMap( roiCurveMap, param.CONGEALING_STACKBINNING, stackProps.get("sample"), null, param.ID_SAMPLE, param.OUTPUT_ROIS_FOLDER.getAbsolutePath(), "" );

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
		outputOverlayStack.show();
		IJ.saveAsTiff( outputOverlayStack, new File( param.OUTPUT_FOLDER + "/" + Main.CONSTANT_FILE_NAME_OUTPUT_OVERLAY ).getAbsolutePath() );
		IJ.log("END RUN save overlay stack");
	}

	public static void main(String[] args) {

		// set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Gui.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        ImageJ imagej = new ImageJ();

        IJ.log("START RUN plugin");
		IJ.log("START RUN gui");
		Gui gui = new Gui();
		IJ.log("END RUN gui");
		//imagej.exitWhenQuitting(true);
		//imagej.quit();
		// alternative exit
//        if (!debug) {
//            System.exit(0);
//        }
        IJ.log("END RUN plugin");
	}
}
