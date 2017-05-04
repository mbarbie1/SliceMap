/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap;

import main.be.ua.mbarbier.slicemap.gui.Gui;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

/**
 *
 * @author mbarbier
 */
public class SliceMap_ implements PlugIn {

	@Override
	public void run(String arg) {
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

        IJ.log("START RUN plugin");
		IJ.log("START RUN SliceMap_");
		
		IJ.runPlugIn(clazz.getName(), "");
		IJ.log("END RUN SliceMap_");
		//imagej.exitWhenQuitting(true);
		//imagej.quit();
		// alternative exit
//        if (!debug) {
//            System.exit(0);
//        }
	}
	
}
