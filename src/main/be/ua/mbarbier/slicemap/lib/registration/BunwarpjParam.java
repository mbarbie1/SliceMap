/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib.registration;

import java.util.LinkedHashMap;

/**
 *
 * @author mbarbier
 */
public class BunwarpjParam {

	int accuracy_mode = 1;
	int img_subsamp_fact = 0;
	int min_scale_deformation = 1;
	int max_scale_deformation = 2;
	double divWeight = 0.1;
	double curlWeight = 0.1;
	double landmarkWeight = 1.0;
	double imageWeight = 1.0;
	double consistencyWeight = 30.0;
	double stopThreshold = 0.01;

	public int getAccuracy_mode() {
		return accuracy_mode;
	}

	public void setAccuracy_mode(int accuracy_mode) {
		this.accuracy_mode = accuracy_mode;
	}

	public int getImg_subsamp_fact() {
		return img_subsamp_fact;
	}

	public void setImg_subsamp_fact(int img_subsamp_fact) {
		this.img_subsamp_fact = img_subsamp_fact;
	}

	public int getMin_scale_deformation() {
		return min_scale_deformation;
	}

	public void setMin_scale_deformation(int min_scale_deformation) {
		this.min_scale_deformation = min_scale_deformation;
	}

	public int getMax_scale_deformation() {
		return max_scale_deformation;
	}

	public void setMax_scale_deformation(int max_scale_deformation) {
		this.max_scale_deformation = max_scale_deformation;
	}

	public double getDivWeight() {
		return divWeight;
	}

	public void setDivWeight(double divWeight) {
		this.divWeight = divWeight;
	}

	public double getCurlWeight() {
		return curlWeight;
	}

	public void setCurlWeight(double curlWeight) {
		this.curlWeight = curlWeight;
	}

	public double getLandmarkWeight() {
		return landmarkWeight;
	}

	public void setLandmarkWeight(double landmarkWeight) {
		this.landmarkWeight = landmarkWeight;
	}

	public double getImageWeight() {
		return imageWeight;
	}

	public void setImageWeight(double imageWeight) {
		this.imageWeight = imageWeight;
	}

	public double getConsistencyWeight() {
		return consistencyWeight;
	}

	public void setConsistencyWeight(double consistencyWeight) {
		this.consistencyWeight = consistencyWeight;
	}

	public double getStopThreshold() {
		return stopThreshold;
	}

	public void setStopThreshold(double stopThreshold) {
		this.stopThreshold = stopThreshold;
	}

	public LinkedHashMap<String, Double> getBunwarpjParamDouble() {

		LinkedHashMap<String, Double> param = new LinkedHashMap<String, Double>(); 
		param.put( "accuracy_mode", (double) this.accuracy_mode );
		param.put( "img_subsamp_fact", (double) this.img_subsamp_fact );
		param.put( "min_scale_deformation", (double) this.min_scale_deformation );
		param.put( "max_scale_deformation", (double) this.max_scale_deformation );
		param.put( "divWeight", (double) this.divWeight );
		param.put( "curlWeight", (double) this.curlWeight );
		param.put( "landmarkWeight", (double) this.landmarkWeight );
		param.put( "imageWeight", (double) this.imageWeight );
		param.put( "consistencyWeight", (double) this.consistencyWeight );
		param.put( "stopThreshold", (double) this.stopThreshold );

		return param;
	}

	public LinkedHashMap<String, String> getBunwarpjParamString() {

		LinkedHashMap<String, String> param = new LinkedHashMap<>(); 
		param.put( "accuracy_mode", Integer.toString(this.accuracy_mode) );
		param.put( "img_subsamp_fact", Integer.toString(this.img_subsamp_fact) );
		param.put( "min_scale_deformation", Integer.toString(this.min_scale_deformation) );
		param.put( "max_scale_deformation", Integer.toString(this.max_scale_deformation) );
		param.put( "divWeight", Double.toString( this.divWeight) );
		param.put( "curlWeight", Double.toString( this.curlWeight ) );
		param.put( "landmarkWeight", Double.toString( this.landmarkWeight ) );
		param.put( "imageWeight", Double.toString( this.imageWeight ) );
		param.put( "consistencyWeight", Double.toString( this.consistencyWeight ) );
		param.put( "stopThreshold", Double.toString( this.stopThreshold ) );

		return param;
	}
	
}
