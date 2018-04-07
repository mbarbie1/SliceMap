/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib;

/**
 *
 * @author mbarbier
 */
public class Meta {
	
	public String fileName;
	public String fileBaseName;
	public String fileFolder;
	public String filePath;
	public int sizeX;
	public int sizeY;
	public int sizeZ;
	public int sizeT;
	public int sizeC;
	public int nSeries;
	public int bitDepth;
	public double pixelSizeX;
	public double pixelSizeY;
	public double pixelSizeZ;

	public Meta(int sizeX, int sizeY, int sizeC, int nSeries, double pixelSizeX, double pixelSizeY) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeC = sizeC;
		this.nSeries = nSeries;
		this.pixelSizeX = pixelSizeX;
		this.pixelSizeY = pixelSizeY;
	}
	
	
	
}
