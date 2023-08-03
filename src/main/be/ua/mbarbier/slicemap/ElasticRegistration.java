/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap;

import main.be.ua.mbarbier.slicemap.lib.BiMap;
import static main.be.ua.mbarbier.slicemap.lib.Lib.prefixMapHeaders;
import main.be.ua.mbarbier.slicemap.lib.error.LibError;
import main.be.ua.mbarbier.slicemap.lib.image.LibImage;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.convertContrastByte;
import main.be.ua.mbarbier.slicemap.lib.registration.LibRegistration;
import static main.be.ua.mbarbier.slicemap.lib.registration.LibRegistration.siftSingle;
import main.be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.getOverlayImage;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.getOverlayImageRGB;
import main.be.ua.mbarbier.slicemap.lib.transform.ElasticTransform2D;
import static main.be.ua.mbarbier.slicemap.lib.transform.Transform2D.applyInverseTransform;
import bunwarpj.Transformation;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import ij.plugin.ContrastEnhancer;
import ij.plugin.RGBStackMerge;
import ij.process.ImageConverter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.imglib2.realtransform.AffineTransform2D;

/**
 *
 * @author mbarbier
 */
public class ElasticRegistration {
	
	ImagePlus source;
	ImagePlus target;
	Roi sourceRoiPoints;
	Roi targetRoiPoints;
	boolean debug = false;
	Main param;
	ImagePlus stack;
	ImagePlus alignedStack;
	ImagePlus regStack;
	int[] sortedIndices;
	int nReferenceImages;
	LinkedHashMap< String, ImageProperties > stackProps;
	LinkedHashMap< String, ImageProperties > refStackProps;
	LinkedHashMap< String, Transformation > transfosElastic;
	LinkedHashMap< String, AffineTransform2D > transfosCongealing;
//	LinkedHashMap< String, AffineTransform2D > transfosPreWarp;
	BiMap<String, Integer> idMap;
	String sampleId;

	public final static String METHOD_FEATURES_SIFT = "SIFT";
	public final static String METHOD_FEATURES_HARRIS = "HARRIS";

	public ElasticRegistration( Main param, ImagePlus stack, ImagePlus alignedStack, int[] sortedIndices, LinkedHashMap< String, ImageProperties > stackProps, BiMap idMap, String sampleId, int nReferenceImages ) {
		this.param = param;
		this.alignedStack = alignedStack;
		this.stack = stack;
		this.sortedIndices = sortedIndices;
		this.stackProps = stackProps;
		this.idMap = idMap;
		this.sampleId = sampleId;
		this.transfosElastic = new LinkedHashMap<>();
		this.nReferenceImages = nReferenceImages;
	};

	public void setTransfosCongealing( LinkedHashMap< String, AffineTransform2D > transfosCongealing ) {
		this.transfosCongealing = transfosCongealing;
	}

//	public void setTransfosPreWarp( LinkedHashMap< String, AffineTransform2D > transfosPreWarp ) {
//		this.transfosPreWarp = transfosPreWarp;
//	}

	public void setTransfosCongealing( LinkedHashMap<String, AffineTransform2D > preTransformVec, LinkedHashMap<String, AffineTransform2D > transformVec ) {
		
		LinkedHashMap<String, AffineTransform2D > congealingMap = new LinkedHashMap<>();
		for ( String key : preTransformVec.keySet() ) {
			AffineTransform2D tMat = transformVec.get(key);
			AffineTransform2D preTMat = preTransformVec.get(key);
			AffineTransform2D mat = preTMat.preConcatenate(tMat);
			congealingMap.put( key, mat);
		}
		this.transfosCongealing	= congealingMap;
	}

	public LinkedHashMap<String, Transformation> getTransfosElastic() {
		return transfosElastic;
	}

	public void run() {

		// TODO: change the input type (stackprops???)
		int sampleIndex = this.sortedIndices[sortedIndices.length-1] + 1;
		this.source = new ImagePlus( "sample", alignedStack.getStack().getProcessor(sampleIndex).convertToByte(true) );
		ImageConverter.setDoScaling(true);
		ImageConverter ic = new ImageConverter( alignedStack );
		ic.convertToGray8();
		// Output stack
		int nChannels = 2;
        int nSlices = alignedStack.getStack().getSize();
        int nFrames = 1;
        //ImagePlus impMerged = IJ.createHyperStack( "merged", this.source.getWidth(), this.source.getHeight(), nChannels, nSlices, nFrames, this.source.getBitDepth() );

		//for ( int refIndex = nReferenceImages - param.CONGEALING_NREFERENCES; refIndex <= nReferenceImages; refIndex++ ) {
		for ( int refIndex = 1; refIndex <= nReferenceImages; refIndex++ ) {
			Transformation transfo = registerOne( sampleId, idMap.getKey(refIndex) );
			transfosElastic.put( idMap.getKey(refIndex), transfo );
			//ImagePlus reg = transfo.getInverseResults();
			//impMerged.getImageStack().setProcessor( LibImage.convertSimilar( reg.getStack().getProcessor(1), this.target.getProcessor()) , 2 * refIndex );
			//impMerged.getImageStack().setProcessor( this.source.getProcessor() , 2 * refIndex - 1 );
		}

		//CompositeImage impComp = new CompositeImage( impMerged, CompositeImage.COMPOSITE );
		//impComp.show();

		//int refIndex = this.stackProps.get( refId ).index;
		//this.source.show();
		//this.target.show();
	}

		public void runSorted( ArrayList< String >  refListSorted ) {

		int sampleIndex = this.sortedIndices[sortedIndices.length-1] + 1;
		this.source = new ImagePlus( "sample", alignedStack.getStack().getProcessor(sampleIndex).convertToByte(true) );
		ImageConverter.setDoScaling(true);
		ImageConverter ic = new ImageConverter( alignedStack );
		ic.convertToGray8();

		for ( String refId : refListSorted ) {
			Transformation transfo = registerOne( sampleId, refId );
			transfosElastic.put( refId, transfo );
		}
	}

	/**
	 * Error on the elastic registration results
	 * 
	 * @return 
	 */
	public LinkedHashMap< String, LinkedHashMap< String, Double> > getErrorList() {

        // TODO Error on the intensity
		LinkedHashMap< String, LinkedHashMap< String, Double> > errorList = new LinkedHashMap<>();
		for ( String key : transfosElastic.keySet() ) {

			Transformation transfo = transfosElastic.get(key);

			ImagePlus refCong = new ImagePlus( "refCong " + key, transfo.getDirectResults().getStack().getProcessor(2) );
			ImagePlus refReg = new ImagePlus( "refReg " + key, transfo.getInverseResults().getStack().getProcessor(1) );
			ImagePlus sampleCong = new ImagePlus( "sampleCong " + key, transfo.getInverseResults().getStack().getProcessor(2) );
			ImagePlus refOri = new ImagePlus( "refOri " + key, this.stack.getStack().getProcessor( this.idMap.get(key) ) );
			refOri = LibImage.convertContrastByte( refOri );
			refOri.setProcessor( refOri.getProcessor().convertToFloat() );

			refCong.setProcessor( refCong.getProcessor().convertToByteProcessor() );
			refReg.setProcessor( refReg.getProcessor().convertToByteProcessor() );
			sampleCong.setProcessor( sampleCong.getProcessor().convertToByteProcessor() );
			refOri.setProcessor( refOri.getProcessor().convertToByteProcessor() );

			double S_bunwarpj = transfo.evaluateImageSimilarity(false);
			LibError libError = new LibError( refReg, sampleCong );
			LinkedHashMap< String, Double> error = libError.measureError(false);

			libError = new LibError( refCong, sampleCong );
			LinkedHashMap< String, Double> errorCong = libError.measureError(false);
			errorCong = prefixMapHeaders( errorCong, "cong_" );
			error.putAll(errorCong);

			libError = new LibError( refOri, sampleCong );
			LinkedHashMap< String, Double> errorOri = libError.measureError(false);
			errorOri = prefixMapHeaders( errorOri, "ori_" );
			error.putAll(errorOri);

			errorList.put(key, error );

			if (debug) {
				new ImagePlus( "ip1", libError.getIp1() ).show();
				new ImagePlus( "ip2", libError.getIp2() ).show();
			}
		}
        //error.putAll( libError.haussdorffError() );
        //error.putAll( LibError.haussdorffErrorRoi( this.targetRoi, this.sourceRoi ));
        //error.putAll( LibError.roiVOP( this.targetRoi, this.sourceRoi ));
		return errorList;
	}


	
	public ImagePlus getRefSampleComposite() {
		
		int nSlices = transfosElastic.size();
		//int bitDepth = this.alignedStack.getBitDepth();
		int width = this.stack.getWidth();
		int height = this.stack.getHeight();
		int nChannels = 4;
		ImagePlus composite = IJ.createHyperStack("elastic_ref_sample_" + sampleId, width, height, nChannels, nSlices, 1, 8);
		int index = 0;
		for ( String key : transfosElastic.keySet() ) {

			Transformation transfo = transfosElastic.get(key);
			ImagePlus refOri = new ImagePlus( "refOri " + key, this.stack.getStack().getProcessor( this.idMap.get(key) ) );
			refOri = LibImage.convertContrastByte( refOri );
			refOri.setProcessor( refOri.getProcessor().convertToFloat() );
			
			//ImagePlus sampleReg = new ImagePlus( "sampleReg " + key, transfo.getDirectResults().getStack().getProcessor(1) );
			ImagePlus ref = new ImagePlus( "ref " + key, transfo.getDirectResults().getStack().getProcessor(2) );
			ImagePlus refReg = new ImagePlus( "refReg " + key, transfo.getInverseResults().getStack().getProcessor(1) );
			ImagePlus sample = new ImagePlus( "sample " + key, transfo.getInverseResults().getStack().getProcessor(2) );
			composite.getStack().setProcessor( ref.getProcessor().convertToByteProcessor(), nChannels*index + 1 );
			composite.getStack().setProcessor( refReg.getProcessor().convertToByteProcessor(), nChannels*index + 2 );
			composite.getStack().setProcessor( sample.getProcessor().convertToByteProcessor(), nChannels*index + 3 );
			composite.getStack().setProcessor( refOri.getProcessor().convertToByteProcessor(), nChannels*index + 4 );
			
			//LibError libError = new LibError( ref, sample );
			//LinkedHashMap< String, Double> error = libError.measureError();
			//errorList.put(key, error );
			
			index++;
		}
		return composite;
	}
	
	public LinkedHashMap< String, LinkedHashMap< String, Roi > > annotate( ArrayList<String> refList ) {

		//overlayStack
		LinkedHashMap< String, LinkedHashMap< String, Roi > > roiMapListT = new LinkedHashMap<>();
		for ( String key : refList ) {
			LinkedHashMap< String, Roi > roiMap = annotateOne( key );
			roiMapListT.put(key, roiMap);
		}
		return roiMapListT;
	}

	public void annotateShow( LinkedHashMap< String, LinkedHashMap< String, Roi > > roiMapList ) {
	
		for ( String key : roiMapList.keySet() ) {
			ImageProcessor ip = this.stack.getStack().getProcessor( idMap.get(this.sampleId) );
			LinkedHashMap< String, Roi > roiMap = roiMapList.get(key);
			ImagePlus imp = new ImagePlus( "ref_overlay", ip );
			ImagePlus overlay = getOverlayImage( roiMap , imp );
			overlay.setHideOverlay(false);
			overlay.setTitle( "overlay " + key );
			ImagePlus overlay_flattened = overlay.flatten();
			//overlay_flattened.show();
			//overlayStack.getStack().setProcessor( ref_overlay_flattened.getProcessor() , 2*index - 1 );
			//overlayStack.getStack().setProcessor( overlay_flattened.getProcessor() , 2*index );
			//overlayStack.getStack().setSliceLabel(sliceLabel, 2*index - 1 );
			//overlayStack.getStack().setSliceLabel(sliceLabel, 2*index );

		}
	}

        
        public static void roisImageShow( ImagePlus imp, LinkedHashMap< String, Roi > roiMap, String title ) {
	
            ImagePlus overlay = getOverlayImage( roiMap , imp );
            overlay.setHideOverlay(false);
            overlay.setTitle( title );
            ImagePlus overlay_flattened = overlay.flatten();
            overlay_flattened.show();

	}
        
        public void annotateShowOne( LinkedHashMap< String, Roi > roiMap, String title ) {
	
            ImageProcessor ip = this.target.getProcessor();
            ImagePlus imp = new ImagePlus( "ref_overlay", ip );
            ImagePlus overlay = getOverlayImage( roiMap , imp );
            overlay.setHideOverlay(false);
            overlay.setTitle( title );
            ImagePlus overlay_flattened = overlay.flatten();
            overlay_flattened.show();

	}

        
	public LinkedHashMap< String, Roi > annotateOne( String refId ) {

		LinkedHashMap<String,Roi> roiMap = this.stackProps.get(refId).roiMap;
		AffineTransform2D tvecRef = this.transfosCongealing.get(refId);
		AffineTransform2D tvecSample = this.transfosCongealing.get(this.sampleId);
                //this.annotateShowOne( roiMap, "ROIs before elastic reg" );
                Transformation elasticT = this.transfosElastic.get(refId);
		//Transformation elasticTSample = this.transfosElastic.get(this.sampleId);
		LinkedHashMap<String,Roi> roisT = ElasticTransform2D.applyRoiRefTransform( roiMap, tvecRef, elasticT, tvecSample );
                //this.annotateShowOne( roisT, "ROIs after elastic reg" );

		return roisT;
	}

	public void annotateShowComposite( LinkedHashMap< String, LinkedHashMap< String, Roi > > roiMapList ) {
	
		int nChannels = 2; 
		int nSlices = roiMapList.size();
		int nFrames = 1;
        ImagePlus overlayStack = IJ.createHyperStack("merged", this.source.getWidth(), this.source.getHeight(), nChannels, nSlices, nFrames, 24);

        int sliceIndex = 0;
        for (String key : roiMapList.keySet()) {

            Transformation transfo = this.transfosElastic.get(key);

            //this.stack.show();
            ImageProcessor ip = this.stack.getStack().getProcessor(idMap.get(this.sampleId)).duplicate();
            LinkedHashMap< String, Roi> roiMap = roiMapList.get(key);
            ImagePlus imp = new ImagePlus("ref_overlay", ip);
            ImagePlus overlay = getOverlayImage(roiMap, imp);
            overlay.setHideOverlay(false);
            overlay.setTitle("overlay " + key);
            ImagePlus overlay_flattened = overlay.flatten();

            ImagePlus reg = new ImagePlus("reg", transfo.getDirectResults().getStack().getProcessor(1));
            ImagePlus regInverse = new ImagePlus("regInverse", transfo.getInverseResults().getStack().getProcessor(1));
            regInverse = applyInverseTransform(this.transfosCongealing.get(this.sampleId), regInverse);

            ImagePlus sample = convertContrastByte(imp);
            reg = convertContrastByte(reg);
            regInverse = convertContrastByte(regInverse);
            int refIndex = idMap.get(key);
            sliceIndex++;
            ImageStack compositeStack = RGBStackMerge.mergeStacks(sample.getStack(), regInverse.getStack(), null, false);
            ImagePlus composite = new ImagePlus();
            composite.setStack(compositeStack);
            ImagePlus composite_overlay = getOverlayImageRGB(roiMap, composite.duplicate());
            composite_overlay.setHideOverlay(false);
            composite_overlay.setTitle("overlay " + key);
            ImagePlus composite_overlay_flattened = composite_overlay.flatten();

            //overlay_flattened.show();
            overlayStack.getStack().setProcessor(overlay_flattened.getProcessor(), 2 * sliceIndex - 1);
            overlayStack.getStack().setProcessor(composite_overlay_flattened.getProcessor(), 2 * sliceIndex);
            //overlayStack.getStack().setSliceLabel(sliceLabel, 2*index - 1 );
            //overlayStack.getStack().setSliceLabel(sliceLabel, 2*index );

        }
        overlayStack.show();
    }

    public Transformation registerOne(String sampleId, String refId) {

        // TODO: change the input type (stackprops???)
        int sampleIndex = sortedIndices[sortedIndices.length - 1] + 1;
        int refIndex = this.stackProps.get(refId).index;
        this.source = new ImagePlus("sample", alignedStack.getStack().getProcessor(sampleIndex).convertToByte(true));
        this.target = new ImagePlus("ref", alignedStack.getStack().getProcessor(refIndex).convertToByte(true));
        //this.source.show();
        //this.target.show();

        // Obtaining SIFT features for the registration
        LinkedHashMap<String, Roi> out;
        switch (param.REGISTRATION_FEATURE_METHOD) {
            case ElasticRegistration.METHOD_FEATURES_SIFT:
                out = siftSingle(this.source, this.target, param.getSiftParam().getSiftParamFloat());
                this.sourceRoiPoints = out.get("roiSource");
                this.targetRoiPoints = out.get("roiTarget");
                IJ.log(" ------------- SIFT FEATURES   ---------------");
                IJ.log("Parameters:");
                IJ.log(param.getSiftParam().toString());
                IJ.log(" ---------------------------------------------");
                break;

            case ElasticRegistration.METHOD_FEATURES_HARRIS:
                LinkedHashMap<String, Double> harrisParam = param.getHarrisParam().getHarrisParamDouble();
                out = LibRegistration.harrisSingle(this.source, this.target, harrisParam);
                this.sourceRoiPoints = out.get("roiSource");
                this.targetRoiPoints = out.get("roiTarget");
                IJ.log(" ------------- SIFT FEATURES   ---------------");
                IJ.log("Parameters:");
                IJ.log(harrisParam.toString());
                IJ.log(" ---------------------------------------------");
                break;
        }

        // Add SIFT features to the ROIs of the source and target images
        this.source.setRoi(this.sourceRoiPoints);
        this.target.setRoi(this.targetRoiPoints);
        if (this.debug) {
            this.source.duplicate().show();
            this.target.duplicate().show();
        }

        // Warping procedure
        LinkedHashMap< String, Double> bunwarpjParam = param.getBunwarpjParam().getBunwarpjParamDouble();
        Transformation transfo = LibRegistration.bunwarpj_param(this.source, this.target, bunwarpjParam);
        IJ.log(" ----------- BunwarpJ Parameters -------------");
        IJ.log(bunwarpjParam.toString());
        IJ.log(" ---------------------------------------------");

        // Save registered image
        String outputImageCompositePath = param.OUTPUT_FOLDER + "/" + param.FILENAME_PREFIX_REGISTERED_COMPOSITE_IMAGE + param.ID_SAMPLE + "_" + refId + ".tif";
        String outputImageRegPath = param.OUTPUT_FOLDER + "/" + param.FILENAME_PREFIX_REGISTERED_IMAGE + param.ID_SAMPLE + "_" + refId + param.FORMAT_OUTPUT_GRAY_IMAGES;

        ImagePlus reg = transfo.getDirectResults();
        if (this.debug) {
            reg.show();
        }
        ContrastEnhancer ce = new ContrastEnhancer();
        ce.stretchHistogram(reg, 0.35);
        ImagePlus impReg = new ImagePlus("Registered image", reg.getStack().getProcessor(1).convertToShort(false));
        //IJ.log("Saving registration image: " + outputImageRegPath);
        //IJ.save(impReg, outputImageRegPath);

        // Save composite image
        int nChannels = 2;
        int nSlices = 1;
        int nFrames = 1;
        ImagePlus impMerged = IJ.createHyperStack("merged", this.target.getWidth(), this.target.getHeight(), 2, 1, 1, this.target.getBitDepth());
        if (this.debug) {
            reg.show();
            ImageProcessor debug1 = reg.getStack().getProcessor(1);
        }
        impMerged.getImageStack().setProcessor(LibImage.convertSimilar(reg.getStack().getProcessor(1), this.target.getProcessor()), 2);
        impMerged.getImageStack().setProcessor(this.target.getProcessor(), 1);
        impMerged.setDimensions(nChannels, nSlices, nFrames);
        CompositeImage impComp = new CompositeImage(impMerged, CompositeImage.COMPOSITE);
        //IJ.run(impComp, "Enhance Contrast", "saturated=0.35");
        if (this.debug) {
            impComp.show();
        }
        //IJ.log("Saving registration image (composite): " + outputImageCompositePath);
        //IJ.saveAsTiff(impComp, outputImageCompositePath);

        return transfo;
    }

	
	
}
