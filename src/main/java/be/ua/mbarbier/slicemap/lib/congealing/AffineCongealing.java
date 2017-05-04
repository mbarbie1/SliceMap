/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ua.mbarbier.slicemap.lib.congealing;

import algorithms.PearsonsCorrelation;
import be.ua.mbarbier.external.Transform_Roi;
import be.ua.mbarbier.slicemap.ImageProperties;
import be.ua.mbarbier.slicemap.Main;
import be.ua.mbarbier.slicemap.lib.BiMap;
import be.ua.mbarbier.slicemap.lib.Lib;
import static be.ua.mbarbier.slicemap.lib.Lib.getCommonKeys;
import static be.ua.mbarbier.slicemap.lib.Lib.linearSpacedSequence;
import be.ua.mbarbier.slicemap.lib.LibIO;
//import be.ua.mbarbier.slicemap.lib.MBLog;
import be.ua.mbarbier.slicemap.lib.features.harris.Corner;
import be.ua.mbarbier.slicemap.lib.features.harris.HarrisCornerDetector;
import be.ua.mbarbier.slicemap.lib.image.SpotOrientation;
import be.ua.mbarbier.slicemap.lib.image.colocalization.Colocalization;
import be.ua.mbarbier.slicemap.lib.transform.Transform2D;
import static be.ua.mbarbier.slicemap.lib.transform.TransformImage.applyRefTransform;
import static be.ua.mbarbier.slicemap.lib.transform.TransformImage.applyRigidTransform;
import static be.ua.mbarbier.slicemap.lib.transform.TransformImage.applyTransform;
import static be.ua.mbarbier.slicemap.lib.transform.TransformRoi.applyRoiRefTransform;
import static be.ua.mbarbier.slicemap.lib.transform.TransformRoi.applyRoiScaleTransform;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.ZProjector;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import net.imglib2.realtransform.AffineTransform2D;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author mbarbier
 */
public class AffineCongealing {

	public ImagePlus preWarpingIllustration;
    ImagePlus alignedStack;
	ImagePlus finalMeanImage;
	ImagePlus meanStack;
    int[] sortedIndices;
	public LinkedHashMap< String, AffineTransform2D > transformations;
    public LinkedHashMap< String, AffineTransform2D > transformMat;
    public LinkedHashMap< String, AffineTransform2D > preTransformMat;
	public LinkedHashMap< String, double[] > preTransformVec;
	public LinkedHashMap< String, double[] > transformVec;

	public double[] methodParameters = new double[]{1.0, 1.0, 4. * (2. * Math.PI / 180.), 1.0, 1.0, 1.0, 0.02, 0.02};
    public final int MAX_PARAMETERS = 10;
    boolean[] normalizedParameters = new boolean[MAX_PARAMETERS];
	public boolean[] integerParameters = new boolean[MAX_PARAMETERS];

    public int nParameters = -1;
    public int nImages = 0;
	public int nReferences = 0;
    public int binCongealing = 1;
    public int binPreWarp = 16;
	public int binTotal; 
    public int scalePreWarp = 30;
    public int refWidth;
    public int refHeight;
	public double saturatedPixelsPercentage;
    public ArrayList<ImageProperties> stackProps;
    public final String[] LIST_METHODS = new String[]{"CONGEALING_INTENSITY"};
    public final String[] LIST_TRANSFORMS = new String[]{"MIRROR", "RIGID", "RIGID_MIRROR", "RIGID_MIRROR_XY", "AFFINE"};
	public final LinkedHashMap< String, Integer > MAP_TRANSFORMS_nPARAMETERS;
    public String METHOD = LIST_METHODS[0];
    public String TRANSFORM = LIST_TRANSFORMS[1];
    public final String LABEL_TRANSFORM = "TRANSFORM_TYPE";
    public final String LABEL_TRANSLATION_X = "TRANSLATION_X";
    public final String LABEL_TRANSLATION_Y = "TRANSLATION_Y";
    public final String LABEL_ROTATION = "ROTATION";
    public final String LABEL_MIRROR_Y = "MIRRORY";
	public final String LABEL_MIRROR_X = "MIRRORX";
	public final String LABEL_MIRROR_XY = "MIRRORXY";
    public final String LABEL_SCALE_Y = "SCALE_Y";
	public final String LABEL_SCALE_X = "SCALE_X";

	public final String LABEL_ID = "IMAGE_ID";
	public final String LABEL_INDEX = "IMAGE_INDEX";
    public final String LABEL_AFFINE_M00 = "AFFINE_XX";
    public final String LABEL_AFFINE_M01 = "AFFINE_XY";
    public final String LABEL_AFFINE_M10 = "AFFINE_YX";
    public final String LABEL_AFFINE_M11 = "AFFINE_YY";
    public final String LABEL_AFFINE_M02 = "AFFINE_TRANSLATION_X";
    public final String LABEL_AFFINE_M12 = "AFFINE_TRANSLATION_Y";

    public final int INDEX_AFFINE_M00 = 0;
    public final int INDEX_AFFINE_M01 = 1;
	public final int INDEX_AFFINE_M02 = 2;
    public final int INDEX_AFFINE_M10 = 3;
    public final int INDEX_AFFINE_M11 = 4;
	public final int INDEX_AFFINE_M12 = 5;

    public final int INDEX_TRANSLATION_X = 0;
    public final int INDEX_TRANSLATION_Y = 1;
    public final int INDEX_ROTATION = 2;
	public final int INDEX_MIRROR_X = 3;
    public final int INDEX_MIRROR_Y = 4;
    public final int INDEX_MIRROR_XY = 5;
	public final int INDEX_SCALE_X = 6;
    public final int INDEX_SCALE_Y = 7;
    double[] entropyTable = new double[]{};
	public double[] pearsonImpact;
	public double[] entropyImpact;
    //MBLog log;
	public boolean DEBUG = false;

    /**
     * 
     */
    public AffineCongealing() {

        IJ.log( "START Computing entropy lookup table" );
        double[] p = linearSpacedSequence(0.00000001, 0.9999999999, 10000);
        this.entropyTable = new double[p.length];
        double log2 = Math.log(2);
        for (int i = 0; i < p.length; i++) {
            double pp = p[i];
            this.entropyTable[i] = -(pp * Math.log(pp) / log2 + (1 - pp) * Math.log(1 - pp) / log2);
        }
        IJ.log("END Computing entropy lookup table");
        stackProps = new ArrayList<>();
        this.normalizedParameters[this.INDEX_TRANSLATION_X] = true;
        this.normalizedParameters[this.INDEX_TRANSLATION_Y] = true;
        this.normalizedParameters[this.INDEX_ROTATION] = true;
        this.normalizedParameters[this.INDEX_MIRROR_Y] = true;
        this.normalizedParameters[this.INDEX_MIRROR_X] = true;
        this.normalizedParameters[this.INDEX_MIRROR_XY] = true;
        this.normalizedParameters[this.INDEX_SCALE_X] = true;
        this.normalizedParameters[this.INDEX_SCALE_Y] = true;

        this.integerParameters[this.INDEX_TRANSLATION_X] = false;
        this.integerParameters[this.INDEX_TRANSLATION_Y] = false;
        this.integerParameters[this.INDEX_ROTATION] = false;
        this.integerParameters[this.INDEX_MIRROR_Y] = true;
        this.integerParameters[this.INDEX_MIRROR_X] = true;
        this.integerParameters[this.INDEX_MIRROR_XY] = true;
        this.integerParameters[this.INDEX_SCALE_X] = false;
        this.integerParameters[this.INDEX_SCALE_Y] = false;

		this.transformMat = new LinkedHashMap<>();
		this.preTransformMat = new LinkedHashMap<>();
		
		this.MAP_TRANSFORMS_nPARAMETERS = new LinkedHashMap<>();
		this.MAP_TRANSFORMS_nPARAMETERS.put( "RIGID", 3);
		this.MAP_TRANSFORMS_nPARAMETERS.put( "RIGID_MIRROR", 4);
		this.MAP_TRANSFORMS_nPARAMETERS.put( "RIGID_MIRROR_XY", 5 );

    };

	public void initTransformMat( ArrayList<String> keys ) {
		this.transformMat = new LinkedHashMap<>();
		for ( String key : keys ) {
			this.transformMat.put( key, Transform2D.unitMatrix() );
		}
	}

	public void initTransformVec( ArrayList<String> keys, int nParameters ) {
		this.transformVec = new LinkedHashMap<>();
		this.preTransformVec = new LinkedHashMap<>();
		double[] zeros = new double[nParameters];
		for (double el : zeros)
			el = 0;
		for ( String key : keys ) {
			this.transformVec.put( key, zeros.clone() );
			this.preTransformVec.put( key, zeros.clone() );
		}
	}

	public AffineTransform2D tvecToTmat( double[] tvec, double centerX, double centerY, double[] methodParameters ) {

		AffineTransform2D E = new AffineTransform2D();
		AffineTransform2D out = E.copy();
		out = Transform2D.rigidMirrorScaleFast( E, 
				tvec[this.INDEX_TRANSLATION_X] * methodParameters[this.INDEX_TRANSLATION_X], 
				tvec[this.INDEX_TRANSLATION_Y] * methodParameters[this.INDEX_TRANSLATION_Y], 
				tvec[this.INDEX_ROTATION] * methodParameters[this.INDEX_ROTATION],
				((int) Math.abs((int) tvec[this.INDEX_MIRROR_X]) % 2) > 0, 
				((int) Math.abs((int) tvec[this.INDEX_MIRROR_Y]) % 2) > 0, 
				((int) Math.abs((int) tvec[this.INDEX_MIRROR_XY]) % 2) > 0, 
				Math.exp( tvec[this.INDEX_SCALE_X] * methodParameters[this.INDEX_SCALE_X] ), 
				Math.exp( tvec[this.INDEX_SCALE_Y] * methodParameters[this.INDEX_SCALE_Y] ), centerX, centerY );

		return out;
	}

	public void saveStackProps(File stackPropsFile) {
		ArrayList< LinkedHashMap< String, String > > stackPropsTemp = new ArrayList< LinkedHashMap< String, String > >();
		for ( ImageProperties props : stackProps ) {
			stackPropsTemp.add(props.getMap());
		}
		LibIO.writeCsv( stackPropsTemp , ",", stackPropsFile.getAbsolutePath() );
	}

	public static void saveStackProps(File stackPropsFile, LinkedHashMap< String, ImageProperties > stackProps) {
		ArrayList< LinkedHashMap< String, String > > stackPropsTemp = new ArrayList< LinkedHashMap< String, String > >();
		for ( String key : stackProps.keySet() ) {
			ImageProperties props = stackProps.get(key);
			stackPropsTemp.add(props.getMap());
		}
		LibIO.writeCsv( stackPropsTemp , ",", stackPropsFile.getAbsolutePath() );
	}

	public void setStackProps( LinkedHashMap< String, ImageProperties > stackProps ) {
		ArrayList< ImageProperties > stackPropsTemp = new ArrayList<>();
		for (String key : stackProps.keySet() ) {
			stackPropsTemp.add( stackProps.get(key) );
		}
		this.stackProps = stackPropsTemp;
		this.binCongealing = this.stackProps.get(0).binning_congealing;
		this.binPreWarp = this.stackProps.get(0).binning;
		this.binTotal = this.stackProps.get(0).binning_total;
		this.refWidth = this.stackProps.get(0).stackWidth;
		this.refHeight = this.stackProps.get(0).stackHeight;
	}

	public void loadStackProps(File stackPropsFile) {
		//ArrayList< ImageProperties > stackPropsTemp = new ArrayList<>();
		this.stackProps = new ArrayList< ImageProperties >();
		ArrayList< LinkedHashMap<String, String> > stackPropsTemp = LibIO.readCsv( stackPropsFile.getAbsolutePath() , "", "," );
		for ( LinkedHashMap<String, String> propsMap : stackPropsTemp ) {
			ImageProperties props = new ImageProperties();
			props.loadMap( propsMap );
			this.stackProps.add( props );
		}
		this.binCongealing = this.stackProps.get(0).binning_congealing;
		this.binPreWarp = this.stackProps.get(0).binning;
		this.binTotal = this.stackProps.get(0).binning_total;
		this.refWidth = this.stackProps.get(0).stackWidth;
		this.refHeight = this.stackProps.get(0).stackHeight;
	}

	public void addImageProps( ImageProperties refProps, int imageIndex, String imageId, int imageWidth, int imageHeight ) {

		ImageProperties prop = refProps.copy();
		prop.id = imageId;
		prop.index = imageIndex;
		prop.width = imageWidth;
		prop.height = imageHeight;
		prop.xOffset = (int) Math.floor( ( prop.stackWidth - prop.width ) / 2.0 );
		prop.yOffset = (int) Math.floor( ( prop.stackHeight - prop.height ) / 2.0 );
		prop.roiMapOri = null;
		prop.roiMapOriFile = null;
		prop.imageOriFile = null;
		prop.roiMap = null;
		this.stackProps.add( prop );
	}

	public void saveTransformVecs( File preTranformVecFile, File tranformVecFile ) {

		ArrayList<LinkedHashMap<String, String>> mList = new ArrayList<LinkedHashMap<String, String>>();
		for (String key : this.transformMat.keySet()) {
			AffineTransform2D tmat = this.transformMat.get(key);

			LinkedHashMap<String, String> m = new LinkedHashMap<String, String>();
			m.put( this.LABEL_ID, key );
			m.put( this.LABEL_AFFINE_M00, Double.toString( tmat.get(0, 0) ) );
			m.put( this.LABEL_AFFINE_M01, Double.toString( tmat.get(0, 1) ) );
			m.put( this.LABEL_AFFINE_M02, Double.toString( tmat.get(0, 2) ) );
			m.put( this.LABEL_AFFINE_M10, Double.toString( tmat.get(1, 0) ) );
			m.put( this.LABEL_AFFINE_M11, Double.toString( tmat.get(1, 1) ) );
			m.put( this.LABEL_AFFINE_M12, Double.toString( tmat.get(1, 2) ) );
			m.put(LABEL_TRANSFORM, this.TRANSFORM);
			mList.add(m);
		}
		LibIO.writeCsv( mList , ",", tranformVecFile.getAbsolutePath() );

		mList = new ArrayList<LinkedHashMap<String, String>>();
		for (String key : this.preTransformMat.keySet()) {
			AffineTransform2D tmat = this.preTransformMat.get(key);

			LinkedHashMap<String, String> m = new LinkedHashMap<String, String>();
			m.put( this.LABEL_ID, key );
			m.put( this.LABEL_AFFINE_M00, Double.toString( tmat.get(0, 0) ) );
			m.put( this.LABEL_AFFINE_M01, Double.toString( tmat.get(0, 1) ) );
			m.put( this.LABEL_AFFINE_M02, Double.toString( tmat.get(0, 2) ) );
			m.put( this.LABEL_AFFINE_M10, Double.toString( tmat.get(1, 0) ) );
			m.put( this.LABEL_AFFINE_M11, Double.toString( tmat.get(1, 1) ) );
			m.put( this.LABEL_AFFINE_M12, Double.toString( tmat.get(1, 2) ) );
			m.put( this.LABEL_TRANSFORM, this.TRANSFORM);
			mList.add(m);
		}
		LibIO.writeCsv( mList , ",", preTranformVecFile.getAbsolutePath() );
	}

	public LinkedHashMap< String, AffineTransform2D > loadTransformVecs( File transformVecFile ) {

		ArrayList<LinkedHashMap<String, String>> transformVecList = LibIO.readCsv( transformVecFile.getAbsolutePath(), "", "," );
		LinkedHashMap< String, AffineTransform2D > transformVecTemp = new LinkedHashMap<>();

		for (int i = 0; i < transformVecList.size(); i++ ) {

			AffineTransform2D tmat = new AffineTransform2D();
			LinkedHashMap< String, String > m = transformVecList.get(i);
			double[] tvec = new double[6];
			tvec[0] = Double.parseDouble( m.get( this.LABEL_AFFINE_M00 ) );
			tvec[1] = Double.parseDouble( m.get( this.LABEL_AFFINE_M01 ) );
			tvec[2] = Double.parseDouble( m.get( this.LABEL_AFFINE_M02 ) );
			tvec[3] = Double.parseDouble( m.get( this.LABEL_AFFINE_M10 ) );
			tvec[4] = Double.parseDouble( m.get( this.LABEL_AFFINE_M11 ) );
			tvec[5] = Double.parseDouble( m.get( this.LABEL_AFFINE_M12 ) );
			tmat.set( tvec );
			transformVecTemp.put( m.get( this.LABEL_ID ), tmat );
		}

		return transformVecTemp;
	}

	public ImagePlus getMeanImage() {
		return this.finalMeanImage;
	}

	public ImagePlus getMeanStack() {
		return this.meanStack;
	}

	public ImagePlus getAlignedStack() {
		return this.alignedStack;
	}
	
    //public void setLog(MBLog log) {
    //    this.log = log;
    //}

    public void setTRANSFORM(String TRANSFORM) {
        this.TRANSFORM = TRANSFORM;
    }

	public String getTRANSFORM() {
        return this.TRANSFORM;
    }

    public void setNImages(int nImages) {
        this.nImages = nImages;
    }
	
	public double[] scaleTransform( double[] tvec, double scale ) {

        double[] tmp = tvec.clone();
        tmp[INDEX_TRANSLATION_X] = tmp[INDEX_TRANSLATION_X] * scale;
        tmp[INDEX_TRANSLATION_Y] = tmp[INDEX_TRANSLATION_Y] * scale;

        return tmp;
    }

    public LinkedHashMap<String,double[]> calcEntropyImpact( ImagePlus impStack ) {
    
        LinkedHashMap<String,double[]> out = new LinkedHashMap<String,double[]>();
        int nImages = impStack.getNSlices();
        ImageStack stack = impStack.getStack();
        double[] entropies = new double[nImages];
        double[] pearson = new double[nImages];
        ImagePlus currentMean = getMeanImage(impStack);
        double stackEntropy = calcEntropy( currentMean );
        PearsonsCorrelation pearsonsCorrelation = new PearsonsCorrelation(PearsonsCorrelation.Implementation.Fast);

        for (int j = 0; j < nImages; j++) {
            ImageProcessor ip = stack.getProcessor(j+1);
            ImageProcessor allButOne = currentMean.getProcessor().duplicate();
            ImageProcessor tmp = ip.duplicate();
            tmp.multiply(1.0 / ((double) nImages));
            allButOne.copyBits(tmp, 0, 0, Blitter.SUBTRACT);
            ImagePlus impAllButOne = new ImagePlus( "allbutone " + j, allButOne);
            // Entropy impact
            double entropy = calcEntropy( impAllButOne );
            entropies[j] = stackEntropy - entropy;
            // Colocalization
            ImagePlus imp = new ImagePlus("image " + j, ip);
            Colocalization coloc = new Colocalization( imp, impAllButOne );
            pearson[j] = coloc.getPearson();
        }
        out.put("entropy", entropies);
        out.put("pearson", pearson);
    
        return out;
    }

    public static <T extends Comparable<T>> List<Integer> sortIndex(final List<T> in) {

        ArrayList<Integer> index = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            index.add(i);
        }

        Collections.sort(index, new Comparator<Integer>() {
            @Override
            public int compare(Integer idx1, Integer idx2) {
                return in.get(idx1).compareTo(in.get(idx2));
            }
        });

        return index;
    }
    
    public int[] sortImages( ImagePlus sample, ImagePlus impStack ) {
    
        int nImages = impStack.getNSlices();
        ImageStack stack = impStack.getStack();
        ArrayList<Double> pearson = new ArrayList<>();
        PearsonsCorrelation pearsonsCorrelation = new PearsonsCorrelation(PearsonsCorrelation.Implementation.Fast);

        for (int j = 0; j < nImages; j++) {
            ImageProcessor ip = stack.getProcessor(j+1);
            // Colocalization
            ImagePlus imp = new ImagePlus("image " + j, ip);
            Colocalization coloc = new Colocalization( imp, sample );
            pearson.add( coloc.getPearson() );
        }
        List<Integer> sortedList = sortIndex( pearson );
        Integer[] sortedIndicesInteger = sortedList.toArray( new Integer[sortedList.size()] );
        int[] sortedIndices = ArrayUtils.toPrimitive( sortedIndicesInteger );

        return sortedIndices;
    }

    public static int[] sortImagesNames( ImagePlus sample, ImagePlus impStack, LinkedHashMap< String, ImageProperties > stackProps ) {
    
        int nImages = impStack.getNSlices();
        ImageStack stack = impStack.getStack();
        ArrayList<Double> pearson = new ArrayList<>();
        PearsonsCorrelation pearsonsCorrelation = new PearsonsCorrelation(PearsonsCorrelation.Implementation.Fast);

        for (int j = 0; j < nImages; j++) {
            ImageProcessor ip = stack.getProcessor(j+1);
            // Colocalization
            ImagePlus imp = new ImagePlus("image " + j, ip);
            Colocalization coloc = new Colocalization( imp, sample );
            pearson.add( coloc.getPearson() );
        }
        List<Integer> sortedList = sortIndex( pearson );
        Integer[] sortedIndicesInteger = sortedList.toArray( new Integer[sortedList.size()] );
        int[] sortedIndices = ArrayUtils.toPrimitive( sortedIndicesInteger );

        return sortedIndices;
    }

    public double[] iterTransform( double[] tvecOld, ImagePlus currentMean, int nImages, ImageProcessor ip, String method, double[] methodParameters ) {

		//Transform2D transform2D = new Transform2D();
		double centerX = ((double) ip.getWidth()) / 2.0;
		double centerY = ((double) ip.getHeight()) / 2.0;
        double[] tvec = new double[tvecOld.length];
        for (int i = 0; i < tvec.length; i++) {
            tvec[i] = tvecOld[i];
        }
		AffineTransform2D tOld = tvecToTmat( tvec, centerX, centerY, this.methodParameters );
		AffineTransform2D t;

        double entropy = calcEntropy(currentMean);
        ImageProcessor oldIp = Transform2D.applyTransform( ip.duplicate(), tOld );
        ImageProcessor allButOne = currentMean.getProcessor().duplicate();
        ImageProcessor tmp = oldIp.duplicate();
        tmp.multiply(1.0 / ((double) nImages));
        allButOne.copyBits(tmp, 0, 0, Blitter.SUBTRACT);

		//currentMean.show();
		
        for (int i = 0; i < nParameters; i++) {

			tvec[i] = tvec[i] + 1.0;
			t = tvecToTmat( tvec, centerX, centerY, this.methodParameters );

			ImageProcessor newIp = Transform2D.applyTransform( ip.duplicate(), t );
			newIp.multiply(1.0 / ((double) nImages));
            ImageProcessor newMean = allButOne.duplicate();
            newMean.copyBits(newIp, 0, 0, Blitter.ADD);
            ImagePlus newMeanImp = new ImagePlus("new mean", newMean);
            double newEntropy = calcEntropy(newMeanImp);
			double oldEntropy = newEntropy;

            if (newEntropy < entropy) {
                entropy = newEntropy;
            } else {
                tvec[i] = tvec[i] - 2.0;
				t = tvecToTmat( tvec, centerX, centerY, this.methodParameters );
                newIp = Transform2D.applyTransform(ip.duplicate(), t );
                newIp.multiply(1.0 / ((double) nImages));
                newMean = allButOne.duplicate();
                newMean.copyBits(newIp, 0, 0, Blitter.ADD);
                newMeanImp = new ImagePlus("new mean +1", newMean);
                newEntropy = calcEntropy(newMeanImp);
                if (newEntropy < entropy) {
                    entropy = newEntropy;
                } else {
                    // Put the transform vector to the original values, the entropy is at a (local) minimum
                    tvec[i] = tvec[i] + 1;
                }
            }
        }

        return tvec;
    }

    public ImagePlus getMeanImage(ImagePlus impStack) {

        //impStack.setProcessor( impStack.getProcessor().convertToFloatProcessor() );
        //impStack.show();
        ZProjector zprojector = new ZProjector();
        zprojector.setImage(impStack);
        zprojector.setMethod(ZProjector.AVG_METHOD);
        zprojector.doProjection();
        ImagePlus meanImage = zprojector.getProjection();

        return meanImage;

    }

    public static double sumOfPixels(ImageProcessor ip) {
        ip = ip.duplicate();
        ip.resetRoi();
        switch (ip.getBitDepth()) {
            case 8:
                ip = ip.convertToShort(false);
                break;
            case 16:
                ip = ip.convertToFloat();
                break;
            case 32:
                break;
        }
        ImageStatistics stats = ip.getStatistics();

        return stats.area * stats.mean;
    }

    public static double meanOfPixels(ImageProcessor ip) {
        ip = ip.duplicate();
        ip.resetRoi();
        switch (ip.getBitDepth()) {
            case 8:
                ip = ip.convertToShort(false);
                break;
            case 16:
                ip = ip.convertToFloat();
                break;
            case 32:
                break;
        }
        ImageStatistics stats = ip.getStatistics();

        return stats.mean;
    }

    public double calcEntropy(ImagePlus impMean) {

        ImagePlus imp = impMean.duplicate();
        double entropy = 0.0;

        ImageProcessor ip = imp.getProcessor().duplicate();
        ip.min(0);
        ip.max(1);
        ip.multiply(9999.999999);
        ip.add(1.0);

        ImageProcessor ipEntropy = ip.duplicate();
        float[] pixels = (float[]) ipEntropy.getPixels();
        for (int i = 0; i < pixels.length; i++) {
            int index = (int) Math.floor(pixels[i]) - 1;
            ipEntropy.setf(i, (float) entropyTable[index]);
        }
        entropy = meanOfPixels(ipEntropy);

        return entropy;
    }

	public double harrisPointsPosition( ImageProcessor ip, int nPointsMax ) {

		// COMPUTE THE POINTS OF INTEREST (FOR NOW HARRIS CORNER POINTS)
        HarrisCornerDetector hd;
        HarrisCornerDetector.Parameters paramHarris = new HarrisCornerDetector.Parameters();
        paramHarris.alpha = 0.01;
        paramHarris.tH = 10;// 10
        paramHarris.dmin = 3;
		
        hd = new HarrisCornerDetector( ip, paramHarris);
        List<Corner> p = hd.findCorners();
        IJ.log("Number of points for image = " + p.size());
		//new ImagePlus("ip Harris " + p.size(), ip).show();

		// TAKE N STRONGEST POINTS (AND SHOW THEM)
        Collections.sort(p);
        int radius = 5;
		int nPoints = Math.min(p.size(), nPointsMax);
        double[] px = new double[nPoints];
        double[] py = new double[nPoints];
        for (int j = 0; j < nPoints; j++) {
            int xx = (int) Math.round(p.get(j).getX());
            int yy = (int) Math.round(p.get(j).getY());
        }

        // OBTAIN MEAN POSITION AND SHOW IT (RED CIRCLE)
        double mx = 0.0;
        for (int j = 0; j < nPoints; j++) {
            mx = mx + p.get(j).getX();
            px[j] = p.get(j).getX();
        }
		mx = Lib.median(px);
		
		return mx - ip.getWidth()/2.0;
	}
	
    public ImagePlus preWarping(ImagePlus impStack, int nPointsMax) {

		double centerX = ((double) impStack.getWidth()) / 2.;
		double centerY = ((double) impStack.getHeight()) / 2.;
        ImagePlus impWarped = impStack.duplicate();
        ImagePlus impColor = IJ.createHyperStack("Points of interest", impStack.getWidth(), impStack.getHeight(), 1, impStack.getNSlices(), 1, 24);

        Overlay overlay = new Overlay();
        for (int jj = 0; jj < impWarped.getNSlices(); jj++) {
			int i = this.stackProps.get(jj).index - 1;
			String id = this.stackProps.get(jj).id;

            // COMPUTE THE POINTS OF INTEREST (FOR NOW HARRIS CORNER POINTS)
            HarrisCornerDetector hd;
            HarrisCornerDetector.Parameters paramHarris = new HarrisCornerDetector.Parameters();
            paramHarris.alpha = 0.01;
            paramHarris.tH = 10;
            paramHarris.dmin = 3;

            hd = new HarrisCornerDetector(impWarped.getStack().getProcessor(i + 1), paramHarris);
            List<Corner> p = hd.findCorners();
            //IJ.log("Number of points for image " + i + " = " + p.size());

            // TAKE N STRONGEST POINTS (AND SHOW THEM)
            Collections.sort(p);
            ImageProcessor ip = impWarped.getStack().getProcessor(i + 1);
            ImageProcessor ip_color = ip.convertToRGB();
            impColor.getStack().setProcessor(ip_color, i + 1);
            int radius = 5;
            //nPoints = p.size();
            //nPoints = 3;
			int nPoints = Math.min(p.size(), nPointsMax);
            double[] px = new double[nPoints];
            double[] py = new double[nPoints];
            for (int j = 0; j < nPoints; j++) {
                int xx = (int) Math.round(p.get(j).getX());
                int yy = (int) Math.round(p.get(j).getY());
                Roi pointRoi = new OvalRoi(xx, yy, radius, radius);
                pointRoi.setPosition(i + 1);
                //pointRoi.setFillColor(Color.yellow);
                pointRoi.setStrokeWidth(1);
                pointRoi.setStrokeColor(Color.blue);
                overlay.add(pointRoi);
            }

            // OBTAIN MEAN POSITION AND SHOW IT (RED CIRCLE)
            double mx = 0.0;
            double my = 0.0;
            for (int j = 0; j < nPoints; j++) {
                mx = mx + p.get(j).getX();
                my = my + p.get(j).getY();
                px[j] = p.get(j).getX();
                py[j] = p.get(j).getY();
            }
			mx = Lib.median(px);
			my = Lib.median(py);
            //mx = mx / ((double) (nPoints));
            //my = my / ((double) (nPoints));
            Roi pointRoi = new OvalRoi((int) mx, (int) my, radius, radius);

            // COMPUTE THE ANGLE OF THE MEAN POINT POSITION (WITH AS ORIGIN IN THE MIDDLE OF THE IMAGE)
            mx = mx - ip.getWidth() / 2.0;
            my = -(my - ip.getHeight() / 2.0);
            double mangle = Math.atan2(my, mx) * 180 / Math.PI;
			mangle = mangle + Math.PI/9.0 * 180 / Math.PI;
            //IJ.log("Angle image " + (i + 1) + " = " + mangle);

            pointRoi.setPosition(i + 1);
            pointRoi.setStrokeWidth(1);
            pointRoi.setStrokeColor(Color.red);
            overlay.add(pointRoi);

			
            // ROTATE THE IMAGE SUCH THAT THE MEAN POINT (INTEREST) POSITION HAS AN ANGLE OF ZERO
            ip = applyRigidTransform(ip, 0.0, 0.0, mangle);

			AffineTransform2D E = new AffineTransform2D();
			E.set( new double[]{ 1., 0., 0.,  0., 1., 0. } );
            this.preTransformMat.put( id, Transform2D.rigidMirror( E.copy(), 0., 0., mangle, false, false, centerX, centerY) );
            this.preTransformVec.get( id )[this.INDEX_ROTATION] = mangle;// / this.methodParameters[this.INDEX_ROTATION];
            impWarped.getStack().setProcessor(ip, i + 1);

            // CHECK WHETHER TO FLIP THE IMAGE VERTICALLY OR NOT
            // 
            // TODO
            // 
        }
        impColor.setHideOverlay(false);
        impColor.setOverlay(overlay);
        impColor.flattenStack();
		this.preWarpingIllustration = new ImagePlus("prewarped sample", impColor.getStack().getProcessor(impColor.getNSlices()));
        //impWarped.show();
        //impWarped.updateAndRepaintWindow();
        //impColor.show();

        return impWarped;
    }

    public ImagePlus alignmentAngle( ImagePlus impStack, double angle, int nPointsMax ) {

		double centerX = ((double) impStack.getWidth()) / 2.;
		double centerY = ((double) impStack.getHeight()) / 2.;
		ImagePlus impWarped = impStack.duplicate();
		double maxValue = impWarped.getProcessor().getMax();
		//impWarped.setProcessor( impWarped.getProcessor().convertToByte(true) );
		// TODO MB
		if ( maxValue <= 1.0 ) {
			ImageStack stack = impWarped.getStack();
			for ( int i = 0; i < stack.getSize(); i++ ) {
				ImageProcessor ip = stack.getProcessor(i+1).duplicate();
				//new ImagePlus( "ip impWarped", ip ).show();
				ip.multiply(255);
				stack.setProcessor( ip, i+1);
			}
		}
		ImagePlus impColor = IJ.createHyperStack("Orientation line", impStack.getWidth(), impStack.getHeight(), 1, impStack.getNSlices(), 1, 24);

		Overlay overlay = new Overlay();
		for (int jj = 0; jj < impWarped.getNSlices(); jj++) {
			int i = this.stackProps.get(jj).index - 1;
			String id = this.stackProps.get(jj).id;

			ImageProcessor ip = impWarped.getStack().getProcessor(i + 1);
			ImageProcessor ip_color = ip.convertToRGB();
			impColor.getStack().setProcessor(ip_color, i + 1);

            // COMPUTE THE ANGLE OF THE LINE
			double mangle = SpotOrientation.getAngle( new ImagePlus( "", ip ) );
			double xPosition = harrisPointsPosition( ip, nPointsMax );
			if (xPosition < 0.0) {
				mangle = mangle + 180;
			}

			// ROTATE THE IMAGE SUCH THAT THE MEAN POINT (INTEREST) POSITION HAS AN ANGLE OF ZERO
            ip = applyRigidTransform(ip, 0.0, 0.0, mangle );
			AffineTransform2D tTemp = Transform2D.rigidMirrorScaleFast( Transform2D.unitMatrix().copy(), 0., 0., mangle * Math.PI / 180., false, false, false, 1., 1., centerX, centerY);
			this.preTransformMat.put( id, tTemp );
			//rigidMirror( Transform2D.unitMatrix().copy(), 0., 0., mangle, false, false, centerX, centerY)
            this.preTransformVec.get( id )[this.INDEX_ROTATION] = mangle;// / this.methodParameters[this.INDEX_ROTATION];
            impWarped.getStack().setProcessor(ip, i + 1);

        }
        //impColor.setHideOverlay(false);
        //impColor.setOverlay(overlay);
        //impColor.flattenStack();
		this.preWarpingIllustration = new ImagePlus("prewarped sample", impColor.getStack().getProcessor(impColor.getNSlices()));
        //impWarped.show();
        //impWarped.updateAndRepaintWindow();
        //impColor.show();

		if ( maxValue <= 1.0 ) {
			ImageStack stack = impWarped.getStack();
			for ( int i = 0; i < stack.getSize(); i++ ) {
				ImageProcessor ip = stack.getProcessor(i+1).duplicate();
				ip.multiply(1.0/255.0);
				stack.setProcessor( ip, i+1);
			}
		}

        return impWarped;
    }



    public double[] normalizeTMat(double[][] tmat, int nParameters, int nImages) {

        double[] tvecMean = new double[nParameters];
        Arrays.fill(tvecMean, 0.0);
        for (int k = 0; k < nParameters; k++) {
            for (int j = 0; j < nImages; j++) {
                tvecMean[k] = tvecMean[k] + tmat[j][k];
            }
            tvecMean[k] = tvecMean[k] / ((double) nImages);
        }

        for (int k = 0; k < nParameters; k++) {
            if ( this.normalizedParameters[k] ) {
                for (int j = 0; j < nImages; j++) {
					if ( this.integerParameters[k] ) {
						tvecMean[k] = Math.round( tvecMean[k] );
					}
					tmat[j][k] = tmat[j][k] - tvecMean[k];
                }
            }
        }

        return tvecMean;
    }

    public void runCongealing(ImagePlus impStack, int nIterations, String transformationType, double[] methodParameters) {

		// j is the image index 
		// k is the parameter index
        int nImages = impStack.getNSlices();
        int sizeX = impStack.getWidth();
        int sizeY = impStack.getHeight();
        int sizeZ = impStack.getNSlices();
        int bitDepth = 32;
        ImageStack stack = impStack.getStack();
        ImagePlus regImages = IJ.createHyperStack("Aligned images", sizeX, sizeY, 1, sizeZ, 1, bitDepth);
        ImageStack regStack = regImages.getStack();
        ImagePlus meanImages = IJ.createHyperStack("Stack of mean images", sizeX, sizeY, 1, nIterations + 1, 1, bitDepth);
        ImageStack meanStack = meanImages.getStack();
        int iter = 1;

        ImagePlus meanImage = getMeanImage(impStack);
        //meanImage.show();
        meanStack.setProcessor(meanImage.getProcessor().duplicate(), iter);
        ImagePlus currentMean = new ImagePlus("Current mean image", meanImage.getProcessor().duplicate());

        // tmat is the transformation matrix: tmat[j,k] with j = image, and k = parameter
        double[][] tmat = new double[nImages][nParameters];
        double[][] tmatOld = new double[nImages][nParameters];
        for (int j = 0; j < nImages; j++) {
            for (int k = 0; k < nParameters; k++) {
                tmat[j][k] = 0.0;
                tmatOld[j][k] = 0.0;
            }
        }

        // For plotting entropy
        double[] yEntropy = new double[nIterations];
        double[] xIter = new double[nIterations];
		ArrayList< AffineTransform2D > tList = new ArrayList<>();

        for (iter = 2; iter < nIterations + 2; iter++) {

			xIter[iter - 2] = iter - 1;
            for (int j = 0; j < nImages; j++) {
				tmat[j] = iterTransform( tmatOld[j], currentMean.duplicate(), nImages, stack.getProcessor(j + 1).duplicate(), transformationType, methodParameters);
            }

	        double[] tvecMean = normalizeTMat( tmat, nParameters, nImages );
			tList = new ArrayList<>();
			double centerX = ((double) currentMean.getWidth()) / 2.0;
			double centerY = ((double) currentMean.getHeight()) / 2.0;
            for (int j = 0; j < nImages; j++) {
	 			tList.add( this.tvecToTmat( tmat[j], centerX, centerY, this.methodParameters) );
			}

            for (int j = 0; j < nImages; j++) {
				ImageProcessor regIp;
                regIp = Transform2D.applyTransform( stack.getProcessor(j + 1).duplicate(), tList.get(j) );
                regStack.setProcessor( regIp, j + 1 );
            }
            currentMean = getMeanImage(new ImagePlus("", regStack));

            double entropy = calcEntropy(currentMean);
            yEntropy[iter - 2] = entropy;
            IJ.log("Iteration " + (iter - 1) + " Entropy = " + entropy);

            meanImages.getStack().setProcessor(currentMean.getProcessor(), iter);
            // tmatOld = tmat;
            for (int k = 0; k < nParameters; k++) {
                for (int j = 0; j < nImages; j++) {
                    tmatOld[j][k] = tmat[j][k];
                }
            }
        }

        Plot plot_entropy = new Plot("Entropy", "Entropy", "iteration", xIter, yEntropy);
        //plot_entropy.show();
		for (int j = 0; j < nImages; j++) {
			String id = this.stackProps.get(j).id;
			this.transformMat.put( id, tList.get(j) );
			this.transformVec.put(id, tmat[j]);
		}
		this.meanStack = meanImages;
		// TODO
		//this.meanStack.show();
		this.finalMeanImage = getMeanImage(new ImagePlus("", regStack));
        this.alignedStack = new ImagePlus("Aligned stack", regStack);
		calcImpacts(yEntropy[yEntropy.length-1]);

    }

	public void calcImpacts(double finalTotalEntropy) {

		LinkedHashMap<String,double[]> sim = calcEntropyImpact( this.alignedStack );
        double[] entropies = sim.get("entropy");
        double[] pearson = sim.get("pearson");
        for ( int i = 0; i < entropies.length; i++ ) {
            entropies[i] = entropies[i] * nImages / finalTotalEntropy;
        }
        double[] xImage = linearSpacedSequence(1, nImages, nImages-1);
        Plot plot_entropyImpact = new Plot("Entropy", "Image index", "Partial Entropy impact", xImage, entropies);
        //plot_entropyImpact.show();
        Plot plot_pearsonImpact = new Plot("Pearson", "Image index", "Partial Pearson impact", xImage, pearson);
        //plot_pearsonImpact.show();

		this.pearsonImpact = pearson;
		this.entropyImpact = entropies;
	}

	public static void main(String[] args) {

		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Congealing.class;
		System.out.println(clazz.getName());
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.out.println(pluginsDir);
		System.setProperty("plugins.dir", pluginsDir);
		// start ImageJ
		new ImageJ();
		IJ.log(" -------------------- START PROGRAM CONGEALING -------------------------");

		AffineCongealing congealing = new AffineCongealing();
		congealing.setTRANSFORM("RIGID_MIRROR_XY");
		boolean doStackAlign = true;
		ImagePlus imp = IJ.openImage("D:/p_prog_output/slicemap_3/input/reference_stack/reference_stack.tif");

		Main param = new Main();
		param.SAMPLE_FOLDER = new File("d:/p_prog_output/slicemap_3/samples");
		param.INPUT_FOLDER = new File("d:/p_prog_output/slicemap_3/input");
		param.APP_FOLDER = new File(param.INPUT_FOLDER.getAbsolutePath() + "/" + "debug");
		param.OUTPUT_FOLDER = new File("d:/p_prog_output/slicemap_3/output");
		param.FILE_REFERENCE_STACK = new File(param.INPUT_FOLDER.getAbsolutePath() + "/" + Main.CONSTANT_SUBDIR_REFERENCE_STACK + "/" + Main.CONSTANT_NAME_REFERENCE_STACK);
		param.FILENAME_REFERENCE_STACK = param.FILE_REFERENCE_STACK.getName();
		param.PATTERN_REF_FILES = "^(.*?)\\.tif";
		param.CONTAINS_REF_FILES = "tif";
		param.DOESNOTCONTAIN_REF_FILES = ".zip";
		param.CONGEALING_STACKBINNING = 16;
		param.CONGEALING_NITERATIONS = 20;
		param.CONGEALING_NREFERENCES = 3;
		param.CONGEALING_BINCONGEALING = 1;
		param.CONGEALING_NPOINTS = 8;
		param.CONGEALING_SATURATED_PIXELS_PERCENTAGE = 0.05;
		param.DO_LOAD_ALIGNED_STACK = doStackAlign;

		File stackPropsFile = new File( param.INPUT_FOLDER.getAbsolutePath() + "/" + Main.CONSTANT_SUBDIR_REFERENCE_STACK + "/" + Main.CONSTANT_STACKPROPS_LABEL + "_" + Main.CONSTANT_NAME_REFERENCE_STACK + ".csv");
		param.FILE_STACKPROPS = stackPropsFile;
		param.FILE_TRANSFORMVEC = new File( param.APP_FOLDER.getAbsolutePath() + "/" + Main.CONSTANT_TRANSFORMVEC_LABEL + "_" +  Main.CONSTANT_NAME_REFERENCE_STACK + ".csv");
		param.FILE_PRETRANSFORMVEC = new File( param.APP_FOLDER.getAbsolutePath() + "/" + Main.CONSTANT_PRETRANSFORMVEC_LABEL + "_" + Main.CONSTANT_NAME_REFERENCE_STACK + ".csv");
		param.FILE_TRANSFORMREALVEC = new File( param.APP_FOLDER.getAbsolutePath() + "/" + Main.CONSTANT_TRANSFORMREALVEC_LABEL + "_" + Main.CONSTANT_NAME_REFERENCE_STACK + ".csv");
		File alignedStackFile = new File( param.APP_FOLDER.getAbsolutePath() + "/" + Main.CONSTANT_ALIGNEDSTACK_LABEL + "_" + Main.CONSTANT_NAME_REFERENCE_STACK );
		param.FILE_ALIGNED_REFERENCE_STACK = alignedStackFile;

		//File sampleFile
		//IJ.log("sampleFile = " + sampleFile.getAbsolutePath() );
		IJ.log("inputFile = " + param.INPUT_FOLDER.getAbsolutePath() );
		IJ.log("appFile = " + param.APP_FOLDER.getAbsolutePath() );
		IJ.log("outputFile = " + param.OUTPUT_FOLDER.getAbsolutePath() );

        IJ.log(" ------------------ START PROGRAM CONGEALING -------------------------");

		//  1) reference stack which is pre-binned and smoothed
		String stackFilePath;
		stackFilePath = param.FILE_REFERENCE_STACK.getAbsolutePath();
		IJ.log("Load congealing reference stack: " + stackFilePath);
		imp = IJ.openImage(stackFilePath);
		// Loading stack properties
		congealing.loadStackProps( param.FILE_STACKPROPS );
		int nImages = imp.getNSlices();
		congealing.nImages = nImages;
		congealing.nParameters = 8;
		int nPointsMax = 20;
		imp = congealing.alignmentAngle(imp, 0., nPointsMax);
		//imp.show();
		
		IJ.log(" --- Normalizing the stack [0..1]");
		ImagePlus impNorm = IJ.createHyperStack("Stack 32 bit", imp.getWidth(), imp.getHeight(), 1, nImages, 1, 32);
		for (int i = 1; i < nImages + 1; i++) {
			ImageProcessor ipNormi = imp.getStack().getProcessor(i).convertToFloatProcessor();
			ipNormi.multiply(1.0 / imp.getProcessor().maxValue() );
			impNorm.getStack().setProcessor(ipNormi, i);
			impNorm.getStack().setSliceLabel( imp.getStack().getSliceLabel(i), i);
		}
		// Show the image normalized image
		//impNorm.show();

		//congealing.initTransformVec( impNorm.getNSlices(), congealing.nParameters);
		congealing.runCongealing(impNorm, param.CONGEALING_NITERATIONS, congealing.TRANSFORM, congealing.methodParameters);
		ImagePlus impAligned = congealing.getAlignedStack();
		//impAligned.show();
        IJ.log(" -------------------- END PROGRAM CONGEALING -------------------------");
    }
}
