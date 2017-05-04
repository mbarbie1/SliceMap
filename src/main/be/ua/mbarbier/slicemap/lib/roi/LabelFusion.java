/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib.roi;

import main.be.ua.mbarbier.slicemap.lib.BiMap;
import static main.be.ua.mbarbier.slicemap.lib.Lib.median;
import main.be.ua.mbarbier.slicemap.lib.image.IndexProjectionZ;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.maskIntersection;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.roiFromMask;
import main.be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.HeightRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.isolineFromProbabilityImage;
import static main.be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.isolineFromRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.isolineMap;
import static main.be.ua.mbarbier.slicemap.lib.roi.RoiInterpolation.maskFromThreshold;
import static main.be.ua.mbarbier.slicemap.lib.roi.RoiMap.mapInvert;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 *
 * @author mbarbier
 */
public class LabelFusion {

	public final static String METHOD_LABELFUSION_THRESHOLD = "threshold";
	public final static String METHOD_LABELFUSION_MAJORITY = "majority";
	public final static String METHOD_PROBABILITY_LINEAR_DISTANCE = "linear_distance";
	public final static String METHOD_PROBABILITY_SUM = "sum";
	public final static String METHOD_PROBABILITY_WEIGHTED_SUM = "weighted_sum";

	/**
	 * 
	 * 
	 * @param sizeX
	 * @param sizeY
	 * @param roiListMap map < Region, < image_id, Roi >
	 * @param method
	 * @param halfDist the distance over which the weight drops to 0.5
	 * @param numberOfImages number of actual references (not all references specify each region), we need this possibly for normalization (if emptyRoisCount = true)
	 * @param normalizeToOne
	 * @return 
	 */
	public static LinkedHashMap< String, ImagePlus > majorityVoting( int sizeX, int sizeY, LinkedHashMap< String, LinkedHashMap< String, Roi > > roiListMap, String method, double halfDist, int numberOfImages, boolean normalizeToOne ) {

		LinkedHashMap<String, ImagePlus> probMap = new LinkedHashMap<>();
		double norm = 1.0;//(double) wMap.size();

		for ( String roiName : roiListMap.keySet() ) {
			LinkedHashMap< String , Roi> rois = roiListMap.get( roiName );
			ImagePlus impProbability;
			if ( normalizeToOne ) {
				norm = 0.0;
			}
			switch(method) {
				case LabelFusion.METHOD_PROBABILITY_WEIGHTED_SUM :
					double normDist = ( sizeX + sizeY ) / 2.0;
					LinkedHashMap< String, Double > sdMap = imageRoisVariance( roiListMap, sizeX, sizeY, normDist );
					LinkedHashMap< String, Double > wMap = imageRoisWeight( sdMap, halfDist );
					impProbability = roiDensityWeight( rois, wMap, sizeX, sizeY, norm, true, numberOfImages );
					probMap.put(roiName, impProbability);
					break;
				case LabelFusion.METHOD_PROBABILITY_SUM :
					LinkedHashMap< String , Double> wOne = new LinkedHashMap<>();
					for ( String imageName : rois.keySet() ) {
						wOne.put( imageName, 1.0 );
					}
					impProbability = roiDensityWeight( rois, wOne, sizeX, sizeY, norm, true, numberOfImages );
					probMap.put(roiName, impProbability);
					break;
				case LabelFusion.METHOD_PROBABILITY_LINEAR_DISTANCE :
					IJ.log(" LabelFusion.METHOD_PROBABILITY_LINEAR_DISTANCE is not functional yet!");
					break;
			}
		}
		return probMap;
	}

	/**
	 * TODO: number of ROIs is now fixed to size of rois, what if a roi fails (e.g. = null), we should work with an ArrayList (slower though)
	 * 
	 * @param rois
	 * @param sizeX
	 * @param sizeY
	 * @return 
	 */
	public static ArrayList< Double > roiVarianceWeight( ArrayList<Roi> rois, int sizeX, int sizeY ) {

		double[] x = new double[rois.size()];
		double[] y = new double[rois.size()];
		double[] d = new double[rois.size()];
		double averageSize = ( sizeX + sizeY ) / 2.0;
		ArrayList< Double > w = new ArrayList<>();

        for ( int i = 0; i < rois.size(); i++ ) {
            Roi roi = rois.get(i);
			ImageStatistics stats = roi.getStatistics();
			x[i] = stats.xCentroid;
			y[i] = stats.yCentroid;
        }
		double medx = median( x );
		double medy = median( y );

		for ( int i = 0; i < rois.size(); i++ ) {
			// xd[i] = Math.abs( x[i] - medx );
			// yd[i] = Math.abs( y[i] - medy );
			d[i] = Math.sqrt( Math.pow( x[i] - medx, 2.0 ) + Math.pow( y[i] - medy, 2.0 ) );
			w.add( 1.0 - ( d[i] / averageSize ) );
        }
		return w;
	}

	/**
	 * Compute the weights from the 
	 * 
	 * @param rois
	 * @param sizeX
	 * @param sizeY
	 * @return 

	public static LinkedHashMap< String, Double > roiVarianceWeight( LinkedHashMap<String, Roi> roiMap, int sizeX, int sizeY ) {

		LinkedHashMap< String, Double > w = new LinkedHashMap<>();
		double averageSize = ( sizeX + sizeY ) / 2.0;
		LinkedHashMap<String, Double> d = roiVarianceDistance( roiMap, sizeX, sizeY );
        for ( String key : roiMap.keySet() ) {
			w.put( key, 1.0 - ( d.get(key) / averageSize ) );
        }
		return w;
	}
	*/

	/**
	 * Map< Image, Roi > weight from the variance of the distance for each image
	 * 
	 * @param sdMap Map< Image, Roi >
	 * @param halfDist the distance in pixels where the weight will be half = 0.5
	 * @return Map< Image, Roi >
	 */
	public static LinkedHashMap< String, Double > imageRoisWeight( LinkedHashMap< String, Double > sdMap, double halfDist ) {

		LinkedHashMap< String, Double > wMap = new LinkedHashMap<>();
		for ( String imageName : sdMap.keySet() ) {
			double w =  Math.pow(  Math.max( 1 - sdMap.get( imageName ) / ( halfDist * 2.0 ), 0.0 ),  2  );
			//double w = Math.max(  1 - Math.pow( sdMap.get( imageName ) / ( halfDist * 2.0 )  , 2 ),  0.0  );
			wMap.put( imageName, w );
		}
		return wMap;
	}

	/**
	 * For each image get the variance from the median of the rois
	 * 
	 * @param roiMaps Map< Region, Map< Image, Roi > >
	 * @param sizeX width of the image
	 * @param sizeY height of the image
	 * @param normDist the distance in pixels for the normalization of the distance from the medians
	 * @return Map< image, Roi >
	 */
	public static LinkedHashMap< String, Double > imageRoisVariance( LinkedHashMap< String, LinkedHashMap< String, Roi> > roiMaps, int sizeX, int sizeY, double normDist) {

		LinkedHashMap< String, Double > ws = new LinkedHashMap<>();
		LinkedHashMap< String, LinkedHashMap< String, Double > > dMaps = new LinkedHashMap<>();
		//double averageSize = ( sizeX + sizeY ) / 2.0;

		for ( String roiName : roiMaps.keySet() ) {
			LinkedHashMap<String, Roi> roiMap = roiMaps.get(roiName);
			// Map< image, distance > Distance from median of the roi for each image
			LinkedHashMap<String, Double> d = roiVarianceDistance( roiMap, sizeX, sizeY );
			dMaps.put(roiName, d);
		}
		// Map< image, Map< region, distance > > Distance from median of the roi (inverted map)
		LinkedHashMap< String, LinkedHashMap< String, Double > > dMapsInverse = mapInvert( dMaps );
		// Map< image, distance > Variance of the distance from median of the rois of each image
		LinkedHashMap< String, Double > sdMap = new LinkedHashMap<>();

		for ( String imageName : dMapsInverse.keySet() ) {

			LinkedHashMap< String, Double > dMap = dMapsInverse.get(imageName);
			double d2 = 0.0;
			for ( String key : dMap.keySet() ) {
				d2 += Math.pow( dMap.get(key) / normDist , 2 );
			}
			double sd = Math.sqrt( d2 ) / dMap.size();
			sdMap.put(imageName, sd);
		}
		return sdMap;
	}

	/**
	 * TODO: number of ROIs is now fixed to size of rois, what if a roi fails (e.g. = null), we should work with an ArrayList (slower though)
	 * 
	 * @param rois
	 * @param sizeX
	 * @param sizeY
	 * @return 
	 */
	public static LinkedHashMap< String, Double > roiVarianceDistance( LinkedHashMap<String, Roi> roiMap, int sizeX, int sizeY ) {

		double[] x = new double[roiMap.size()];
		double[] y = new double[roiMap.size()];
		double[] d = new double[roiMap.size()];
		LinkedHashMap< String, Double > w = new LinkedHashMap<>();

		int i = 0;
        for ( String key : roiMap.keySet() ) {

			Roi roi = roiMap.get(key);
			ImageStatistics stats = roi.getStatistics();
			x[i] = stats.xCentroid;
			y[i] = stats.yCentroid;
			i++;
        }
		double medx = median( x );
		double medy = median( y );

		i = 0;
        for ( String key : roiMap.keySet() ) {

			d[i] = Math.sqrt( Math.pow( x[i] - medx, 2.0 ) + Math.pow( y[i] - medy, 2.0 ) );
			w.put( key, d[i] );
			i++;
        }
		return w;
	}

    /**
     * Interpolates multiple ROIs by taking a (sum) z-projection of the stack of ROIs
     * 
     * @param rois
     * @param sizeX
     * @param sizeY
     * @return 
     */
    public static ImagePlus roiDensity( ArrayList<Roi> rois, int sizeX, int sizeY ) {
        
        Roi roi = null;

        ImageProcessor mask = new FloatProcessor( sizeX, sizeY );
        for ( int i = 0; i < rois.size(); i++ ) {
            Roi roiCurrent = rois.get(i);
            mask.copyBits( roiCurrent.getMask().convertToFloat(), (int) Math.round(roiCurrent.getXBase()), (int) Math.round(roiCurrent.getYBase()), Blitter.ADD );
        }

        ImageStatistics stats = ImageStatistics.getStatistics( mask, ImageStatistics.MIN_MAX, new Calibration() );

        mask.multiply( 1.0 / stats.max );
        ImagePlus impSum = new ImagePlus("sum of roi-masks", mask );
        //impSum.show();

        return impSum;
    }

		
    public static ImagePlus roiDensityWeight( ArrayList<Roi> rois, ArrayList<Double> ws, int sizeX, int sizeY ) {
        
        Roi roi = null;
        
        ImageProcessor mask = new FloatProcessor( sizeX, sizeY );
        for ( int i = 0; i < rois.size(); i++ ) {
            Roi roiCurrent = rois.get(i);
            double w = ws.get(i);
            ImageProcessor ip = roiCurrent.getMask().convertToFloat();
            ip.multiply(w);
            mask.copyBits( ip, (int) Math.round(roiCurrent.getXBase()), (int) Math.round(roiCurrent.getYBase()), Blitter.ADD );
        }

        ImageStatistics stats = ImageStatistics.getStatistics( mask, ImageStatistics.MIN_MAX, new Calibration() );
        mask.multiply( 1.0 / stats.max );
        ImagePlus impSum = new ImagePlus("sum of roi-masks", mask );
        //impSum.show();
        
        return impSum;
    }

    public static ImagePlus roiDensityWeight( LinkedHashMap< String, Roi > roiMap, LinkedHashMap< String, Double > wMap, int sizeX, int sizeY ) {
        
        Roi roi = null;

        ImageProcessor mask = new FloatProcessor( sizeX, sizeY );
        for ( String key : roiMap.keySet() ) {
            Roi roiCurrent = roiMap.get(key);
            double w = wMap.get(key);
            ImageProcessor ip = roiCurrent.getMask().convertToFloat();
            ip.multiply(w);
            mask.copyBits( ip, (int) Math.round(roiCurrent.getXBase()), (int) Math.round(roiCurrent.getYBase()), Blitter.ADD );
        }

        ImageStatistics stats = ImageStatistics.getStatistics( mask, ImageStatistics.MIN_MAX, new Calibration() );
        mask.multiply( 1.0 / stats.max );
        ImagePlus impSum = new ImagePlus("sum of roi-masks", mask );
        //impSum.show();
        
        return impSum;
    }

	/**
	 * 
	 * 
	 * @param roiMap
	 * @param wMap
	 * @param sizeX
	 * @param sizeY
	 * @param norm 
	 * @return 
	 */
	public static ImagePlus roiDensityWeight( LinkedHashMap< String, Roi > roiMap, LinkedHashMap< String, Double > wMap, int sizeX, int sizeY, double norm, boolean emptyRoisCount, int numberOfImages ) {
        
        Roi roi = null;

        ImageProcessor mask = new FloatProcessor( sizeX, sizeY );
        double wtot = 0.0;
        for ( String key : roiMap.keySet() ) {
            wtot += wMap.get(key);
		}
		if ( emptyRoisCount ) {
			wtot += numberOfImages - roiMap.size();
		}
        for ( String key : roiMap.keySet() ) {
            Roi roiCurrent = roiMap.get(key);
            double w = wMap.get(key);
			ImageProcessor ip = roiCurrent.getMask().convertToFloat();
			ImageStatistics stats = ImageStatistics.getStatistics( ip, ImageStatistics.MIN_MAX, new Calibration() );
			ip.multiply( 1.0 / stats.max );
			//IJ.log( "key " + key + " 1/stats.max = " + Double.toString( 1.0 / stats.max) );
			//IJ.log( "key " + key + " w = " + Double.toString( w ) );
			ip.multiply( w );
			mask.copyBits( ip, (int) Math.round( roiCurrent.getXBase()), (int) Math.round(roiCurrent.getYBase()), Blitter.ADD );
        }
		
		//IJ.log( "numberOfImages = " + Double.toString( (double) numberOfImages) );
		//IJ.log( "roiMap.size = " + Double.toString( (double) roiMap.size()) );
		//IJ.log( "numberOfImages - wMap.size = " + Double.toString( (double) numberOfImages - wMap.size() ) );
		//IJ.log( "w_tot = " + Double.toString( wtot ) );
		mask.multiply( 1.0 / wtot );

		//IJ.log( "Norm = " + Double.toString( norm ) );
        if (norm > 0.0) {
			mask.multiply( 1.0 / norm );
		} else {
			ImageStatistics stats = ImageStatistics.getStatistics( mask, ImageStatistics.MIN_MAX, new Calibration() );
			mask.multiply( 1.0 / stats.max );
		}
        ImagePlus impSum = new ImagePlus("sum of roi-masks", mask );
        //impSum.show();
        
        return impSum;
    }

	
	/**
     * Interpolates multiple ROIs by taking a (sum) z-projection of the stack of ROIs
     * 
     * @param rois
     * @param sizeX
     * @param sizeY
     * @return 
     */
    public static ImagePlus maskDensityLinearInterpolation( ImagePlus density ) {
        
        ImageProcessor ip = density.getProcessor();
        
        ImageStatistics stats = ImageStatistics.getStatistics( ip, ImageStatistics.MIN_MAX, new Calibration() );
        
        return density;
    }


	public static LinkedHashMap< String, ImagePlus > getProbabilityMap( int sizeX, int sizeY, LinkedHashMap< String, ArrayList< Roi > > roiListMap, boolean weighted ) {

		LinkedHashMap<String, ImagePlus> probMap = new LinkedHashMap<String, ImagePlus>();
		for (String roiName : roiListMap.keySet()) {
			ArrayList< Roi> rois = roiListMap.get(roiName);
			ImagePlus impProbability;
			if ( weighted ) {
				ArrayList< Double > w = roiVarianceWeight( rois, sizeX, sizeY );
				impProbability = roiDensityWeight(rois, w, sizeX, sizeY);
				probMap.put(roiName, impProbability);
			} else {
				impProbability = roiDensity(rois, sizeX, sizeY);
				probMap.put(roiName, impProbability);
			}
		}
		return probMap;
	}


	/**
	 *
	 * @param sizeX
	 * @param sizeY
	 * @param roiListMap
	 * @return
	public static LinkedHashMap< String, ImagePlus > getProbabilityMapAlternative( int sizeX, int sizeY, LinkedHashMap< String, LinkedHashMap< String, Roi > > roiListMap ) {

		LinkedHashMap<String, ImagePlus> probMap = new LinkedHashMap<String, ImagePlus>();
		LinkedHashMap<String, LinkedHashMap< String, Roi > > ww = new LinkedHashMap<>();
		
		for (String roiName : roiListMap.keySet()) {

			LinkedHashMap< String, Roi > roiMap = roiListMap.get(roiName);

			LinkedHashMap< String, Double > w = roiVarianceWeight( roiMap, sizeX, sizeY );
			//ww.put( roiName, w );
			
			ArrayList< Roi> rois = new ArrayList<>();
			for ( String key : roiMap.keySet() ) {
				rois.add(roiMap.get(key));
			}
		}

		for (String roiName : roiListMap.keySet()) {
			LinkedHashMap< String, Roi > roiMap = roiListMap.get(roiName);
			//ImagePlus impProbability = roiDensityWeight( roiMap, w, sizeX, sizeY);
			//probMap.put(roiName, impProbability);
		}
		return probMap;
	}
	 */
	
	/**
	 * 
	 * @param probMap
	 * @param fusionMethod
	 * @param useIsolines
	 * @return 
	 */
	public static LinkedHashMap<String, Roi > getInterpolationMap( LinkedHashMap< String, ImagePlus > probMap, String fusionMethod, boolean useIsolines ) {
		LinkedHashMap<String, Roi> roiInterpolationMap = new LinkedHashMap<String, Roi>();
		
		switch ( fusionMethod ) {
			case METHOD_LABELFUSION_THRESHOLD :
				for (String roiName : probMap.keySet()) {
					
					ImagePlus prob = probMap.get(roiName);
					//prob.setTitle("useIsolines = false");
					//prob.duplicate().show();

					if ( useIsolines ) {
						ArrayList< HeightRoi > isoRois = isolineFromProbabilityImage( prob );
						prob.deleteRoi();
						ImagePlus empty = prob.duplicate();
						empty.getProcessor().multiply(0);
						empty.setProcessor( empty.getProcessor().convertToByte(false) );
						if ( isoRois.size() > 0 ) {
							prob = isolineMap( empty, isoRois);
						} else {
							prob = empty.duplicate();
						}
						//prob.setTitle("useIsolines = true");
						//prob.duplicate().show();
					}
					ImagePlus mask = maskFromThreshold( prob, 0.5 );
					ImageStatistics stats = ImageStatistics.getStatistics( mask.getProcessor(), ImageStatistics.MIN_MAX, new Calibration() );
					if ( stats.max > 0.0 ) {
						Roi roi = roiFromMask(mask);
						roiInterpolationMap.put(roiName, roi);
					} else {
					}
				}
				break;
			case METHOD_LABELFUSION_MAJORITY :
				ImagePlus first = probMap.get( probMap.keySet().iterator().next() );
				ImagePlus imp = IJ.createImage( "stack probmap", first.getWidth(), first.getHeight(), probMap.size(), first.getBitDepth() );
				int i = 0;
				BiMap< String, Integer > idMap = new BiMap<>();
				for (String roiName : probMap.keySet()) {
					i++;
					idMap.put( roiName, i );
					imp.getStack().setProcessor( probMap.get(roiName).getProcessor().duplicate(), i );
				}
				// TODO remove
				//imp.duplicate().show();
				IndexProjectionZ indexProjectionZ = new IndexProjectionZ( imp );
				indexProjectionZ.doProjection();
				ImagePlus hMasks = indexProjectionZ.getHMasks();
				// TODO remove
				//hMasks.duplicate().show();
				for (String roiName : probMap.keySet()) {
					ImageProcessor ip = hMasks.getStack().getProcessor( idMap.get(roiName) );
					ImagePlus mask = new ImagePlus( "mask", ip );
					ImagePlus tmask = maskFromThreshold( probMap.get(roiName), 0.5 );
					mask.setProcessor( maskIntersection( mask.getProcessor(), tmask.getProcessor() ) );
					//mask.duplicate().show();
					Roi roi = roiFromMask( mask );
					roiInterpolationMap.put(roiName, roi);
				}
				break;
		}
		return roiInterpolationMap;
	}

	public static LinkedHashMap<String, Roi > getInterpolationMap( LinkedHashMap< String, ImagePlus > probMap, boolean useIsolines ) {

		LinkedHashMap<String, Roi> roiInterpolationMap = new LinkedHashMap<String, Roi>();

		for (String roiName : probMap.keySet()) {

			ImagePlus prob = probMap.get(roiName);
			prob.setTitle("useIsolines = false");
			//prob.duplicate().show();

			if ( useIsolines ) {
				ArrayList<HeightRoi> isoRois = isolineFromProbabilityImage( prob );
				ImagePlus empty = prob.duplicate();
				empty.getProcessor().multiply(0);
				prob = isolineMap( empty, isoRois);
				prob.setTitle("useIsolines = true");
				//prob.duplicate().show();
			}
			ImagePlus mask = maskFromThreshold( prob, 0.5 );
			Roi roi = roiFromMask(mask);
			roiInterpolationMap.put(roiName, roi);
		}
		return roiInterpolationMap;
	}

}
