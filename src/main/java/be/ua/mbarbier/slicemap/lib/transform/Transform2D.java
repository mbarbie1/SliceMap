/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ua.mbarbier.slicemap.lib.transform;

import be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import static be.ua.mbarbier.slicemap.lib.roi.LibRoi.getOverlayImage;
import static be.ua.mbarbier.slicemap.lib.transform.TransformRoi.applyRoiScaleTransform;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.ContrastEnhancer;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import io.scif.img.ImgOpener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.view.IntervalView;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author mbarbier
 */
public class Transform2D {

	/**
	 * 
	 * @return Unit affine matrix
	 */
	public static AffineTransform2D unitMatrix() {

		AffineTransform2D E = new AffineTransform2D();
		E.set( new double[]{  1., 0., 0.,   0., 1., 0. } );

		return E;
	}

	public static AffineTransform2D removeAffineScalingMagnitude(AffineTransform2D affineTransform) {

		double a = affineTransform.get(0, 0);
		double b = affineTransform.get(0, 1);
		double c = affineTransform.get(1, 0);
		double d = affineTransform.get(1, 1);
		double sx = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
		double sy = Math.sqrt(Math.pow(c, 2) + Math.pow(d, 2));

		a = a / sx;
		b = b / sx;
		c = c / sy;
		d = d / sy;
		AffineTransform2D out = affineTransform.copy();
		out.set( new double[]{ a, b, affineTransform.get( 0, 2), c, d, affineTransform.get( 1, 2) } );

		return out;
	}

	public static AffineTransform2D translation( AffineTransform2D in, double tx, double ty ) {

		AffineTransform2D out = in.copy();
		double[] t = new double[]{tx,ty};
		out.translate(t);

		return out;
	}

	public static AffineTransform2D rotation( AffineTransform2D in, double angle, double centerX, double centerY ) {

		AffineTransform2D out = in.copy();
		out.translate(new double[]{-centerX, -centerY});
		out.rotate(angle);
		out.translate(new double[]{centerX, centerY});

		return out;
	}

	public static AffineTransform2D scale( AffineTransform2D in, double scale, double centerX, double centerY ) {

		AffineTransform2D out = in.copy();
		out.translate(new double[]{-centerX, -centerY});
		out.scale(scale);
		out.translate(new double[]{centerX, centerY});

		return out;
	}

	public static AffineTransform2D scale( AffineTransform2D in, double sx, double sy, double centerX, double centerY ) {

		AffineTransform2D out = in.copy();
		out.translate(new double[]{-centerX, -centerY});

		AffineTransform2D ES = new AffineTransform2D();
		ES.set( new double[]{ sx, 0., 0.,  0., sy, 0. } );
		out.preConcatenate(ES);
		out.translate(new double[]{centerX, centerY});

		return out;
	}

	public static AffineTransform2D mirror( AffineTransform2D in, boolean mirrorX, boolean mirrorY, double centerX, double centerY ) {

		AffineTransform2D out = in.copy();
		AffineTransform2D E = Transform2D.unitMatrix();
		if (mirrorX & mirrorY) {
				AffineTransform2D EXY = E.copy();
				EXY.set( new double[]{ -1., 0., 0.,  0., -1., 0.,  0., 0., 1. } );
				AffineTransform2D ET1 = E.copy();
				ET1.set( new double[]{ 1., 0., -centerX,  0., 1., -centerY,  0., 0., 1. } );
				AffineTransform2D ET2 = E.copy();
				ET2.set( new double[]{ 1., 0., centerX,  0., 1., centerY,  0., 0., 1. } );
				out = out.preConcatenate(ET1);
				out = out.preConcatenate(EXY);
				out = out.preConcatenate(ET2);
		} else {
			if (mirrorX) {
					AffineTransform2D EX = E.copy();
					EX.set( new double[]{ -1., 0., 0.,  0., 1., 0. } );
					AffineTransform2D ET1 = E.copy();
					ET1.set( new double[]{ 1., 0., -centerX,  0., 1., 0. } );
					AffineTransform2D ET2 = E.copy();
					ET2.set( new double[]{ 1., 0., centerX,  0., 1., 0. } );
					out = out.preConcatenate(ET1);
					out = out.preConcatenate(EX);
					out = out.preConcatenate(ET2);
			} else {
				if (mirrorY) {
					AffineTransform2D EY = E.copy();
					EY.set( new double[]{ 1., 0., 0.,  0., -1., 0. } );
					AffineTransform2D ET1 = E.copy();
					ET1.set( new double[]{ 1., 0., 0.,  0., 1., -centerY } );
					AffineTransform2D ET2 = E.copy();
					ET2.set( new double[]{ 1., 0., 0.,  0., 1., centerY } );
					out = out.preConcatenate(ET1);
					out = out.preConcatenate(EY);
					out = out.preConcatenate(ET2);
				}
			}
		}

		return out;
	}

	public static AffineTransform2D rigidMirror( AffineTransform2D in, double tx, double ty, double angle, boolean mirrorx, boolean mirrory, double centerX, double centerY ) {

		AffineTransform2D out = in.copy();
		out = translation( out, tx, ty );
		out = rotation( out, angle, centerX, centerY );
		out = mirror( out, mirrorx, mirrory, centerX, centerY );

		return out;
	}

	public static AffineTransform2D rigidMirrorScale( AffineTransform2D in, double tx, double ty, double angle, boolean mirrorx, boolean mirrory, double sx, double sy, double centerX, double centerY ) {

		AffineTransform2D out = in.copy();
		out = translation( out, tx, ty );
		out = rotation( out, angle, centerX, centerY );
		out = mirror( out, mirrorx, mirrory, centerX, centerY );
		out = scale( out, sy, sy, centerX, centerY );

		return out;
	}

	public static AffineTransform2D rigidMirrorScaleFast( AffineTransform2D in, double tx, double ty, double angle, boolean mirrorx, boolean mirrory, boolean mirrorxy, double sx, double sy, double centerX, double centerY ) {

		AffineTransform2D out = in.copy();

		out.translate(new double[]{-centerX, -centerY});

		// Rotation
		out.rotate(angle);

		// Mirror
		AffineTransform2D EM = new AffineTransform2D();
		if( mirrorx & mirrory ) {
			EM.set( new double[]{ -1., 0., 0.,  0., -1., 0. } );
			out = out.preConcatenate(EM);
		} else {
			if ( mirrorx ) {
				EM.set( new double[]{ -1., 0., 0.,  0., 1., 0. } );
				out = out.preConcatenate(EM);
			} else {
				if ( mirrory ) {
					EM.set( new double[]{ 1., 0., 0.,  0., -1., 0. } );
					out = out.preConcatenate(EM);
				}
			}
		}

		// Double mirror = 180 deg turn
		if( mirrorxy ) {
			EM.set( new double[]{ -1., 0., 0.,  0., -1., 0. } );
			out = out.preConcatenate(EM);
		}

		// Scaling
		if ( sx != 1. | sy != 1. ) {
			AffineTransform2D ES = new AffineTransform2D();
			ES.set( new double[]{ sx, 0., 0.,  0., sy, 0. } );
			out.preConcatenate(ES);
		}

		// Translation
		out.translate(new double[]{tx,ty});

		out.translate(new double[]{centerX, centerY});

		return out;
	}

	public static RandomAccessibleInterval applyTransform( AffineTransform2D in, Img img ) {

		// Extend the image with zero pixels
		RandomAccessible< FloatType> imgExtended =
			Views.extendValue( img, new FloatType( 0 ) );

		// Setup RealRandomAcces by interpolation
		final RealRandomAccessible field = Views.interpolate( imgExtended, new ClampingNLinearInterpolatorFactory<>());

		// Transform the field
		final AffineRandomAccessible< FloatType, AffineGet > img_t = RealViews.affine( field, in );

		// apply the original bounding box to the result image
		final IntervalView< ? > bounded = Views.interval(img_t, (RandomAccessibleInterval) img);

		return bounded;
	}

	public static ImagePlus applyTransform( AffineTransform2D in, ImagePlus imp ) {

		// Convert input to Img
		Img< FloatType > img = ImageJFunctions.convertFloat(imp);

		// Do transformation
		RandomAccessibleInterval bounded = applyTransform( in, img );

		// Convert output to ImagePlus
		ImagePlus out = ImageJFunctions.wrapFloat( bounded, "out");

		return out;
	}

	public static ImageProcessor applyTransform( ImageProcessor ip, AffineTransform2D in ) {

		ImagePlus imp = new ImagePlus("", ip );

		return applyTransform( in, imp ).getProcessor();
	}

	public static ImagePlus applyInverseTransform( AffineTransform2D in, ImagePlus imp ) {

		// Convert input to Img
		Img< FloatType > img = ImageJFunctions.convertFloat(imp);

		// Do transformation
		RandomAccessibleInterval bounded = applyTransform( in.inverse(), img );

		// Convert output to ImagePlus
		ImagePlus out = ImageJFunctions.wrapFloat( bounded, "out");

		return out;
	}

	public static ImageProcessor applyInverseTransform( ImageProcessor ip, AffineTransform2D in ) {

		ImagePlus imp = new ImagePlus("", ip );

		return applyInverseTransform( in, imp ).getProcessor();
	}

    public static ImagePlus applyRefTransform( ImagePlus imp, AffineTransform2D pretvecRef, AffineTransform2D tvecRef, AffineTransform2D pretvecSample, AffineTransform2D tvecSample ) {

		ImagePlus transformedImage = applyTransform( pretvecRef, imp );
		transformedImage = applyTransform( tvecRef, transformedImage );
		transformedImage = applyInverseTransform( tvecSample, transformedImage );
		transformedImage = applyInverseTransform( pretvecSample, transformedImage );

        return transformedImage;
    }

	public static Roi applyRoiTransform( AffineTransform2D in, Roi roi ) {

        ArrayList<Float> xt = new ArrayList<Float>();
        ArrayList<Float> yt = new ArrayList<Float>();
        // Get ROI points
        FloatPolygon polygon = roi.getFloatPolygon();
        int n_points = polygon.npoints;
        float[] x = polygon.xpoints;
        float[] y = polygon.ypoints;

        for (int i = 0; i < n_points; i++) {
			float[] p = new float[]{ x[i], y[i] };
			float[] pt = new float[]{ 0.0f, 0.0f };
			in.apply(p, pt);
            xt.add( pt[0] );
            yt.add( pt[1] );
        }
        Float[] xta = xt.toArray(new Float[0]);
        Float[] yta = yt.toArray(new Float[0]);
        Roi transfoRoi = new PolygonRoi(ArrayUtils.toPrimitive(xta), ArrayUtils.toPrimitive(yta), xt.size(), PolygonRoi.FREEROI);
        transfoRoi.setName(roi.getName());
        transfoRoi.setStrokeColor(roi.getStrokeColor());

        return transfoRoi;
    }

	public static Roi applyRoiInverseTransform( AffineTransform2D in, Roi roi ) {

		Roi transfoRoi = applyRoiTransform( in.inverse(), roi );
        return transfoRoi;
    }   

	public static LinkedHashMap<String,Roi> applyRoiMapTransform( LinkedHashMap<String,Roi> roiMap, AffineTransform2D affine ) {

        LinkedHashMap<String,Roi> roiMapT = new LinkedHashMap<>();
        for (String key : roiMap.keySet() ) {
            Roi roi = roiMap.get(key);
            Roi transformedRoi = applyRoiTransform( affine, roi );
            roiMapT.put(key, transformedRoi);
        }

        return roiMapT;
    }

	public static LinkedHashMap<String,Roi> applyRoiRefTransform( LinkedHashMap<String,Roi> roiMap, AffineTransform2D pretvecRef, AffineTransform2D tvecRef, AffineTransform2D pretvecSample, AffineTransform2D tvecSample ) {

        LinkedHashMap<String,Roi> roiMapT = new LinkedHashMap<>();
        for (String key : roiMap.keySet() ) {
            Roi roi = roiMap.get(key);
            Roi transformedRoi = applyRoiTransform( pretvecRef, roi );
            transformedRoi = applyRoiTransform( tvecRef, transformedRoi );
            transformedRoi = applyRoiInverseTransform( tvecSample, transformedRoi );
            transformedRoi = applyRoiInverseTransform( pretvecSample, transformedRoi );
            roiMapT.put(key, transformedRoi);
        }

        return roiMapT;
    }

	public < T extends RealType< T > & NativeType< T > > void  Example1d(AffineTransform2D affine) throws IncompatibleTypeException, io.scif.img.ImgIOException {

		ImgOpener imgOpener = new ImgOpener();
		String path = "D:/p_prog_output/slicemap_3/input/reference_stack/reference_stack.tif";
		ImagePlus imp = IJ.openImage(path, 1);
		ImagePlus imp_out = applyTransform( affine, imp );
		imp_out.show();

		String roiPath = "D:/p_prog_output/slicemap_3/input/reference_rois/ref-01.zip";
		File roiFile = new File( roiPath );

		try {
			LinkedHashMap<String, Roi> roiMap = LibRoi.loadRoiAlternative( roiFile );
			roiMap = applyRoiScaleTransform( roiMap, 0., 0., 1./16. );
			LinkedHashMap<String, Roi> roiMapT = applyRoiMapTransform( roiMap, affine );

			ContrastEnhancer ec = new ContrastEnhancer();
			ImageProcessor ip = imp.getProcessor().duplicate().convertToShort(true);
			ec.stretchHistogram(ip, 0.1);
			ImagePlus impS = new ImagePlus( "imp intensity scaled", ip );

			ec = new ContrastEnhancer();
			ImageProcessor ip_out = imp_out.getProcessor().duplicate();//.convertToByte(true);
			ec.stretchHistogram(ip_out, 0.1);
			ip_out = ip_out.convertToByte(true);
			ImagePlus impS_out = new ImagePlus( "imp_out intensity scaled", ip_out );

			ImagePlus impOverlay = getOverlayImage(roiMap, impS );
			ImagePlus impOverlay_out = getOverlayImage(roiMapT, impS_out);
			impOverlay.show();
			impOverlay_out.show();

		} catch (ZipException | IOException ex) {
			Logger.getLogger(Transform2D.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	public static void main(String[] args) {

		new ImageJ();
        IJ.log(" ------------------ START TRANSFORM2D TESTING -------------------------");
		try {
			//String stackFilePath = ;
			Transform2D ex = new Transform2D();
			AffineTransform2D affine = new AffineTransform2D();
			affine.set(new double[][] {{1., 0., 0.}, {0., 1., 0.}, {0., 0., 1.} });
			affine = rigidMirror( affine, -20., 40., 0.25 * Math.PI, true, false, 158., 158.);
			//affine = scale( affine, 1.2, 158., 158.);
			ex.Example1d( affine );
		} catch (IncompatibleTypeException | io.scif.img.ImgIOException ex) {
			Logger.getLogger(Transform2D.class.getName()).log(Level.SEVERE, null, ex);
		}
		//IJ.log("Load Congealing reference stack: " + stackFilePath);
		//ImagePlus imp = IJ.openImage(stackFilePath);
		IJ.log(" -------------------- END TRANSFORM2D TESTING -------------------------");
    }

}
