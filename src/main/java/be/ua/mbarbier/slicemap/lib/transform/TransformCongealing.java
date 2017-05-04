/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ua.mbarbier.slicemap.lib.transform;

import be.ua.mbarbier.slicemap.ImageProperties;
import static be.ua.mbarbier.slicemap.lib.transform.Transform2D.applyRefTransform;
import ij.ImagePlus;
import ij.gui.Roi;
import java.util.LinkedHashMap;
import net.imglib2.realtransform.AffineTransform2D;

/**
 *
 * @author mbarbier
 */
public class TransformCongealing {

	public int sampleIndex;
	public int lastRefIndex;
	public LinkedHashMap< String, Integer > sampleMirrorIndexMap;
	public LinkedHashMap< String, Integer > refIndexMap;

	public LinkedHashMap< String, AffineTransform2D > transformVec;
    public LinkedHashMap< String, AffineTransform2D > preTransformVec;

	public LinkedHashMap< String, ImageProperties > stackProps;

	public TransformCongealing( LinkedHashMap< String, AffineTransform2D > preTransformVec, LinkedHashMap< String, AffineTransform2D > transformVec, int sampleIndex, int lastRefIndex, LinkedHashMap< String, ImageProperties > stackProps ) {

		this.lastRefIndex = lastRefIndex;
		this.sampleIndex = sampleIndex;
		this.preTransformVec = preTransformVec;
		this.transformVec = transformVec;
		this.stackProps = stackProps;
		this.sampleMirrorIndexMap = new LinkedHashMap<>();
		for ( int i = 0; i < lastRefIndex; i++ ) {
		
//			this.sampleMirrorIndexMap.put(key, i)
		}
	}

	public LinkedHashMap<String, AffineTransform2D> getTransformVec() {
		return transformVec;
	}

	public LinkedHashMap<String, AffineTransform2D> getPreTransformVec() {
		return preTransformVec;
	}
	
	

	/**
	 * refIndex index of the reference slice (1-based)
	 * imageIndex index of the image slice (1-based)
	 * 
	 * @param refId
	 * @param imageId 
	 * @param scale Get the transformed roi's being scaled with scale
	 * @return roiRefT the transformed ROI for the 
	 */
	public LinkedHashMap< String, Roi > getTransformedRois(String refId, String imageId, double scale ) {

		LinkedHashMap<String, Roi> roiRef = this.stackProps.get(refId).roiMap;
		double xOffset = (double) this.stackProps.get(refId).xOffset;
		double yOffset = (double) this.stackProps.get(refId).yOffset;
		int refIndex = this.stackProps.get(refId).index;
		int imageIndex = this.stackProps.get(imageId).index;

		AffineTransform2D pretvecRef = Transform2D.scale( this.preTransformVec.get(refId), scale, 0., 0. );
		AffineTransform2D tvecRef = Transform2D.scale( this.transformVec.get(refId), scale, 0., 0. );
		AffineTransform2D pretvecSample = Transform2D.scale( this.preTransformVec.get(imageId), scale, 0., 0. );
		AffineTransform2D tvecSample = Transform2D.scale( this.transformVec.get(imageId), scale, 0., 0. );

		LinkedHashMap<String, Roi> roiRefS = roiRef;
		LinkedHashMap<String, Roi> roiRefT = Transform2D.applyRoiRefTransform(roiRefS, pretvecRef, tvecRef, pretvecSample, tvecSample );
		return roiRefT;
	}

	/**
	 * refIndex index of the reference slice (1-based)
	 * imageIndex index of the image slice (1-based)
	 * 
	 * @param refId
	 * @param imageId 
	 * @param scale Get the transformed roi's being scaled with scale
	 * @param stack The unaligned stack (before prewarping) 
	 *	(TODO: What with the mirrored samples???, luckily we only use the refs, 
	 *	samples & refs should be separated in the future though)
	 * @return roiRefT the transformed ROI for the 
	 */
	public ImagePlus getTransformedRef( String refId, String imageId, double scale, ImagePlus stack ) {

		int refIndex = this.stackProps.get(refId).index;
		int imageIndex = this.stackProps.get(imageId).index;

		AffineTransform2D preTRef = Transform2D.scale( this.preTransformVec.get(refId), scale, 0., 0. );
		AffineTransform2D tRef = Transform2D.scale( this.transformVec.get(refId), scale, 0., 0. );
		AffineTransform2D preTSample = Transform2D.scale( this.preTransformVec.get(imageId), scale, 0., 0. );
		AffineTransform2D tSample = Transform2D.scale( this.transformVec.get(imageId), scale, 0., 0. );

		ImagePlus ref = new ImagePlus( stack.getStack().getSliceLabel(refIndex), stack.getStack().getProcessor(refIndex) );
		ImagePlus refT = applyRefTransform( ref, preTRef, tRef, preTSample, tSample );

		return refT;
	}
}
