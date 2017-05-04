package be.ua.mbarbier.external;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import mpicbg.ij.FeatureTransform;
import mpicbg.ij.SIFT;
import mpicbg.ij.util.Util;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.models.AbstractModel;
import mpicbg.models.AffineModel2D;
import mpicbg.models.HomographyModel2D;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.RigidModel2D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.TranslationModel2D;

/**
 * Extract landmark correspondences in two images as PointRoi.
 * 
 * The plugin uses the Scale Invariant Feature Transform (SIFT) by David Lowe
 * \cite{Lowe04} and the Random Sample Consensus (RANSAC) by Fishler and Bolles
 * \citet{FischlerB81} with respect to a transformation model to identify
 * landmark correspondences.
 * 
 * BibTeX:
 * <pre>
 * &#64;article{Lowe04,
 *   author    = {David G. Lowe},
 *   title     = {Distinctive Image Features from Scale-Invariant Keypoints},
 *   journal   = {International Journal of Computer Vision},
 *   year      = {2004},
 *   volume    = {60},
 *   number    = {2},
 *   pages     = {91--110},
 * }
 * &#64;article{FischlerB81,
 *	 author    = {Martin A. Fischler and Robert C. Bolles},
 *   title     = {Random sample consensus: a paradigm for model fitting with applications to image analysis and automated cartography},
 *   journal   = {Communications of the ACM},
 *   volume    = {24},
 *   number    = {6},
 *   year      = {1981},
 *   pages     = {381--395},
 *   publisher = {ACM Press},
 *   address   = {New York, NY, USA},
 *   issn      = {0001-0782},
 *   doi       = {http://doi.acm.org/10.1145/358669.358692},
 * }
 * </pre>
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.4b
 */
public class SIFT_ExtractPointRoi implements PlugIn
{
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
	
	private ImagePlus imp1;
	private ImagePlus imp2;
	
	final private List< Feature > fs1 = new ArrayList< Feature >();
	final private List< Feature > fs2 = new ArrayList< Feature >();;
	
	static private class Param
	{	
		final public FloatArray2DSIFT.Param sift = new FloatArray2DSIFT.Param();
		
		/**
		 * Closest/next closest neighbour distance ratio
		 */
		public float rod = 0.92f;
		
		public boolean useGeometricConsensusFilter = true;
		
		/**
		 * Maximal allowed alignment error in px
		 */
		public float maxEpsilon = 25.0f;
		
		/**
		 * Inlier/candidates ratio
		 */
		public float minInlierRatio = 0.05f;
		
		/**
		 * Minimal absolute number of inliers
		 */
		public int minNumInliers = 7;
		
		/**
		 * Implemeted transformation models for choice
		 */
		final static public String[] modelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine", "Perspective" };
		public int modelIndex = 1;
	}
	
	final static private Param p = new Param();
	
	public SIFT_ExtractPointRoi()
	{
		decimalFormatSymbols.setGroupingSeparator( ',' );
		decimalFormatSymbols.setDecimalSeparator( '.' );
		decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
		decimalFormat.setMaximumFractionDigits( 3 );
		decimalFormat.setMinimumFractionDigits( 3 );		
	}
	
	@Override
	public void run( final String args )
	{
		// cleanup
		fs1.clear();
		fs2.clear();
		
		if ( IJ.versionLessThan( "1.40" ) ) return;
		
		final int[] ids = WindowManager.getIDList();
		if ( ids == null || ids.length < 2 )
		{
			IJ.showMessage( "You should have at least two images open." );
			return;
		}
		
		final String[] titles = new String[ ids.length ];
		for ( int i = 0; i < ids.length; ++i )
		{
			titles[ i ] = ( WindowManager.getImage( ids[ i ] ) ).getTitle();
		}
		
		final GenericDialog gd = new GenericDialog( "Extract SIFT Landmark Correspondences" );
		
		gd.addMessage( "Image Selection:" );
		final String current = WindowManager.getCurrentImage().getTitle();
		gd.addChoice( "source_image", titles, current );
		gd.addChoice( "target_image", titles, current.equals( titles[ 0 ] ) ? titles[ 1 ] : titles[ 0 ] );
		
		gd.addMessage( "Scale Invariant Interest Point Detector:" );
		gd.addNumericField( "initial_gaussian_blur :", p.sift.initialSigma, 2, 6, "px" );
		gd.addNumericField( "steps_per_scale_octave :", p.sift.steps, 0 );
		gd.addNumericField( "minimum_image_size :", p.sift.minOctaveSize, 0, 6, "px" );
		gd.addNumericField( "maximum_image_size :", p.sift.maxOctaveSize, 0, 6, "px" );
		
		gd.addMessage( "Feature Descriptor:" );
		gd.addNumericField( "feature_descriptor_size :", p.sift.fdSize, 0 );
		gd.addNumericField( "feature_descriptor_orientation_bins :", p.sift.fdBins, 0 );
		gd.addNumericField( "closest/next_closest_ratio :", p.rod, 2 );
		
		gd.addMessage( "Geometric Consensus Filter:" );
		gd.addCheckbox( "filter matches by geometric consensus", p.useGeometricConsensusFilter );
		gd.addNumericField( "maximal_alignment_error :", p.maxEpsilon, 2, 6, "px" );
		gd.addNumericField( "minimal_inlier_ratio :", p.minInlierRatio, 2 );
		gd.addNumericField( "minimal_number_of_inliers :", p.minNumInliers, 0 );
		gd.addChoice( "expected_transformation :", Param.modelStrings, Param.modelStrings[ p.modelIndex ] );
		
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		imp1 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		imp2 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		
		p.sift.initialSigma = ( float )gd.getNextNumber();
		p.sift.steps = ( int )gd.getNextNumber();
		p.sift.minOctaveSize = ( int )gd.getNextNumber();
		p.sift.maxOctaveSize = ( int )gd.getNextNumber();
		
		p.sift.fdSize = ( int )gd.getNextNumber();
		p.sift.fdBins = ( int )gd.getNextNumber();
		p.rod = ( float )gd.getNextNumber();
		
		p.useGeometricConsensusFilter = gd.getNextBoolean();
		p.maxEpsilon = ( float )gd.getNextNumber();
		p.minInlierRatio = ( float )gd.getNextNumber();
		p.minNumInliers = ( int )gd.getNextNumber();
		p.modelIndex = gd.getNextChoiceIndex();

		exec(imp1, imp2);
	}

	/** If unsure, just use default parameters by using exec(ImagePlus, ImagePlus, int) method, where only the model is specified. */
	public void exec(final ImagePlus imp1, final ImagePlus imp2,
			 final float initialSigma, final int steps,
			 final int minOctaveSize, final int maxOctaveSize,
			 final int fdSize, final int fdBins,
			 final float rod, final float maxEpsilon,
			 final float minInlierRatio, final int modelIndex) {

		p.sift.initialSigma = initialSigma;
		p.sift.steps = steps;
		p.sift.minOctaveSize = minOctaveSize;
		p.sift.maxOctaveSize = maxOctaveSize;

		p.sift.fdSize = fdSize;
		p.sift.fdBins = fdBins;
		p.rod = rod;

		p.useGeometricConsensusFilter = true;
		p.maxEpsilon = maxEpsilon;
		p.minInlierRatio = minInlierRatio;
		p.minNumInliers = 7;
		p.modelIndex = modelIndex;

		exec( imp1, imp2 );
	}

	/** Execute with default parameters, except the model.
	 *  @param modelIndex: 0=Translation, 1=Rigid, 2=Similarity, 3=Affine */
	public void exec(final ImagePlus imp1, final ImagePlus imp2, final int modelIndex) {
		if ( modelIndex < 0 || modelIndex > 3 ) {
			IJ.log("Invalid model index: " + modelIndex);
			return;
		}
		p.modelIndex = modelIndex;
		exec( imp1, imp2 );
	}

	/** Execute with default parameters (model is Rigid) */
	public void exec(final ImagePlus imp1, final ImagePlus imp2) {

		final FloatArray2DSIFT sift = new FloatArray2DSIFT( p.sift );
		final SIFT ijSIFT = new SIFT( sift );
		
		long start_time = System.currentTimeMillis();
		IJ.log( "Processing SIFT ..." );
		ijSIFT.extractFeatures( imp1.getProcessor(), fs1 );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
		IJ.log( fs1.size() + " features extracted." );
		
		start_time = System.currentTimeMillis();
		IJ.log( "Processing SIFT ..." );
		ijSIFT.extractFeatures( imp2.getProcessor(), fs2 );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
		IJ.log( fs2.size() + " features extracted." );
		
		start_time = System.currentTimeMillis();
		IJ.log( "Identifying correspondence candidates using brute force ..." );
		final List< PointMatch > candidates = new ArrayList< PointMatch >();
		FeatureTransform.matchFeatures( fs1, fs2, candidates, p.rod );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );	
		
		final ArrayList< Point > p1 = new ArrayList< Point >();
		final ArrayList< Point > p2 = new ArrayList< Point >();
		final List< PointMatch > inliers;
		
		if ( p.useGeometricConsensusFilter )
		{
			IJ.log( candidates.size() + " potentially corresponding features identified." );
			
			start_time = System.currentTimeMillis();
			IJ.log( "Filtering correspondence candidates by geometric consensus ..." );
			inliers = new ArrayList< PointMatch >();
			
			AbstractModel< ? > model;
			switch ( p.modelIndex )
			{
			case 0:
				model = new TranslationModel2D();
				break;
			case 1:
				model = new RigidModel2D();
				break;
			case 2:
				model = new SimilarityModel2D();
				break;
			case 3:
				model = new AffineModel2D();
				break;
			case 4:
				model = new HomographyModel2D();
				break;
			default:
				return;
			}
			
			boolean modelFound;
			try
			{
				modelFound = model.filterRansac(
						candidates,
						inliers,
						1000,
						p.maxEpsilon,
						p.minInlierRatio,
						p.minNumInliers );
			}
			catch ( final NotEnoughDataPointsException e )
			{
				modelFound = false;
			}
				
			IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
			
			if ( modelFound )
			{
				PointMatch.apply( inliers, model );
				
				IJ.log( inliers.size() + " corresponding features with an average displacement of " + decimalFormat.format( PointMatch.meanDistance( inliers ) ) + "px identified." );
				IJ.log( "Estimated transformation model: " + model );
			}
			else
				IJ.log( "No correspondences found." );
		}
		else
		{
			inliers = candidates;
			IJ.log( candidates.size() + " corresponding features identified." );
		}
		
		if ( inliers.size() > 0 )
		{
			PointMatch.sourcePoints( inliers, p1 );
			PointMatch.targetPoints( inliers, p2 );
			imp1.setRoi( Util.pointsToPointRoi( p1 ) );
			imp2.setRoi( Util.pointsToPointRoi( p2 ) );
		}
	}
}
