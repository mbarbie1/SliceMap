/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap;

import main.be.ua.mbarbier.slicemap.lib.LibIO;
import main.be.ua.mbarbier.slicemap.lib.congealing.Congealing;
import main.be.ua.mbarbier.slicemap.lib.image.LibImage;
import static main.be.ua.mbarbier.slicemap.lib.image.LibImage.subtractBackground;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.loadPointRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.loadRoiAlternative;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.roiColor;
import static main.be.ua.mbarbier.slicemap.lib.transform.TransformRoi.applyRoiScaleTransform;
import static main.be.ua.mbarbier.slicemap.lib.transform.TransformRoi.applyRoiScaleTransformAlternative;
import static main.be.ua.mbarbier.slicemap.lib.transform.TransformRoi.scaleRoi;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.Opener;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.lingala.zip4j.exception.ZipException;

/**
 *
 * @author mbarbier
 */
public class RefStack {

	Main param;
	File inputImageFolderFile;
	String inputImageFolder;
	File inputRoiFolderFile;
	File inputPointRoiFolderFile;
	String inputRoiFolder;
	File outputFolderFile;
	String outputFolder;
	File inputFolderFile;
	String inputFolder;
	String appFolder;
	File appFolderFile;
	String refStackPath;
	String refNameContains;
	String refNameDoesNotContain;
	String refNamePattern;
	String roiNamePattern;
	File stackPropsFile;
	int stackBinning;
	int congealingBinning;
	File stackFile;
	File logFile;
	//MBLog log;
	ArrayList< File > refList;
	LinkedHashMap< String, ImageProperties > stackProps;
	ImagePlus stack;
	int maxSizeX;
	int maxSizeY;
	int maxSize;
	
	String roiPattern_prefix = ".*";

	public void setRoiPattern_prefix( String roiPattern_prefix ) {
		this.roiPattern_prefix = roiPattern_prefix;
	}
	
	public int getMaxSizeX() {
		return maxSizeX;
	}

	public int getMaxSizeY() {
		return maxSizeY;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public ImagePlus getStack() {
		return this.stack;
	}

	public LinkedHashMap< String, ImageProperties > getStackProps() {
		return this.stackProps;
	}
	
	/**
	 * 
	 * @param refList
	 * @param inputRoiFolder
	 * @param inputFolder
	 * @param patternRefName should be a group pattern --> with brackets, e.g. "$(*.?)_" which would match everything between the start and an underscore
	 */
    public void initRefProperties( ArrayList<File> refList, String inputRoiFolder, String inputPointRoiFolder, String inputFolder, String patternRefName, int stackBinning, int congealingBinning ) {

        //this.log.log("Initiate reference image-properties and ROIs");
		this.stackProps = new LinkedHashMap< String, ImageProperties >();

		for (File ref : this.refList) {
			try {
				ImageProperties prop = new ImageProperties();
				ImagePlus impTmp = IJ.openImage( ref.getAbsolutePath() ); // Change into virtual stack opener (but include png format)
				Pattern pattern = Pattern.compile( patternRefName );
				Matcher matcher = pattern.matcher(ref.getName());
				String id = ref.getName();
				if (matcher.find())	{
				    id = matcher.group(1);
				}
				// Assume the roiFile should contain the slice id
				String roiPattern = this.roiPattern_prefix + id + ".*zip";
				File roiFile = LibIO.findSimilarFile( new File(inputRoiFolder), roiPattern );
				File pointRoiFile = LibIO.findSimilarFile( new File(inputPointRoiFolder), roiPattern );
				LinkedHashMap<String, Roi> roiMapOri;
				LinkedHashMap<String, Roi> roiMap;
				LinkedHashMap<String, Roi> pointRoiOri;
				LinkedHashMap<String, Roi> pointRoi = new LinkedHashMap<>();
				if ( roiFile != null ) {
					roiMapOri = loadRoiAlternative( roiFile );
					prop.id = id;
					prop.bitDepth = impTmp.getBitDepth();
					prop.binning = stackBinning;
					prop.binning_congealing = congealingBinning;
					prop.binning_total = stackBinning * congealingBinning;
					prop.width = impTmp.getWidth();
					prop.height = impTmp.getHeight();
					prop.stackWidth = this.maxSize;
					prop.stackHeight = this.maxSize;
					prop.xOffset = (int) Math.floor( ( prop.stackWidth - prop.width ) / 2.0 );
					prop.yOffset = (int) Math.floor( ( prop.stackHeight - prop.height ) / 2.0 );
					prop.roiMapOri = new LinkedHashMap<>();
					prop.roiMapOri.putAll( roiMapOri );
					prop.roiMapOriFile = new File( roiFile.getAbsolutePath() );
					prop.imageOriFile = new File( ref.getAbsolutePath() );
					prop.roiMap = new LinkedHashMap<>();
					prop.pointRoi = new LinkedHashMap<>();
					roiMap = applyRoiScaleTransformAlternative( roiMapOri, prop.xOffset, prop.yOffset, 1. / prop.binning );
					prop.roiMap.putAll( roiMap );

					if ( pointRoiFile != null ) {
						try {
							pointRoiOri = loadRoiAlternative( pointRoiFile );
							prop.pointRoiOri = new LinkedHashMap<>();
							prop.pointRoiOri.putAll( pointRoiOri );
							prop.pointRoiOriFile = pointRoiFile;
							pointRoi = applyRoiScaleTransformAlternative( pointRoiOri, prop.xOffset, prop.yOffset, 1./prop.binning );
							prop.pointRoi.putAll( pointRoi );
						} catch (Exception exp) {
							Logger.getLogger(RefStack.class.getName()).log(Level.SEVERE, null, exp);
						}
					} else {
						try {
							//prop.pointRoi
							// generate float[] x and y from mean values of the 
							
							for ( String key : roiMap.keySet() ) {
								Roi roi = prop.roiMap.get(key);
								ImageStatistics stats = roi.getStatistics();
								float roiMeanX = (float) stats.xCentroid;
								float roiMeanY = (float) stats.yCentroid;
								PointRoi roiMean = new PointRoi( new float[]{roiMeanX}, new float[]{roiMeanY}, 1 );
								pointRoi.put( key, roiMean );
							}
							prop.pointRoi.putAll( pointRoi );
						} catch( Exception e) {
							Logger.getLogger(RefStack.class.getName()).log(Level.SEVERE, null, e);
						}
					}
					this.stackProps.put( id, prop );
				}
			} catch (ZipException ex) {
				Logger.getLogger(RefStack.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(RefStack.class.getName()).log(Level.SEVERE, null, ex);
			} catch (Exception ex) {
				Logger.getLogger(RefStack.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

    }

	public void init( Main param ) {

		this.param = param;
		this.appFolder = param.APP_FOLDER.getAbsolutePath();
		this.appFolderFile = param.APP_FOLDER;
		this.inputFolder = param.INPUT_FOLDER.getAbsolutePath();
		this.inputFolderFile = param.INPUT_FOLDER;
		this.inputRoiFolderFile = new File( this.inputFolder + "/" + Main.CONSTANT_SUBDIR_ROI );
		this.inputPointRoiFolderFile = new File( this.inputFolder + "/" + Main.CONSTANT_SUBDIR_POINTROI );
		this.inputRoiFolder = this.inputRoiFolderFile.getAbsolutePath();
		this.inputImageFolderFile = new File( this.inputFolder + "/" + Main.CONSTANT_SUBDIR_MONTAGE );
		this.inputImageFolder = this.inputImageFolderFile.getAbsolutePath();
		this.outputFolder = param.OUTPUT_FOLDER.getAbsolutePath();
		this.refStackPath = param.FILE_REFERENCE_STACK.getAbsolutePath();
		this.stackFile = param.FILE_REFERENCE_STACK;
		this.stackBinning = param.CONGEALING_STACKBINNING;
		this.congealingBinning = param.CONGEALING_BINCONGEALING;
		this.stackPropsFile = param.FILE_STACKPROPS;
		this.roiNamePattern = param.PATTERN_ROI_FILES;
		this.refNamePattern = param.PATTERN_REF_FILES;
		this.refNameContains = param.CONTAINS_REF_FILES;
		this.refNameDoesNotContain = param.DOESNOTCONTAIN_REF_FILES;

		String logFileName = Main.CONSTANT_FILE_NAME_LOG;
		try {
			Files.createDirectories( new File( this.outputFolder).toPath() );
		} catch (IOException ex) {
			Logger.getLogger(RefStack.class.getName()).log(Level.SEVERE, null, ex);
		}
		File logFile = new File( this.outputFolder + "/" + logFileName );
		//this.log = new MBLog( logFile.getAbsolutePath() );
	}

	public void init( Main param, File inputImageFolderFile, File inputRoiFolderFile ) {

		this.param = param;
		this.appFolder = param.APP_FOLDER.getAbsolutePath();
		this.appFolderFile = param.APP_FOLDER;
		//this.inputFolder = param.INPUT_FOLDER.getAbsolutePath();
		//this.inputFolderFile = param.INPUT_FOLDER;
		this.inputRoiFolderFile = inputRoiFolderFile;
		this.inputPointRoiFolderFile = new File( inputRoiFolderFile.getAbsolutePath() + "/" + Main.CONSTANT_SUBDIR_POINTROI );
		this.inputRoiFolder = this.inputRoiFolderFile.getAbsolutePath();
		this.inputImageFolderFile = inputImageFolderFile;
		this.inputImageFolder = this.inputImageFolderFile.getAbsolutePath();
		this.outputFolder = param.OUTPUT_FOLDER.getAbsolutePath();
		this.refStackPath = param.FILE_REFERENCE_STACK.getAbsolutePath();
		this.stackFile = param.FILE_REFERENCE_STACK;
		this.stackBinning = param.CONGEALING_STACKBINNING;
		this.congealingBinning = param.CONGEALING_BINCONGEALING;
		this.stackPropsFile = param.FILE_STACKPROPS;
		this.roiNamePattern = param.PATTERN_ROI_FILES;
		this.refNamePattern = param.PATTERN_REF_FILES;
		this.refNameContains = param.CONTAINS_REF_FILES;
		this.refNameDoesNotContain = param.DOESNOTCONTAIN_REF_FILES;

		String logFileName = Main.CONSTANT_FILE_NAME_LOG;
		try {
			Files.createDirectories( new File( this.outputFolder).toPath() );
		} catch (IOException ex) {
			Logger.getLogger(RefStack.class.getName()).log(Level.SEVERE, null, ex);
		}
		File logFile = new File( this.outputFolder + "/" + logFileName );
		//this.log = new MBLog( logFile.getAbsolutePath() );
	}
	
	
	public void generateStack( double sigmaRatio, double saturatedPixelPercentage ) {

		int strokeWidth = 2;
		Set<String> keys = this.stackProps.keySet();
		ImageProperties firstProps = this.stackProps.get( keys.iterator().next() );
		int stackSizeX = firstProps.stackWidth;
		int stackSizeY = firstProps.stackHeight;
		int bitDepth = firstProps.bitDepth;
        int stackSizeXScaled = (int) Math.floor( stackSizeX / ((double) firstProps.binning) );
        int stackSizeYScaled = (int) Math.floor( stackSizeY / ((double) firstProps.binning) );
		this.stack = IJ.createHyperStack("Stack 32 bit", stackSizeXScaled, stackSizeYScaled, 1, keys.size(), 1, bitDepth);
		int sliceIndex = 0;
        //RoiManager rm = RoiManager.getRoiManager();
        //rm.reset();
		Overlay overlay = new Overlay();
        for ( String key : this.stackProps.keySet() ) {
			sliceIndex++;
			ImageProperties props = this.stackProps.get(key);
			props.index = sliceIndex;
            String sliceLabel = props.imageOriFile.getName();
			ImagePlus impOri = IJ.openImage( props.imageOriFile.getAbsolutePath() );
            ImageProcessor ipOri = impOri.getProcessor();
            double xOffset = (double) props.xOffset;
            double yOffset = (double) props.yOffset;
            int xOffsetScaled = (int) Math.floor( xOffset / ((double) props.binning) );
            int yOffsetScaled = (int) Math.floor( yOffset / ((double) props.binning) );
			
			GaussianBlur gb = new GaussianBlur();
			gb.blurGaussian( ipOri, maxSize * sigmaRatio );
			props.sigma_smooth = maxSize * sigmaRatio;

			ImagePlus imp = LibImage.binImage(impOri, props.binning);

			imp.setProcessor( subtractBackground(imp.getProcessor(), 5) );

			ContrastEnhancer ce = new ContrastEnhancer();
			ce.setNormalize(true);
			ce.stretchHistogram( imp, saturatedPixelPercentage );

			//ce.equalize(imp);
			ImageProcessor ipSlice = this.stack.getStack().getProcessor( sliceIndex );
			ipSlice.copyBits(imp.getProcessor(), xOffsetScaled, yOffsetScaled, Blitter.COPY);
			this.stack.getStack().setSliceLabel(sliceLabel, sliceIndex);
			this.stack.setPosition( sliceIndex );
			// Scale the roiMapOri to roiMap
			props.roiMap = applyRoiScaleTransformAlternative( props.roiMapOri, xOffset, yOffset, 1.0 / ((double) props.binning) );

			//props.roiMap
			for ( String roiName : props.roiMap.keySet() ) {

				try {// Verify whether ROI exists, if not just don't add it
					Roi roi = props.roiMap.get(roiName);
					roi.setName(roiName);
					roi.setPosition( sliceIndex );
					roi.setStrokeWidth( strokeWidth );
					if (roiName != null && roiName.startsWith("EVT_Regions_")) {
						roiName = key.substring(roiName.lastIndexOf("_") + 1, roiName.length());
					}
					Color color = roiColor().get(roiName);
					if (color != null) {
					} else if (roi.getStrokeColor() != null) {
						color = roi.getStrokeColor();
					} else {
						color = Color.GRAY;
					}
					roi.setStrokeColor(color);
					//rm.addRoi(roi);
					overlay.add( roi, roiName );
				} catch(Exception e) {
				}
			}
//			ImagePlus impSlice = new ImagePlus( "ipSlice", ipSlice );
//			impSlice.setOverlay( overlay );
//			impSlice.show();
		}
		this.stack.setOverlay( overlay );
		this.stack.setHideOverlay( false );
		//this.stack.show();
	}

	public void run() {

		// --- Find the reference in the input folder
		IJ.log( "Folder with references: " + this.inputImageFolderFile.getAbsolutePath() );
		this.refList = LibIO.findFiles( this.inputImageFolderFile, this.refNameContains, this.refNameDoesNotContain );

		// --- Find maximal size (virtual stack?)
		for (File ref : this.refList) {
			IJ.log( "Reference file: " + ref.getAbsolutePath() );
            String format = Opener.getFileFormat( ref.getName() );
            if ( format == "tiff" |  format == "tif" |  format == "TIFF" |  format == "TIF" )  {
				ImagePlus imp = IJ.openVirtual( ref.getAbsolutePath() ); // change into virtual stack opener (but include png format)
                this.maxSizeX = Math.max( this.maxSizeX, imp.getWidth() );
                this.maxSizeY = Math.max( this.maxSizeY, imp.getHeight() );
                this.maxSize = Math.max( this.maxSizeX, this.maxSizeY );
            } else {
				ImagePlus imp = IJ.openImage( ref.getAbsolutePath() );
                this.maxSizeX = Math.max( this.maxSizeX, imp.getWidth() );
                this.maxSizeY = Math.max( this.maxSizeY, imp.getHeight() );
                this.maxSize = Math.max( this.maxSizeX, this.maxSizeY );
            }
    	}
		initRefProperties( this.refList, this.inputRoiFolder, this.inputPointRoiFolderFile.getAbsolutePath(), this.inputImageFolder, this.refNamePattern, this.stackBinning, this.congealingBinning );

		// --- Smooth & downscale (smoothing dependent on binning?)
		// --- background correction
		// --- Histogram normalization
		double sigmaRatio = 0.005 * ( (double) this.stackBinning ) / 16.0;
		double saturatedRatio = 0.05;
		generateStack( sigmaRatio, saturatedRatio );

		// --- Save stack
		//this.log.log("Saving reference stack to " + this.stackFile.getAbsolutePath());
		IJ.saveAsTiff( this.stack, this.stackFile.getAbsolutePath() );
		//this.log.log("Saving reference stack properties to " + this.stackPropsFile.getAbsolutePath());
		Congealing.saveStackProps( this.stackPropsFile, this.stackProps );

	}
	
	/**
	 * Generate reference stack with fixed width and height
	 * 
	 * @param maxSizeX
	 * @param maxSizeY 
	 */
	public void run( int maxSizeX, int maxSizeY ) {

		// --- Find the reference in the input folder
		this.refList = LibIO.findFiles( this.inputImageFolderFile, this.refNameContains, this.refNameDoesNotContain );

		// --- Find maximal size (virtual stack?)
		this.maxSizeX = maxSizeX;
		this.maxSizeY = maxSizeY;
		this.maxSize = Math.max( this.maxSizeX, this.maxSizeY );
		initRefProperties( this.refList, this.inputRoiFolder, this.inputPointRoiFolderFile.getAbsolutePath(), this.inputImageFolder, this.refNamePattern, this.stackBinning, this.congealingBinning );

		// --- Smooth & downscale (smoothing dependent on binning?)
		// --- background correction
		// --- Histogram normalization
		double sigmaRatio = 0.005;
		double saturatedRatio = 0.05;
		generateStack( sigmaRatio, saturatedRatio );

		// --- Save stack
		//this.log.log("Saving reference stack to " + this.stackFile.getAbsolutePath());
		IJ.saveAsTiff( this.stack, this.stackFile.getAbsolutePath() );
		//this.log.log("Saving reference stack properties to " + this.stackPropsFile.getAbsolutePath());
		Congealing.saveStackProps( this.stackPropsFile, this.stackProps );

	}	
	/**
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {

		// set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = RefStack.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        new ImageJ();

		RefStack refStack = new RefStack();

		Main param = new Main();
		param.INPUT_FOLDER = new File("d:/p_prog_output/slicemap/input");
		param.OUTPUT_FOLDER = new File("d:/p_prog_output/slicemap/output_ref_stack");
		param.FILE_REFERENCE_STACK = new File( "Beerse21.tif" );
		param.PATTERN_REF_FILES = "^(.*?)_.*";
		param.CONTAINS_REF_FILES = "C3";
		param.DOESNOTCONTAIN_REF_FILES = ".png";
		param.CONGEALING_STACKBINNING = 8;
		refStack.init( param );

        IJ.log("START RUN plugin");
		refStack.run();
        IJ.log("END RUN plugin");

		//refStack.log.close();
	}

}
