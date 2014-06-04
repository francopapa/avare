/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.util.Xml;

//	XML Parser that reads KML formatted files. The Placemark definitions are extracted
//	and converted to user defined waypoints. The essential syntax is as follows:
//
//	<kml>
//		<Document>
//			<Placemark> +
//				<name/>
//				<description/>
//				<Point>
//					<coordinates/>
//				</Point>
//			</Placemark>
//			<Folder>
//				<Placemark> +
//					<name/>
//					<description/>
//					<Point>
//						<coordinates/>
//					</Point>
//				</Placemark>
//			</Folder>
//		</Document>
//	</kml>

/***
 * This class reads a file in kml format and extracts all the placemarks.
 * 
 * @author Ron
 *
 */
public class KmlPlacemarkParser {
    private static final String NS = null;
    private static final String KML = "kml";
    private static final String DOCUMENT = "Document";
    private static final String PLACEMARK = "Placemark";
    private static final String FOLDER = "Folder";
    private static final String POINT = "Point";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String COORDINATES = "coordinates";

    // Constructor. Instantiate the parser and start reading from the input stream
    //
    public List<Placemark> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readKmlData(parser);
        } finally {
            in.close();
        }
    }

    // The root tag should be "<kml>", search for the opening "<Document>" tag
    //
    private List<Placemark> readKmlData(XmlPullParser parser) throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, NS, KML);	// We must be inside the <kml> tag now
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(DOCUMENT)) {
                return readDocument(parser);
            } else {
                skip(parser);
            }
        }  
        return null;
    }

    // We are in the document tag, now search for either "Folder" or "Placemark"
    //
    private List<Placemark> readDocument(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Placemark> entries = new ArrayList<Placemark>();

        parser.require(XmlPullParser.START_TAG, NS, DOCUMENT);	// Must be inside of <Document> now
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(PLACEMARK)) {
                entries.add(readPlacemark(parser));
            } else if (name.equals(FOLDER)) {
                return readFolder(parser);
            } else {
                skip(parser);
            }
        }  
        return entries;
    }

    // Found "Folder", now search for the "Placemark"
    //
    private List<Placemark> readFolder(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, FOLDER);	// We must be inside <Folder> at this point

        List<Placemark> entries = new ArrayList<Placemark>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals(PLACEMARK)) {
                entries.add(readPlacemark(parser));
            } else {
                skip(parser);
            }
        }  
        return entries;
    }

    // Class to hold the definition of a placemark
    //
    public static class Placemark {
        public final String mName;
        public final String mDescription;
        public final float  mLat;
        public final float  mLon;
        public final float  mAlt;

        private Placemark(String name, String description, float lat, float lon, float alt) {
            this.mName = name;
            this.mDescription = description;
            this.mLat = lat;
            this.mLon = lon;
            this.mAlt = alt;
        }
    }
    
    // We are inside a "Placemark" tag - read the details
    //
    private Placemark readPlacemark(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, PLACEMARK);
        String name = null;
        String description = null;
        float lat = 0;
        float lon = 0;
        float alt = 0;
        
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String nodeName = parser.getName();
            if (nodeName.equals(NAME)) {
                name = readName(parser);
            } else if (nodeName.equals(DESCRIPTION)) {
                description = readDescription(parser);
            } else if (nodeName.equals(POINT)) {
            	String coordinates = readPoint(parser);
            	String[] values = coordinates.split(",");
            	lat = Float.parseFloat(values[1]);
                lon = Float.parseFloat(values[0]);
                alt = Float.parseFloat(values[2]);
            } else {
                skip(parser);
            }
        }
        return new Placemark(name, description, lat, lon, alt);
    }

    // Inside of the "Point" tag, we only care about the coordinates
    //
    private String readPoint(XmlPullParser parser) throws XmlPullParserException, IOException {
    	String coordinates = null;
        parser.require(XmlPullParser.START_TAG, NS, POINT);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(COORDINATES)) {
                coordinates = readCoordinates(parser);
            } else {
                skip(parser);
            }
        }  
        return coordinates;
    }

    // Extract the "name"
    //
    private String readName(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, NAME);
        String name = readText(parser);
        parser.require(XmlPullParser.END_TAG, NS, NAME);
        return name;
    }
      
    // Extract the "description"
    //
    private String readDescription(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, DESCRIPTION);
        String name = readText(parser);
        parser.require(XmlPullParser.END_TAG, NS, DESCRIPTION);
        return name;
    }

    // Extract the "coordinates"
    //
    private String readCoordinates(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, COORDINATES);
        String name = readText(parser);
        parser.require(XmlPullParser.END_TAG, NS, COORDINATES);
        return name;
    }

    // Read the text from the current tag
    //
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    /***
     * Skip this next entire sub-block of XML tags
     * 
     * @param parser
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                depth--;
                break;
            case XmlPullParser.START_TAG:
                depth++;
                break;
            }
        }
     }
}
