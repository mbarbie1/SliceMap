/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib;

import java.util.ArrayList;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Set;

public class Lib {

	public static ArrayList< String > readCommaSeparatedList( String nameString) {
		
		ArrayList< String > nameList = new ArrayList<>();
		String[] nameListSplit = nameString.split(",");
		for ( String name : nameListSplit ) {
			nameList.add( name );
		}
		return nameList;
	}


	
    public static int[] IntegerArrayToIntArray(ArrayList<Integer> a) {
        int[] out = new int[a.size()];
        for (int i = 0; i < a.size(); i++) {
            out[i] = a.get(i);
        }
        return out;
    }

    public static double[] DoubleArrayTodoubleArray(ArrayList<Double> a) {
        double[] out = new double[a.size()];
        for (int i = 0; i < a.size(); i++) {
            out[i] = a.get(i);
        }
        return out;
    }

    public static float[] FloatArrayTofloatArray(ArrayList<Float> a) {
        float[] out = new float[a.size()];
        for (int i = 0; i < a.size(); i++) {
            out[i] = a.get(i);
        }
        return out;
    }

    public static double[] intArrayToDoubleArray(int[] a) {
        int n = a.length;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            out[i] = (double) a[i];
        }
        return out;
    }

    public static int[] doubleArrayToIntArray(double[] a) {
        int n = a.length;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = (int) Math.round(a[i]);
        }
        return out;
    }

    public static Roi[] RoiArrayListToRoiArray(ArrayList<Roi> a) {
        Roi[] out = new Roi[a.size()];
        for (int i = 0; i < a.size(); i++) {
            out[i] = a.get(i);
        }
        return out;
    }

    public static int[] intSequence(int nStart, int nEnd) {
        int n = nEnd - nStart + 1;
        int[] x = new int[n];
        for (int i = 0; i < n; i++) {
            x[i] = nStart + i;
        }

        return x;
    }

    public static double[] linearSpacedSequence(double xStart, double xEnd, int n) {

        double[] x = new double[n + 1];
        double d = (xEnd - xStart) / n;
        for (int i = 0; i < n + 1; i++) {
            x[i] = xStart + i * d;
        }
        
        return x;
    }

    public static double[] logSpacedSequence(double xStart, double xEnd, int n) {

        double[] x = new double[n + 1];
        double d = ( Math.log( xEnd ) - Math.log( xStart ) ) / n;
        for (int i = 0; i < n + 1; i++) {
            x[i] = Math.exp( Math.log( xStart ) + i * d );
        }
        
        return x;
    }

    public static double sumDouble( double[] x ) {

        double sum = 0.0;
        int n = x.length;
        for (int i = 0; i < n; i++) {
            sum += x[i];
        }
        
        return sum;
    }

    public static double prodDouble( double[] x ) {

        double prod = 1.0;
        int n = x.length;
        for (int i = 0; i < n; i++) {
            prod = prod * x[i];
        }
        
        return prod;
    }

	public static double median(double[] m) {
		Arrays.sort(m);
		int middle = m.length/2;
		if (m.length%2 == 1) {
			return m[middle];
		} else {
			return (m[middle-1] + m[middle]) / 2.0;
		}
	}
	
 /**
     * Sort double[]
     * 
     * @param a
     * @return 
     */
    public static LinkedHashMap< Integer, Double > sortDoubleArray( double[] a ) {

        LinkedHashMap< Integer, Double > unsorted = new LinkedHashMap< Integer, Double >();
        LinkedHashMap< Integer, Double > sorted = new LinkedHashMap< Integer, Double >();

        for ( int i = 0; i < a.length; i++ ) {
            int pos = 0;
            double ref = a[i];
            for ( int j = 0; j < a.length; j++ ) {
                if ( ref < a[j] ) {
                    pos++;
                }
            }
            while ( unsorted.containsKey(pos) ) {
                pos++;
            }
            unsorted.put( pos, ref );
        }
        
        return unsorted;
    }

    
	/**
	 * 
	 * @param map1
	 * @param map2
	 * @return 
	 */
	public static ArrayList< String > getCommonKeys( LinkedHashMap< String, ? > map1, LinkedHashMap< String, ? > map2 ) {
		
		ArrayList< String > commonKeys = new ArrayList< String >();
		if (map1.size() > 0) {
			for ( String key : map1.keySet() ) {
				if ( map2.containsKey(key) ) {
					commonKeys.add(key);
				}
			}
		}
		return commonKeys;
	}

	/**
	 * 
	 * @param map1
	 * @param map2
	 * @return 
	 */
	public static ArrayList< String > getFirstKeys( LinkedHashMap< String, ? > map1, LinkedHashMap< String, ? > map2 ) {
		
		ArrayList< String > firstKeys = new ArrayList< String >();
		if (map1.size() > 0) {
			for ( String key : map1.keySet() ) {
				firstKeys.add(key);
			}
		}
		return firstKeys;
	}

	/**
     * Sort objects in a map
     * 
     * @param map
     * @return 
     */
    public static LinkedHashMap< String, Object > sortMap( LinkedHashMap< String, Object > map ) {
        
        LinkedHashMap< String, Object > sorted = new LinkedHashMap< String, Object >();
        
        Object[] keysTemp = map.keySet().toArray();
        String[] keys = new String[keysTemp.length];
        Arrays.sort(keys);
        for ( int i = 0; i < keys.length; i++ ) {
            String key = keys[i];
            sorted.put( key, map.get(key) );
        }

        return sorted;
    }

    /**
     * Sort rois in a map
     * 
     * @param map
     * @return 
     */
    public static LinkedHashMap< String, Roi > sortRois( LinkedHashMap< String, Roi > map ) {
        
        LinkedHashMap< String, Roi > sorted = new LinkedHashMap< String, Roi >();
        
        Object[] keysTemp = map.keySet().toArray();
        String[] keys = new String[keysTemp.length];
        for ( int i = 0; i < keys.length; i++ ) {
            keys[i] = (String) keysTemp[i];
        }
        Arrays.sort(keys);
        for ( int i = 0; i < keys.length; i++ ) {
            String key = keys[i];
            sorted.put( key, map.get(key) );
        }

        return sorted;
    }

    public static LinkedHashMap< String, Double > convertMapStringToDouble( LinkedHashMap< String, String > mapString ) {
        
        LinkedHashMap< String, Double > mapDouble = new LinkedHashMap< String, Double >();
        
        for ( String k : mapString.keySet() ) {
            mapDouble.put( k, Double.valueOf( mapString.get(k) ) );
        }
        
        return mapDouble;
    }

    public static LinkedHashMap< String, String > convertMapDoubleToString( LinkedHashMap< String, Double > mapDouble ) {
        
        LinkedHashMap< String, String > mapString = new LinkedHashMap<>();
        
        for ( String k : mapDouble.keySet() ) {
            mapString.put( k, Double.toString( mapDouble.get(k) ) );
        }

		return mapString;
    }

	public static LinkedHashMap< String, Double> getMeanMap( LinkedHashMap< String, LinkedHashMap< String, Double > > maps ) {

		LinkedHashMap< String, Double> mapMean = new LinkedHashMap<>();
		Set< String > rows = maps.keySet();
		String firstKey = maps.keySet().iterator().next();
		Set< String > cols = maps.get( firstKey ).keySet();

		for ( String col : cols ) {
			mapMean.put( col, 0.);
			for ( String row : rows ) {
				double val = maps.get(row).get(col);
				mapMean.put( col, mapMean.get(col) + val );
			}
			mapMean.put( col, mapMean.get(col) / ((double) rows.size()) );
		}

		return mapMean;
	}

	public static <T extends Object> LinkedHashMap< String, T > prefixMapHeaders( LinkedHashMap< String, T > map, String prefix ) {
		
		String[] keys = map.keySet().toArray(new String[]{""});
		for ( String key : keys ) {
			map.put( prefix + key, map.get(key) );
			map.remove(key);
		}
		return map;
	}

    public static ArrayList< LinkedHashMap< String, String > > resultsTableToMap( ResultsTable rt ) {
        ArrayList< LinkedHashMap< String, String > > map = new ArrayList< LinkedHashMap< String, String > >();

        String[] headers = rt.getHeadings();
        for ( int j = 0; j < rt.size(); j++ ) {
            LinkedHashMap< String, String > row = new LinkedHashMap< String, String >();
            for ( int i = 0; i < headers.length; i++ ) {
                String value = rt.getStringValue( headers[i], j);
                row.put(headers[i], value);
            }
            map.add(row);
        }
        
        return map;
    
    }
    
    public static java.util.Map.Entry< Integer, Double > max( double[] a ) {
        double maxValue = a[0];
        int maxIndex = 0;
        for ( int i = 0; i < a.length; i++ ) {
            if (a[i] > maxValue) {
                maxValue = a[i];
                maxIndex = i;
            }
        }
        return new java.util.AbstractMap.SimpleEntry( maxIndex, maxValue );
    }

    public static void main(String[] args) {
        
        // TESTS:
        
        // TEST END sortDoubleArray ------------------------------------------------
        LinkedHashMap< Integer, Double > sortedArray = sortDoubleArray( new double[]{0.3, -0.3, 0.2, 0.1, 0.3, 0.4, -0.5, 0.3} );
        System.out.printf( "TEST START sortDoubleArray -------------------------\n" );
        System.out.printf( "The array\n" );
        for ( int k : sortedArray.keySet() ) {
            System.out.printf( "(" + k + ", " + sortedArray.get(k) + ")\n" );
        }
        System.out.printf( "TEST END sortDoubleArray ---------------------------\n" );
        // TEST END sortDoubleArray ------------------------------------------------
        
    }

}
