/*
 * The MIT License
 *
 * Copyright 2023 University of Antwerp.
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
package test.java.main.be.ua.mbarbier.slicemap;

import ij.IJ;
import ij.ImageJ;
import java.io.File;
import main.be.ua.mbarbier.slicemap.Main;
import main.be.ua.mbarbier.slicemap.RefStack;

/**
 *
 * @author mbarbier
 */
public class RefStackTest {
    	/**
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {

		// set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = RefStack.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        new ImageJ();

		RefStack refStack = new RefStack();

		Main param = new Main();
		param.INPUT_FOLDER = new File("d:/p_prog_output/slicemap/input");
		param.OUTPUT_FOLDER = new File("d:/p_prog_output/slicemap/output_ref_stack");
		param.FILE_REFERENCE_STACK = new File( "Beerse21.tif" );
		param.PATTERN_REF_FILES = "^(.*?)_.*";
		param.CONTAINS_REF_FILES = "C3";
		param.DOESNOTCONTAIN_REF_FILES = ".png";
		param.CONGEALING_STACKBINNING = 8;
		refStack.init( param );

        IJ.log("START RUN plugin");
		refStack.run();
        IJ.log("END RUN plugin");

		//refStack.log.close();
	}

}
