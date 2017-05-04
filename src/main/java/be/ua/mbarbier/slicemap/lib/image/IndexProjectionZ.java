/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ua.mbarbier.slicemap.lib.image;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 *
 * @author mbarbier
 */
public class IndexProjectionZ {
	
	/** Compute max intensity projection. */
	int BYTE_TYPE = 8;
	int SHORT_TYPE = 16;
	int FLOAT_TYPE = 32;
	int startSlice;
	int stopSlice;
	int increment;
	ImagePlus imp;
	ImagePlus h;
	ImagePlus hMasks;

	public IndexProjectionZ( ImagePlus imp ) {
		this.startSlice = 1;
		this.stopSlice = imp.getStackSize();
		this.increment = 1;
		this.imp = imp;
	}
	
	public ImagePlus getH() {
		return this.h;
	}
	
	public ImagePlus getHMasks() {
		return this.hMasks;
	}

	public void doProjection() {

		// Create new float processor for projected pixels.
		FloatProcessor fp = new FloatProcessor(imp.getWidth(),imp.getHeight()); 
		ImageStack stack = imp.getStack();

		// Determine type of input image. Explicit determination of
		// processor type is required for subsequent pixel
		// manipulation.  This approach is more efficient than the
		// more general use of ImageProcessor's getPixelValue and
		// putPixel methods.
		int ptype; 
		if (stack.getProcessor(1) instanceof ByteProcessor) ptype = BYTE_TYPE; 
		else if (stack.getProcessor(1) instanceof ShortProcessor) ptype = SHORT_TYPE; 
		else if (stack.getProcessor(1) instanceof FloatProcessor) ptype = FLOAT_TYPE; 
		else {
	    	IJ.error("Z Project", "Non-RGB stack required");
	    	return; 
		}

		// Do the projection
		int sliceCount = 0;
		MaxProjectionZ mz = new MaxProjectionZ( (FloatProcessor) stack.getProcessor(1));
		for (int n=this.startSlice; n<=this.stopSlice; n+=this.increment) {
			switch ( this.imp.getBitDepth() ) {
				case 8 :
					mz.projectSlice( (byte[]) stack.getPixels(n), n);
					break;
				case 16 :
					mz.projectSlice( (short[]) stack.getPixels(n), n);
					break;
				case 32 :
					mz.projectSlice( (float[]) stack.getPixels(n), n);
					break;
			}
	    	sliceCount++;
		}
		this.h = mz.getHeightMap();
		this.hMasks = mz.getHeightMasks();
    }
	
	class MaxProjectionZ {

		private float[] fpixels;
		private int[] ipixels;
		private int len; 

		/** Simple constructor since no preprocessing is necessary. */
		public MaxProjectionZ( FloatProcessor fp ) {

			fpixels = (float[]) fp.getPixels();
			len = fpixels.length;
			ipixels = new int[len];
			for (int i=0; i<len; i++) {
				fpixels[i] = 0.0001f;//-Float.MAX_VALUE;
				ipixels[i] = 0;
			}
		}

		public void projectSlice( byte[] pixels, int index ) {
			for( int i = 0; i < len; i++ ) {
				if( (pixels[i]&0xff) > fpixels[i] ) {
					fpixels[i] = pixels[i]&0xff;
					ipixels[i] = index;
				}
			}
		}

		public void projectSlice(short[] pixels, int index) {
			for( int i = 0; i < len; i++ ) {
				if( (pixels[i]&0xffff) > fpixels[i]) {
					fpixels[i] = pixels[i]&0xffff;
					ipixels[i] = index;
				}
			}
		}

		public void projectSlice(float[] pixels, int index) {
			for(int i = 0; i < len; i++ ) {
				if( pixels[i] > fpixels[i] ) {
					fpixels[i] = pixels[i]; 
					ipixels[i] = index;
				}
			}
		}

		/**
		 * 
		 * 
		 * @return 
		 */
		public ImagePlus getHeightMap() {

			ImagePlus h = IJ.createImage("h", imp.getWidth(), imp.getHeight(), 1, SHORT_TYPE );
			ImageProcessor oip = h.getProcessor();
			short[] ipixels16 = (short[]) oip.getPixels(); 
			for(int i=0; i < ipixels16.length; i++) {
				ipixels16[i] = (short) this.ipixels[i];
			}

			return h;
		}

		/**
		 * 
		 * 
		 * @return 
		 */
		public ImagePlus getHeightMasks() {

			ImagePlus oimp = IJ.createImage( "heightMasks", imp.getWidth(), imp.getHeight(), imp.getNSlices(), BYTE_TYPE );
			for( int n=1; n < (imp.getNSlices()+1); n++ ) {
				byte[] ipixels8 = (byte[]) oimp.getStack().getProcessor(n).getPixels();
				for( int i = 0; i < ipixels8.length; i++) {
					if ( this.ipixels[i] == n ) {
						ipixels8[i] = Byte.MAX_VALUE;
					}
				}
			}
			return oimp;
		}
		
    } // end MaxIntensity
}