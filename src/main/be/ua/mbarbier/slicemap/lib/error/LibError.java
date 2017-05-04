/**
 * 
 */
package main.be.ua.mbarbier.slicemap.lib.error;

import java.util.LinkedHashMap;
import main.be.ua.mbarbier.external.Hausdorff_Distance;
import main.be.ua.mbarbier.external.Hausdorff_Distance2;
import main.be.ua.mbarbier.slicemap.lib.Lib;
import main.be.ua.mbarbier.slicemap.lib.image.LibImage;
import main.be.ua.mbarbier.slicemap.lib.image.colocalization.Colocalization;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortBlitter;

/**
 * @author mbarbie1
 *
 */
public class LibError {

	ImageProcessor ip1;
	ImageProcessor ip2;
	Roi roi1;
	Roi roi2;

	public ImageProcessor getIp1() {
		return this.ip1;
	}
	public ImageProcessor getIp2() {
		return this.ip2;
	}

	public LibError( ImagePlus imp1, ImagePlus imp2 ) {
		// We make a duplicate of the image processors
		this.ip1 = imp1.getProcessor().duplicate();
		this.ip2 = imp2.getProcessor().duplicate();
		// Copy the ROI to a separate variables roi1 and roi2
		this.roi1 = imp1.getRoi();
		this.roi2 = imp2.getRoi();
		// Remove the ROI from the duplicates
		this.ip1.resetRoi();
		this.ip2.resetRoi();
	}

	/**
	 * The crossCorrelation (unnormalized) is defined as 
	 * CC(X,Y) = sum_i(Xi*Yi) / sqrt( sum_i(Xi)^2 * sum_i(Yi)^2 )
	 * 
	 * @return crossCorrelation (unnormalized)
	 */
	public double CC() {
		double cc = 0;
		double denom = Math.sqrt( LibImage.sumOfSquares( ip1 ) ) * Math.sqrt( LibImage.sumOfSquares( ip2 ) );
		double num = LibImage.sumOfProduct( ip1, ip2 );
		cc = num / denom;

		return cc;
	}

	/**
	 * The Pearson correlation
	 * 
	 * @return 
	 */
	public double pearson() {
            Colocalization coloc = new Colocalization( new ImagePlus( "ip1 dup for coloc", ip1.duplicate() ), new ImagePlus( "ip2 dup for coloc", ip2.duplicate() ) );

            return coloc.getPearson();
        }        

	/**
	 * The multi-scale Pearson correlation
	 * 
	 * @return 
	 */
	public double[] pearsonScale( double[] scales ) {
            
            int n = scales.length;
            double[] pcs = new double[n];
            for (int i = 0; i < n; i++ ) {
                ip1.blurGaussian(scales[i]);
                Colocalization coloc = new Colocalization( new ImagePlus( "ip1 dup for coloc", ip1.duplicate() ), new ImagePlus( "ip2 dup for coloc", ip2.duplicate() ) );
                pcs[i] = coloc.getPearson();
            }
            return pcs;
        }        
        
	/**
	 * The normalized crossCorrelation is defined as 
	 * NCC(X,Y) = sum_i((Xi-X)*(Yi-Y)) / sqrt( sum_i(Xi-X)^2 * sum_i(Yi-Y)^2 )
	 * 
	 * @return normalized crossCorrelation
	 */
	public double NCC() {
		double ncc = 0;
		ImageStatistics stats1 = ip1.getStatistics();
		ImageStatistics stats2 = ip2.getStatistics();
                //if ( ip1.getBitDepth() == 16 )
		ImageProcessor ipd1 = ip1.duplicate().convertToFloat();
		ImageProcessor ipd2 = ip2.duplicate().convertToFloat();
		ipd1.subtract(stats1.mean);
		ipd2.subtract(stats2.mean);
                
                //new ImagePlus("ip1 NCC", ip1 ).show();
                //new ImagePlus("ip2 NCC", ip2 ).show();
                //new ImagePlus("subtracting mean from ip1", ipd1 ).show();
                //new ImagePlus("subtracting mean from ip2", ipd2 ).show();
                
		double denom = Math.sqrt( LibImage.sumOfSquares( ipd1 ) * LibImage.sumOfSquares( ipd2 ) );
		double num = LibImage.sumOfProduct( ipd1, ipd2 );
		ncc = num / denom;

		return ncc;
	}

	/**
	 * The MSE (= Mean Square Error) is defined as
	 * MSE(X,Y) = sum_i(Yi-Xi)^2 / N, 
	 * Here we take into account only occupied pixels 
	 * (only pixels where at least one of the images is nonzero)
	 * 
	 * @return Mean Square Error (using only occupied pixels)
	 */
	public double MSE_nonEmpty() {
		ImageProcessor ip1Temp = ip1.duplicate();
		ip1Temp.copyBits(ip2, 0, 0, ShortBlitter.SUBTRACT);

		return LibImage.sumOfSquares( ip1Temp ) / LibImage.maskArea(  LibImage.maskUnion( ip1, ip2 ) );
	}

	/**
	 * The MSE (= Mean Square Error) is defined as 
	 * MSE(X,Y) = sum_i(Yi-Xi)^2 / N
	 * 
	 * @return Mean Square Error
	 */
	public double MSE() {
		ImageProcessor ip1Temp = ip1.duplicate();
		ip1Temp.copyBits(ip2, 0, 0, ShortBlitter.SUBTRACT);

		return LibImage.sumOfSquares( ip1Temp ) / LibImage.area( ip1 );
	}

	/**
	 * The RMSE (= Root Mean Square Error) is defined as 
	 * RMSE(X,Y)  = sqrt[ sum_i(Yi-Xi)^2 / N ]
	 * 
	 * @return Root Mean Square Error
	 */
	public double RMSE() {

		return Math.sqrt( MSE_nonEmpty() );
	}

	/**
	 * The normalized RMSE (= Root Mean Square Error) is defined as 
	 * NRMSE(X,Y)  = sqrt[ sum_i(Yi-Xi)^2 / N ] / ( max(Yi) - min(Yi) )
	 * 
	 * @return normalized RMSE
	 */
	public double NRMSE() {
		ImageStatistics stats = ip1.getStatistics();

		return RMSE() / (stats.max-stats.min);
	}

	/**
	 * The Coefficient of Variation normalized RMSE (= Root Mean Square Error) is defined as 
	 * CVRMSE(X,Y)  = sqrt[ sum_i(Yi-Xi)^2 / N ] / mean(Yi) )
	 * 
	 * @return Coefficient of Variation
	 */
	public double CVRMSE() {
		ImageStatistics stats = ip1.getStatistics();

		return RMSE() / ( stats.mean );
	}

	/**
	 * Calculation of the pixel intensity based error measures between both images:
	 * 	Cross-correlation
	 *  Normalized cross-correlation
	 *  Mean Square Error
	 *  Root Mean Square Error
	 *  Normalized Root Mean Square Error 
	 *  Coefficient of Variation
	 * 
	 * @return LinkedHashMap of error measures
	 */
	public LinkedHashMap<String, Double> measureError( Boolean extendedOutput ) {
		ImageStatistics stats = ip1.getStatistics();

                //new ImagePlus("ip1 in LibError", ip1).show();
                //new ImagePlus("ip2 in LibError", ip2).show();
                //new ImagePlus("subtracting ip2 from ip1 in LibError", LibUtilities.minus(ip1,ip2) ).show();
                //IJ.log("AREA whole image = " + LibUtilities.area(ip1) );
                double mse = LibImage.sumOfSquares( LibImage.minus(ip1,ip2) ) / LibImage.area(ip1);
                //IJ.log("MSE = " + mse);
		double rmse = Math.sqrt( mse );
		double n_rmse = rmse / (stats.max-stats.min);
		double cv_rmse = rmse / stats.mean;

		LinkedHashMap<String, Double> error = new LinkedHashMap<String, Double>();
                error.put("pearson", pearson() );
                double[] scales = Lib.logSpacedSequence( 1.0, 128.0, 14 );
                double[] pcs = pearsonScale( scales );
				if ( extendedOutput ) {
					for (int i = 0; i < pcs.length; i++ ) {
						error.put( "pearson_" + Integer.toString(i) , pcs[i] );
						error.put( "scale_" + Integer.toString(i), scales[i] );
					}
				}
                double pearsonMaxValue = Lib.max( pcs ).getValue();
                int pearsonMaxIndex = Lib.max( pcs ).getKey();
                error.put( "pearson_max" , pearsonMaxValue );
                error.put( "scale_max", scales[pearsonMaxIndex] );
		error.put("cc", CC() );
		error.put("ncc", NCC() );
		error.put("mse_roi", MSE_nonEmpty());
		error.put("mse", mse );
		error.put("rmse", rmse );
		error.put("n_rmse", n_rmse );
		error.put("cv_rmse", cv_rmse );

                return error;
	}

	/**
	 * The VOP (= Volume Overlap Percentage, or SI = Similarity Index) is defined as:
	 *  VOPj  =  2 * V( intersection(Aj,Bj) ) / ( V(Aj) + V(Bj) )
	 *  with V() the volume function, label j, and labeled pixels in image A and B.
	 * 
	 * @return VOP = si, the VOP = si1 with reference of the first image, the VOP = si2 with reference of the second image
	 */
	public static LinkedHashMap<String, Double> VOP( ImageProcessor mask1, ImageProcessor mask2 ) {
		//ImageProcessor mask1 = LibUtilities.mask( ip1, 0.0, 1.0 ).getProcessor();
		//ImageProcessor mask2 = LibUtilities.mask( ip2, 0.0, 1.0 ).getProcessor();
		double MaskArea1 = LibImage.maskAreaRoi( mask1 );
		double MaskArea2 = LibImage.maskAreaRoi( mask2 );
		ImageProcessor mI = LibImage.maskIntersection( mask1, mask2 );
		double MaskAreaI = LibImage.maskArea(mI);
		double si = 2 * MaskAreaI / ( MaskArea1 + MaskArea2);
		double si1 = MaskAreaI / MaskArea1;
		double si2 = MaskAreaI / MaskArea2;
		LinkedHashMap<String, Double> vop = new LinkedHashMap<String, Double>();
		vop.put( "si", si );
		vop.put( "si1", si1 );
		vop.put( "si2", si2 );
		
		return vop;
	}

	/**
	 * The VOP (= Volume Overlap Percentage, or SI = Similarity Index) is defined as:
	 *  VOPj  =  2 * V( intersection(Aj,Bj) ) / ( V(Aj) + V(Bj) )
	 *  with V() the volume function, label j, and labeled pixels in image A and B.
	 * 
	 * @return VOP = si, the VOP = si1 with reference of the first image, the VOP = si2 with reference of the second image
	 */
	public static LinkedHashMap<String, Double> roiVOP( Roi roi1, Roi roi2 ) {

		double MaskArea1 = roi1.getStatistics().area;
		double MaskArea2 = roi2.getStatistics().area;
                
                // Testing the rois --------------------------------------------
                // TODO
                // Roi[] rois = new Roi[]{roi1,roi2};
                // ImagePlus imp = getOverlayImage( rois, NewImage.createByteImage("roiTest", 1000, 1000, 1, 0) );
                // imp.show();
                // ------------------------------------------------------------- 
                 
		Roi mI = LibImage.maskIntersectionRoi( roi1, roi2 );
		double MaskAreaI = 0.0;
		if (mI != null) {
                    MaskAreaI = mI.getStatistics().area;
		}
		double si = 2 * MaskAreaI / ( MaskArea1 + MaskArea2);
		double si1 = MaskAreaI / MaskArea1;
		double si2 = MaskAreaI / MaskArea2;
		LinkedHashMap<String, Double> vop = new LinkedHashMap<String, Double>();
		vop.put( "si", si );
		vop.put( "si1", si1 );
		vop.put( "si2", si2 );

		return vop;
	}

	/**
	 * The Hausdorff distance is the maximum of the minimal border distance between the pixels of the two mask borders
	 * The averaged Hausdorff distance is the average of the minimal border distance between the pixels of the two mask borders
	 * 
	 * @return  Hausdorff distance = hd, Averaged Hausdorff distance = hda
	 */
	public LinkedHashMap<String, Double> haussdorffError() {

		LinkedHashMap<String, Double> error = new LinkedHashMap<String, Double>();

		ImagePlus mask1 = LibImage.smoothMask( new ImagePlus("ip1",ip1), 1, 2, "Triangle");//
		ImagePlus mask2 = LibImage.smoothMask( new ImagePlus("ip2",ip2), 1, 2, "Triangle");//
		//mask1.show();
		//mask2.show();
		//ImagePlus mask1 = LibUtilities.mask( ip1, 0.0, 1.0 );
		//ImagePlus mask2 = LibUtilities.mask( ip2, 0.0, 1.0 );
		Hausdorff_Distance hd = new Hausdorff_Distance();
		hd.exec( mask1, mask2 );
		error.put("hd", hd.getHausdorffDistance() );
		error.put("hda", hd.getAveragedHausdorffDistance() );

		return error;
	}

	/**
	 * The Hausdorff distance is the maximum of the minimal border distance between the pixels of the two mask borders
	 * The averaged Hausdorff distance is the average of the minimal border distance between the pixels of the two mask borders
	 * 
	 * @return  Hausdorff distance = hd, Averaged Hausdorff distance = hda
	 */
	public static LinkedHashMap<String, Double> haussdorffErrorRoi( Roi roi1, Roi roi2 ) {
		LinkedHashMap<String, Double> error = new LinkedHashMap<String, Double>();
		
		Hausdorff_Distance2 hd = new Hausdorff_Distance2();
		hd.exec( roi1, roi2 );
		error.put("hd", hd.getHausdorffDistance() );
		error.put("hda", hd.getAveragedHausdorffDistance() );

		return error;
	}


        
}
