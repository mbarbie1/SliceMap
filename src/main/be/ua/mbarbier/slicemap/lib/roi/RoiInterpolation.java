/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib.roi;

import static main.be.ua.mbarbier.slicemap.lib.Lib.linearSpacedSequence;
import static main.be.ua.mbarbier.slicemap.lib.roi.LabelFusion.majorityVoting;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.intersectRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.mulRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.roiFromMask;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.roisFromThreshold;
import main.be.ua.mbarbier.slicemap.lib.transform.TransformRoi;
import ij.plugin.filter.EDM;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Plot;
import ij.gui.ShapeRoi;
import ij.gui.Wand;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.lingala.zip4j.exception.ZipException;

/**
 *
 * @author mbarbier
 */
public class RoiInterpolation {
	
	public static final String SMOOTH_MEAN = "mean";
	public static final String SMOOTH_GAUSS = "gauss";
	public final String INTERPOLATION_DISTANCE_SQRT = "INTERPOLATION_DISTANCE_SQRT";
	public final String INTERPOLATION_DISTANCE_LINEAR = "INTERPOLATION_DISTANCE_LINEAR";
	public final String INTERPOLATION_DISTANCE_SIGMOID_1 = "INTERPOLATION_DISTANCE_SIGMOID_1";
	public final double SIGMA_SMOOTH = 3.0;
	public final double INTERVAL_RATIO_SMOOTH = 0.005;
	public PolygonRoi roiCurve;
	public double[] roiValues;
	public ImagePlus mse;
	public ImagePlus mde;
	public ImagePlus mie;
	public ImagePlus mle;
	public ImagePlus me_sigmoid;
	public PolygonRoi roiBandOuter;
	public PolygonRoi roiBandInner;
	public String interpolation_distance = this.INTERPOLATION_DISTANCE_LINEAR;
	//public PolygonRoi roiMean;

	public static class HeightRoi {
		public Roi roi;
		public double h;
		
		public HeightRoi(Roi roi, double h) {
			this.roi = roi;
			this.h = h;
		}
	}	

	public static class WeightRoi {
		public Roi roi;
		public double w;
		
		public WeightRoi(Roi roi, double w) {
			this.roi = roi;
			this.w = w;
		}
	}	
	
	public static double[] kernelGauss1D(double s) {
		
		double[] xs = linearSpacedSequence( -2.0*Math.round(s), 2.0*Math.round(s), 2*((int) (Math.round(s))+1) );
		double[] kern = xs.clone();
		int nkern = kern.length;
		double N = 1.0 / ( s*Math.sqrt(2.0*Math.PI));
		double a = Math.pow(1.0/s, 2.0) / 2.0;
		for (int i = 0; i < nkern; i++) {
			double x = xs[i];
			kern[i] = N * Math.exp( - a * Math.pow(x, 2.0) );
		}
					
		return kern;
	}

	public static double[] conv1D( double[] x, double[] kern) {
    
		double[] cx = x.clone();
		int nx = x.length;
		int nkern = kern.length;
		for (int i = 0; i < nx; i++) {
			cx[i] = 0;
			for (int j = 0; j < nkern; j++) {
				cx[i] = cx[i] + x[(i+j) % nx] * kern[j];
			}
		}
		
		return cx;
	}

	public static double[] smooth1D( double[] ys, String method, double w) {

		int ny = ys.length;
		double[] yt = ys.clone();
		switch( method ) {
			case "mean":
				for (int i = 0; i < ny; i++)
					ys[i] = (yt[(i-1) % ny] + yt[i] + yt[(i+1) % ny]) / 3.0;
				break;
			case "gauss":
				double s = w;
				double[] kernel = kernelGauss1D(s);
				ys = conv1D( yt, kernel);
				break;
		}

		return ys;
	}

	public static ImagePlus maskFromThreshold(ImagePlus imp, double threshold) {
			//Mask generated from threshold: All pixels above the threshold are retained (works also for floats)
		// Empty mask image
		ImagePlus mask = IJ.createImage("mask_" + imp.getTitle(), imp.getWidth(), imp.getHeight(), 1, 8);
		ImageProcessor maskIP = mask.getProcessor();
		byte[] maskPixels = (byte[]) maskIP.getPixels();
		ImageProcessor ip = imp.getProcessor().convertToFloat();
		float[] pixels = (float[]) ip.getPixels();
		// Obtain the list of indices of pixels whose value is above threshold  
		for (int i = 0; i < pixels.length; i++ ) {
			if ( pixels[i] > threshold ) {
				maskPixels[i] = 1;
			}
		}
		//mask.show();
		return mask;
	}

	public static void plotRoiValues(Roi roi, double[] roiValues) {
    
		FloatPolygon p = roi.getFloatPolygon();
		int n = p.npoints;
		float[] x = p.xpoints;
		float[] y = p.ypoints;

		double[] xd = new double[n];
		double[] yd = new double[n];
		for (int i = 0; i < n; i++) {
			xd[i] = x[i];
			yd[i] = y[i];
		}
		Plot plot = new Plot("Roi curve", "x", "y", x, y);
		plot.add("CIRCLE", xd, yd);
		//plot.show();

		double[] indices = linearSpacedSequence(1.0, (double) n, n);
		Plot plotValues = new Plot("Values", "i", "value", indices, roiValues);
		//plotValues.show();
	}

	public void getRoiValues(ImagePlus imp, PolygonRoi roi) {

		int w = imp.getWidth();
		int h = imp.getHeight();
		ImageProcessor ip = imp.getProcessor();
		double sz = (w+h)/2;
		//double ratio = 0.01;
		boolean smooth = true;
		double interval = Math.max(1.0, INTERVAL_RATIO_SMOOTH*sz);
		FloatPolygon p = roi.getInterpolatedPolygon( interval, smooth);
		this.roiValues = new double[p.npoints];
		float[] px = p.xpoints;
		float[] py = p.ypoints;
		for (int i = 0; i < p.npoints; i++) {
			this.roiValues[i] = (ip.getInterpolatedValue( px[i], py[i]));
		}
		this.roiCurve = new PolygonRoi( p, Roi.POLYGON);

	}
	
	public Point2D perpPoint( double x0, double y0, double x1, double y1, double x2, double y2, double s) {
		//""" computes a point on a distance s from, and perpendicular to the line-segment [p1,p2] (and at the position of p1) """
		double eps = 0.001;
		double dx = x2-x0;
		double dy = y2-y0;
		double a = Math.atan2(dy,dx);
		double x = s * Math.cos( a + Math.PI/2 ) + x1;
		double y = s * Math.sin( a + Math.PI/2 ) + y1;
		Point2D p = new Point2D.Double(x,y);
				
		return p;
	}

	public void roiBands(PolygonRoi roiMean, double[] roiSigma) {
		//""" Compute confidence interval ROIs """
		FloatPolygon p = roiMean.getFloatPolygon();
		float[] px = p.xpoints;
		float[] py = p.ypoints;
		int n = p.npoints;
		float[] ox = new float[n];
		float[] oy = new float[n];
		float[] ix = new float[n];
		float[] iy = new float[n];
		for (int i = 0; i < n; i++ ) {
			float x0;
			float y0;
			if (i-1 >= 0) {
				x0 = px[(i-1) % n];
				y0 = py[(i-1) % n];
			} else {
				x0 = px[(n+i-1) % n];
				y0 = py[(n+i-1) % n];
			}
			float x1 = px[i % n];
			float y1 = py[i % n];
			float x2 = px[(i+1) % n];
			float y2 = py[(i+1) % n];
			double s = roiSigma[i];
			Point2D pxy = perpPoint( x0, y0, x1, y1, x2, y2, s);
			ox[i] = (float) pxy.getX();
			oy[i] = (float) pxy.getY();
			pxy = perpPoint( x0, y0, x1, y1, x2, y2, -s);
			ix[i] = (float) pxy.getX();
			iy[i] = (float) pxy.getY();
		}

		this.roiBandOuter = new PolygonRoi(ox, oy, Roi.POLYGON);
		this.roiBandInner = new PolygonRoi(ix, iy, Roi.POLYGON);

	}
	
	public void roiMSE(ImagePlus imp, ArrayList<Roi> rois) {
		// Parameters
		boolean innerNegative = true;
		int backgroundValue = 0;
		boolean edgesAreBackground = false;

		ImageProcessor ip = imp.getProcessor();
		ArrayList<ImagePlus> masks = new ArrayList<>();
		ArrayList<ImagePlus> dist = new ArrayList<>();
		ArrayList<ImagePlus> distr = new ArrayList<>();
		EDM edm = new EDM();
		
		for (Roi r : rois) {
			ImageProcessor ipt = ip.duplicate();
			ipt.setLineWidth(1);
			ipt.setColor(Color.WHITE);
			r.drawPixels(ipt);
			ipt.invert();
			ImagePlus impt = new ImagePlus("contour Roi", ipt);
			masks.add(impt);
			//impt.show();
			ImageProcessor ipd = edm.makeFloatEDM(ipt, backgroundValue, edgesAreBackground);
			// Flip the distance sign of the inner pixels
			ImagePlus impd = new ImagePlus("dist", ipd);
			ImageProcessor ipdr = ipd.duplicate();
			ipdr.sqrt();
			ImagePlus impdr = new ImagePlus("dist_sqrt", ipdr);
			if (innerNegative) {
				impd = mulRoi(impd, r);
				impdr = mulRoi(impdr, r);
			}
			dist.add(impd);
			distr.add(impdr);
			impd.setTitle("dist " + r.getName());
			impdr.setTitle("dist sqrt " + r.getName());
			//impd.duplicate().show();
		}
		
		this.mse = IJ.createImage("mse_" + imp.getTitle(), imp.getWidth(), imp.getHeight(), 1, 32);
		this.mle = IJ.createImage("mle_" + imp.getTitle(), imp.getWidth(), imp.getHeight(), 1, 32);
		this.mie = IJ.createImage("mle_" + imp.getTitle(), imp.getWidth(), imp.getHeight(), 1, 32);
		this.mde = IJ.createImage("mde_" + imp.getTitle(), imp.getWidth(), imp.getHeight(), 1, 32);
		ImageProcessor pmse = mse.getProcessor();
		ImageProcessor pmie = mie.getProcessor();
		ImageProcessor pmle = mle.getProcessor();
		ImageProcessor pmde = mde.getProcessor();
		
		
		if ( this.interpolation_distance == this.INTERPOLATION_DISTANCE_SQRT ) {
			for (ImagePlus dr : distr) {
				ImageProcessor pd = dr.getProcessor();
				ImagePlus di = new ImagePlus("root distance", pd.duplicate());
				pmie.copyBits(di.getProcessor(), 0, 0, Blitter.ADD);
			}
		}
		
		for (ImagePlus d : dist) {
			ImageProcessor pd = d.getProcessor();
			ImagePlus d1 = new ImagePlus("distance", pd.duplicate());
			pd.sqr();
			ImagePlus d2 = new ImagePlus("squared_distance", pd);
			//d2.show();
			pmle.copyBits(d1.getProcessor(), 0, 0, Blitter.ADD);
			pmse.copyBits(d2.getProcessor(), 0, 0, Blitter.ADD);
		}
		pmse.multiply( 1.0/((double) dist.size()-1));
		pmde = pmse.duplicate();
		mde.setProcessor(pmde);
		pmde.sqrt();

		// Sigmoid distance for interpolation: This gives worse results as the sqrt distance, abandon this approach
		if ( this.interpolation_distance == this.INTERPOLATION_DISTANCE_SIGMOID_1 ) {
			this.me_sigmoid = IJ.createImage("mse_" + imp.getTitle(), imp.getWidth(), imp.getHeight(), 1, 32);
			ImageProcessor pme_sigmoid = me_sigmoid.getProcessor();
			for (ImagePlus d : dist) {
				ImageProcessor pd = d.getProcessor();
				//pd.multiply(0.1);
				ImagePlus d1 = new ImagePlus("distance", pd.duplicate());
				pd.sqr();
				ImagePlus d2 = new ImagePlus("squared_distance", pd);
				ImageProcessor pNum = d1.getProcessor().duplicate();
				ImageProcessor pDen = d2.getProcessor().duplicate();
				pDen.add(1.0);
				pNum.copyBits(pDen, 0, 0, Blitter.DIVIDE);
				pme_sigmoid.copyBits(pNum, 0, 0, Blitter.ADD);
			}
		}
	}

	/**
	 * Create isoline-ROIs from ROIs
	 * 
	 * @param rois
	 * @param w
	 * @param h
	 * @return 
	 */
	public static ArrayList<HeightRoi> isolineFromRoi(ArrayList< WeightRoi > rois, int w, int h) {

		ImagePlus maskSum = IJ.createImage("maskSum", w, h, 1, 32);
		ImagePlus imp = IJ.createImage("mask", w, h, 1, 8);
		ImageProcessor ip = imp.getProcessor();
		ImageProcessor pmaskSum = maskSum.getProcessor();

		//# Make a height map by z-projection of all ROIs
		for ( WeightRoi hr : rois ) {
			ImageProcessor ipt = ip.duplicate();
			ipt.setRoi(hr.roi);
			ipt.setColor(Color.WHITE);
			ipt.fill(hr.roi);
			ipt.max(1);
			ImagePlus impt = new ImagePlus("mask Roi", ipt);
			//impt.show();
			pmaskSum.copyBits(ipt, 0, 0, Blitter.ADD);
		}
		//maskSum.show();

		// Remove separate ROI's

		//# Generate the isoline-ROIs
		ImageStatistics stats = maskSum.getStatistics(ImageStatistics.MIN_MAX);
		double maxh = stats.max;
		ArrayList< HeightRoi > isoRois = new ArrayList<>();
		for (int i = 0; i < maxh; i++) {
			ImageProcessor iph = pmaskSum.duplicate();
			iph.subtract((double) i);
			iph.min(0);
			ImagePlus imph = new ImagePlus("h", iph);
			iph.max(1);
			//imph.show();
			Roi roi = roiFromMask(imph);
			HeightRoi hr = new HeightRoi( roi, (double) (i) );
			isoRois.add( hr );
		}

		return isoRois;
	}

	/**
	 * Create isoline-ROIs from ROIs: this is the function where we allow weights to be given to the ROIs,  ()
	 * TODO : this does actually not work because we assume the heightmap to have steps of one. This has to be modified.
	 * 
	 * @param rois
	 * @param w
	 * @param h
	 * @return 
	 */
	public static ArrayList<Roi> isolineFromRoi( ArrayList<Roi> rois, ArrayList<Double> ws, int w, int h) {

		ImagePlus maskSum = IJ.createImage("maskSum", w, h, 1, 32);
		ImagePlus imp = IJ.createImage("mask", w, h, 1, 8);
		ImageProcessor ip = imp.getProcessor();
		ImageProcessor pmaskSum = maskSum.getProcessor();

		//# Make a height map by z-projection of all ROIs
		for (int index = 0; index < rois.size(); index++ ) {
			Roi r = rois.get(index);
			ImageProcessor ipt = ip.duplicate();
			ipt.setRoi(r);
			ipt.setColor(Color.WHITE);
			ipt.fill(r);
			ipt.max(1);
			ipt.multiply( ws.get(index) );
			ImagePlus impt = new ImagePlus("mask Roi", ipt);
			//impt.show();
			pmaskSum.copyBits(ipt, 0, 0, Blitter.ADD);
		}
		//maskSum.show();

		// Remove separate ROI's


		//# Generate the isoline-ROIs
		ImageStatistics stats = maskSum.getStatistics(ImageStatistics.MIN_MAX);
		double maxh = stats.max;
		ArrayList<Roi> isoRois = new ArrayList<>();
		for (int i = 0; i < maxh; i++) {
			ImageProcessor iph = pmaskSum.duplicate();
			iph.subtract((double) i);
			iph.min(0);
			ImagePlus imph = new ImagePlus("h", iph);
			iph.max(1);
			//imph.show();
			Roi roi = roiFromMask(imph);
			isoRois.add(roi);
		}

		return isoRois;
	}

	/**
	 * Create isoline-ROIs from probability image: this is the function where we allow weights to be given to the ROIs
	 * 
	 * @param maskSum
	 * @param w
	 * @param h
	 * @return 
	 */
	public static ArrayList<HeightRoi> isolineFromProbabilityImage( ImagePlus maskSum ) {

		//# Generate the isoline-ROIs
		ImageProcessor pmaskSum = maskSum.getProcessor();
		ImageStatistics stats = maskSum.getStatistics( ImageStatistics.MIN_MAX );
		double maxh = stats.max;
		double smallValue = 0.01;
		ArrayList<HeightRoi> isoRois = new ArrayList<>();

		double max = maxh;
		double h = 0.0;
		int iteration = 0;
		while ( max > smallValue && iteration < 100 ) {//&& Math.pow( isoRois.size(), 2.0 ) > iteration ) {
			iteration++;
			ImageProcessor iph = pmaskSum.duplicate();
			//iph.subtract( h + smallValue );
			ImagePlus imph = new ImagePlus("h", iph);
			
			Roi sroi = roisFromThreshold( maskSum, h + smallValue );
			Roi roi;
			if ( sroi == null ) {
				roi = null;
			} else {
				roi = roiMaxRegion( maskSum, sroi );
			}

			//ImagePlus mask = maskFromThreshold( imph, h + smallValue ).duplicate();
			// roiListFromMask
			//Roi roi = roiFromMask( mask );
			if ( roi == null || roi.contains(1, 1) ) {
				// THIS SHOULD NOT HAPPEN
				h = maxh;
				max = 0.0;
				// set last roi h to max
				if (isoRois.size() > 0) {
					isoRois.get( isoRois.size()-1 ).h = h;
				}
			} else {
				iph.setRoi(roi);
				ImageStatistics statsRoi = ImageStatistics.getStatistics( iph , ImageStatistics.MIN_MAX, new Calibration() );
				iph.resetRoi();
				imph.deleteRoi();
				h = statsRoi.min;
				isoRois.add( new HeightRoi(roi, h) );
				//IJ.log( "isoRois.add, h = " + h );
				max = maxh - h;
			}
		}

		return isoRois;
	}

	/**
	 * fill with value outside ROI-band
	 * 
	 * @param imp
	 * @param roiInner
	 * @param roiOuter
	 * @param bgValue
	 * @param fgValue
	 * @return 
	 */
	public static ImagePlus fillRoiBand( ImagePlus imp, Roi roiInner, Roi roiOuter, double bgValue, double fgValue) {
		ImageProcessor fp = imp.getProcessor();
		fp.setRoi(roiInner);
		fp.setValue(fgValue);
		fp.fill(roiInner.getMask());
		fp.setRoi(roiOuter);
		fp.setValue(bgValue);
		if ( ( roiOuter.getBounds().width >= fp.getWidth() ) & ( roiOuter.getBounds().height >= fp.getHeight() ) ) {
		} else {
			fp.fillOutside(roiOuter);
		}

		return imp;
	}

	public static ImagePlus isolineMap(ImagePlus imp, ArrayList<HeightRoi> isoRois ) {

		double backgroundValue = 0.0;
		boolean edgesAreBackground = false;

		ImageProcessor ip = imp.getProcessor();
		ArrayList<ImagePlus> dist = new ArrayList<>();
		EDM edm = new EDM();

		// the first roi height
		ImagePlus firstRoiMask = IJ.createImage("firstRoiMask", imp.getWidth(), imp.getHeight(), 1, 32);
		firstRoiMask.getProcessor().setValue( isoRois.get(0).h );
		firstRoiMask.getProcessor().fill(isoRois.get(0).roi);
		dist.add( firstRoiMask );

		//IJ.log(" Iso-ROIs size = " + isoRois.size());
		for (int i = 0; i < (isoRois.size()-1); i++) {
			Roi r1 = isoRois.get(i).roi;
			ImageProcessor ip1 = ip.duplicate();
			ip1.setLineWidth(1);
			ip1.setColor(Color.WHITE);
			r1.drawPixels(ip1);
			ip1.invert();

			Roi r2 = isoRois.get(i+1).roi;
			ImageProcessor ip2 = ip.duplicate();
			ip2.setLineWidth(1);
			ip2.setColor(Color.WHITE);
			r2.drawPixels(ip2);
			ip2.invert();

			ImageProcessor ipd1 = edm.makeFloatEDM(ip1, (int) backgroundValue, edgesAreBackground);
			ImageProcessor ipd2 = edm.makeFloatEDM(ip2, (int) backgroundValue, edgesAreBackground);
			ImageProcessor ipd = ipd1.duplicate();
			ImageProcessor ipd_den = ipd1.duplicate();
			ipd_den.copyBits(ipd2, 0, 0, Blitter.ADD);
			ipd.add(0.0001);
			ipd_den.add(0.0001);
			ipd.copyBits(ipd_den, 0, 0, Blitter.DIVIDE);
			double heightDifference = Math.abs( isoRois.get(i+1).h - isoRois.get(i).h );
			ipd.multiply( heightDifference );
			ImagePlus impd = new ImagePlus("dist iso-contour intervals", ipd);
			
			impd = fillRoiBand(impd, r2, r1, 0.0, heightDifference);

			dist.add(impd);
		}

		ImagePlus impd = IJ.createImage("iso map", imp.getWidth(), imp.getHeight(), 1, 32);
		ImageProcessor ipd = impd.getProcessor();
		for (ImagePlus d : dist) {
			ipd.copyBits(d.getProcessor(), 0, 0, Blitter.ADD);
		}
		//impd.duplicate().show();
		
		ImageStatistics stats = impd.getStatistics(ImageStatistics.MIN_MAX);
		double maxh = stats.max;
		if (maxh > 0) {
			ipd.multiply( isoRois.get(isoRois.size()-1).h / maxh );
		}
		//impd.duplicate().show();

		return impd;
	}

	public void roiConfidence( ImagePlus imp , ArrayList<Roi> rois ) {
		//""" Compute mean ROI and rois for confidence interval """
		//IJ.log("roiConfidense:: Compute mean ROI and ROIs for confidence interval");
		//IJ.log("    Calculating MSE and MLE (as the sum of errors) from the distance matrix image (the not-squared version with negative error values for pixels inside the ROIs)");
		roiMSE( imp, rois );
		//this.mse.show();
		//this.mde.show();
		//this.mle.show();

		//# Mean ROI
		//IJ.log("    Obtaining mean ROI from MLE");
		ImagePlus maskMle = null;
		if ( this.interpolation_distance == this.INTERPOLATION_DISTANCE_LINEAR ) {
			ImageProcessor pmle = this.mle.getProcessor();
			pmle.multiply(-1);
			maskMle = maskFromThreshold(mle, 0.0);
		}
		if ( this.interpolation_distance == this.INTERPOLATION_DISTANCE_SQRT ) {
			ImageProcessor pmie = this.mie.getProcessor();
			pmie.multiply(-1);
			maskMle = maskFromThreshold(mie, 0.0);
		}
		if ( this.interpolation_distance == this.INTERPOLATION_DISTANCE_SIGMOID_1 ) {
			ImageProcessor pme_sigmoid = this.me_sigmoid.getProcessor();
			pme_sigmoid.multiply(-1);
			//me_sigmoid.show();
			maskMle = maskFromThreshold(me_sigmoid, 0.0);
		}
		PolygonRoi roiInterpol = (PolygonRoi) roiFromMask(maskMle);

		//# Bands from standard deviation of the mean ROI
		//IJ.log("    Obtaining the confidence interval by taking the standard deviation for each point/pixel of the mean ROI by means of the MSE");
		getRoiValues(mde, roiInterpol);
		this.roiValues = smooth1D( this.roiValues, "gauss", SIGMA_SMOOTH);
		roiBands( this.roiCurve, this.roiValues );
	}

	public void roiConfidenceSimple( ImagePlus imp , ArrayList<Roi> rois ) {
		//""" Compute mean ROI and rois for confidence interval """
		//IJ.log("roiConfidence:: Compute mean ROI and ROIs for confidence interval");
		//IJ.log("    Calculating MSE and MLE (as the sum of errors) from the distance matrix image (the not-squared version with negative error values for pixels inside the ROIs)");
		roiMSE( imp, rois );

		//# Mean ROI
		//IJ.log("    Obtaining mean ROI from MLE");
		//ImagePlus maskMle = null;
		//ImageProcessor pmle = this.mle.getProcessor();
		//pmle.multiply(-1);
		//maskMle = maskFromThreshold(mle, 0.0);
		PolygonRoi roiInterpol = (PolygonRoi) LibRoi.interpolateRois( rois, imp.getWidth(), imp.getHeight() );

		//# Bands from standard deviation of the mean ROI
		//IJ.log("    Obtaining the confidence interval by taking the standard deviation for each point/pixel of the mean ROI by means of the MSE");
		getRoiValues(mde, roiInterpol);
		this.roiValues = smooth1D( this.roiValues, "gauss", SIGMA_SMOOTH);
		roiBands( this.roiCurve, this.roiValues );
	}

	public void roiConfidenceProbability( ImagePlus imp , ArrayList<WeightRoi> rois, double perc ) {

		try {

			//ArrayList<HeightRoi> isoRois = isolineFromProbabilityImage( maskSum );
			ArrayList<HeightRoi> isoRois = isolineFromRoi(rois, imp.getWidth(), imp.getHeight());
			ImagePlus isoMap = isolineMap(imp, isoRois);

			ImagePlus meanMask = maskFromThreshold(isoMap, 0.5);
			Roi meanRoi = roiFromMask(meanMask);

			ImagePlus innerMask = maskFromThreshold(isoMap, perc);
			Roi innerRoi = roiFromMask(innerMask);

			ImagePlus outerMask = maskFromThreshold(isoMap, 1-perc);
			Roi outerRoi = roiFromMask(outerMask);

			boolean smooth = true;
			double interval = 5;
			this.roiCurve = new PolygonRoi(meanRoi.getInterpolatedPolygon( interval, smooth), Roi.POLYGON );
			this.roiBandOuter = new PolygonRoi(outerRoi.getInterpolatedPolygon( interval, smooth), Roi.POLYGON );
			this.roiBandInner = new PolygonRoi(innerRoi.getInterpolatedPolygon( interval, smooth), Roi.POLYGON );
		} catch( Exception e ) {
			this.roiCurve = null;
			this.roiBandOuter = null;
			this.roiBandInner = null;
		} 
	}

	public void roiConfidenceProbability( ImagePlus prob , double perc ) {

		try {
			ArrayList<HeightRoi> isoRois = isolineFromProbabilityImage( prob );
			ImagePlus empty = IJ.createImage("empty", prob.getWidth(), prob.getHeight(), prob.getNSlices(), 8 );
			ImagePlus isoMap = isolineMap( empty, isoRois);

			//isoMap.show();
			ImagePlus meanMask = maskFromThreshold(isoMap, 0.5);
			Roi meanRoi = roiFromMask(meanMask);

			ImagePlus innerMask = maskFromThreshold(isoMap, perc);
			Roi innerRoi = roiFromMask(innerMask);

			ImagePlus outerMask = maskFromThreshold(isoMap, 1-perc);
			Roi outerRoi = roiFromMask(outerMask);

			boolean smooth = true;
			double interval = 5;
			this.roiCurve = new PolygonRoi(meanRoi.getInterpolatedPolygon( interval, smooth), Roi.POLYGON );
			this.roiBandOuter = new PolygonRoi(outerRoi.getInterpolatedPolygon( interval, smooth), Roi.POLYGON );
			this.roiBandInner = new PolygonRoi(innerRoi.getInterpolatedPolygon( interval, smooth), Roi.POLYGON );
		} catch( Exception e ) {
			IJ.log(e.getMessage());
			//IJ.log(e.getCause().toString());
			//IJ.log(e.getStackTrace().toString());
			this.roiCurve = null;
			this.roiBandOuter = null;
			this.roiBandInner = null;
		}
	}

	/**
	 * 
	 * TODO: This is at this moment not a weighted version yet, it just removes ROIs which are outliers
	 * 
	 * @param imp
	 * @param rois
	 * @param ws
	 * @param perc 
	 */
	public void roiConfidenceWeightedProbability( ImagePlus imp , ArrayList<WeightRoi> rois, double perc ) {

		for ( int i = 0; i < rois.size(); i++ ) {
			if ( rois.get(i).w == 0.0 ) {
				rois.remove(i);
			}
		}

		if (rois.size() > 0) {

			ArrayList<HeightRoi> isoRois = isolineFromRoi( rois, imp.getWidth(), imp.getHeight() );
			ImagePlus isoMap = isolineMap(imp, isoRois);

			ImagePlus meanMask = maskFromThreshold(isoMap, 0.5);
			Roi meanRoi = roiFromMask(meanMask);

			ImagePlus innerMask = maskFromThreshold(isoMap, perc);
			Roi innerRoi = roiFromMask(innerMask);

			ImagePlus outerMask = maskFromThreshold(isoMap, 1-perc);
			Roi outerRoi = roiFromMask(outerMask);

			boolean smooth = true;
			double interval = 5;
			this.roiCurve = new PolygonRoi(meanRoi.getInterpolatedPolygon( interval, smooth ), Roi.POLYGON );
			this.roiBandOuter = new PolygonRoi(outerRoi.getInterpolatedPolygon( interval, smooth ), Roi.POLYGON );
			this.roiBandInner = new PolygonRoi(innerRoi.getInterpolatedPolygon( interval, smooth ), Roi.POLYGON );

		} else {
			
			this.roiCurve = null;
			this.roiBandOuter = null;
			this.roiBandInner = null;
		}
	}

	public ImagePlus getOverlayRoiConfidence( LinkedHashMap< String, ArrayList<Roi> > roiListMap, ImagePlus imp ) {

		LinkedHashMap< String, Roi > roiMapInner = new LinkedHashMap<>();
		LinkedHashMap< String, Roi > roiMapOuter = new LinkedHashMap<>();
		LinkedHashMap< String, Roi > roiMapMean = new LinkedHashMap<>();
		LinkedHashMap< String, Roi > roiMapBand = new LinkedHashMap<>();

		for (String key : roiListMap.keySet() ) {

			ArrayList<Roi> rois = roiListMap.get(key);

			// sets mse, mle, roiBandOuter, roiBandInner, roiCurve
			roiConfidence( imp , rois );
			//roiConfidenceSimple( imp , rois );
			double perc = 0.68;
			ArrayList< WeightRoi > wrois = new ArrayList<>();
			for ( Roi roi : rois ) {
				WeightRoi wr = new WeightRoi( roi, 1 );
				wrois.add(wr);
			}
			roiConfidenceProbability( imp , wrois, perc );
			roiMapMean.put( key, this.roiCurve );
			roiMapInner.put( key, this.roiBandInner );
			roiMapOuter.put( key, this.roiBandOuter );
		}

		ImageProcessor ip = imp.getProcessor();
		for (String key2 : roiMapInner.keySet() ) {
			
			Roi roi1 = roiMapInner.get(key2);
			Roi roi2 = roiMapOuter.get(key2);
			
			ShapeRoi roiOr = LibRoi.xorRoi( roi1, roi2);
			
			imp.setRoi(roiOr);
			roiOr.setFillColor(LibRoi.roiColor().get(key2));
			ip.fill(roiOr);
			Overlay overlay = new Overlay();
			overlay.add(roiOr);
			imp.setOverlay(overlay);
			imp.flatten();
			//imp.show();
			//IJ.log("save" + new File( outputFolder + "/" + key2 + ".png").getAbsolutePath() );
			//IJ.saveAs(imp, "png", new File( outputFolder + "/" + key2 + ".png").getAbsolutePath() );
			roiMapBand.put( key2 + "_XOR", roiOr );
		}
		
		ImagePlus overlayImage = imp.duplicate();
		Overlay overlay = new Overlay();
		for (String key : roiMapBand.keySet() ) {
			overlay.add( roiMapBand.get(key) );
		}
		overlayImage.setOverlay(overlay);
		overlayImage.setHideOverlay( false );
		overlayImage.flatten();
		
		return overlayImage;
	}
	
	public void testROI_60V( String outputPath) {
		
		String imageFolder = "D:/p_prog_output/tau_analysis/test4/congealing/montage";
		String imageName = "sample_montage_registration.tif";
		File imageFile = new File( imageFolder + "/" + imageName );
		String roiFolder = "D:/p_prog_output/tau_analysis/test4/congealing/roi";
		String outputFolder = new File(outputPath).getAbsolutePath();
		
		File outFile = new File(outputFolder);
		File roiFile = new File(roiFolder);
		ImagePlus imp = IJ.openImage(imageFile.getAbsolutePath());
		int binning = 4;
		imp = IJ.createImage("imp_" + imp.getTitle(), imp.getWidth()/binning, imp.getHeight()/binning, 1, 8);

		//imp.show();
		LinkedHashMap< String, LinkedHashMap< String, Roi> > roiMapMap = new LinkedHashMap< String, LinkedHashMap< String, Roi> >();
		File[] roiFiles = roiFile.listFiles();
		for (File file : roiFiles ) {
			if (file.isFile() & file.getName().endsWith(".zip")) {
				try {
					LinkedHashMap< String, Roi> roiMap = LibRoi.loadRoiAlternative(file);
					roiMap = TransformRoi.applyRoiScaleTransform(roiMap, 0.0, 0.0, 1.0/((double) binning));
					roiMapMap.put(file.getName(), roiMap);
				} catch (ZipException ex) {
					Logger.getLogger(RoiInterpolation.class.getName()).log(Level.SEVERE, null, ex);
				} catch (IOException ex) {
					Logger.getLogger(RoiInterpolation.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
		LinkedHashMap< String, ArrayList<Roi> > roiListMap = LibRoi.getMapOfLists(roiMapMap);
		LinkedHashMap< String, Roi > roiMapInner = new LinkedHashMap<>();
		LinkedHashMap< String, Roi > roiMapOuter = new LinkedHashMap<>();
		LinkedHashMap< String, Roi > roiMapMean = new LinkedHashMap<>();
		LinkedHashMap< String, Roi > roiMapBand = new LinkedHashMap<>();
		
		for (String key : roiListMap.keySet() ) {

			ArrayList<Roi> rois = roiListMap.get(key);

			// sets mse, mle, roiBandOuter, roiBandInner, roiCurve
			roiConfidence( imp , rois );
			//roiConfidenceSimple( imp , rois );
			double perc = 0.68;
			ArrayList< WeightRoi > wrois = new ArrayList<>();
			for ( Roi roi : rois ) {
				WeightRoi wr = new WeightRoi( roi, 1 );
				wrois.add(wr);
			}
			
			roiConfidenceProbability( imp , wrois, perc );
			roiMapMean.put( key, this.roiCurve );
			roiMapInner.put( key, this.roiBandInner );
			roiMapOuter.put( key, this.roiBandOuter );
		}

		ImageProcessor ip = imp.getProcessor();
		Overlay overlay = new Overlay();
		for (String key2 : roiMapInner.keySet() ) {
			
			Roi roi1 = roiMapInner.get(key2);
			Roi roi2 = roiMapOuter.get(key2);
			
			ShapeRoi roiOr = LibRoi.xorRoi( roi1, roi2);
			
			imp.setRoi(roiOr);
			roiOr.setFillColor(LibRoi.roiColor().get(key2));
			ip.fill(roiOr);
			overlay.add(roiOr);
			//imp.setOverlay(overlay);
			//imp.flatten();
			//imp.show();
			//IJ.log("save" + new File( outputFolder + "/" + key2 + ".png").getAbsolutePath() );
			//IJ.saveAs(imp, "png", new File( outputFolder + "/" + key2 + ".png").getAbsolutePath() );
			roiMapBand.put( key2 + "_XOR", roiOr );
		}
		imp.setOverlay( overlay );
		imp.setHideOverlay(false);
		imp.flatten();
		imp.show();
		//for (String key : roiMapBand.keySet() ) {
		//	roiMapBand.get(key);
		//}

		//roiMapOuter = TransformRoi.applyRoiScaleTransform(roiMapOuter, 0.0, 0.0, ((double) binning));
		//roiMapInner = TransformRoi.applyRoiScaleTransform(roiMapInner, 0.0, 0.0, ((double) binning));
		//roiMapMean = TransformRoi.applyRoiScaleTransform(roiMapMean, 0.0, 0.0, ((double) binning));
		//roiMapBand = TransformRoi.applyRoiScaleTransform(roiMapBand, 0.0, 0.0, ((double) binning));
		
		LibRoi.saveRoiAlternative( new File(outputFolder + "/" + "roiMapMean.zip"), roiMapMean);
		LibRoi.saveRoiAlternative( new File(outputFolder + "/" + "roiMapInner.zip"), roiMapInner);
		LibRoi.saveRoiAlternative( new File(outputFolder + "/" + "roiMapOuter.zip"), roiMapOuter);
		LibRoi.saveRoiAlternative( new File(outputFolder + "/" + "roiMapBand.zip"), roiMapBand);
	}

	/**
	 * Exclude outlier ROIs of different images (outliers are defined as those being outside, having no intersection with, the given (interpolated) ROI )
	 * ( this function takes ArrayLists as input/output )
	 * 
	 * @param rois List of ROIs
	 * @param roiInterp Interpolated ROI which serves as a mask for the other ROIs
	 * @return A copy of the rois where the outlier ROIs are removed from.
	 */
	public static ArrayList<Roi> excludeOutlierRois( ArrayList< Roi > rois, Roi roiInterp ) {

		ArrayList<Roi> outRois = new ArrayList<>();
		outRois.addAll(rois);
		for ( int i = 0; i < outRois.size(); i++ ) {
			Roi roi = outRois.get( i );
			if ( roiInterp == null ) {
				outRois.remove(i);
				i--;
			} else {
				Roi roiIntersection = intersectRoi( roi, roiInterp );
				if (  ( roiIntersection == null )  ||  roiIntersection.contains(1, 1)  ) {
					outRois.remove(i);
					i--;
				}
			}
		}
		return outRois;
	}

	/**
	 * Exclude outlier ROIs of different images (outliers are defined as those being outside, having no intersection with, the given (interpolated) ROI )
	 * ( this function takes Map< roiImage, Roi > as input/output )
	 * 
	 * @param roiMap Map< roiImage, Roi >
	 * @param roiInterp Interpolated ROI which serves as a mask for the other ROIs
	 * @return A copy of the roiMap where the outlier ROIs are removed from.
	 */
	public static LinkedHashMap< String, Roi > excludeOutlierRois( LinkedHashMap< String, Roi > roiMap, Roi roiInterp ) {

		LinkedHashMap< String, Roi> outRoiMap = new LinkedHashMap<>();
		outRoiMap.putAll(roiMap);
		Set<String> keys = outRoiMap.keySet();
		String[] keysa = keys.toArray(new String[]{""});
		for ( String key : keysa ) {
			Roi roi = outRoiMap.get( key );
			if ( roiInterp == null ) {
				outRoiMap.remove( key );
			} else {
				Roi roiIntersection = intersectRoi( roi, roiInterp );
				if (  ( roiIntersection == null )  ||  roiIntersection.contains(1, 1)  ) {
					outRoiMap.remove( key );
				}
			}
		}
		return outRoiMap;
	}
	
	/**
	 * Exclude outlier ROIs of different images (outliers are defined as those being outside, having no intersection with, the given (interpolated) ROI )
	 * ( this function takes Map< roiImage, Roi > as input/output )
	 * 
	 * @param roiMap Map< roiImage, Roi >
	 * @param roiInterp Interpolated ROI which serves as a mask for the other ROIs
	 * @param overlapPercentage
	 * @return A copy of the roiMap where the outlier ROIs are removed from.
	 */
	public static LinkedHashMap< String, Roi > excludeOutlierRois( LinkedHashMap< String, Roi > roiMap, Roi roiInterp, double overlapPercentage ) {

		LinkedHashMap< String, Roi> outRoiMap = new LinkedHashMap<>();
		outRoiMap.putAll(roiMap);
		Set<String> keys = outRoiMap.keySet();
		String[] keysa = keys.toArray(new String[]{""});
		for ( String key : keysa ) {
			Roi roi = outRoiMap.get( key );
			if ( roiInterp == null ) {
				outRoiMap.remove( key );
			} else {
				Roi roiIntersection = intersectRoi( roi, roiInterp );
				if (  ( roiIntersection == null )  ||  roiIntersection.contains(1, 1)  ) {
					outRoiMap.remove( key );
				} else {
					ImageStatistics stats = roi.getStatistics();
					ImageStatistics statsInterpolation = roiIntersection.getStatistics();
					if ( statsInterpolation.area < overlapPercentage * stats.area ) {
						outRoiMap.remove( key );
					}
				}
			}
		}
		return outRoiMap;
	}

	public static Roi roiMaxRegion( ImagePlus prob, Roi sroi ) {

		ShapeRoi srois = new ShapeRoi(sroi);
		Roi[] rois = srois.getRois();
		double[] max = new double[rois.length];
		double hmax = 0.0;
		int maxi = 0;
		Roi mroi = rois[0];
		for ( int i = 0; i < rois.length; i++ ) {
			Roi roi = rois[i];
			prob.setRoi( roi );
			ImageStatistics stats = ImageStatistics.getStatistics( prob.getProcessor() );
			max[i] = stats.max;
			if (hmax < max[i]) {
				hmax = max[i];
				maxi = i;
			}
			mroi = roi;
		}
		return mroi;
	}

	public void testProbToRois( String probPath ) {
		ImagePlus prob = IJ.openImage( probPath );
		prob.show();
		
		//double h1 = 0.001;
		//double h2 = 100.0;
		//prob.getProcessor().setThreshold(h1, h2, 0);
		//Roi sroi = roisFromThreshold( prob, 0.5 );
		//Roi roi = roiMaxRegion( prob, sroi );
		
		ArrayList<HeightRoi> hrois = isolineFromProbabilityImage( prob );
		prob.deleteRoi();
		prob.duplicate().show();
		ImagePlus empty = prob.duplicate();
		empty.deleteRoi();
		empty.getProcessor().multiply(0.0);
		Overlay overlay = new Overlay();
		for ( HeightRoi hroi : hrois ) {
			overlay.add( hroi.roi );
		}
		empty.setOverlay(overlay);
		empty.setHideOverlay(false);
		empty.show();
		
		ImagePlus empty2 = IJ.createImage("empty2", prob.getWidth(), prob.getHeight(), prob.getNSlices(), 8 );
		ImagePlus probLinear = isolineMap( empty2, hrois );
		probLinear.show();
	}
	
	public static void main(String[] args) {
		
		RoiInterpolation roiInterpolation = new RoiInterpolation();
		//roiInterpolation.run();
		
//		String outputPath = "d:/p_prog_output/output_test";
//		roiInterpolation.testROI_60V( outputPath );
		
		new ImageJ();
		String imageFolder = "D:/p_prog_output/slicemap_3/input";
		String imageName = "bs_prob.tif";
		String path = imageFolder + "/" + imageName;
		roiInterpolation.testProbToRois( path );
		
		//getOverlayRoiConfidence( LinkedHashMap< String, ArrayList<Roi> > roiListMap, ImagePlus imp );
	}
}
