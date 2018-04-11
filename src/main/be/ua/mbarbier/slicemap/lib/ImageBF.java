/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib;

import ij.IJ;
import ij.ImagePlus;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.formats.FormatException;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import loci.common.Region;
import loci.common.services.DependencyException;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import static main.be.ua.mbarbier.slicemap.lib.Lib.getFileExtension;

public class ImageBF {

	
	public static ImagePlus openGeneralImage( File imageFile, int seriesIndex, int channelIndex ) {
		
		ImagePlus imp = null;
		String format = getFileExtension( imageFile );
        if ( "czi".equals(format) )  {
			try {
				imp = openSeries( imageFile.getAbsolutePath(), seriesIndex, channelIndex );
				imp.show();
			} catch (IOException | FormatException ex) {
				Logger.getLogger(ImageBF.class.getName()).log(Level.SEVERE, null, ex);
			}
        } else {
			imp = IJ.openImage( imageFile.getAbsolutePath() );
		}
		return imp;
	}
	

	
	public static ImagePlus openGeneralImage( File imageFile, int seriesIndex ) {
		
		ImagePlus imp = null;
		String format = getFileExtension( imageFile );
        if ( "czi".equals(format) )  {
			try {
				imp = openSeries( imageFile.getAbsolutePath(), seriesIndex );
				imp.show();
			} catch (IOException | FormatException ex) {
				Logger.getLogger(ImageBF.class.getName()).log(Level.SEVERE, null, ex);
			}
        } else {
			imp = IJ.openImage( imageFile.getAbsolutePath() );
		}
		return imp;
	}
	
	/**
	 * Get image width and height
	 * 
	 * @param id the filepath of the image
	 * @param binning
	 * @return 
	 * @throws loci.common.services.DependencyException
	 * @throws loci.formats.FormatException
	 * @throws java.io.IOException
	 */
	public static int[] getSeriesXYbitDepth( String id, int binning ) throws DependencyException, FormatException, IOException {

		ImageReader reader = new ImageReader();
		reader.setId( id );
		int sizeX = (int) ( (float)( reader.getSizeX() ) / ((float)(binning)) );
		int sizeY = (int) ( (float)( reader.getSizeY() ) / ((float)(binning)) );
		int bitDepth = reader.getBitsPerPixel();
		
		return new int[]{ sizeX, sizeY, bitDepth };
	}


	/**
	 * Obtains some simple metadata information from the image series
	 * 
	 * @param id the filepath of the image
	 * @param seriesIndex the index of the series
	 * @return 
	 * @throws loci.common.services.DependencyException
	 * @throws loci.formats.FormatException
	 * @throws java.io.IOException
	 */
	public static Meta getSeriesMetadata( String id, int seriesIndex ) throws DependencyException, FormatException, IOException {

		// create OME-XML metadata store
		IMetadata meta = MetadataTools.createOMEXMLMetadata();
		// create format reader
		ImageReader reader = new ImageReader();
		reader.setMetadataStore( meta );
		reader.setId( id );

		// output dimensional information
		int sizeX = reader.getSizeX();
		int sizeY = reader.getSizeY();
		int sizeZ = reader.getSizeZ();
		int sizeC = reader.getSizeC();
		int sizeT = reader.getSizeT();
		int nSeries = reader.getImageCount();
		double pixelSizeX = meta.getPixelsPhysicalSizeX( seriesIndex ).value().doubleValue();
		double pixelSizeY = meta.getPixelsPhysicalSizeY( seriesIndex ).value().doubleValue();
		
		return new Meta( sizeX, sizeY, sizeC, nSeries, pixelSizeX, pixelSizeY );
	}

	/**
	 * Obtains some simple metadata information from the image series
	 * 
	 * @param id the filepath of the image
	 * @param seriesIndex the index of the series
	 * @return
	 */
	public static Meta getSeriesMetadataNoEx( String id, int seriesIndex ) {
		try {
			Meta meta = getSeriesMetadata( id, seriesIndex );
			return meta;
		} catch (Exception ex) {
			Logger.getLogger(ImageBF.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}
	
	/**
	 * Open a single series of an image using bioformats
	 * 
	 * @param filePath
	 * @param seriesIndex
	 * @return 
	 * @throws java.io.IOException 
	 * @throws loci.formats.FormatException 
	 */
	public static ImagePlus openSeries( String filePath, int seriesIndex ) throws IOException, FormatException {

		ImagePlus imp = null;
		
		ImporterOptions options = new ImporterOptions();
		// Unselect all series
		options.clearSeries();
		// Select the series we want to get: 0 = original, 1 = 2-binned, 2 = 4-binned, 3 = 8-binned
		options.setSeriesOn( seriesIndex, true );
		options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
		options.setId( filePath );
		ImagePlus[] imps = BF.openImagePlus( options );
		imp = imps[0];
		
		return imp;
	}

	/**
	 * Open a selected region of a single series of an image using bioformats
	 *
	 * @param filePath
	 * @param seriesIndex
	 * @param channelIndex
	 * @param tileRegion as a loci.commnon.Region region
	 * @return
	 * @throws java.io.IOException
	 * @throws loci.formats.FormatException
	 */
	public static ImagePlus openSeries(String filePath, int seriesIndex, int channelIndex, loci.common.Region tileRegion) throws IOException, FormatException {

		ImagePlus imp = null;

		ImporterOptions options = new ImporterOptions();
		
		// Select the series we want to get: 0 = original, 1 = 2-binned, 2 = 4-binned, 3 = 8-binned
		options.clearSeries();
		options.setSeriesOn(seriesIndex, true);

		// Selection of the channel (channels are zero-based ? )
		options.setCBegin( seriesIndex, channelIndex-1 );
		options.setCEnd( seriesIndex, channelIndex-1 );

		// Selection of the region
		options.setCrop(true);
		options.setCropRegion(seriesIndex, tileRegion);

		options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
		options.setId(filePath);
		ImagePlus[] imps = BF.openImagePlus(options);
		imp = imps[0];

		return imp;
	}

	/**
	 * Open a selected region of a single series of an image using bioformats
	 *
	 * @param filePath
	 * @param seriesIndex
	 * @param channelIndex
	 * @return
	 * @throws java.io.IOException
	 * @throws loci.formats.FormatException
	 */
	public static ImagePlus openSeries(String filePath, int seriesIndex, int channelIndex ) throws IOException, FormatException {

		ImagePlus imp = null;

		ImporterOptions options = new ImporterOptions();
		
		// Select the series we want to get: 0 = original, 1 = 2-binned, 2 = 4-binned, 3 = 8-binned
		options.clearSeries();
		options.setSeriesOn(seriesIndex, true);

		// Selection of the channel (channels are zero-based ? )
		options.setCBegin( seriesIndex, channelIndex-1 );
		options.setCEnd( seriesIndex, channelIndex-1 );

		options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
		options.setId(filePath);
		ImagePlus[] imps = BF.openImagePlus(options);
		imp = imps[0];

		return imp;
	}
	
	
	/**
	 * Open a selected region of a single series of an image using bioformats (region defined as java rectangle)
	 *
	 * @param filePath
	 * @param seriesIndex
	 * @param channelIndex
	 * @param tileRect as a java Rectangle
	 * @return
	 * @throws java.io.IOException
	 * @throws loci.formats.FormatException
	 */
	public static ImagePlus openSeries(String filePath, int seriesIndex, int channelIndex, java.awt.Rectangle tileRect) throws IOException, FormatException {

		Region tileRegion = new Region( tileRect.x, tileRect.y, tileRect.width, tileRect.height);
		ImagePlus imp = openSeries( filePath, seriesIndex, channelIndex, tileRegion );

		return imp;
	}
	
	/**
	 * Open a selected region of a single series of an image using bioformats (region defined as java rectangle), catch all exceptions and return null in that case
	 *
	 * @param filePath
	 * @param seriesIndex
	 * @param channelIndex
	 * @param tileRect as a java Rectangle
	 * @return
	 */
	public static ImagePlus openSeriesNoEx( String filePath, int seriesIndex, int channelIndex, java.awt.Rectangle tileRect) {

		try {

			Region tileRegion = new Region( tileRect.x, tileRect.y, tileRect.width, tileRect.height);
			ImagePlus imp = openSeries( filePath, seriesIndex, channelIndex, tileRegion );
			return imp;

		} catch (IOException | FormatException ex) {
			Logger.getLogger(ImageBF.class.getName()).log(Level.SEVERE, null, ex);
		}

		return null;
	}

	/**
	 * Open a single series channel of an image using bioformats, catch all exceptions and return null in that case
	 *
	 * @param filePath
	 * @param seriesIndex
	 * @param channelIndex
	 * @return
	 */
	public static ImagePlus openSeriesNoEx( String filePath, int seriesIndex, int channelIndex ) {

		try {

			ImagePlus imp = openSeries( filePath, seriesIndex, channelIndex );
			return imp;

		} catch (IOException | FormatException ex) {
			Logger.getLogger(ImageBF.class.getName()).log(Level.SEVERE, null, ex);
		}

		return null;
	}

}
