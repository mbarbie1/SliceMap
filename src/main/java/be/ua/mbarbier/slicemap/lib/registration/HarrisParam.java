/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ua.mbarbier.slicemap.lib.registration;

import java.util.LinkedHashMap;

/**
 *
 * @author mbarbier
 */
public class HarrisParam {

	double alpha = 0.01;
	double tH = 10.0;
	double dmin = 3.0;
	int nPoints = 10;
	double sigma = 15.0;
	
	public LinkedHashMap<String, Double> getHarrisParamDouble() {

		LinkedHashMap<String, Double> param = new LinkedHashMap<>(); 
		param.put( "alpha", alpha );
		param.put( "tH", tH );
		param.put( "dmin", dmin );
		param.put( "nPoints", (double) nPoints );
		param.put( "sigma", sigma );

		return param;
	}

	public LinkedHashMap<String, String> getHarrisParamString() {

		LinkedHashMap<String, String> param = new LinkedHashMap<>(); 
		param.put( "alpha", Double.toString(alpha) );
		param.put( "tH", Double.toString(tH) );
		param.put( "dmin", Double.toString(dmin) );
		param.put( "nPoints", Integer.toString(nPoints) );
		param.put( "sigma", Double.toString(sigma) );

		return param;
	}
	
}
