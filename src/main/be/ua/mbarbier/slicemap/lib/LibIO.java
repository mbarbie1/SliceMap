package main.be.ua.mbarbier.slicemap.lib;

import com.opencsv.CSVParser;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVParserBuilder;
import com.opencsv.exceptions.CsvValidationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.opencsv.CSVReader;
import ij.ImagePlus;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;
//import loci.formats.FormatException;
//import loci.plugins.BF;
//import loci.plugins.in.ImporterOptions;

public class LibIO {

	
		/**
	 * Open a single series of an image using bioformats
	 * 
	 * @param filePath
	 * @param seriesIndex
	 * @return 
	 * @throws java.io.IOException 
	 * @throws loci.formats.FormatException 
	 */
/*
	public static ImagePlus openSeries( String filePath, int seriesIndex ) throws IOException, FormatException {

		ImagePlus imp = null;
		
		ImporterOptions options = new ImporterOptions();
		// Unselect all series
		options.clearSeries();
		// Select the series we want to get: 0 = original, 1 = 2-binned, 2 = 4-binned, 3 = 8-binned
		options.setSeriesOn( seriesIndex, true );
		options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
		options.setId( filePath );
		ImagePlus[] imps = BF.openImagePlus( options );
		imp = imps[0];
		
		return imp;
	}
*/
	/**
	 * Open a selected region of a single series of an image using bioformats
	 *
	 * @param filePath
	 * @param seriesIndex
	 * @param channelIndex
	 * @param tileRegion
	 * @return
	 * @throws java.io.IOException
	 * @throws loci.formats.FormatException
	 */
/*
	public static ImagePlus openSeries(String filePath, int seriesIndex, int channelIndex, loci.common.Region tileRegion) throws IOException, FormatException {

		ImagePlus imp = null;

		ImporterOptions options = new ImporterOptions();
		
		// Select the series we want to get: 0 = original, 1 = 2-binned, 2 = 4-binned, 3 = 8-binned
		//options.clearSeries();
		//options.setSeriesOn(seriesIndex, true);

		// Selection of the channel (channels are zero-based ? )
		options.setCBegin( seriesIndex, channelIndex-1 );
		options.setCEnd( seriesIndex, channelIndex-1 );

		// Selection of the region
		options.setCrop(true);
		options.setCropRegion(seriesIndex, tileRegion);

		options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
		options.setId(filePath);
		ImagePlus[] imps = BF.openImagePlus(options);
		imp = imps[0];

		return imp;
	}
*/	
	/**
	 * Write text file
	 * 
	 * @param string
	 * @param filePath
	 */
	public static void writeTextFile( String string, String filePath ) {
		BufferedWriter writer = null;
	    try {
	        File file = new File(filePath);
	        writer = new BufferedWriter(new FileWriter( file ));
	        writer.write(string);
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        try {
	        	writer.close();
	        } catch (Exception e) {
	        }
	    }
	}
	
	/**
	 * Convert LinkedHashMap of String-values to csv-line (String) of values
	 * 
	 * @param m
	 * @return csv row line
	 */
	public static String csvRow( LinkedHashMap<String, String> m ) {

		String sep = ",";
		return concatenateStringArray(m.values().toArray(new String[]{""}), sep);
	}

	/**
	 * Convert LinkedHashMap of Strings to csv-line (String) with the headers
	 * 
	 * @param m
	 * @return csv header
	 */
	public static String csvHeader( LinkedHashMap<String, String> m ) {

		String sep = ",";
		return concatenateStringArray(m.keySet().toArray(new String[]{""}), sep);
	}

	/**
	 * Write ArrayList of LinkedHashMap's of Strings to a csv-file
	 * 
	 * @param al
	 * @param filePath
	 */
	public static void writeCsv( ArrayList<LinkedHashMap<String, String>> al, String filePath ) {

		PrintWriter writer;
		try {
			writer = new PrintWriter(filePath);
			writer.println( csvHeader( al.get(0) ) );
			for (LinkedHashMap<String, String> m: al) {
				writer.println( csvRow( m ) );
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Write String rois to a csv-file
	 * 
	 * @param al
	 * @param filePath
	 */
	public static void writeRois( String roisHeader, String roisRow, String filePath ) {

		PrintWriter writer;
		try {
			writer = new PrintWriter(filePath);
			writer.println( roisHeader );
			writer.println( roisRow );
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static String concatenateStringArray(String[] s, String separator ) {
		String str = "";
		for (int i = 0; i < s.length; i++ ) {
			if (i < s.length-1) {
				str = str + s[i] + separator;
			} else {
				str = str + s[i];
			}
		}

		return str;
	} 
	
	/**
	 * Convert LinkedHashMap of String-values to csv-line (String) of values
	 * 
	 * @param m
	 * @return csv row line
	 */
	public static String csvRow( LinkedHashMap<String, String> m, String separator ) {

		return concatenateStringArray(m.values().toArray(new String[]{""}), separator);
	}
	
	/**
	 * Convert LinkedHashMap of Strings to csv-line (String) with the headers
	 * 
	 * @param m
	 * @return csv header
	 */
	public static String csvHeader( LinkedHashMap<String, String> m, String separator ) {

		return concatenateStringArray(m.keySet().toArray(new String[]{""}), separator );
	}
	
	/**
	 * Write ArrayList of LinkedHashMap's of Strings to a csv-file
	 * 
	 * @param al
	 * @param filePath
	 */
	public static void writeCsv( ArrayList<LinkedHashMap<String, String>> al, String separator, String filePath ) {

		PrintWriter writer;
		try {
			writer = new PrintWriter(filePath);
			writer.println( csvHeader( al.get(0), separator ) );
			for (LinkedHashMap<String, String> m: al) {
				writer.println( csvRow( m, separator ) );
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Read ArrayList of LinkedHashMap's of Strings from a csv-file with header
	 * 
	 * @param filePath
	 * @param delimiter
	 * @param separator
	 */
	public static ArrayList<LinkedHashMap<String, String>> readCsv(String filePath, String delimiter, String separator) {

		try {
			char separatorChar = separator.charAt(0);
			//char delimiterChar = delimiter.charAt(0);
                        
                        final CSVParser parser = new CSVParserBuilder()
                                .withSeparator(separatorChar)
                                .withIgnoreQuotations(true)
                                .build();
                        final CSVReader reader = new CSVReaderBuilder(new FileReader(filePath))
                                .withSkipLines(0)
                                .withCSVParser(parser)
                                .build();
                        //CSVParser parser = new CSVParser();//separatorChar, CSVParser.DEFAULT_QUOTE_CHARACTER, '\0', CSVParser.DEFAULT_IGNORE_QUOTATIONS, CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE, CSVParser.DEFAULT_IGNORE_QUOTATIONS, CSVParser.DEFAULT_NULL_FIELD_INDICATOR, java.util.Locale.US);
			//CSVReader reader = new CSVReader(new FileReader(filePath));//, 0, parser);//, delimiterChar );
			String[] nextLine;
                        try {
                            String[] header = reader.readNext();
                            ArrayList<LinkedHashMap<String, String>> out = new ArrayList<LinkedHashMap<String, String>>();
                            int n = header.length;
                            while ((nextLine = reader.readNext()) != null) {
                                    LinkedHashMap<String, String> row = new LinkedHashMap<String, String>();
                                    // nextLine[] is an array of values from the line
                                    for (int i = 0; i < n; i++) {
                                            row.put(header[i].trim(), nextLine[i]);
                                    }
                                    out.add(row);
                            }
        		return out;
                        } catch(IOException ex) {
        			Logger.getLogger(LibIO.class.getName()).log(Level.SEVERE, "Error while reading a line of file: " + filePath, ex);
                        } catch(CsvValidationException ex) {
        			Logger.getLogger(LibIO.class.getName()).log(Level.SEVERE, "Error in CVS format while reading a line of file: " + filePath, ex);
                        }
		} catch (FileNotFoundException ex) {
			Logger.getLogger(LibIO.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(LibIO.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	public static ArrayList<File> findFiles(File folder) {

		ArrayList<File> fileList = new ArrayList<File>();
		File[] files = folder.listFiles();
		for (File file : files ) {
			if ( file.isFile() ) {
				String fileName = file.getName();
				fileList.add(file);
			}
		}
		
		return fileList;
	}

	public static ArrayList<File> findFiles(File folder, String pattern) {

		ArrayList<File> fileList = new ArrayList<File>();
		File[] files = folder.listFiles();
		for (File file : files ) {
			if ( file.isFile() ) {
				String fileName = file.getName();
				if ( fileName.matches(pattern)  ) {
					fileList.add(file);
				}
			}
		}
		
		return fileList;
	}
	
	public static ArrayList<File> findFiles(File folder, String contains, String doesNotContain) {

		ArrayList<File> fileList = new ArrayList<File>();
		File[] files = folder.listFiles();
		for (File file : files ) {
			if ( file.isFile() ) {
				String fileName = file.getName();
				if ( fileName.contains(contains) & !fileName.contains(doesNotContain) ) {
					fileList.add(file);
				}
			}
		}

		return fileList;
	}

	/**
	 *
	 * @param folder
	 * @param contains
	 * @param doesNotContain
	 * @param listOfPossibleLowerCaseExtensions
	 * @return
	 */
	public static ArrayList<File> findFiles(File folder, String contains, String doesNotContain, ArrayList<String> listOfPossibleLowerCaseExtensions ) {

		ArrayList<File> fileList = new ArrayList<File>();
		File[] files = folder.listFiles();
		for (File file : files ) {
			if ( file.isFile() ) {
				String fileName = file.getName();
				if ( fileName.contains(contains) & !fileName.contains(doesNotContain) ) {
					for ( String ext : listOfPossibleLowerCaseExtensions ) {
						if ( fileName.toLowerCase().endsWith(ext) ) {
							fileList.add(file);
						}
					}
				}
			}
		}

		return fileList;
	}

	
	/**
	 * Returns the first match in the searchFolder with the pattern
	 * 
	 * @param searchFolder
	 * @param pattern
	 * @return 
	 */
	public static File findSimilarFile( File searchFolder, String pattern ) {

		File similarFile = null;
		File[] files = searchFolder.listFiles();

		if (files != null) {
			for (File file : files ) {
				if ( file.isFile() ) {
					String fileNameCandidate = file.getName();
					if ( fileNameCandidate.matches(pattern)  ) {
						similarFile = file;
						return similarFile;
					}
				}
			}
		}
		return similarFile;
	}
        
        public static void copyFolder(String sourceDirectoryLocation, String destinationDirectoryLocation) throws IOException {
            Files.walk(Paths.get(sourceDirectoryLocation))
                .forEach(source -> {
                    Path destination = Paths.get(destinationDirectoryLocation, source.toString()
                        .substring(sourceDirectoryLocation.length()));
                    try {
                        Files.copy(source, destination);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

}

