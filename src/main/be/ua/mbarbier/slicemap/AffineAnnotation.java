/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap;

import main.be.ua.mbarbier.slicemap.lib.BiMap;
import main.be.ua.mbarbier.slicemap.lib.congealing.AffineCongealing;
import main.be.ua.mbarbier.slicemap.lib.congealing.Congealing;
import main.be.ua.mbarbier.slicemap.lib.image.LibImage;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.binStack;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.convertContrastByte;
import static main.be.ua.mbarbier.slicemap.lib.roi.LabelFusion.getInterpolationMap;
import static main.be.ua.mbarbier.slicemap.lib.roi.LabelFusion.getProbabilityMap;
import main.be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.getMapOfLists;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.getOverlayImage;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.getOverlayImageRGB;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.saveRoiAlternative;
import main.be.ua.mbarbier.slicemap.lib.transform.Transform2D;
import static main.be.ua.mbarbier.slicemap.lib.transform.Transform2D.applyRefTransform;
import main.be.ua.mbarbier.slicemap.lib.transform.TransformCongealing;
import static main.be.ua.mbarbier.slicemap.lib.transform.TransformImage.applyTransform;
import static main.be.ua.mbarbier.slicemap.lib.transform.TransformRoi.applyRoiScaleTransform;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.ContrastEnhancer;
import ij.plugin.RGBStackMerge;
import ij.process.ImageProcessor;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.imglib2.realtransform.AffineTransform2D;

/**
 *
 * @author mbarbier
 */
public class AffineAnnotation {

	//MBLog log;

	public static int[] sortPartition( int[] a, int N ) {
		
		int[] out = new int[a.length];
		
		// M = 1-based last index of partition 1
		int M = a.length;
		// N = number of elements in partition 2
		//		[ 1, .. , M-N ], [ M-(N-1), .. , M-(N-N)=M ]
		//
		ArrayList< Integer > newa = new ArrayList<>();
		ArrayList< Integer > new1 = new ArrayList<>();
		ArrayList< Integer > wrong1 = new ArrayList<>(); 
		ArrayList< Integer > new2 = new ArrayList<>();
		ArrayList< Integer > wrong2 = new ArrayList<>(); 
		for ( int i = 0; i < (M-N); i++ ) {
			if ( a[i] < M-N )
				new1.add(a[i]);
			else
				wrong1.add(a[i]);
		}
		for ( int i = (M-N); i < M; i++ ) {
			if ( a[i] >= M-N )
				new2.add(a[i]);
			else
				wrong2.add(a[i]);
		}
		newa.addAll(new1);
		newa.addAll(wrong2);
		newa.addAll(wrong1);
		newa.addAll(new2);
		for (int i = 0; i < out.length; i++) {
			out[i] = newa.get(i);
		}

		return out;
	}
	
	public static int[] sortAnnotate( ImagePlus stack, ImagePlus alignedStack, String sampleId, int lastRefIndex, LinkedHashMap< String, ImageProperties > stackProps, LinkedHashMap< String, LinkedHashMap< String, Roi > > roiMapList, Main param, BiMap< String, Integer > idMap ) {

		String outputFolder = param.OUTPUT_FOLDER.getAbsolutePath();
		String sliceName = sampleId;
		int sampleIndex = stackProps.get(sampleId).index;
		int sizeX = alignedStack.getWidth();
		int sizeY = alignedStack.getHeight();
		// ---------------------------------------------------------------------
		// ---  -------------------------------------------------
		// ---------------------------------------------------------------------

		IJ.log(" --- Sort the images according to colocalization with image " + sampleIndex);
		ImagePlus alignedSample = new ImagePlus("Aligned sample", alignedStack.getStack().getProcessor(sampleIndex));
		//alignedSample.show();
		ImagePlus sample = new ImagePlus("sample", stack.getStack().getProcessor(sampleIndex));

		int[] tempSortedIndices = AffineCongealing.sortImagesNames( alignedSample, alignedStack, stackProps );
		int[] sortedIndices = sortPartition( tempSortedIndices, tempSortedIndices.length - lastRefIndex );
		int nImages = alignedStack.getNSlices();
		ImagePlus impSorted = IJ.createHyperStack("Sorted", alignedSample.getWidth(), alignedSample.getHeight(), 2, nImages, 1, 32);
		for (int i = 1; i < nImages + 1; i++) {
			int j = i * 2 - 1;
			impSorted.getStack().setProcessor( alignedStack.getStack().getProcessor( sortedIndices[i - 1] + 1 ), j );
			impSorted.getStack().setProcessor( alignedSample.getProcessor(), j + 1 );
		}
		//impSorted.duplicate().show();

		// Do the same as before but with less references
		LinkedHashMap<String, LinkedHashMap<String, Roi>> roiMapListSubset = new LinkedHashMap<>();
                if ( param.CONGEALING_NREFERENCES > lastRefIndex ) {
                    param.CONGEALING_NREFERENCES = lastRefIndex;
                    IJ.log("The number of reference images to keep after congealing step is larger than the total number of reference images! We reduced the number to keep to the number of reference images.");
                }
		int nReferences = param.CONGEALING_NREFERENCES;
		IJ.log("All sorted indices : " + sortedIndices.toString() + "");
		for (int i = lastRefIndex+1 - nReferences; i < (lastRefIndex+1); i++) {
			String refIdTemp = idMap.getKey( sortedIndices[i - 1] + 1 );
			//String refIdTemp = stackProps.get(i)
			//IJ.log(sliceLabel + " = index : " + sortedIndices[i - 1] + " , sorted index : " + (i - 1));
			roiMapListSubset.put( refIdTemp, roiMapList.get(refIdTemp) );
		}

		// get ROI arrays
		LinkedHashMap< String, ArrayList< Roi>> roiListMapSubset = getMapOfLists(roiMapListSubset);
		// Probability images (or other types of confidence intervals?)
		LinkedHashMap< String, ImagePlus> probMapSubset = getProbabilityMap( sizeX, sizeY, roiListMapSubset, true );
		// Interpolated ROIs from probability
		LinkedHashMap<String, Roi> roiInterpolationMapSubset = getInterpolationMap(probMapSubset, false);
		// ROIs Overlay image
		ImagePlus impOverlaySubset = getOverlayImage(roiInterpolationMapSubset, sample );
		//impOverlaySubset.show();
		//String outputSamplePath = outputFolder + "/" + "sample_congealingSubsetOverlay_" + sliceName + ".png";
		//IJ.log("Saving sample with ROIs overlay image after sorted congealing with subset: " + outputSamplePath );
		//IJ.saveAs(impOverlaySubset, "png", outputSamplePath );

		return sortedIndices;
	}
	
	public static LinkedHashMap< String, LinkedHashMap< String, Roi > > annotate( ImagePlus stack, String sampleId, Roi sampleRoi, TransformCongealing transformCongealing, Main param ) {

		// ---------------------------------------------------------------------
		// --- Initialization: Get the transformations, input sample/ref -------
		// ---------------------------------------------------------------------
		String sliceName = param.ID_SAMPLE;
		String outputFolder = param.APP_CONGEALING_FOLDER.getAbsolutePath();
		LinkedHashMap< String, ImageProperties > stackProps = transformCongealing.stackProps; 
		int bestSampleIndex = stackProps.get(sampleId).index;

		ImageProcessor ipOri = stack.getStack().getProcessor( bestSampleIndex );
		ImagePlus before_pretvec = new ImagePlus( "sample", ipOri );

		// All transformed ROI
		LinkedHashMap<String, LinkedHashMap<String, Roi>> roiMapList = new LinkedHashMap<>();
		int lastRefIndex = transformCongealing.lastRefIndex;

		// ---------------------------------------------------------------------
		// --- Transform the ROIs to the samples and get an overlay stack ------
		// ---------------------------------------------------------------------
		int nChannels = 3;
		ImagePlus overlayStack = IJ.createHyperStack( "Congealing overlay sample", ipOri.getWidth(), ipOri.getHeight(), nChannels, lastRefIndex, 1, 24 );
		int index = 0;
		for ( String refIdTemp : stackProps.keySet() ) {
			ImageProperties props = stackProps.get(refIdTemp);
			int refIndex = props.index;
			if ( refIndex <= lastRefIndex ) {
				index++;
				String sliceLabel = refIdTemp;
				double scale = 1. / ((double) param.CONGEALING_BINCONGEALING);
				// Transform ROIs
				LinkedHashMap<String, Roi> roiTS = transformCongealing.getTransformedRois( refIdTemp, sampleId, scale );
				roiMapList.put( refIdTemp, roiTS );
				//ImagePlus impSorted = IJ.createHyperStack("Sorted", alignedSample.getWidth(), alignedSample.getHeight(), 2, 1, 1, 32);
				ImagePlus before_pretvec_overlay = getOverlayImage( roiTS , before_pretvec.duplicate() );
				before_pretvec_overlay.setHideOverlay(false);
				before_pretvec_overlay.setTitle( "overlay " + sliceLabel );
				ImagePlus overlay_flattened = before_pretvec_overlay.flatten();
				// Transform image
				ImagePlus imp = transformCongealing.getTransformedRef( refIdTemp, sampleId, scale, stack );
				ImagePlus ref_overlay = getOverlayImage( roiTS , imp.duplicate() );
				ref_overlay.setHideOverlay(false);
				ref_overlay.setTitle( "overlay " + sliceLabel );
				ImagePlus ref_overlay_flattened = ref_overlay.flatten();
				// Composite
				ImagePlus composite = new ImagePlus();// = IJ.createHyperStack("composite", before_pretvec.getWidth(), before_pretvec.getHeight(), 2, 1, 1, before_pretvec.getBitDepth());
				//composite.getStack().setProcessor( imp.getProcessor().duplicate(), 1);
				//composite.getStack().setProcessor( before_pretvec.getProcessor().duplicate(), 2);
				//composite.;
				//ImageProcessor ip = LibImage.convertSimilar( imp.getProcessor(), before_pretvec.getProcessor() );
				ImagePlus oriType = convertContrastByte( before_pretvec );
				ImagePlus impType = convertContrastByte( imp );
				ImageStack compositeStack = RGBStackMerge.mergeStacks( impType.getStack(), oriType.getStack(), null, false);
				//ImagePlus composite = 
				composite.setStack( compositeStack );
				ImagePlus composite_overlay = getOverlayImageRGB( roiTS , composite.duplicate() );
				composite_overlay.setHideOverlay(false);
				composite_overlay.setTitle( "overlay " + sliceLabel );
				ImagePlus composite_overlay_flattened = composite_overlay.flatten();

				//composite.show();
				// Make overlays
				overlayStack.getStack().setProcessor( ref_overlay_flattened.getProcessor() , nChannels*index - 2 );
				overlayStack.getStack().setProcessor( overlay_flattened.getProcessor() , nChannels*index - 1 );
				overlayStack.getStack().setProcessor( composite_overlay_flattened.getProcessor() , nChannels*index );
				overlayStack.getStack().setSliceLabel(sliceLabel, 2*index - 1 );
				overlayStack.getStack().setSliceLabel(sliceLabel, 2*index );
			}
		}
		//overlayStack.show();


		// ---------------------------------------------------------------------
		// --- Calculate interpolated ROIs -------------------------------------
		// ---------------------------------------------------------------------
		//
		// get ROI arrays
		LinkedHashMap< String, ArrayList< Roi>> roiListMap = getMapOfLists(roiMapList);
		// Probability images (or other types of confidence intervals?)
		int sizeX = ipOri.getWidth();
		int sizeY = ipOri.getHeight();
		LinkedHashMap< String, ImagePlus> probMap = getProbabilityMap(sizeX, sizeY, roiListMap, true);
		// Interpolated ROIs from probability
		LinkedHashMap<String, Roi> roiInterpolationMap = getInterpolationMap(probMap, false);
		// ROIs Overlay image
		ImagePlus sample = new ImagePlus( "sample", ipOri );
		ImagePlus impOverlay = getOverlayImage( roiInterpolationMap, sample);
		//impOverlay.show();
		//String outputSamplePath = outputFolder + "/" + "sample_congealingOverlay_" + sliceName + ".png";
		//IJ.log("Saving sample with ROIs overlay image after congealing: " + outputSamplePath);
		//IJ.saveAs( impOverlay, "png", outputSamplePath);

		//LinkedHashMap< String, Roi> roiTempScale = applyRoiScaleTransform( roiInterpolationMap, 0.0, 0.0, 2.0 );
		//getOverlayImage( roiTempScale , after_pretvec ).show();
		//getOverlayImage( roiTempScale , after_tvec ).show();

		
		// ---------------------------------------------------------------------
		// --- Scale all the ROIs to original size -----------------------------
		// ---------------------------------------------------------------------
		LinkedHashMap<String, Roi> roiL = applyRoiScaleTransform(roiInterpolationMap, 0.0, 0.0, param.CONGEALING_STACKBINNING);
		// Smooth the large ROIs
		boolean smooth = param.DO_SMOOTH_ROIS;
		double interval = (double) param.CONGEALING_STACKBINNING;
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
		int cropX = stackProps.get(sampleId).xOffset;
		int cropY = stackProps.get(sampleId).yOffset;
		// OK now for the ROIs
		LinkedHashMap<String, Roi> roiCrop = new LinkedHashMap<>();
		for (String key : roiL.keySet()) {
			Roi roi = roiL.get(key);
			double xx = roi.getXBase();
			double yy = roi.getYBase();
			roi.setLocation(xx - cropX, yy - cropY);
			try {
				Roi cropRoi = LibRoi.intersectRoi(sampleRoi, roi);
				try {
					if (cropRoi != null) {
						roiCrop.put(key, cropRoi);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch( Exception e2 ) {
				roiCrop.put(key, roi);
				e2.printStackTrace();
			}
		}

		// ---------------------------------------------------------------------
		// --- Save all the ROIs -----------------------------------------------
		// ---------------------------------------------------------------------
		//
		// SAVE THE ROIs
		String roiFileNameSample = "affine_roiSample_" + sliceName + ".zip";
		String roiFileNameL = "affine_roi_" + sliceName + ".zip";
		String roiFileNameC = "affine_roiCrop_" + sliceName + ".zip";
		String roiFileNameS = "affine_roiSmall_" + sliceName + ".zip";
		String roiFilePathSample = outputFolder + "/" + roiFileNameSample;
		String roiFilePathS = outputFolder + "/" + roiFileNameS;
		String roiFilePathC = outputFolder + "/" + roiFileNameC;
		String roiFilePathL = outputFolder + "/" + roiFileNameL;
		File roiFileSample = new File(roiFilePathSample);
		LinkedHashMap<String, Roi> sampleRoiMap = new LinkedHashMap<String, Roi>();
		sampleRoiMap.put("bg", sampleRoi);
		saveRoiAlternative(roiFileSample, sampleRoiMap);
		File roiFileL = new File(roiFilePathL);
		saveRoiAlternative(roiFileL, roiL);
		File roiFileS = new File(roiFilePathS);
		saveRoiAlternative(roiFileS, roiInterpolationMap);
		File roiFileC = new File(roiFilePathC);
		saveRoiAlternative(roiFileC, roiCrop);
		
		return roiMapList;
	}

	
	public static void annotateOne( ImagePlus stack, String refId, String sampleId, Roi sampleRoi, TransformCongealing transformCongealing, Main param ) {

		String sliceName = param.ID_SAMPLE;
		String outputFolder = param.OUTPUT_FOLDER.getAbsolutePath();
		LinkedHashMap< String, ImageProperties > stackProps = transformCongealing.stackProps; 
		int bestSampleIndex = stackProps.get(sampleId).index;
		int refIndexTemp = stackProps.get(refId).index;
		AffineTransform2D preTRef = transformCongealing.preTransformVec.get(refId);
		AffineTransform2D preTSample = transformCongealing.preTransformVec.get(sampleId);
		AffineTransform2D tRef = transformCongealing.transformVec.get(refId);
		AffineTransform2D tSample = transformCongealing.transformVec.get(sampleId);

		ImageProcessor ipOriT;
		ImageProcessor ipOri = stack.getStack().getProcessor(bestSampleIndex);
		ImagePlus before_pretvec = new ImagePlus( "before pretvec", ipOri.duplicate() );
		// TODO MB: Inverse or forward transformations?
		ipOriT = Transform2D.applyTransform(  ipOri.duplicate(), preTSample );
		ImagePlus after_pretvec = new ImagePlus( "after pretvec", ipOriT.duplicate() );
		ipOriT = Transform2D.applyTransform( ipOriT.duplicate(), tSample );
		ImagePlus after_tvec = new ImagePlus( "after tvec", ipOriT.duplicate() );
		//before_pretvec.show();
		//after_pretvec.show();
		//after_tvec.show();

		LinkedHashMap<String, Roi> roiTS = transformCongealing.getTransformedRois( refId, sampleId, 1.0 );
		ImagePlus before_pretvec_overlay = getOverlayImage( roiTS , before_pretvec.duplicate() );
		ImagePlus after_tvec_overlay = getOverlayImage( roiTS , after_tvec.duplicate() );
		//before_pretvec_overlay.show();
		//after_tvec_overlay.show();
	}


	public void run( ImagePlus impOri, int sampleIndex, ImagePlus sampleOri, ImagePlus sampleT, Roi sampleRoi, Main param, Congealing congealing ) {

		String sliceName = param.ID_SAMPLE;
		String outputFolder = param.OUTPUT_FOLDER.getAbsolutePath();

		String logFileName = Main.CONSTANT_FILE_NAME_LOG;
		File logFile = new File( outputFolder + "/" + logFileName );
		//this.log = new MBLog(logFile.getAbsolutePath());

		String transformationType = congealing.getTRANSFORM();

		double[] tvec = congealing.transformVec.get(sampleIndex-1);
		double[] pretvec = congealing.preTransformVec.get(sampleIndex - 1);
		ImageProcessor ipOriT;
		ImageProcessor ipOri = sampleOri.getProcessor();
		ImagePlus before_pretvec = new ImagePlus( "before pretvec", ipOri.duplicate() );
		ipOriT = applyTransform(ipOri.duplicate(), pretvec, transformationType, congealing.methodParameters);
		ImagePlus after_pretvec = new ImagePlus( "after pretvec", ipOriT.duplicate() );
		ipOriT = applyTransform(ipOriT.duplicate(), tvec, transformationType, congealing.methodParameters);
		ImagePlus after_tvec = new ImagePlus( "after tvec", ipOriT.duplicate() );

		RGBStackMerge merger = new RGBStackMerge();
		ImagePlus[] imps = new ImagePlus[2];
		imps[0] = sampleT;
		imps[1] = new ImagePlus("transformed image", ipOriT);
		ImagePlus merged = merger.mergeHyperstacks(imps, true);
		//merged.show();

		// All transformed ROI
		LinkedHashMap<String, LinkedHashMap<String, Roi>> roiMapList = new LinkedHashMap<String, LinkedHashMap<String, Roi>>();
		for (int refIndex = 0; refIndex < sampleIndex - 1; refIndex++) {
			String sliceLabel = impOri.getStack().getSliceLabel(refIndex + 1);
			LinkedHashMap<String, Roi> roiTS = congealing.getTransformedRoisTemp( impOri, refIndex, sampleIndex );
			roiMapList.put(sliceLabel, roiTS);
		}

		// get ROI arrays
		LinkedHashMap< String, ArrayList< Roi>> roiListMap = getMapOfLists(roiMapList);
		// Probability images (or other types of confidence intervals?)
		int sizeX = impOri.getWidth();
		int sizeY = impOri.getHeight();
		LinkedHashMap< String, ImagePlus> probMap = getProbabilityMap(sizeX, sizeY, roiListMap, true);
		// Interpolated ROIs from probability
		LinkedHashMap<String, Roi> roiInterpolationMap = getInterpolationMap(probMap, false);
		// ROIs Overlay image
		ImagePlus impOverlay = getOverlayImage( roiInterpolationMap, sampleOri );
		
		//before_pretvec.show();
		LinkedHashMap< String, Roi> roiTempScale = applyRoiScaleTransform( roiInterpolationMap, 0.0, 0.0, 2.0 );
		//getOverlayImage( roiTempScale , after_pretvec ).show();
		//getOverlayImage( roiTempScale , after_tvec ).show();

		// --------------------------------------
		// Save the ROIs on 8-binned size
		// --------------------------------------
		LinkedHashMap<String, Roi> roiL = applyRoiScaleTransform(roiInterpolationMap, 0.0, 0.0, param.CONGEALING_STACKBINNING);
		// Smooth the large ROIs
		boolean smooth = true;
		double interval = (double) param.CONGEALING_STACKBINNING;
		LinkedHashMap<String, Roi> roiTemp = new LinkedHashMap<String, Roi>();
		for (String key : roiL.keySet()) {
			Roi roi = roiL.get(key);
			Roi smoothRoi = new PolygonRoi(roi.getInterpolatedPolygon(interval, smooth), Roi.POLYGON);
			roiTemp.put(key, smoothRoi);
		}
		roiL = roiTemp;

		// CROP THE TRANSFORMED ROIs TO THE ORIGINAL SAMPLE WIDTH
		int cropWidth = sampleOri.getWidth();
		int cropHeight = sampleOri.getHeight();
		int cropX = (int) Math.round((congealing.refWidth - cropWidth) / 2.0);
		int cropY = (int) Math.round((congealing.refHeight - cropHeight) / 2.0);
		// OK now for the ROIs
		LinkedHashMap<String, Roi> roiCrop = new LinkedHashMap<String, Roi>();
		for (String key : roiL.keySet()) {
			Roi roi = roiL.get(key);
			double xx = roi.getXBase();
			double yy = roi.getYBase();
			roi.setLocation(xx - cropX, yy - cropY);
			Roi cropRoi = LibRoi.intersectRoi(sampleRoi, roi);
			try {
				if (cropRoi != null) {
					roiCrop.put(key, cropRoi);
				}
			} catch (Exception e) {
			}
		}
		// SAVE THE ROIs
		String roiFileNameSample = "affine_roiSample_" + sliceName + ".zip";
		String roiFileNameL = "affine_roi_" + sliceName + ".zip";
		String roiFileNameC = "affine_roiCrop_" + sliceName + ".zip";
		String roiFileNameS = "affine_roiSmall_" + sliceName + ".zip";
		String roiFilePathSample = outputFolder + "/" + roiFileNameSample;
		String roiFilePathS = outputFolder + "/" + roiFileNameS;
		String roiFilePathC = outputFolder + "/" + roiFileNameC;
		String roiFilePathL = outputFolder + "/" + roiFileNameL;
		File roiFileSample = new File(roiFilePathSample);
		LinkedHashMap<String, Roi> sampleRoiMap = new LinkedHashMap<String, Roi>();
		sampleRoiMap.put("bg", sampleRoi);
		saveRoiAlternative(roiFileSample, sampleRoiMap);
		File roiFileL = new File(roiFilePathL);
		saveRoiAlternative(roiFileL, roiL);
		File roiFileS = new File(roiFilePathS);
		saveRoiAlternative(roiFileS, roiInterpolationMap);
		File roiFileC = new File(roiFilePathC);
		saveRoiAlternative(roiFileC, roiCrop);

		
		String outputSamplePath = outputFolder + "/" + "sample_congealingOverlay_" + sliceName + ".png";
		//this.log.log("Saving sample with ROIs overlay image after congealing: " + outputSamplePath);
		//IJ.saveAs(impOverlay, "png", outputSamplePath);

		IJ.log(" --- Sort the images according to colocalization with image " + sampleIndex);
		//new ImagePlus("transformed",ipOriT).show();
		ImagePlus alignedSample = new ImagePlus("Aligned sample", congealing.getAlignedStack().getStack().getProcessor(sampleIndex));
		//alignedSample.show();
		int[] sortedIndices = congealing.sortImages(alignedSample, congealing.getAlignedStack());
		int nImages = congealing.getAlignedStack().getNSlices();
		ImagePlus impSorted = IJ.createHyperStack("Sorted", alignedSample.getWidth(), alignedSample.getHeight(), 2, nImages, 1, 32);
		for (int i = 1; i < congealing.nImages + 1; i++) {
			int j = i * 2 - 1;
			//IJ.log("j = " + j);
			//congealing.getAlignedStack().getStack().getProcessor( sortedIndices[i - 1] + 1 );
			//IJ.log("getProcessor index = " + ( sortedIndices[i - 1] + 1 ) );
			impSorted.getStack().setProcessor( congealing.getAlignedStack().getStack().getProcessor( sortedIndices[i - 1] + 1 ), j );
			impSorted.getStack().setProcessor( alignedSample.getProcessor(), j + 1 );
		}
		//impSorted.duplicate().show();

		// Do the same as before but with less references
		LinkedHashMap<String, LinkedHashMap<String, Roi>> roiMapListSubset = new LinkedHashMap<>();
		int nReferences = param.CONGEALING_NREFERENCES;
		IJ.log("All sorted indices : " + sortedIndices.toString() + "");
		for (int i = sampleIndex - nReferences; i < sampleIndex; i++) {
			String sliceLabel = impOri.getStack().getSliceLabel(sortedIndices[i - 1] + 1);
			//IJ.log(sliceLabel + " = index : " + sortedIndices[i - 1] + " , sorted index : " + (i - 1));
			roiMapListSubset.put(sliceLabel, roiMapList.get(sliceLabel));
		}

		// get ROI arrays
		LinkedHashMap< String, ArrayList< Roi>> roiListMapSubset = getMapOfLists(roiMapListSubset);
		// Probability images (or other types of confidence intervals?)
		LinkedHashMap< String, ImagePlus> probMapSubset = getProbabilityMap(sizeX, sizeY, roiListMapSubset, true);
		// Interpolated ROIs from probability
		LinkedHashMap<String, Roi> roiInterpolationMapSubset = getInterpolationMap(probMapSubset, false);
		// ROIs Overlay image
		ImagePlus impOverlaySubset = getOverlayImage(roiInterpolationMapSubset, sampleOri);
		//impOverlaySubset.show();
		outputSamplePath = outputFolder + "/" + "sample_congealingSubsetOverlay_" + sliceName + ".png";
		//this.log.log("Saving sample with ROIs overlay image after sorted congealing with subset: " + outputSamplePath);
		//IJ.saveAs(impOverlaySubset, "png", outputSamplePath);

	}
}
