/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib.roi;

import ij.gui.Roi;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 *
 * @author mbarbier
 */
public class RoiMap {

	// Region name 
	LinkedHashMap< String, LinkedHashMap< String, Roi > > map = new LinkedHashMap<>();
	LinkedHashMap< String, LinkedHashMap< String, Roi > > mapInverse = new LinkedHashMap<>();

	public RoiMap( LinkedHashMap< String, LinkedHashMap< String, Roi > > map, boolean order_region_image ) {
		if ( order_region_image ) {
			this.map.putAll(map);
		} else {
			for ( String keyImage : map.keySet() ) {
				LinkedHashMap< String, Roi > regionMap = new LinkedHashMap<>();
				for ( String keyRegion : map.keySet() ) {
					regionMap.put( keyImage, map.get(keyRegion).get(keyImage) );
				}
				
			}
		}
	}

	public static <T extends Object> LinkedHashMap< String, LinkedHashMap< String, T > > mapInvert( LinkedHashMap< String, LinkedHashMap< String, T > > map ) {

		LinkedHashMap< String, LinkedHashMap< String, T > > imap = new LinkedHashMap<>();
		for ( String key1 : map.keySet() ) {
			for ( String key2 : map.get( key1 ).keySet() ) {
				if ( imap.containsKey(key2) ) {
					imap.put(key2, new LinkedHashMap<String, T>() );
				}
				imap.get(key2).put(key1, map.get( key1 ).get( key2 ) ) ;
			}
		}
		return imap;
	}

	/*
	public ArrayList< String > getRoiNames(  ) {
		
	}
	
	public ArrayList< String > getImageNames(  ) {
	}
	
	public LinkedHashMap< String, Roi > getRoisOfImage( String imageName ) {
		
		
	}
	
	public LinkedHashMap< String, Roi > getRoisOfRegion( String roiName ) {

		LinkedHashMap< String, Roi > roiMap = new LinkedHashMap< String, Roi >();
		
		return 
	}
	*/
}
