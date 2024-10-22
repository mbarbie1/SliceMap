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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import main.be.ua.mbarbier.slicemap.gui.Gui;
import main.be.ua.mbarbier.slicemap.lib.LibIO;

/**
 *
 * @author mbarbier
 */
class BatchProcess {
		
	Main param = new Main();
	
	public void run( File paramCsvFile ) {
		
		ArrayList< LinkedHashMap< String, String > > csvMapList = LibIO.readCsv( paramCsvFile.getAbsolutePath() , "", ",");
		
		for ( int i = 0; i < csvMapList.size(); i++ ) {
			LinkedHashMap< String, String > paramMap = csvMapList.get(i);

			run( paramMap );
		}
		
	}
	
	public void run( LinkedHashMap< String, String > paramMap ) {
		new Gui( paramMap );
	}
}
