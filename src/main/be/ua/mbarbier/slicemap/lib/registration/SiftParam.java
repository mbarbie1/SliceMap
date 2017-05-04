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
public class SiftParam {

	float initialSigma = 1.6f;
	int steps = 3;
	int minOctaveSize = 64;
	int maxOctaveSize = 1024;
	int fdSize = 4;
	int fdBins = 8;
	float rod = 0.92f;
	float maxEpsilon = 25.0f;
	float minInlierRatio = 0.05f;
	int modelIndex = 2;

	public float getInitialSigma() {
		return initialSigma;
	}

	public int getSteps() {
		return steps;
	}

	public int getMinOctaveSize() {
		return minOctaveSize;
	}

	public int getMaxOctaveSize() {
		return maxOctaveSize;
	}

	public int getFdSize() {
		return fdSize;
	}

	public int getFdBins() {
		return fdBins;
	}

	public float getRod() {
		return rod;
	}

	public float getMaxEpsilon() {
		return maxEpsilon;
	}

	public float getMinInlierRatio() {
		return minInlierRatio;
	}

	public int getModelIndex() {
		return modelIndex;
	}

	public LinkedHashMap<String, Float> getSiftParamFloat() {

		LinkedHashMap<String, Float> param = new LinkedHashMap<>(); 
		param.put( "initialSigma", this.initialSigma );
		param.put( "steps", (float) this.steps );
		param.put( "minOctaveSize", (float) this.minOctaveSize );
		param.put( "maxOctaveSize", (float) this.maxOctaveSize );
		param.put( "fdSize", (float) this.fdSize );
		param.put( "fdBins", (float) this.fdBins );
		param.put( "rod", this.rod );
		param.put( "maxEpsilon", this.maxEpsilon );
		param.put( "minInlierRatio", this.minInlierRatio );
		param.put( "modelIndex", (float) this.modelIndex );

		return param;
	}

	public LinkedHashMap<String, String> getSiftParamString() {

		LinkedHashMap<String, String> param = new LinkedHashMap<>(); 
		param.put( "initialSigma", Float.toString(this.initialSigma) );
		param.put( "steps", Integer.toString(this.steps) );
		param.put( "minOctaveSize", Integer.toString(this.minOctaveSize) );
		param.put( "maxOctaveSize", Integer.toString(this.maxOctaveSize) );
		param.put( "fdSize", Integer.toString(this.fdSize) );
		param.put( "fdBins", Integer.toString(this.fdBins) );
		param.put( "rod", Float.toString(this.rod) );
		param.put( "maxEpsilon", Float.toString(this.maxEpsilon) );
		param.put( "minInlierRatio", Float.toString(this.minInlierRatio) );
		param.put( "modelIndex", Float.toString(this.modelIndex) );

		return param;
	}

	
}
