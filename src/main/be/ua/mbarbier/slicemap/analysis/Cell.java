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
package main.be.ua.mbarbier.slicemap.analysis;

import java.util.LinkedHashMap;

/**
 *
 * @author mbarbier
 */
public class Cell {
	
//	nucleus
//	cytoplasm
	
	public long x_pixels;
	public long y_pixels;
	public double x_um;
	public double y_um;
	public long nucleus_area_pixels;
	public long cytoplasm_area_pixels;
	public double nucleus_area_um;
	public double cytoplasm_area_um;

	public long[] nucleusPixelX;
	public long[] nucleusPixelY;
	public long[] cytoplasmPixelX;
	public long[] cytoplasmPixelY;

	public class Channel {
		public String channelId;
		public double[] pixelIntensity;
		public double mean;
		public double stddev;
		public double max;
		public double min;
		public double median;
	}
	
	public LinkedHashMap< String, Channel > meanIntensity = new LinkedHashMap<>();

	public double[] nucleusPixelIntensity;
	public double[] cytoplasmPixelIntensity;
			
}
