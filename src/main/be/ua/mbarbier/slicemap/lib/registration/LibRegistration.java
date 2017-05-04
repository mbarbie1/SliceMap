/**
 * 
 */
package main.be.ua.mbarbier.slicemap.lib.registration;

import main.be.ua.mbarbier.slicemap.lib.features.harris.Corner;
import main.be.ua.mbarbier.slicemap.lib.features.harris.HarrisCornerDetector;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortBlitter;

import java.util.LinkedHashMap;

//import mpicbg.imagefeatures.Feature;
//import mpicbg.imagefeatures.FloatArray2DSIFT;

import bunwarpj.Transformation;
import bunwarpj.bUnwarpJ_;
import bunwarpj.Param;

import main.be.ua.mbarbier.external.SIFT_ExtractPointRoi;
import main.be.ua.mbarbier.slicemap.lib.image.LibImage;
import ij.IJ;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.plugin.filter.GaussianBlur;
import java.awt.Color;
import java.util.Collections;
import java.util.List;

/**
 * @author mbarbie1
 *
 */
public class LibRegistration {

	ImagePlus imp1;
	ImagePlus imp2;

	float initialSigma = 1.60f;
	int steps = 3;
	int minOctaveSize = 64;
	int maxOctaveSize = 1024;
	int fdSize = 4;
	int fdBins = 8;
	float rod = 0.92f;
	float maxEpsilon = 25.0f;
	float minInlierRatio = 0.05f;
	int modelIndex = 2;

	int accuracy_mode = 1;
	int img_subsamp_fact = 0;
	int  min_scale_deformation = 1;
	int max_scale_deformation = 2;
	double divWeight = 0.1;
	double curlWeight = 0.1;
	double landmarkWeight = 0.5;
	double imageWeight = 0.5;
	double consistencyWeight = 30.0;
	double stopThreshold = 0.01;

	public static LinkedHashMap<String, Float> siftParamDefault() {
		LinkedHashMap<String, Float> paramDefault = new LinkedHashMap<String, Float>(); 
		paramDefault.put( "initialSigma", 1.6f );//1.6//1.0
		paramDefault.put( "steps", 3.0f );
		paramDefault.put( "minOctaveSize", 64.0f ); //64//16
		paramDefault.put( "maxOctaveSize", 1024.0f );
		paramDefault.put( "fdSize", 4.0f );//4//2
		paramDefault.put( "fdBins", 8.0f );//8//4
		paramDefault.put( "rod", 0.92f );
		paramDefault.put( "maxEpsilon", 25.0f );
		paramDefault.put( "minInlierRatio", 0.05f );
		paramDefault.put( "modelIndex", 2.0f );

		return paramDefault;
	}

	public static LinkedHashMap<String, Double> harrisParamDefault() {
		LinkedHashMap<String, Double> paramDefault = new LinkedHashMap<String, Double>(); 
		paramDefault.put( "alpha", 0.01 );
		paramDefault.put( "tH", 10.0 );
		paramDefault.put( "dmin", 3.0 );
		paramDefault.put( "nPoints", 10.0 );
		paramDefault.put( "sigma", 15.0 );

		return paramDefault;
	}

	public static LinkedHashMap<String, Double> bunwarpjParamDefault() {
		LinkedHashMap<String, Double> paramDefault = new LinkedHashMap<String, Double>(); 
		paramDefault.put( "accuracy_mode", 1.0);
		paramDefault.put( "img_subsamp_fact", 0.0);
		paramDefault.put( "min_scale_deformation", 1.0);
		paramDefault.put( "max_scale_deformation", 2.0);
		paramDefault.put( "divWeight", 0.1);
		paramDefault.put( "curlWeight", 0.1);
		paramDefault.put( "landmarkWeight", 1.0);
		paramDefault.put( "imageWeight", 1.0);
		paramDefault.put( "consistencyWeight", 30.0);
		paramDefault.put( "stopThreshold", 0.01);

		return paramDefault;
	}

	public static LinkedHashMap<String, Roi> siftSingle( ImagePlus impSource, ImagePlus impTarget, LinkedHashMap param ) {
		SIFT_ExtractPointRoi t = new SIFT_ExtractPointRoi();
		t.exec(
			impTarget, 
			impSource, 
			(float) param.get("initialSigma"),
			new Float((float) param.get("steps")).intValue(),
			new Float((float) param.get("minOctaveSize")).intValue(),
			new Float((float) param.get("maxOctaveSize")).intValue(),
			new Float((float) param.get("fdSize")).intValue(),
			new Float((float) param.get("fdBins")).intValue(),
			(float) (param.get("rod")),
			(float) (param.get("maxEpsilon")),
			(float) (param.get("minInlierRatio")),
			new Float((float) param.get("modelIndex")).intValue()
		);
		Roi roiSource = impSource.getRoi();
		Roi roiTarget = impTarget.getRoi();
		LinkedHashMap<String, Roi> out = new LinkedHashMap<String, Roi>();
		out.put( "roiSource", roiSource );
		out.put( "roiTarget", roiTarget );

		return out;
	}
	public static LinkedHashMap<String, Roi> siftSingle( ImagePlus impSource, ImagePlus impTarget, SiftParam param ) {
		SIFT_ExtractPointRoi t = new SIFT_ExtractPointRoi();
		t.exec(
			impTarget, 
			impSource, 
			param.getInitialSigma(),
			param.getSteps(),
			param.getMinOctaveSize(),
			param.getMaxOctaveSize(),
			param.getFdSize(),
			param.fdBins,
			param.getRod(),
			param.getMaxEpsilon(),
			param.getMinInlierRatio(),
			param.getModelIndex()
		);
		Roi roiSource = impSource.getRoi();
		Roi roiTarget = impTarget.getRoi();
		LinkedHashMap<String, Roi> out = new LinkedHashMap<String, Roi>();
		out.put( "roiSource", roiSource );
		out.put( "roiTarget", roiTarget );

		return out;
	}

	public static LinkedHashMap<String, Roi> harrisSingle( ImagePlus impSource, ImagePlus impTarget, LinkedHashMap param ) {
		
		// COMPUTE THE POINTS OF INTEREST (FOR NOW HARRIS CORNER POINTS)
		HarrisCornerDetector hds;
		HarrisCornerDetector hdt;
		HarrisCornerDetector.Parameters paramHarris = new HarrisCornerDetector.Parameters();
		//paramHarris.alpha = 0.01;
		//paramHarris.tH = 10;
		//paramHarris.dmin = 3;
		paramHarris.alpha = (double) param.get("alpha");
		paramHarris.tH = (double) param.get("tH");
		paramHarris.dmin = (double) param.get("dmin");
		int nPoints = new Double((double) param.get("nPoints")).intValue();
		double sigma = (double) param.get("sigma");
		ImageProcessor ips = impSource.getProcessor().duplicate();
		ImageProcessor ipt = impTarget.getProcessor().duplicate();
		GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(ips, sigma);
        gb.blurGaussian(ipt, sigma);


		hds = new HarrisCornerDetector( ips, paramHarris);
		List<Corner> ps = hds.findCorners();
		Collections.sort(ps);
		IJ.log("Number of Harris points for sample image = " + ps.size());

		hdt = new HarrisCornerDetector( ipt, paramHarris);
		List<Corner> pt = hdt.findCorners();
		Collections.sort(pt);
		IJ.log("Number of Harris points for reference image = " + pt.size());

		nPoints = Math.min(nPoints, pt.size());
		nPoints = Math.min(nPoints, ps.size());

		// TAKE N STRONGEST POINTS
        float[] px = new float[nPoints];
        float[] py = new float[nPoints];
        for (int j = 0; j < nPoints; j++) {
            px[j] = ps.get(j).getX();
            py[j] = ps.get(j).getY();
        }
		Roi roiSource = new PointRoi(px, py);
        for (int j = 0; j < nPoints; j++) {
            px[j] = pt.get(j).getX();
            py[j] = pt.get(j).getY();
        }
		Roi roiTarget = new PointRoi(px, py);

		LinkedHashMap<String, Roi> out = new LinkedHashMap<String, Roi>();
		out.put( "roiSource", roiSource );
		out.put( "roiTarget", roiTarget );

		return out;
	}

	
	
	public static Transformation bunwarpj_param( ImagePlus impSource, ImagePlus impTarget, LinkedHashMap param ) {
		impSource.setTitle("bunwarpj_source");
		impTarget.setTitle("bunwarpj_target");
		ImageProcessor targetMskIp = null;//LibUtilities.mask( impTarget.getProcessor().duplicate(), 0.0, 0.5 ).getProcessor();
		ImageProcessor sourceMskIp = null;//LibUtilities.mask( impSource.getProcessor().duplicate(), 0.0, 0.5 ).getProcessor();
                
                //computeTransformationBatch(
                    //ImagePlus targetImp, 
                    //ImagePlus sourceImp, 
                    //ImageProcessor targetMskIP, 
                    //ImageProcessor sourceMskIP, 
                    //int mode, 
                    //int img_subsamp_fact, 
                    //int min_scale_deformation, 
                    //int max_scale_deformation, 
                    //double divWeight, 
                    //double curlWeight, 
                    //double landmarkWeight, 
                    //double imageWeight, 
                    //double consistencyWeight, 
                    //double stopThreshold)
                
		Transformation transfo = bUnwarpJ_.computeTransformationBatch(
			impTarget, 
			impSource,
			targetMskIp,
			sourceMskIp, 
			new Double((double) param.get("accuracy_mode")).intValue(),
			new Double((double) param.get("img_subsamp_fact")).intValue(),
			new Double((double) param.get("min_scale_deformation")).intValue(),
			new Double((double) param.get("max_scale_deformation")).intValue(),
			(double) (param.get("divWeight")),
			(double) (param.get("curlWeight")),
			(double) (param.get("landmarkWeight")),
			(double) (param.get("imageWeight")),
			(double) (param.get("consistencyWeight")),
			(double) (param.get("stopThreshold"))
		);

		return transfo;
	}

}
