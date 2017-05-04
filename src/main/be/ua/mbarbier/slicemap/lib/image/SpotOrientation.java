/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib.image;

import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.roiFromMask;
import main.be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.io.File;

/**
 *
 * @author mbarbier
 */
public class SpotOrientation {
	
	public static double getAngle( ImagePlus imp ) {
		double angle = 0;
		
		double averageSize = Math.sqrt( imp.getWidth() * imp.getHeight() );
		double scaleSize = 100;
		double sigmaRatio = 0.2;
		ImageProcessor ip = imp.getProcessor().duplicate();
		double maxValue = ip.getMax();
		ip.setBackgroundValue(0.0);
		//ip.scale( scaleSize / averageSize, scaleSize / averageSize );
		ip.resize( (int) Math.round( (scaleSize / averageSize) * imp.getWidth() ) );
		ip.blurGaussian(sigmaRatio * scaleSize);
		//new ImagePlus("ip blur", ip.duplicate()).show();
		ImageProcessor ipt = ip.duplicate();
		ipt.setBackgroundValue(0.0);
		ipt.autoThreshold();
		ImagePlus impt = new ImagePlus("ipt",ipt);
		//impt.show();
		Roi roi = roiFromMask( impt );
		ipt.setRoi(roi);
		//ipt.rotate(80);
		ImageStatistics stats = ImageStatistics.getStatistics(ipt, ImageStatistics.ELLIPSE, imp.getCalibration() );
		//IJ.log( stats.toString() + stats.angle );
		angle = stats.angle;
		
		return angle;
	}
	
	public static void main(String[] args) {
        
        ImageJ imagej = new ImageJ();

		String srcPath = "D:/p_prog_output/slicemap/input/reference_stack/Beerse21.tif";
        File srcFile = new File(srcPath);
        ImagePlus impStack = IJ.openImage(srcFile.getAbsolutePath());
		ImageStack stack = impStack.getStack();
	
		int nSlices = 1;//stack.getSize();
		for (int i = 1; i < nSlices+1; i++) {
	        ImageProcessor ip = stack.getProcessor(i);
			ImageStatistics stats = ip.getStatistics();
			int[] h = stats.histogram;
			ImagePlus imp = new ImagePlus( "ip ori", ip);
			imp.show();
			getAngle( imp );
		}
	}

}
