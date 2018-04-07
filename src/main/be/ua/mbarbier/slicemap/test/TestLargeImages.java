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
package main.be.ua.mbarbier.slicemap.test;

import java.util.LinkedHashMap;
import main.be.ua.mbarbier.slicemap.gui.Gui;

/**
 *
 * @author mbarbier
 */
public class TestLargeImages {

	public TestLargeImages() {
	}

	public static void main(String[] args) {
		LinkedHashMap< String, String > paramMap = new LinkedHashMap<>();
		paramMap.put("param_ID", "p1");
		paramMap.put("sampleFile", "G:/slicemap_workflow/samples_neun");
//		paramMap.put("inputFile", "G:/slicemap_workflow/input_neun");
//		paramMap.put("sampleFile", "G:/slicemap_workflow/samples");
		paramMap.put("inputFile", "G:/slicemap_workflow/input");
		paramMap.put("outputFile", "G:/slicemap_workflow/output_testLargeImages");
		paramMap.put("stackBinnnig", "16");
		paramMap.put("nIterations", "10");
		paramMap.put("nReferences", "5");
		paramMap.put("sampleFilter", "");
		paramMap.put("regenerateStack", "false");

		Gui gui = new Gui( paramMap );
	}
}
