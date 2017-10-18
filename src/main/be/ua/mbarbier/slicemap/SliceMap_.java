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

import main.be.ua.mbarbier.slicemap.gui.Gui;
import ij.IJ;
import ij.ImageJ;
import ij.Prefs;
import ij.gui.MessageDialog;
import ij.plugin.PlugIn;

/**
 *
 * @author mbarbier
 */
public class SliceMap_ implements PlugIn {

	@Override
	public void run(String arg) {
		//Prefs.set("plugin.name", "Grizzly Adams");
		//Prefs.set("plugin.id", 10);
		//Prefs.
		Gui gui = new Gui();
	}

	public static void main(String[] args) {

		// set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = SliceMap_.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        ImageJ imagej = new ImageJ();

		IJ.log("START RUN SliceMap_");
		IJ.runPlugIn(clazz.getName(), "");
		IJ.log("END RUN SliceMap_");
		//imagej.exitWhenQuitting(true);
		//imagej.quit();
		// alternative exit
//        if (!debug) {
//            System.exit(0);
//        }
		//String SliceMapTitle = "";
		//String message = "The SliceMap tools can be found in the menu under:\n Plugins > SliceMap > [tool of your choice]\n\n	The region annotation tool is SliceMap\n	The region curation tool is \n";
		//MessageDialog msg = new MessageDialog(imagej, SliceMapTitle, message);
		//msg.setVisible(true);
	}
	
}
