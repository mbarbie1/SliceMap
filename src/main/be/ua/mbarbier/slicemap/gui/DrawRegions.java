/*
 * The MIT License
 *
 * Copyright 2017 mbarbier.
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
package main.be.ua.mbarbier.slicemap.gui;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import java.awt.Button;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mbarbier
 */
public class DrawRegions {

	
	public class ChoiceList extends PlugInFrame implements ActionListener {

		String title;
		LinkedHashMap< String, String > roiNameMap;
		LinkedHashMap< String, Button > roiButtonMap;
		LinkedHashMap< String, Roi > roiMap;
		LinkedHashMap< String, Boolean > roiActive;
		String selectedRoi = null;
		Roi editRoi = null;
		//int selectedSlice = 1;
		
		public ChoiceList(String title, LinkedHashMap< String, String > roiNameMap, LinkedHashMap< String, Roi > roiMap ) {
			super(title);
			this.setVisible(false);
			this.title = title;
			this.roiMap = roiMap;
			this.roiNameMap = roiNameMap; 
			this.roiButtonMap = new LinkedHashMap();
			this.roiActive = new LinkedHashMap();
			
			Button buttonSave = new Button( "Save" );
			buttonSave.addActionListener(this);
			this.add( buttonSave );

			for ( String roiKey : this.roiNameMap.keySet() ) {
				Button button = new Button( this.roiNameMap.get(roiKey) );
				button.addActionListener(this);
				this.roiButtonMap.put( roiKey, button );
				this.add( button );
				this.roiActive.put( roiKey, false );
			}
			int nButtons = this.roiNameMap.keySet().size();
			this.setLayout(new GridLayout(nButtons + 1, 1, 5, 0));
			this.pack();
			this.setVisible(true);
		}

		@Override
	    public void actionPerformed(ActionEvent e) {
			String label = e.getActionCommand();
			IJ.log("Action performed with label: " + label );
			if (label==null)
				return;
			String command = label;

			if ( command.equals( "Save" ) ) {
				saveRoi();
			}

			for ( String roiName : roiMap.keySet() ) {
				if ( command.equals( roiName ) ) {
					if ( roiActive.get(roiName) ) {
						doneRoi();
						this.roiButtonMap.get( roiName ).setEnabled(false);
					} else {
						this.selectedRoi = roiName;
						this.roiButtonMap.get( roiName ).setForeground(Color.yellow);
					}
				}
			}
		}
		
		public void doneRoi() {
			
		} 
		
		public void saveRoi() {
			
		}
	}
	
	public static void process( ImagePlus imp, ArrayList< String > roiNameList ) {

		if ( !imp.isVisible() ) {
			imp.show();
		}

		imp.setDisplayMode(IJ.COMPOSITE);
		//imp.setActiveChannels("1010");
		imp.setC(1);
		IJ.run(imp, "Enhance Contrast", "saturated=0.05");
		//imp.setC(3)
		//IJ.run(imp, "Enhance Contrast", "saturated=0.05");
		// //imageMessage(imp, inputName, 1)

		// RoiManager
		RoiManager rm = RoiManager.getInstance();
		if (rm == null) {
			rm = new RoiManager();
			rm.reset();
		}
		// Polygon point selection tool
		IJ.setTool("polygon");

	}

	public static void imageMessage( ImagePlus imp, String text, int line) {
		int tx = 10;
		int ty = line * 10;
		Font font = new Font( "Arial", Font.PLAIN, 12 );
		Color color = Color.red;
		TextRoi t = new TextRoi( tx, ty, text, font);
		t.setNonScalable( true );
		imp.setOverlay( t, color, 1, Color.black );
	}
	
	public static void main(String[] args) {

// set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = DrawRegions.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        ImageJ imagej = new ImageJ();

		IJ.log("START RUN Manual annotation");
		IJ.runPlugIn(clazz.getName(), "");
		IJ.log("END RUN Manual annotation");
	}
}
