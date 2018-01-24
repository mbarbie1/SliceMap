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

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;
import java.io.File;

/**
 *
 * @author mbarbier
 */
public class Batch_Process implements PlugIn {

	public Batch_Process() {
		super();
	}
	
	@Override
	public void run(String string) {
		BatchProcess bp = new BatchProcess();

		File csvFile = new File( string );

		bp.run( csvFile );
	}
	
	/**
	 *
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		Class<?> clazz = Batch_Process.class;
        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

		//ImageJ imagej = new ImageJ();

		IJ.log("START RUN Batch_Process");
		String csvPath = "";
		if (args.length > 0) {
			csvPath = args[0];
		}
		IJ.log("Parameter csv-file in: " + csvPath);
		//IJ.runPlugIn(clazz.getName(), csvPath);
		IJ.log("END RUN Batch_Process");
	}
	
}
