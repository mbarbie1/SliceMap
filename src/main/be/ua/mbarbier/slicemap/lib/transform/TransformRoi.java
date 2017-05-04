/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib.transform;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.RoiEncoder;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import java.awt.Shape;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author mbarbier
 */
public class TransformRoi {
    
    static int INDEX_TRANSLATION_X = 0;
    static int INDEX_TRANSLATION_Y = 1;
    static int INDEX_ROTATION = 2;
    static int INDEX_MIRRORY = 3;
    static int INDEX_MIRRORX = 4;
    
    /**
     * Translate the points in an imagej Roi
     */
    public static Roi translateRoi( Roi roi, double dx, double dy ) {
        ArrayList<Float> xt = new ArrayList<Float>();
        ArrayList<Float> yt = new ArrayList<Float>();
        // Get ROI points
        FloatPolygon polygon = roi.getFloatPolygon();
        int n_points = polygon.npoints;
        float[] x = polygon.xpoints;
        float[] y = polygon.ypoints;
        for (int i = 0; i < n_points; i++) {
            xt.add( (float) ( (double) x[i] + dx ) );
            yt.add( (float) ( (double )y[i] + dy ) );
        }
        Float[] xta = xt.toArray(new Float[0]);
        Float[] yta = yt.toArray(new Float[0]);
        Roi transfoRoi = new PolygonRoi(ArrayUtils.toPrimitive(xta), ArrayUtils.toPrimitive(yta), xt.size(), PolygonRoi.FREEROI);
        transfoRoi.setName(roi.getName());
        transfoRoi.setStrokeColor(roi.getStrokeColor());

        return transfoRoi;
    }    

    /**
     * Rotate the points in an imagej Roi
     */
    public static Roi rotateRoi( Roi roi, double angle, double centerX, double centerY) {
        ArrayList<Float> xt = new ArrayList<Float>();
        ArrayList<Float> yt = new ArrayList<Float>();
        // Get ROI points
        FloatPolygon polygon = roi.getFloatPolygon();
        int n_points = polygon.npoints;
        float[] x = polygon.xpoints;
        float[] y = polygon.ypoints;
        for (int i = 0; i < n_points; i++) {
            xt.add( (float) ( (double) centerX + ( ( (double) x[i] - centerX ) * Math.cos( angle / 180.0 * Math.PI ) ) - ( (double) y[i] - centerY ) * Math.sin( angle / 180.0 * Math.PI ) ) );
            yt.add( (float) ( (double) centerY + ( ( (double) x[i] - centerX ) * Math.sin( angle / 180.0 * Math.PI ) ) + ( (double) y[i] - centerY ) * Math.cos( angle / 180.0 * Math.PI ) ) );
        }
        Float[] xta = xt.toArray(new Float[0]);
        Float[] yta = yt.toArray(new Float[0]);
        Roi transfoRoi = new PolygonRoi(ArrayUtils.toPrimitive(xta), ArrayUtils.toPrimitive(yta), xt.size(), PolygonRoi.FREEROI);
        transfoRoi.setName(roi.getName());
        transfoRoi.setStrokeColor(roi.getStrokeColor());

        return transfoRoi;
    }
    
    /**
     * Flip the points in an imagej Roi
     */
    public static Roi flipRoi( Roi roi, double centerY) {
        ArrayList<Float> xt = new ArrayList<Float>();
        ArrayList<Float> yt = new ArrayList<Float>();
        // Get ROI points
        FloatPolygon polygon = roi.getFloatPolygon();
        int n_points = polygon.npoints;
        float[] x = polygon.xpoints;
        float[] y = polygon.ypoints;
        for (int i = 0; i < n_points; i++) {
            xt.add( (float) x[i] );
            yt.add( (float) ( (double) centerY - ( (double) y[i] - centerY ) ) );
        }
        Float[] xta = xt.toArray(new Float[0]);
        Float[] yta = yt.toArray(new Float[0]);
        Roi transfoRoi = new PolygonRoi(ArrayUtils.toPrimitive(xta), ArrayUtils.toPrimitive(yta), xt.size(), PolygonRoi.FREEROI);
        transfoRoi.setName(roi.getName());
        transfoRoi.setStrokeColor(roi.getStrokeColor());

        return transfoRoi;
    }   
    /**
     * Flip the points in an imagej Roi
     */
    public static Roi flipRoiX( Roi roi, double centerX) {
        ArrayList<Float> xt = new ArrayList<Float>();
        ArrayList<Float> yt = new ArrayList<Float>();
        // Get ROI points
        FloatPolygon polygon = roi.getFloatPolygon();
        int n_points = polygon.npoints;
        float[] x = polygon.xpoints;
        float[] y = polygon.ypoints;
        for (int i = 0; i < n_points; i++) {
            xt.add( (float) ( (double) centerX - ( (double) x[i] - centerX ) ) );
            yt.add( (float) y[i] );
        }
        Float[] xta = xt.toArray(new Float[0]);
        Float[] yta = yt.toArray(new Float[0]);
        Roi transfoRoi = new PolygonRoi(ArrayUtils.toPrimitive(xta), ArrayUtils.toPrimitive(yta), xt.size(), PolygonRoi.FREEROI);
        transfoRoi.setName(roi.getName());
        transfoRoi.setStrokeColor(roi.getStrokeColor());

        return transfoRoi;
    }   
      
    /**
     * Rotate the points in an imagej Roi
     */
    public static Roi scaleRoi( Roi roi, double centerX, double centerY, double scale ) {
        ArrayList<Float> xt = new ArrayList<Float>();
        ArrayList<Float> yt = new ArrayList<Float>();
        // Get ROI points
        FloatPolygon polygon = roi.getFloatPolygon();
        int n_points = polygon.npoints;
        float[] x = polygon.xpoints;
        float[] y = polygon.ypoints;
        for (int i = 0; i < n_points; i++) {
            xt.add( (float) ( (double) centerX + scale * ( (double) x[i] - centerX ) ) );
            yt.add( (float) ( (double) centerY + scale * ( (double) y[i] - centerY ) ) );
        }
        Float[] xta = xt.toArray(new Float[0]);
        Float[] yta = yt.toArray(new Float[0]);
        Roi transfoRoi = new PolygonRoi(ArrayUtils.toPrimitive(xta), ArrayUtils.toPrimitive(yta), xt.size(), PolygonRoi.FREEROI);
        transfoRoi.setName(roi.getName());
        transfoRoi.setStrokeColor(roi.getStrokeColor());

        return transfoRoi;
    }   
    
	/**
     * Rotate the points in an imagej Roi, alternative version where topleft pixel in the input ROI roi has coordinates ( topLeftX, topLeftY )
     */
    public static Roi scaleRoiAlternative( Roi roi, double topLeftX, double topLeftY, double scale ) {
        ArrayList<Float> xt = new ArrayList<Float>();
        ArrayList<Float> yt = new ArrayList<Float>();
        // Get ROI points
        FloatPolygon polygon = roi.getFloatPolygon();
        int n_points = polygon.npoints;
        float[] x = polygon.xpoints;
        float[] y = polygon.ypoints;
        for (int i = 0; i < n_points; i++) {
            xt.add( (float) ( scale * ( (double) x[i] + topLeftX ) ) );
            yt.add( (float) ( scale * ( (double) y[i] + topLeftY ) ) );
        }
        Float[] xta = xt.toArray(new Float[0]);
        Float[] yta = yt.toArray(new Float[0]);
        Roi transfoRoi = new PolygonRoi(ArrayUtils.toPrimitive(xta), ArrayUtils.toPrimitive(yta), xt.size(), PolygonRoi.FREEROI);
        transfoRoi.setName(roi.getName());
        transfoRoi.setStrokeColor(roi.getStrokeColor());

        return transfoRoi;
    }   
	
    public static LinkedHashMap<String,Roi> applyRoiScaleTransform( LinkedHashMap<String,Roi> roiMap, double centerX, double centerY, double scale ) {

        LinkedHashMap<String,Roi> roiMapT = new LinkedHashMap<String,Roi>();
        for (String key : roiMap.keySet() ) {
            Roi roi = roiMap.get(key);
            Roi transformedRoi = scaleRoi(roi, centerX, centerY, scale);
            roiMapT.put(key, transformedRoi);
        }

        return roiMapT;
    }
	
	/**
	 * This is the alternative version (the other is wrong???)
	 * 
	 * @param roiMap
	 * @param centerX left-top pixel x
	 * @param centerY left-top pixel y
	 * @param scale
	 * @return 
	 */
    public static LinkedHashMap<String,Roi> applyRoiScaleTransformAlternative( LinkedHashMap<String,Roi> roiMap, double topLeftX, double topLeftY, double scale ) {

        LinkedHashMap<String,Roi> roiMapT = new LinkedHashMap<String,Roi>();
        for (String key : roiMap.keySet() ) {
            Roi roi = roiMap.get(key);
            Roi transformedRoi = scaleRoiAlternative(roi, topLeftX, topLeftY, scale);
            roiMapT.put(key, transformedRoi);
        }

        return roiMapT;
    }
    
    public static LinkedHashMap<String,Roi> applyRoiRefTransform( LinkedHashMap<String,Roi> roiMap, double centerX, double centerY, double[] pretvecRef, double[] tvecRef, double[] pretvecSample, double[] tvecSample, String method, double[] methodParameters) {

        LinkedHashMap<String,Roi> roiMapT = new LinkedHashMap<String,Roi>();
        for (String key : roiMap.keySet() ) {
            Roi roi = roiMap.get(key);
            Roi transformedRoi = applyRoiTransform(roi, centerX, centerY, pretvecRef, method, methodParameters);
            transformedRoi = applyRoiTransform(transformedRoi, centerX, centerY, tvecRef, method, methodParameters);
            transformedRoi = applyRoiInverseTransform(transformedRoi, centerX, centerY, tvecSample, method, methodParameters);
            transformedRoi = applyRoiInverseTransform(transformedRoi, centerX, centerY, pretvecSample, method, methodParameters);
            roiMapT.put(key, transformedRoi);
        }

        return roiMapT;
    }

    public static Roi applyRoiRigidTransform(Roi roi, double centerX, double centerY, double tx, double ty, double angle) {

        Roi transformedRoi = roi;//(Roi) roi.clone();

        //transformedRoi.setInterpolationMethod();
        //transformedRoi.setBackgroundValue(0);
        transformedRoi = translateRoi(transformedRoi, tx, -ty);
        transformedRoi = rotateRoi(transformedRoi, angle, centerX, centerY);

        return transformedRoi;
    }

    public static Roi applyRoiRigidMirrorYTransform(Roi roi, double centerX, double centerY, double tx, double ty, double angle, boolean mirrory) {
        Roi transformedRoi = applyRoiRigidTransform(roi, centerX, centerY, tx, ty, angle);
        if (mirrory) {
            transformedRoi = flipRoi(transformedRoi, centerY);
        }
        return transformedRoi;
    }

	public static Roi applyRoiRigidMirrorXYTransform(Roi roi, double centerX, double centerY, double tx, double ty, double angle, boolean mirrory, boolean mirrorx) {
        Roi transformedRoi = applyRoiRigidTransform(roi, centerX, centerY, tx, ty, angle);
        if (mirrorx) {
            transformedRoi = flipRoiX(transformedRoi, centerX);
        }
        if (mirrory) {
            transformedRoi = flipRoi(transformedRoi, centerY);
        }
        return transformedRoi;
    }

    public static Roi applyRoiInverseRigidMirrorYTransform(Roi roi, double centerX, double centerY, double tx, double ty, double angle, boolean mirrory) {
        Roi transformedRoi = roi;
        if (mirrory) {
            transformedRoi = flipRoi(roi, centerY);
        }
        transformedRoi = applyRoiInverseRigidTransform(transformedRoi, centerX, centerY, tx, ty, angle);
        return transformedRoi;
    }

	public static Roi applyRoiInverseRigidMirrorXYTransform(Roi roi, double centerX, double centerY, double tx, double ty, double angle, boolean mirrory, boolean mirrorx) {
        Roi transformedRoi = roi;
        if (mirrory) {
            transformedRoi = flipRoi(roi, centerY);
        }
        if (mirrorx) {
            transformedRoi = flipRoiX(roi, centerX);
        }
        transformedRoi = applyRoiInverseRigidTransform(transformedRoi, centerX, centerY, tx, ty, angle);
        return transformedRoi;
    }

    public static Roi applyRoiInverseRigidTransform(Roi roi, double centerX, double centerY, double tx, double ty, double angle) {
        Roi transformedRoi = (Roi) roi.clone();

        transformedRoi = rotateRoi(transformedRoi, -angle, centerX, centerY);
        transformedRoi = translateRoi(transformedRoi, -tx, ty);

        return transformedRoi;
    }
    
    public static Roi applyRoiTransform(Roi roi, double centerX, double centerY, double[] tvec, String method, double[] methodParameters) {

        Roi transformedRoi = roi;
        switch (method) {
            case "RIGID":
                transformedRoi = applyRoiRigidTransform(roi, centerX, centerY, methodParameters[INDEX_TRANSLATION_X] * tvec[INDEX_TRANSLATION_X], methodParameters[INDEX_TRANSLATION_Y] * tvec[INDEX_TRANSLATION_Y], methodParameters[INDEX_ROTATION] * tvec[INDEX_ROTATION]);
                break;
            case "RIGID_MIRROR":
                transformedRoi = applyRoiRigidMirrorYTransform(roi, centerX, centerY, methodParameters[INDEX_TRANSLATION_X] * tvec[INDEX_TRANSLATION_X], methodParameters[INDEX_TRANSLATION_Y] * tvec[INDEX_TRANSLATION_Y], methodParameters[INDEX_ROTATION] * tvec[INDEX_ROTATION], ( ( (int) Math.abs((int) tvec[INDEX_MIRRORY]) ) % 2 ) > 0);
                break;
            case "RIGID_MIRROR_XY":
                transformedRoi = applyRoiRigidMirrorXYTransform(roi, centerX, centerY, methodParameters[INDEX_TRANSLATION_X] * tvec[INDEX_TRANSLATION_X], methodParameters[INDEX_TRANSLATION_Y] * tvec[INDEX_TRANSLATION_Y], methodParameters[INDEX_ROTATION] * tvec[INDEX_ROTATION], ( ( (int) Math.abs((int) tvec[INDEX_MIRRORY]) ) % 2 ) > 0, ( ( (int) Math.abs((int) tvec[INDEX_MIRRORX]) ) % 2 ) > 0);
                break;
            default:
                IJ.log("applyRoiTransform:: Transformation method " + method + " unknown");
                break;
        }
        return transformedRoi;
    }

    public static Roi applyRoiInverseTransform(Roi roi, double centerX, double centerY, double[] tvec, String method, double[] methodParameters) {

        Roi transformedRoi = roi;
        switch (method) {
            case "RIGID":
                transformedRoi = applyRoiInverseRigidTransform(roi, centerX, centerY, methodParameters[INDEX_TRANSLATION_X] * tvec[INDEX_TRANSLATION_X], methodParameters[INDEX_TRANSLATION_Y] * tvec[INDEX_TRANSLATION_Y], methodParameters[INDEX_ROTATION] * tvec[INDEX_ROTATION]);
                break;
            case "RIGID_MIRROR":
                transformedRoi = applyRoiInverseRigidMirrorYTransform(roi, centerX, centerY, methodParameters[INDEX_TRANSLATION_X] * tvec[INDEX_TRANSLATION_X], methodParameters[INDEX_TRANSLATION_Y] * tvec[INDEX_TRANSLATION_Y], methodParameters[INDEX_ROTATION] * tvec[INDEX_ROTATION], ( ( (int) Math.abs((int) tvec[INDEX_MIRRORY]) ) % 2 ) > 0);
                break;
            case "RIGID_MIRROR_XY":
                transformedRoi = applyRoiInverseRigidMirrorXYTransform(roi, centerX, centerY, methodParameters[INDEX_TRANSLATION_X] * tvec[INDEX_TRANSLATION_X], methodParameters[INDEX_TRANSLATION_Y] * tvec[INDEX_TRANSLATION_Y], methodParameters[INDEX_ROTATION] * tvec[INDEX_ROTATION], ( ( (int) Math.abs((int) tvec[INDEX_MIRRORY]) ) % 2 ) > 0, ( (int) Math.abs((int) tvec[INDEX_MIRRORX]) % 2 ) > 0);
                break;

            default:
                IJ.log("applyRoiInverseTransform:: Transformation method " + method + " unknown");
                break;
        }
        return transformedRoi;
    }
    
	
}
