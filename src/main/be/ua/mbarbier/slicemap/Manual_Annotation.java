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
package main.be.ua.mbarbier.slicemap;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.io.File;
import ij.plugin.frame.RoiManager;
import java.util.ArrayList;
import ij.gui.ImageCanvas;
import ij.gui.TextRoi;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static main.be.ua.mbarbier.slicemap.lib.LibIO.findFiles;

/**
 *
 * @author mbarbier
 */
public class Manual_Annotation implements PlugIn {

	public class Tap extends KeyAdapter {
	
		RoiManager rm;
		ImagePlus imp;
		ArrayList< String > roiNameList;
		String outputPath;

		public Tap( RoiManager rm, ImagePlus imp, ArrayList< String > roiNameList, String outputPath ) {
			this.rm = rm;
			this.imp = imp;
			this.roiNameList = roiNameList;
			this.outputPath = outputPath;
		}
	
		public void keyPressed( KeyEvent event ) {
			
			int keyCode = event.getKeyCode();
			if (KeyEvent.VK_G == keyCode) {
				if ( this.roiNameList.size() < this.rm.getCount()+1 ) {
					IJ.log("All ROIs are already defined, it not possible to create new ones");
					event.consume(); // AVOID propagation to other key listeners
					return;
				}
				this.rm.addRoi( this.imp.getRoi() );
				int roiIndex = this.rm.getCount()-1;
				this.rm.select(roiIndex);
				String roiName = this.roiNameList.get(roiIndex);
				this.rm.runCommand("Rename", roiName);
				this.rm.runCommand("Show All");
				IJ.log("ROI added: " + roiName);
				if ( this.roiNameList.size() > roiIndex+1 ) {
					String roiNameNext = this.roiNameList.get(roiIndex+1);
					imageMessage(this.imp, "Next ROI = " + roiNameNext, 2);
				} else {
					IJ.log("All ROIs defined!");
					imageMessage(this.imp, "All ROIs defined!", 2);
				}
				event.consume(); // AVOID propagation to other key listeners
			}
			
			if (KeyEvent.VK_Z == keyCode) {
				IJ.log("Saving to " + this.outputPath);
				this.rm.runCommand("Save", this.outputPath);
				this.imp.close();
				this.rm.close();
				event.consume(); // AVOID propagation to other key listeners
			}
		}
	}
	
	@Override
	public void run(String arg) {

		// PARAMETER INPUT
		GenericDialogPlus gdp = new GenericDialogPlus("SliceMap: Manual Annotation");
		gdp.addHelp( "https://github.com/mbarbie1/SliceMap" );
		gdp.addDirectoryField( "Sample folder", "d:/p_prog_output/slicemap_3/samples" );
		gdp.addDirectoryField( "Output folder", "d:/p_prog_output/slicemap_3/output" );
		gdp.addStringField("Sample file extension", "");
		gdp.addStringField("Sample name contains", "");
		gdp.addStringField("Output file name prefix", "");
		gdp.addCheckbox( "Overwriting existing ROIs", true );
		gdp.addStringField("List of ROI-names (comma separated)", "hp,cx,cb,th,bs,mb");

		gdp.showDialog();
		if ( gdp.wasCanceled() ) {
			return;
		}

		// EXTRACTION OF PARAMETERS FROM DIALOG
		File sampleFile = new File( gdp.getNextString() );
		File outputFile = new File( gdp.getNextString() );
		outputFile.mkdirs();
		String ext = gdp.getNextString();
		String sampleFilter = gdp.getNextString();
		String outputNamePrefix = gdp.getNextString();
		boolean overwriteRois = gdp.getNextBoolean();
		String nameList = gdp.getNextString();
		ArrayList< String > roiNameList = new ArrayList<>();
		// nameList is a comma separated list of roiNames as a single String,
		// convert to ArrayList of roiNames
		String[] nameListSplit = nameList.split(",");
		for ( String roiName : nameListSplit ) {
			roiNameList.add(roiName);
		}
		
		ArrayList< File > fileList = findFiles( sampleFile, sampleFilter, "sl;dj;klsd" );
		for (File file : fileList) {
			// Check for file extension
			String fileName = file.getName();
			if( !fileName.endsWith(ext) )
				continue;
			String sample_id;
			if (fileName.contains(".")) {
				sample_id = fileName.substring(0,fileName.lastIndexOf("."));
			} else {
				sample_id = fileName;
			}
			String outputName = outputNamePrefix + sample_id + ".zip";
			// check for existing output files
			String outputPath = outputFile.getAbsolutePath() + "/" + outputName;
			if ( overwriteRois ) {
				try {
					Files.deleteIfExists( new File(outputPath).toPath() );
				} catch (IOException ex) {
					Logger.getLogger(Manual_Annotation.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			process( sampleFile, outputFile, fileName, roiNameList, outputName);
		}
	}
	
	public void process( File inputFolder, File outputFolder, String inputName, ArrayList< String > roiNameList, String outputName ) {
	
		String inputPath = inputFolder.getAbsolutePath() + "/" + inputName;
		String outputPath = outputFolder.getAbsolutePath() + "/" + outputName;

		// Get current ImagePlus
		ImagePlus imp = IJ.openImage( inputPath );
		if ( !imp.isVisible() ) {
			imp.show();
			//imp.updateAndRepaintWindow()
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

		// Select ROIs
		selectROI(imp, rm, roiNameList, outputPath );

		// Wait for the ROIs to be written before continuing
		try {
			while ( !new File(outputPath).exists() ) {
					TimeUnit.SECONDS.sleep(1);
			}
		} catch (Exception ex) {
			Logger.getLogger(Manual_Annotation.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void imageMessage( ImagePlus imp, String text, int line) {
		int tx = 10;
		int ty = line * 10;
		Font font = new Font( "Arial", Font.PLAIN, 12 );
		Color color = Color.red;
		TextRoi t = new TextRoi( tx, ty, text, font);
		t.setNonScalable( true );
		imp.setOverlay( t, color, 1, Color.black );
	}
	
	public void selectROI( ImagePlus imp, RoiManager rm, ArrayList< String > roiNameList, String outputPath ) {
	
		// Remove keyListeners
		ImageCanvas canvas = imp.getWindow().getCanvas();
		KeyListener[] kls = canvas.getKeyListeners();
		for (KeyListener kl : kls) {
			canvas.removeKeyListener( kl );
		}

		// Add custom keylisteners
		KeyListener listener = new Tap( rm, imp, roiNameList, outputPath );
		canvas.addKeyListener( listener );

		// Re-add existing key listeners
		for (KeyListener kl : kls) {
			canvas.addKeyListener( kl );
		}
		
		imageMessage( imp, "Next ROI = " + roiNameList.get(0), 2);
	}

	
	public static void main(String[] args) {

// set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Manual_Annotation.class;

        System.out.println(clazz.getName());
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.out.println(pluginsDir);
        System.setProperty("plugins.dir", pluginsDir);

        ImageJ imagej = new ImageJ();

		IJ.log("START RUN Manual annotation");
		IJ.runPlugIn(clazz.getName(), "");
		IJ.log("END RUN Manual annotation");
		//imagej.exitWhenQuitting(true);
		//imagej.quit();
		// alternative exit
//        if (!debug) {
//            System.exit(0);
//        }
	}

}

/*
# @File(label = "Input directory", style = "directory") srcFile
# @File(label = "Output directory", style = "directory") dstFile
# @String(label = "File extension", value=".tif") ext
# @String(label = "File name contains", value = "") containString
# @Boolean(label = "Overwrite existing ROIs", value = 1) overwrite

import time
import os
import re
from ij import IJ, ImageJ
from ij.gui import Roi, Overlay, TextRoi, WaitForUserDialog
from ij.plugin.frame import RoiManager
from java.io import File
from java.awt.event import KeyEvent, KeyAdapter
from java.awt import Color, Font
from fiji.util.gui import GenericDialogPlus

def imageMessage(imp, text, line):
	tx = 10
	ty = line * 10
	font = Font("Arial", Font.PLAIN, 12)
	color = Color.red
	t = TextRoi( tx, ty, text, font)
	t.setNonScalable(1)
	imp.setOverlay(t, color, 1, Color.black)


class Tap(KeyAdapter):

	def __init__(self, rm, imp, roiNameList, outputPath):
		self.rm = rm
		self.imp = imp
		self.roiNameList = roiNameList
		self.outputPath = outputPath

	def keyPressed(self, event):
		keyCode = event.getKeyCode()
		if KeyEvent.VK_G == keyCode:
			if ( len(self.roiNameList) < self.rm.getCount()+1 ):
				IJ.log("All ROIs are already defined, it not possible to create new ones")
				event.consume() # AVOID propagation to other key listeners
				return
			self.rm.addRoi(self.imp.getRoi())
			roiIndex = self.rm.getCount()-1
			self.rm.select(roiIndex)
			roiName = self.roiNameList[roiIndex]
			self.rm.runCommand("Rename", roiName)
			self.rm.runCommand("Show All")
			IJ.log("ROI added: " + roiName)
			if ( len(self.roiNameList) > roiIndex+1 ):
				roiNameNext = self.roiNameList[roiIndex+1]
				imageMessage(self.imp, "Next ROI = " + roiNameNext, 2)
			else:
				IJ.log("All ROIs defined!")
				imageMessage(self.imp, "All ROIs defined!", 2)
			event.consume() # AVOID propagation to other key listeners
		if KeyEvent.VK_Z == keyCode:
			IJ.log("Saving to " + self.outputPath)
			self.rm.runCommand("Save", self.outputPath)
			self.imp.close()
			self.rm.close()
			event.consume() # AVOID propagation to other key listeners


def selectROI(imp, rm, roiNameList, outputPath ):
	
	# Remove keyListeners
	canvas = imp.getWindow().getCanvas()
	kls = canvas.getKeyListeners()
	map(canvas.removeKeyListener, kls)

	# Add custom keylisteners
	print(rm)
	listener = Tap( rm, imp, roiNameList, outputPath)
	canvas.addKeyListener(listener)

	# Re-add existing key listeners
	map(canvas.addKeyListener, kls)

	imageMessage(imp, "Next ROI = " + roiNameList[0], 2)
	

def run():

	# @File(label = "Input directory", style = "directory") srcFile
	# @File(label = "Output directory", style = "directory") dstFile
	# @String(label = "File extension", value=".tif") ext
	# @String(label = "File name contains", value = "") containString
	# @Boolean(label = "Overwrite existing ROIs", value = 1) overwrite
	gdp = GenericDialogPlus("");
	gdp.addDirectoryField("Input directory", "D:/d_data/astrid/Beerse31_rois");
	gdp.addDirectoryField("Output directory", "D:/d_data/astrid/Beerse31_rois/rois");
	gdp.addStringField("File extension", "tif")
	gdp.addStringField("File name prefix", "stack_")
	gdp.addCheckbox("Overwrite existing ROIs", 1);
	gdp.showDialog();
	if (gdp.wasCanceled()):
		return
	srcFile = File(gdp.getNextString())
	dstFile = File(gdp.getNextString())
	ext = gdp.getNextString()
	containString = gdp.getNextString()
	overwrite = gdp.getNextBoolean()

	# Starting imagej ui
	ImageJ()

	# io: inputFolder (images), inputImagePath, outputPath (ROIs)
	inputFolder = srcFile.getAbsolutePath()
	outputFolder = dstFile.getAbsolutePath()
	expId = "B31"

	for root, directories, fileNames in os.walk(inputFolder):
		for fileName in fileNames:
			# Check for file extension
			if not fileName.endswith(ext):
				continue
			# Check for file name pattern
			if containString not in fileName:
				continue
			pattern = re.compile( containString + "(.*)-(.*)\." + ext)
			res = re.search(pattern, fileName)
			index = res.group(1)
			treatment = res.group(2)
			inputSlice = index + "-" + treatment
			inputName = fileName
			outputName = "RoiSet_" + expId + "_" + inputSlice + ".zip"
			#check for existing output files
			outputPath = outputFolder + "/" + outputName
			if os.path.isfile(outputPath):
				if (overwrite):
					os.remove(outputPath)
					process(inputFolder, outputFolder, inputName, inputSlice, outputName)
			else:
				process(inputFolder, outputFolder, inputName, inputSlice, outputName)

def process(inputFolder, outputFolder, inputName, inputSlice, outputName ):
	
	inputPath = inputFolder + "/" + inputName
	outputPath = outputFolder + "/" + outputName
	
	# Get current ImagePlus
	imp = IJ.openImage(inputPath)
	if (not imp.isVisible()):
		imp.show()
	#imp.updateAndRepaintWindow()

	imp.setDisplayMode(IJ.COMPOSITE);
	imp.setActiveChannels("1010");
	imp.setC(1)
	IJ.run(imp, "Enhance Contrast", "saturated=0.05");
	imp.setC(3)
	IJ.run(imp, "Enhance Contrast", "saturated=0.05");
	#imageMessage(imp, inputName, 1)
	
	# RoiManager
	rm = RoiManager.getInstance()
	if not rm:
		rm = RoiManager()
	rm.reset()
	
	# Polygon point selection tool
	IJ.setTool("polygon");
	# predefined ROIs
	roiNameList = ["hp","cx","cb","th","bs","mb"]
	
	# Select ROIs
	selectROI(imp, rm, roiNameList, outputPath )

	# Wait for the ROIs to be written before continuing
	outputPath = outputFolder + "/" + outputName
	while not os.path.exists(outputPath):
		time.sleep(1)

# If a Jython script is run, the variable __name__ contains the string '__main__'.
# If a script is loaded as module, __name__ has a different value.
if __name__ == '__main__':
	run()
*/