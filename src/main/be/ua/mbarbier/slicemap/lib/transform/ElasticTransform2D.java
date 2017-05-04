/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib.transform;

import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.transformRoi;
import static main.be.ua.mbarbier.slicemap.lib.roi.LibRoi.transformRoiInverse;
import static main.be.ua.mbarbier.slicemap.lib.transform.Transform2D.applyRoiInverseTransform;
import static main.be.ua.mbarbier.slicemap.lib.transform.Transform2D.applyRoiTransform;
import bunwarpj.Transformation;
import ij.gui.Roi;
import java.util.LinkedHashMap;
import net.imglib2.realtransform.AffineTransform2D;

/**
 *
 * @author mbarbier
 */
public class ElasticTransform2D {
	
	public static LinkedHashMap<String,Roi> applyRoiRefTransform( LinkedHashMap<String,Roi> roiMap, AffineTransform2D pretvecRef, AffineTransform2D tvecRef, Transformation elasticTRef, AffineTransform2D pretvecSample, AffineTransform2D tvecSample, Transformation elasticTSample ) {

        LinkedHashMap<String,Roi> roiMapT = new LinkedHashMap<>();
        for (String key : roiMap.keySet() ) {
            Roi roi = roiMap.get(key);
            Roi transformedRoi = applyRoiTransform( pretvecRef, roi );
            transformedRoi = applyRoiTransform( tvecRef, transformedRoi );
			transformedRoi = transformRoiInverse( elasticTRef, transformedRoi); 
			
			//transformedRoi = transformRoi( elasticTSample, transformedRoi );
            transformedRoi = applyRoiInverseTransform( tvecSample, transformedRoi );
            transformedRoi = applyRoiInverseTransform( pretvecSample, transformedRoi );
            roiMapT.put(key, transformedRoi);
        }

        return roiMapT;
    }

	public static LinkedHashMap<String,Roi> applyRoiRefTransform( LinkedHashMap<String,Roi> roiMap, AffineTransform2D tvecRef, Transformation elasticTRef, AffineTransform2D tvecSample ) {

        LinkedHashMap<String,Roi> roiMapT = new LinkedHashMap<>();
        for (String key : roiMap.keySet() ) {
            Roi roi = roiMap.get(key);
            Roi transformedRoi = applyRoiTransform( tvecRef, roi );
			transformedRoi = transformRoiInverse( elasticTRef, transformedRoi); 
			
			//transformedRoi = transformRoi( elasticTSample, transformedRoi );
            transformedRoi = applyRoiInverseTransform( tvecSample, transformedRoi );
            roiMapT.put(key, transformedRoi);
        }

        return roiMapT;
    }

}
