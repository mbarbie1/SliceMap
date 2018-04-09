package main.be.ua.mbarbier.slicemap;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import main.be.ua.mbarbier.slicemap.lib.LibIO;

/**
 *
 * @author mbarbier
 */
public class StackProperties {
	
	public static void saveStackProps(File stackPropsFile, LinkedHashMap< String, ImageProperties > stackProps) {
		ArrayList< LinkedHashMap< String, String > > stackPropsTemp = new ArrayList< LinkedHashMap< String, String > >();
		for ( String key : stackProps.keySet() ) {
			ImageProperties props = stackProps.get(key);
			stackPropsTemp.add(props.getMap());
		}
		LibIO.writeCsv( stackPropsTemp , ",", stackPropsFile.getAbsolutePath() );
	}
	
}
