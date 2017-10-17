/*
 * The MIT License
 *
 * Copyright 2017 University of Antwerp.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package main.be.ua.mbarbier.slicemap;

import main.be.ua.mbarbier.slicemap.lib.BiMap;

/**
 *
 * @author mbarbier
 */
public class SingleCellAnalysisRegions {
	
	public boolean DEBUG = false;
	public static final String ANALYSIS_METHOD_INTENSITY = "intensity";
	public static final String[] METHODS = new String[]{ ANALYSIS_METHOD_INTENSITY };
	public static final String THRESHOLD_METHOD_NONE = "NONE";
	
	public static class AnalysisParameter {
		
		double pixelSize = 1.0;
		String thresholdMethod = "Otsu";
		double minSize = 0.0;
		double maxSize = 0.0;
		double threshold = 0.0;
		double dilateSize = 0.0;
		int channelThreshold = 1;
		int[] channelIntensity = {1};
		double saturationPercentage = 0.05;
		BiMap< String, Integer > channelsMap;

		public AnalysisParameter( BiMap< String, Integer > channelsMap, double pixelSize, String thresholdMethod, double minSize, double maxSize, double threshold, double dilateSize, int channelThreshold, int[] channelIntensity, double saturationPercentage ) {
			this.channelsMap = channelsMap;
			this.pixelSize = pixelSize;
			this.thresholdMethod = thresholdMethod;
			this.minSize = minSize;
			this.maxSize = maxSize;
			this.threshold = threshold;
			this.dilateSize = dilateSize;
			this.channelThreshold = channelThreshold;
			this.channelIntensity = channelIntensity;
			this.saturationPercentage = saturationPercentage;
		}

		public double getSaturationPercentage() {
			return saturationPercentage;
		}

		public double getPixelSize() {
			return pixelSize;
		}

		public String getThresholdMethod() {
			return thresholdMethod;
		}

		public double getMinSize() {
			return minSize;
		}

		public double getMaxSize() {
			return maxSize;
		}

		public double getThreshold() {
			return threshold;
		}

		public double getDilateSize() {
			return dilateSize;
		}

		public int getChannelThreshold() {
			return channelThreshold;
		}

		public int[] getChannelIntensity() {
			return channelIntensity;
		}
	}
	
	public SingleCellAnalysisRegions() {
		
	}

	
	
	public void segmentationNuclei( AnalysisParameter ) {
		
	}

	public void segmentationCytoPlasm( AnalysisParameter ) {
		
	}
	
	public void analysisCell( AnalysisParameter ) {
		
	}
	

	switch ( analysisMethod ) {

		case SingleCell_Analysis_Regions.ANALYSIS_METHOD_INTENSITY :
		break;


					if ( param.thresholdMethod != Analysis_Regions.THRESHOLD_METHOD_NONE ) {

						ImageProcessor ipChannelThreshold = imp.getStack().getProcessor( param.channelThreshold );
						ImagePlus impThreshold = new ImagePlus("image for threshold", ipChannelThreshold );
						impThreshold = normalizeIntensity( impThreshold, param.getSaturationPercentage(), 0.5 );
						impThreshold = new ImagePlus( impThreshold.getTitle(), impThreshold.getProcessor().convertToShort(true) );
						//impThreshold.show();

						Roi spotsRoi = extractSpotsRoi( impThreshold, param.getThresholdMethod() );
						ShapeRoi sroi = new ShapeRoi( spotsRoi );
						Roi[] rois = sroi.getRois();
						LinkedHashMap< String, String > spotsMap = new LinkedHashMap<>();
						Overlay overlay = new Overlay();
						overlay.add(spotsRoi);
						ImagePlus impShow = impThreshold.duplicate();
						impShow.setOverlay(overlay);
						impShow.deleteRoi();
						impShow.show();

						ArrayList< LinkedHashMap< String, String > > spotFeaturesAllList = new ArrayList<>();
						for (int channelIntensityIndex : param.channelIntensity) {
							ImageProcessor ipChannel = imp.getStack().getProcessor( channelIntensityIndex );
							ImagePlus impChannel = new ImagePlus( imp.getStack().getSliceLabel( channelIntensityIndex ), ipChannel );

							for (String roiName : roiMap.keySet()) {

								Roi roi = roiMap.get(roiName);
								// spot detection (features)
								ArrayList< LinkedHashMap< String, String > > spotFeaturesList = new ArrayList<>();
								spotFeaturesList = spotsExtraction( impChannel, new ShapeRoi(spotsRoi), roi, minSpotAreaPixels );
								for ( LinkedHashMap< String, String > spotFeatures : spotFeaturesList ) {
									//spotFeatures.put("channel_id", channelIndexString );
									spotFeatures.put("image_id", imageName);
									spotFeatures.put("region_id", roiName);
									spotFeatures.put("channel_id", channelsMap.getKey( channelIntensityIndex ) );
								}
								spotFeaturesAllList.addAll( spotFeaturesList );
							}

						}
						writeCsv( spotFeaturesAllList, ",", new File( outputFolder.getAbsolutePath() + "/" + "spotFeatures_" + imageName + ".csv").getAbsolutePath());

					}
					break;
				}

		}

	
}
