package main.be.ua.mbarbier.slicemap.lib.roi;

import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.org.apache.xpath.internal.NodeSet;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.RoiDecoder;
import ij.plugin.RoiScaler;
import ij.plugin.frame.RoiManager;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import ij.ImageJ;
import ij.gui.OvalRoi;
import ij.io.RoiEncoder;
import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import main.be.ua.mbarbier.slicemap.lib.Lib;
import main.be.ua.mbarbier.slicemap.lib.LibIO;
import main.be.ua.mbarbier.slicemap.lib.LibText;
//import net.lingala.zip4j.io.ZipOutputStream;
//import net.lingala.zip4j.model.ZipParameters;

public class LibEvotecRois {

    // XML specific helper functions
    /**
     * XML: add node with text content to parent node
     *
     * @param doc
     * @param parent
     * @param name
     * @param textValue
     */
    public static Element addTextNode(Document doc, Element parent, String name, String textValue) {
        Element node = doc.createElement(name);
        node.setTextContent(textValue);
        parent.appendChild(node);

        return node;
    }

    /**
     * XML: add node with (no content and) attributes to parent node
     *
     * @param doc
     * @param parent
     * @param name
     * @param attributes
     */
    public static Element addNodeAttr(Document doc, Element parent, String name, LinkedHashMap<String, String> attributes) {

        Element node = doc.createElement(name);
        for (String attr : attributes.keySet()) {
            node.setAttribute(attr, attributes.get(attr));
        }
        parent.appendChild(node);

        return node;
    }

    /**
     * XML: add node with text content and attributes to parent node
     *
     * @param doc
     * @param parent
     * @param name
     * @param textValue
     * @param attributes
     */
    public static Element addTextNodeAttr(Document doc, Element parent, String name, String textValue, LinkedHashMap<String, String> attributes) {

        Element node = doc.createElement(name);
        node.setTextContent(textValue);
        for (String attr : attributes.keySet()) {
            node.setAttribute(attr, attributes.get(attr));
        }
        parent.appendChild(node);

        return node;
    }

    /**
     * Convert the imagej rois to evotec rois use the imagej roi to evotec roi
     * function
     *
     * @return Evotec roi string
     */
    public static String roiToEvotecRegion(Roi roi, int w, int h, float Xlt, float Ylt, float Xrb, float Yrb) {
        String text = "[";
        String typeRegion = "IN-spline";
        String headerRegion = "[" + typeRegion + ";"
                + Integer.toString(w) + "," + Integer.toString(h) + ";"
                + Float.toString(Xlt) + "," + Float.toString(Ylt) + ";" + Float.toString(Xrb) + "," + Float.toString(Yrb) + "]";
        text = text + headerRegion;
        if (roi != null) {
            // Get ROI points
            Polygon polygon = roi.getPolygon();
            int[] x = polygon.xpoints;
            int[] y = polygon.ypoints;
            for (int i = 0; i < x.length; i++) {
                text = text + Float.toString(x[i]) + "," + Float.toString(y[i]);
                if (i < (x.length - 1)) {
                    text = text + ";";
                }
            }
            text = text + "]";
        }

        return text;
    }

    public static String getEvotecHeaderOut(String header_prefix, String[] name_rois) {
        String header = header_prefix;
        ArrayList<String> header_out = new ArrayList<String>();
        header_out.add(header);
        header_out.add("overviewbasename");
        for (String s : name_rois) {
            header_out.add("EVT_Regions_" + s);
        }
        String text = "";
        for (int i = 1; i < header_out.size(); i++) {
            text = text + header_out.get(i);
            if (i < (header_out.size() - 1)) {
                text = text + "\t";
            }
        }

        return text;
    }

    public static String getEvotecHeader(String header_prefix) {
        String header = header_prefix;

        return header;
    }

    public static String getEvotecOverviewFileName(String plateName, String measurementDate, String wellName, String overviewfileExt) {
        return plateName + "__" + measurementDate.replaceAll("[^a-zA-Z0-9.-]", "") + "__" + wellName + "." + overviewfileExt;
    }

    public static String getEvotecOverviewFilePath(String plateName, String measurementDate, String wellName, String overviewfileDir, String overviewfileExt) {
        String overviewbasename = getEvotecOverviewFileName(plateName, measurementDate, wellName, overviewfileExt);
        File file = new File(overviewfileDir, overviewbasename);

        return file.getAbsolutePath();
    }

    public static String getEvotecRow(String plateName, String measurementDate, String wellName, String overviewfilename, int binning, float xltUnit, float yltUnit, float xrbUnit, float yrbUnit, int sizeX, int sizeY) {
        String row = plateName + '\t' + measurementDate + '\t' + wellName + '\t' + overviewfilename + '\t' + Integer.toString(binning) + '\t'
                + Float.toString(xltUnit) + '\t' + Float.toString(yltUnit) + '\t' + Float.toString(xrbUnit) + '\t' + Float.toString(yrbUnit) + '\t'
                + Integer.toString(sizeX) + '\t' + Integer.toString(sizeY);

        return row;
    }

    public static String getEvotecRowOut(String plateName, String measurementDate, String wellName, String overviewfilename, String overviewbasename, int binning, float xltUnit, float yltUnit, float xrbUnit, float yrbUnit, int sizeX, int sizeY, String[] rois_Evotec) {
        String row = getEvotecRow(plateName, measurementDate, wellName, overviewfilename, binning, xltUnit, yltUnit, xrbUnit, yrbUnit, sizeX, sizeY);
        ArrayList<String> row_out = new ArrayList<String>();
        row_out.add(row);
        row_out.add(overviewbasename);
        for (String s : rois_Evotec) {
            row_out.add(s);
        }
        String text = "";
        for (int i = 1; i < row_out.size(); i++) {
            text = text + row_out.get(i);
            if (i < (row_out.size() - 1)) {
                text = text + "\t";
            }
        }

        return text;
    }

    public static String getEvotecRoisRow(float xltUnit, float yltUnit, float xrbUnit, float yrbUnit, int sizeX, int sizeY, Roi[] rois) {
        String evotecRoiString;
        String outString = "";
        ArrayList<String> row_out = new ArrayList<String>();
        for (Roi r : rois) {
            evotecRoiString = roiToEvotecRegion(r, sizeX, sizeY, xltUnit, yltUnit, xrbUnit, yrbUnit);
            row_out.add(evotecRoiString);
        }
        outString = LibText.concatenateStringArray(row_out.toArray(new String[]{""}), "\t");

        return outString;
    }

    public static Document loadXMLFromFile(String xmlPath) {
        DocumentBuilder db;
        Document doc = null;
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = db.parse(xmlPath);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return doc;
    }

	public static LinkedHashMap< String, String > EvotecXMLRegionToCsvMap( String xmlPath ) {
	
        Document doc = loadXMLFromFile(xmlPath);
        XPath xpath = XPathFactory.newInstance().newXPath();
        String expressionNodes = "//Value";
        NodeList nodeList;
        LinkedHashMap< String, String > roiStringMap = new LinkedHashMap<>();
        try {
            nodeList = (NodeList) xpath.evaluate(expressionNodes, doc, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
				// check whether the node is a region description
                String nodeRoiStr = node.getTextContent();
                String nodeRoiName = node.getParentNode().getAttributes().getNamedItem("GroupName").getNodeValue();
				if (nodeRoiName.startsWith("EVT_Regions_")) {
					nodeRoiName = nodeRoiName.replaceAll("EVT_Regions_", "");
					nodeRoiName = nodeRoiName.replaceAll("Hip", "Hp");
					nodeRoiName = nodeRoiName.toLowerCase();
					roiStringMap.put(nodeRoiName, nodeRoiStr);
				}
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return roiStringMap;
	}
	
    public static ArrayList<Roi> EvotecXMLRegionToRois(String xmlPath) {

        Document doc = loadXMLFromFile(xmlPath);
        XPath xpath = XPathFactory.newInstance().newXPath();
        String expressionNodes = "//Value";
        NodeList nodeList;
        ArrayList<Roi> rois = new ArrayList<Roi>();
        try {
            nodeList = (NodeList) xpath.evaluate(expressionNodes, doc, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
				// check whether the node is a region description
                String nodeRoiStr = node.getTextContent();
                String nodeRoiName = node.getParentNode().getAttributes().getNamedItem("GroupName").getNodeValue();
				if (nodeRoiName.startsWith("EVT_Regions_")) {
					nodeRoiName = nodeRoiName.replaceAll("EVT_Regions_", "");
					nodeRoiName = nodeRoiName.replaceAll("Hip", "Hp");
					nodeRoiName = nodeRoiName.toLowerCase();
					rois.add(EvotecStringRegionToRoi(nodeRoiStr, nodeRoiName));
				}
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return rois;
    }

    public static void convertToXML(String imageId, String annotatorId, String inputFolder, String roiFileName, String imageFileName, String xmlPath) {

        String roiFilePath = inputFolder + "/" + roiFileName;
        String imageFilePath = inputFolder + "/" + imageFileName;
        File roiFile = new File(roiFilePath);
//		LinkedHashMap<String,Roi> rois = loadRoi( roiFile );
        LinkedHashMap<String, Roi> rois = loadRoiAlternative(roiFile);

        ImagePlus imp = IJ.openImage(imageFilePath);
        writeXmlRegions(imp, rois, imageId, annotatorId, roiFileName, roiFilePath, xmlPath);

    }

    public static void convertToXMLPath(String imageId, String annotatorId, String roiFilePath, String imageFilePath, String xmlPath) {

        File roiFile = new File(roiFilePath);
//		LinkedHashMap<String,Roi> rois = loadRoi( roiFile );
        LinkedHashMap<String, Roi> rois = loadRoiAlternative(roiFile);

        ImagePlus imp = IJ.openImage(imageFilePath);
        writeXmlRegions(imp, rois, imageId, annotatorId, "", roiFilePath, xmlPath);

    }

    public static void writeCsvRegions(ImagePlus imp, LinkedHashMap<String, Roi> rois, String imageId, String annotatorId, String roiFileName, String roiFilePath, String csvPath) {

        ArrayList<LinkedHashMap<String, String>> evtRois = new ArrayList<LinkedHashMap<String, String>>();
        int w = imp.getWidth();
        int h = imp.getHeight();
        float Xlt = 0;
        float Ylt = 0;
        float Xrb = w - 1;
        float Yrb = h - 1;

        for (String key : rois.keySet()) {
            LinkedHashMap<String, String> evtRoiMap = new LinkedHashMap<String, String>();
            Roi roi = rois.get(key);
            String roiName = roi.getName();
            roiName = "EVT_Regions_" + roiName;
            //System.out.println(roiName);
            String evtRoi = LibEvotecRois.roiToEvotecRegion(roi, w, h, Xlt, Ylt, Xrb, Yrb);
            //System.out.println(evtRoi);
            evtRoiMap.put("imageId", imageId);
            evtRoiMap.put("annotatorId", annotatorId);
            evtRoiMap.put("roiFileName", roiFileName);
            evtRoiMap.put("roiFilePath", roiFilePath);
            evtRoiMap.put("roiName", roiName);
            evtRoiMap.put("roi", evtRoi);
            evtRois.add(evtRoiMap);
        }
        LibIO.writeCsv(evtRois, "	", csvPath);
    }

    public static void writeXmlRegions(ImagePlus imp, LinkedHashMap<String, Roi> rois, String imageId, String annotatorId, String roiFileName, String roiFilePath, String xmlPath) {

        ArrayList<LinkedHashMap<String, String>> evtRois = new ArrayList<LinkedHashMap<String, String>>();
        int w = imp.getWidth();
        int h = imp.getHeight();
        float Xlt = 0;
        float Ylt = 0;
        float Xrb = w - 1;
        float Yrb = h - 1;

        for (String key : rois.keySet()) {
            LinkedHashMap<String, String> evtRoiMap = new LinkedHashMap<String, String>();
            Roi roi = rois.get(key);
            String roiName = roi.getName();
            roiName = "EVT_Regions_" + roiName;
            //System.out.println(roiName);
            String evtRoi = LibEvotecRois.roiToEvotecRegion(roi, w, h, Xlt, Ylt, Xrb, Yrb);
            //System.out.println(evtRoi);
            evtRoiMap.put("imageId", imageId);
            evtRoiMap.put("annotatorId", annotatorId);
            evtRoiMap.put("roiFileName", roiFileName);
            evtRoiMap.put("roiFilePath", roiFilePath);
            evtRoiMap.put("roiName", roiName);
            evtRoiMap.put("roi", evtRoi);
            evtRois.add(evtRoiMap);
        }
        try {
            String xml = LibEvotecRois.RoisToEvotecXMLRegion(rois, xmlPath, w, h, Xlt, Ylt, Xrb, Yrb);
            LibIO.writeTextFile(xml, xmlPath);
        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }

    }

    public static LinkedHashMap<String, Roi> loadRoiAlternative(File roiFile) {

        LinkedHashMap<String, Roi> roiM = new LinkedHashMap<String, Roi>();

        RoiDecoder rd = new RoiDecoder(roiFile.getAbsolutePath());
        try {
            ZipFile zipFile = new ZipFile(roiFile);
            List<FileHeader> hs = zipFile.getFileHeaders();
            for (FileHeader h : hs) {
                ZipInputStream z = zipFile.getInputStream(h);
                ArrayList<Byte> bytesa = new ArrayList<Byte>();
                int bi = z.read();
                byte b;
                while (bi > -1) {
                    b = (byte) bi;
                    bytesa.add(b);
                    bi = z.read();
                }
                //byte[] be = new byte[]{b};
                byte[] bytes = new byte[bytesa.size()];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = bytesa.get(i);
                }
                //bytesa.toArray(be);
                Roi roi = RoiDecoder.openFromByteArray(bytes);
                roiM.put(roi.getName(), roi);
            }
        } catch (ZipException | IOException e) {
            e.printStackTrace();
        } finally {
        }
        //zipFile.extractAll( roiFile.getParent() + "/ziptest" );

        Lib.sortRois(roiM);

        return roiM;
    }

    public static void saveRoiAlternative(File roiFile, LinkedHashMap<String, Roi> roiM) {

        
        try {
            ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( roiFile.getAbsolutePath() ) );
            RoiEncoder re = new RoiEncoder( zos );
            for (String key : roiM.keySet()) {
                Roi roi = roiM.get(key);
				if (roi != null) {
					ZipEntry ze = new ZipEntry( key + ".roi" );
					zos.putNextEntry( ze );
					re.write(roi);
				}
            }
            zos.finish();
            zos.close();
        } catch (IOException ex) {
            Logger.getLogger(LibEvotecRois.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static LinkedHashMap<String, Roi> loadRoi(File roiFile) {

        RoiManager rm = RoiManager.getInstance2();
        if (rm == null) {
            rm = new RoiManager(false);
        } else {
            rm.runCommand("Delete");
        }
        rm.setVisible(false);

        rm.runCommand("Open", roiFile.getAbsolutePath());
        Roi[] rois = rm.getRoisAsArray();
        LinkedHashMap<String, Roi> roiM = new LinkedHashMap<String, Roi>();
        ArrayList<String> keys = new ArrayList<String>();
        for (Roi roi : rois) {
            String key = roi.getName();
            keys.add(key);
        }
        //TODO how to sort in java 1.7: this is not crucial does it break something??
        //keys.sort(null);

        for (String key : keys) {
            for (Roi roi : rois) {
                if (roi.getName().equals(key)) {
                    roiM.put(key, roi);
                }
            }
        }
        Lib.sortRois(roiM);

        return roiM;
    }

    public static String RoisToEvotecXMLRegion(LinkedHashMap<String, Roi> rois, String xmlPath, int w, int h, float Xlt, float Ylt, float Xrb, float Yrb) throws ParserConfigurationException, TransformerException {

        System.out.println("START RoisToEvotecXMLRegion");

        String rootName = "AssayDefinition";
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.newDocument();

        // ROOT
        Element eRoot = doc.createElement(rootName);
        eRoot.setAttribute("xmlns", "http://www.perkinelmer.com/Columbus");
        eRoot.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        eRoot.setAttribute("xsi:schemaLocation", "http://www.perkinelmer.com/Columbus AssayDefinitionV1.xsd");
        eRoot.setAttribute("version", "1");
        eRoot.setAttribute("code_version", "2011-04-20");

        // CHILDS:
        // Plate node
        Element plateNode = doc.createElement("Plate");
        addTextNode(doc, plateNode, "PlateRows", "1");
        addTextNode(doc, plateNode, "PlateColumns", "1");

        // Wells node
        Element wellsNode = doc.createElement("Wells");
        LinkedHashMap<String, String> m = new LinkedHashMap<String, String>();
        m.put("WellID", "A1");
        m.put("Row", "1");
        m.put("Column", "1");
        Element wellNode = addNodeAttr(doc, wellsNode, "Well", m);

        for (String roiKey : rois.keySet()) {
            m = new LinkedHashMap<String, String>();
            m.put("ContentID", roiKey);
            Element roiNode = addNodeAttr(doc, wellNode, "Content", m);
            m = new LinkedHashMap<String, String>();
            m.put("Type", "string");
            String evtString = roiToEvotecRegion(rois.get(roiKey), w, h, Xlt, Ylt, Xrb, Yrb);
            addTextNodeAttr(doc, roiNode, "Value", evtString, m);
            //XmlInsert( node = doc.Wells , insertion = xmlraw("<Content ContentID=\"" & roiName & "\">") )
            //XmlInsert( node = doc.Wells , insertion = xmlraw("<Value Type=\"string\">\n" & roiString & "\n</Value>")  )
            //XmlInsert( node = doc.Wells , insertion = xmlraw("</Content>") ) 
        }

        // Registration node
        Element registrationNode = doc.createElement("Registration");
        for (String roiKey : rois.keySet()) {
            m = new LinkedHashMap<String, String>();
            m.put("GroupName", roiKey);
            m.put("Category", "UNDEFINED");
            m.put("Type", "AREA");
            m.put("ID", roiKey);
            addNodeAttr(doc, registrationNode, "Content", m);
            //xmlraw( "<Content GroupName=\"" & roiName & "\" Category=\"UNDEFINED\" Type=\"AREA\" ID=\"" & roiName & "\" />" ) 
        }

        eRoot.appendChild(plateNode);
        eRoot.appendChild(registrationNode);
        eRoot.appendChild(wellsNode);
        doc.appendChild(eRoot);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new StringWriter());

        //t.setParameter(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "5");
        transformer.transform(source, result);
        //System.out.println( result.getWriter().toString() );

        System.out.println("END RoisToEvotecXMLRegion");

        return result.getWriter().toString();
    }

    public static ArrayList<Roi> getEvotecRoiArray(ImagePlus imp, int[] impOffset, double scaleImp, String evtPath, double scaleEvtRoi) {
        ArrayList<Roi> rois = EvotecXMLRegionToRois( evtPath );
        rois = scaleRois(rois, scaleImp / scaleEvtRoi, impOffset);
        //impRois = getOverlayImage( rois, imp );
        //impRois.setTitle("Evotec Rois");
        return rois;
    }

    public static Roi EvotecStringRegionToRoi(String roiStr, String roiName) {
        ArrayList<Integer> xp = new ArrayList<Integer>();
        ArrayList<Integer> yp = new ArrayList<Integer>();
        String[] roiStrSplit = roiStr.split("]");
        String[] evtHeaderStr = roiStrSplit[0].split(";");
        evtHeaderStr = Arrays.copyOfRange(evtHeaderStr, 3, evtHeaderStr.length);
        String[] evtCoordStrList = roiStrSplit[1].split(";");
        for (int i = 0; i < evtCoordStrList.length; i++) {
            String s = evtCoordStrList[i];
            String[] xy = s.split(",");
            xp.add((int) Math.round(Double.valueOf(xy[0])));
            yp.add((int) Math.round(Double.valueOf(xy[1])));
        }

        PolygonRoi roi = new PolygonRoi(Lib.IntegerArrayToIntArray(xp), Lib.IntegerArrayToIntArray(yp), xp.size(), PolygonRoi.FREEROI);
        //roi.fitSpline();
        roi.setName(roiName);

        return roi;
    }

    public static ArrayList<Roi> scaleRois(ArrayList<Roi> rois, double s, int[] offset) {
        for (int i = 0; i < rois.size(); i++) {
            Roi roi = rois.get(i);
            if (s != 1.0) {
                roi = RoiScaler.scale(roi, s, s, false);
            }
            Rectangle rec = roi.getBounds();
            roi.setLocation(rec.getX() + offset[0], rec.getY() + offset[1]);
            roi.setName(rois.get(i).getName());
            //roi.setStrokeColor( roiColor()[ rois.get(i).getName() ] );
            roi.setStrokeWidth(2);
            rois.set(i, roi);
        }
        return rois;
    }

	/**
	 *	For the TRIAD meeting: With this function we want to convert all the Evotec ROIs defined in their XML format to ImageJ ROIs and save them.
	 *	The ROI XML definitions (inside assaydefinition files) are located in subfolders (each subfolder a slice image).
	 * 
	 * @param parentFolder
	 * @param outputFolder
	 * @param prefix
	 */
	public static void test_convertEvotecXmlRegionToRois( File parentFolder, File outputFolder, String prefix ) {

		String xmlPath = null;

		// Find all assaydefinition files
		File[] fileList = parentFolder.listFiles();
		ArrayList<String> extList = new ArrayList<>();
		extList.add("xml");
		for ( int i = 0; i < fileList.length; i++ ) {
			File subfolder = fileList[i];
			String plateName = subfolder.getName().substring(0, 4);
			File subfolder2 = subfolder.listFiles()[0];
			if ( subfolder2.isDirectory() ) {
				ArrayList<File> assayDefFileList = LibIO.findFiles( subfolder2, "assaydefinition.xml", "blaaat", extList );
				for ( File assayDefFile : assayDefFileList ) {
					try {
						xmlPath = assayDefFile.toURI().toURL().toExternalForm();
					} catch (MalformedURLException ex) {
						IJ.log( ex.getMessage() );
						Logger.getLogger(LibEvotecRois.class.getName()).log(Level.SEVERE, null, ex);
					}
					IJ.log("Processing file : " + xmlPath);
					try {
						ArrayList<Roi> roiList = EvotecXMLRegionToRois( xmlPath );
						IJ.log(roiList.toString());
						File roiFile = new File( outputFolder.getAbsolutePath() + File.separator + plateName + ".zip" );
						LinkedHashMap<String, Roi> roiM = new LinkedHashMap<>();
						for (Roi roi : roiList ) {
							roiM.put( roi.getName(), roi );
						}
						IJ.log("Write ImageJ ROIs: " + roiFile.getAbsolutePath() );
						saveRoiAlternative( roiFile, roiM);
						File roiCsvFile = new File( outputFolder.getAbsolutePath() + File.separator + plateName + ".csv" );
						LinkedHashMap< String, String > roiStringMap = EvotecXMLRegionToCsvMap( xmlPath );
						ArrayList< LinkedHashMap< String, String > > roiStringMapList = new ArrayList<>();
						roiStringMapList.add( roiStringMap );
						IJ.log("Write csv: " + roiCsvFile.getAbsolutePath() );
						LibIO.writeCsv( roiStringMapList, "	", roiCsvFile.getAbsolutePath() );
					} catch( Exception e ) {
						IJ.log("exception");
						continue;
					}
				}
			}
		}
	}

    public static void main(String[] args) {

        /*
		File roiFile = new File("F:/tau_analysis/test.zip");
        LinkedHashMap<String, Roi> roiM = new LinkedHashMap<String, Roi>();
        roiM.put( "oval", new OvalRoi( 45, 45, 23, 12 ) );
        roiM.put( "oval2", new OvalRoi( 10, 45, 23, 12 ) );
        roiM.put( "oval3", new OvalRoi( 45, 20, 5, 30 ) );
                
        saveRoiAlternative( roiFile, roiM);
        LinkedHashMap<String, Roi> rois = loadRoiAlternative( roiFile );
        ImagePlus imp = IJ.createImage("Image", 100, 100, 1, 8);
        for ( String key : rois.keySet() ) {
            Roi roi = rois.get(key);
            imp.setRoi(roi);
            imp.setOverlay(roi, Color.yellow, 3, Color.green);
        }
        imp.setHideOverlay(false);
        imp.show();
		*/
       ImageJ imagej = new ImageJ();

		IJ.log("START RUN");
 		File parentFolder = new File( "G:\\data\\EVT\\P301S_Characterization_Columbus part2" );
		File outputFolder = new File( "G:\\data\\EVT" );
		test_convertEvotecXmlRegionToRois( parentFolder, outputFolder, "evt_" );
		IJ.log("END RUN");

		
    }
}
