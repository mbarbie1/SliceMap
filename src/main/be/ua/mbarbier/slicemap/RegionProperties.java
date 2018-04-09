/*
 * The MIT License
 *
 * Copyright 2018 University of Antwerp.
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

import ij.gui.Roi;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import main.be.ua.mbarbier.slicemap.lib.error.LibError;

/**
 *
 * @author mbarbier
 */
public class RegionProperties {
	
	String sliceName = "";
	String regionName = "";
	double area_mm = -1;
	double area_ref_mm = -1;
	double dice = -1;
	ArrayList< String > regionList = null;
	
	/**
	 * Contructor for RegionProperties, also calculates all properties
	 * 
	 * @param sliceName
	 * @param regionName
	 * @param regionList
	 * @param roi
	 * @param roi_ref
	 * @param pixelSize 
	 */
	public RegionProperties( String sliceName, String regionName, ArrayList< String > regionList, Roi roi, Roi roi_ref, double pixelSize ) {
		this.regionList = regionList;
		this.sliceName = sliceName;
		this.regionName = regionName;
		this.area_mm = (double) roi.getStatistics().area * pixelSize * pixelSize / 1000000.0;
		try {
			LinkedHashMap< String, Double > vops = LibError.roiVOP( roi, roi_ref );
			this.dice = vops.get("si");
			this.area_ref_mm = (double) roi_ref.getStatistics().area * pixelSize * pixelSize / 1000000.0;
		} catch( Exception e ) {
			this.dice = -1;
			this.area_ref_mm = -1;
		} 
	}
	
	/**
	 * Generates a map from the RegionProperties
	 * 
	 * @return LinkedHashMap< String, String > map with keys: "region_name", "area_ref_mm", "area_mm", "dice"
	 */
	public LinkedHashMap< String, String > toMap() {

		LinkedHashMap< String, String > map = new LinkedHashMap<>();
		map.put( "slice_id", sliceName);
		map.put( "region_name", regionName);
		map.put( "area_ref_mm", Double.toString( area_ref_mm) );
		map.put( "area_mm", Double.toString( area_mm) );
		map.put( "dice", Double.toString( dice ) );

		return map;
	}
}
