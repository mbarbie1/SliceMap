/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib.congealing;

import main.be.ua.mbarbier.external.Transform_Roi;
import static main.be.ua.mbarbier.external.Transform_Roi.fitGlobalAffine;
import main.be.ua.mbarbier.slicemap.lib.BiMap;
import static main.be.ua.mbarbier.slicemap.lib.Lib.getCommonKeys;
import main.be.ua.mbarbier.slicemap.lib.transform.Transform2D;
import static main.be.ua.mbarbier.slicemap.lib.transform.Transform2D.removeAffineScalingMagnitude;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.Roi;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.imglib2.realtransform.AffineTransform2D;

/**
 *
 * @author mbarbier
 */
public class LandmarksRegistration {
	/**
	 * 
	 * @param impStack
	 * @param roiList
	 * @param idMap
	 * @param roi0
	 * @param imp0
	 * @return 
	 */
	public static ImagePlus alignmentPoints( ImagePlus impStack, LinkedHashMap< String, LinkedHashMap< String, Roi > > roiList, BiMap<String, Integer> idMap, LinkedHashMap< String, Roi > roi0, ImagePlus imp0 ) {

		ImagePlus alignedStack = IJ.createHyperStack( "alignedStack alignmentPoints", impStack.getWidth(), impStack.getHeight(), 1, impStack.getNSlices(), 1, impStack.getBitDepth());

		for ( String keySlice : roiList.keySet() ) {
			LinkedHashMap< String, Roi > roiMapRef = roiList.get(keySlice);
			ArrayList< String > keys = getCommonKeys( roiMapRef, roi0 );
			float[] x = new float[keys.size()];
			float[] y = new float[keys.size()];
			float[] x0 = new float[keys.size()];
			float[] y0 = new float[keys.size()];
			int j = 0;
			for ( String key : keys )  {
				Roi roi = roiMapRef.get(key);
				x[j] = roi.getPolygon().xpoints[0];
				y[j] = roi.getPolygon().ypoints[0];
				x0[j] = roi0.get(key).getPolygon().xpoints[0];
				y0[j] = roi0.get(key).getPolygon().ypoints[0];
				j++;
			}
			PointRoi proi0 = new PointRoi( x0, y0 );
			PointRoi proi = new PointRoi( x, y );
			imp0.setRoi( proi0 );
			ImagePlus imp = new ImagePlus( "source" + idMap.get(keySlice), impStack.getStack().getProcessor(idMap.get(keySlice)).duplicate() );
			ImagePlus imptest = new ImagePlus( "sourcetest" + idMap.get(keySlice), impStack.getStack().getProcessor(idMap.get(keySlice)).duplicate() );
			imp0.duplicate().show();
			imp.setRoi( proi );
			imptest.setRoi( proi0 );
			imp.setOverlay(proi, Color.yellow, 3, Color.yellow);
			imptest.setOverlay(proi0, Color.yellow, 3, Color.yellow);
			imp.duplicate().show();
			imptest.duplicate().show();

			//Transform_Roi tr = new Transform_Roi();
			//tr.init( imp, imp0, 1, 2.f, 32, 3, true, false);
			//tr.run( "" );//"source_image=" + imp.getTitle() + " template_image=" + imp0.getTitle() + " transformation_method=[Least Squares] alpha=1 mesh_resolution=32 transformation_class=Affine interpolate" );
			//alignedStack.getStack().setProcessor(imp.getProcessor(), idMap.get(keySlice));

			AffineTransform2D affineTransform = fitGlobalAffine( imp0, imp, 3 );
			ImagePlus impTest = Transform2D.applyTransform( affineTransform.copy(), imp.duplicate() );
			impTest.setTitle( "test" );
			impTest.show();
			IJ.log( "Affine before remove scaling " + affineTransform.toString() );
			AffineTransform2D affineTransformT = removeAffineScalingMagnitude( affineTransform );
			IJ.log( "Affine after remove scaling " + affineTransformT.toString() );

			AffineTransform2D E = Transform2D.unitMatrix();
			AffineTransform2D T = E.copy();
			T.translate( new double[]{-165,-165} );
			IJ.log( "T " + T.copy().toString() );
			AffineTransform2D Q = affineTransformT.copy().concatenate( T );
			IJ.log( "A T " + Q.copy().toString() );
			AffineTransform2D P = Q.preConcatenate( T.inverse() );
			IJ.log( "Affine T^(-1) A T " + P.copy().toString() );
			imp.duplicate().show();
			ImagePlus impT1 = Transform2D.applyTransform( E, imp.duplicate() );
			ImagePlus impT2 = Transform2D.applyTransform( T, imp.duplicate() );
			ImagePlus impT3 = Transform2D.applyTransform( Q, imp.duplicate() );
			ImagePlus impT4 = Transform2D.applyTransform( P, imp.duplicate() );
			ImagePlus impT = new ImagePlus( " - impT - ", Transform2D.applyTransform( imp.getProcessor().duplicate(), P ) );
			impT1.show();
			impT2.show();
			impT3.show();
			impT4.show();
			impT.show();
		}

		return alignedStack;
	}
}
