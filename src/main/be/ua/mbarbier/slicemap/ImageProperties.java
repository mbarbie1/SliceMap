/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap;

import main.be.ua.mbarbier.slicemap.lib.congealing.Congealing;
import main.be.ua.mbarbier.slicemap.lib.roi.LibRoi;
import ij.gui.PointRoi;
import ij.gui.Roi;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;

/**
 *
 * @author mbarbier
 */
public class ImageProperties {

	public final String LABEL_ID = "IMAGE_ID";
	public final String LABEL_INDEX = "IMAGE_INDEX";
	public final String LABEL_SIGMA_SMOOTH = "SIGMA_SMOOTH";
	public final String LABEL_BINNING = "BINNING";
	public final String LABEL_BINNING_CONGEALING = "BINNING_CONGEALING";
	public final String LABEL_BINNING_TOTAL = "BINNING_TOTAL";
	public final String LABEL_ROIFILE = "ROIFILE";
	public final String LABEL_POINTROIFILE = "POINTROIFILE";
	public final String LABEL_IMAGEFILE = "IMAGEFILE";
	public final String LABEL_WIDTH = "WIDTH";
	public final String LABEL_HEIGHT = "HEIGHT";
	public final String LABEL_STACKWIDTH = "STACKWIDTH";
	public final String LABEL_STACKHEIGHT = "STACKHEIGHT";
	public final String LABEL_XOFFSET = "XOFFSET";
	public final String LABEL_YOFFSET = "YOFFSET";

	public String id;
	public int index;
	public int bitDepth;
	public int binning;
	public int binning_congealing;
	public int binning_total;
	public double sigma_smooth;
	public int width;
	public int height;
	public int stackWidth;
	public int stackHeight;
	public int xOffset;
	public int yOffset;
	public LinkedHashMap< String, Roi > pointRoi;
	public LinkedHashMap< String, Roi > pointRoiOri;
	public File pointRoiOriFile;
	public LinkedHashMap< String, Roi > roiMapOri;
	public File roiMapOriFile;
	public LinkedHashMap< String, Roi > roiMap;
	public File imageOriFile;

	public ImageProperties copy() {

		ImageProperties copy = new ImageProperties();
		copy.id = this.id;
		copy.index = this.index;
		copy.bitDepth = this.bitDepth;
		copy.binning = this.binning;
		copy.binning_congealing = this.binning_congealing;
		copy.binning_total = this.binning_total;
		copy.sigma_smooth = this.sigma_smooth;
		copy.width = this.width;
		copy.height = this.height;
		copy.stackWidth = this.stackWidth;
		copy.stackHeight = this.stackHeight;
		copy.xOffset = this.xOffset;
		copy.yOffset = this.yOffset;
		try {
			copy.pointRoiOri = new LinkedHashMap<>();
			copy.pointRoiOri.putAll( this.pointRoiOri );
			copy.pointRoi = new LinkedHashMap<>();
			copy.pointRoi.putAll( this.pointRoi );
			copy.pointRoiOriFile = this.pointRoiOriFile;

			copy.roiMapOri = new LinkedHashMap<>();
			copy.roiMapOri.putAll( this.roiMapOri );
			copy.roiMapOriFile = this.roiMapOriFile;
			copy.roiMap = new LinkedHashMap<>();
			copy.roiMap.putAll( this.roiMap );
			copy.imageOriFile = this.imageOriFile;
		} catch(Exception e) {
			copy.pointRoi = null;
			copy.pointRoiOri = null;
			copy.pointRoiOriFile = null;
			copy.roiMapOri = null;
			copy.roiMapOriFile = null;
			copy.roiMap = null;
			copy.imageOriFile = null;
		}

		return copy;
	}
	
	public LinkedHashMap<String, String> getMap() {

		LinkedHashMap< String, String> m = new LinkedHashMap<>();
		m.put(this.LABEL_ID, this.id);
		m.put(this.LABEL_INDEX, Integer.toString(this.index) );
		m.put(this.LABEL_SIGMA_SMOOTH, Double.toString(this.sigma_smooth));
		m.put(this.LABEL_BINNING, Integer.toString(this.binning));
		m.put(this.LABEL_BINNING_CONGEALING, Integer.toString(this.binning_congealing));
		m.put(this.LABEL_BINNING_TOTAL, Integer.toString(this.binning_total));
		try {
			m.put(this.LABEL_IMAGEFILE, this.imageOriFile.getAbsolutePath());
		} catch(Exception e) {
			m.put(this.LABEL_IMAGEFILE, "None");
		} 
		try {
			m.put(this.LABEL_ROIFILE, this.roiMapOriFile.getAbsolutePath());
		} catch(Exception e) {
			m.put(this.LABEL_ROIFILE, "None" );
		}
		try {
			m.put(this.LABEL_POINTROIFILE, this.pointRoiOriFile.getAbsolutePath());
		} catch(Exception e) {
			m.put(this.LABEL_POINTROIFILE, "None" );
		}
		m.put(this.LABEL_WIDTH, Integer.toString(width));
		m.put(this.LABEL_HEIGHT, Integer.toString(height));
		m.put(this.LABEL_STACKWIDTH, Integer.toString(stackWidth));
		m.put(this.LABEL_STACKHEIGHT, Integer.toString(stackHeight));
		m.put(this.LABEL_XOFFSET, Integer.toString(xOffset));
		m.put(this.LABEL_YOFFSET, Integer.toString(yOffset));
		return m;
	}

	public void loadMap(LinkedHashMap<String, String> m) {

		this.id = m.get(this.LABEL_ID);
		this.index = Integer.parseInt( m.get(this.LABEL_INDEX) );
		this.sigma_smooth = Double.parseDouble(m.get(this.LABEL_BINNING));
		this.binning = Integer.parseInt(m.get(this.LABEL_BINNING));
		this.binning_congealing = Integer.parseInt(m.get(this.LABEL_BINNING_CONGEALING));
		this.binning_total = Integer.parseInt(m.get(this.LABEL_BINNING_TOTAL));
		this.imageOriFile = new File(m.get(this.LABEL_IMAGEFILE));
		this.roiMapOriFile = new File(m.get(this.LABEL_ROIFILE));
		this.pointRoiOriFile = new File(m.get(this.LABEL_POINTROIFILE));
		try {
			this.roiMapOri = LibRoi.loadRoiAlternative(this.roiMapOriFile);
		} catch (ZipException ex) {
			Logger.getLogger(Congealing.class.getName()).log(Level.SEVERE, null, ex);
		} catch (net.lingala.zip4j.exception.ZipException | IOException ex) {
			Logger.getLogger(ImageProperties.class.getName()).log(Level.SEVERE, null, ex);
		}
		try {
			this.pointRoiOri = LibRoi.loadRoiAlternative( this.pointRoiOriFile );
		} catch ( net.lingala.zip4j.exception.ZipException | IOException ex ) {
			Logger.getLogger(ImageProperties.class.getName()).log(Level.SEVERE, null, ex);
		}
		this.width = Integer.parseInt(m.get(this.LABEL_WIDTH));
		this.height = Integer.parseInt(m.get(this.LABEL_HEIGHT));
		this.stackWidth = Integer.parseInt(m.get(this.LABEL_STACKWIDTH));
		this.stackHeight = Integer.parseInt(m.get(this.LABEL_STACKHEIGHT));
		this.xOffset = Integer.parseInt(m.get(this.LABEL_XOFFSET));
		this.yOffset = Integer.parseInt(m.get(this.LABEL_YOFFSET));
	}
}

