package com.personal.schoolfinder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class GreatSchoolsNearbyXMLParser {

    private static final String ns = null;
    
    public List<School> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }

    private List<School> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<School> entries = new ArrayList<School>();

        parser.require(XmlPullParser.START_TAG, ns, "schools");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("school")) {
                entries.add(readSchool(parser));
            } else {
                skip(parser);
            }
        }  
        return entries;
    }
    
    public static class School {
        public final String schoolName;
        public final int gsRating;
        public final String address;
        public final double lat;
        public final double lon;

        private School(String schoolName, int gsRating, String address, double lat, double lon) {
            this.schoolName = schoolName;
            this.gsRating = gsRating;
            this.address = address;
            this.lat = lat;
            this.lon = lon;
        }
    }
    
	 // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
	 // to their respective "read" methods for processing. Otherwise, skips the tag.
	private School readSchool(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "school");
		String schoolName = null;
		int gsRating = 0;
		String address = null;
		double lat = 0, lon = 0;
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("name")) {
				schoolName = readSchoolName(parser);
			} else if (name.equals("gsRating")) {
				gsRating = readRating(parser);
			} else if (name.equals("address")) {
				address = readAddress(parser);
			} else if (name.equals("lat")) {
				lat = readLat(parser);
			} else if (name.equals("lon")) {
				lon = readLon(parser);
			} else {
				skip(parser);
			}
		}
		return new School(schoolName, gsRating, address, lat, lon);
	}
    
	private String readSchoolName(XmlPullParser parser) throws IOException, XmlPullParserException {
	    parser.require(XmlPullParser.START_TAG, ns, "name");
	    String schoolName = readText(parser);
	    parser.require(XmlPullParser.END_TAG, ns, "name");
	    return schoolName;
	}	

	private int readRating(XmlPullParser parser) throws IOException, XmlPullParserException {
	    parser.require(XmlPullParser.START_TAG, ns, "gsRating");
	    int rating = readInt(parser);
	    parser.require(XmlPullParser.END_TAG, ns, "gsRating");
	    return rating;
	}	

	private String readAddress(XmlPullParser parser) throws IOException, XmlPullParserException {
	    parser.require(XmlPullParser.START_TAG, ns, "address");
	    String address = readText(parser);
	    parser.require(XmlPullParser.END_TAG, ns, "address");
	    return address;
	}	

	private double readLat(XmlPullParser parser) throws IOException, XmlPullParserException {
	    parser.require(XmlPullParser.START_TAG, ns, "lat");
	    double lat = readDouble(parser);
	    parser.require(XmlPullParser.END_TAG, ns, "lat");
	    return lat;
	}	

	private double readLon(XmlPullParser parser) throws IOException, XmlPullParserException {
	    parser.require(XmlPullParser.START_TAG, ns, "lon");
	    double lon = readDouble(parser);
	    parser.require(XmlPullParser.END_TAG, ns, "lon");
	    return lon;
	}	

	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
	    String result = "";
	    if (parser.next() == XmlPullParser.TEXT) {
	        result = parser.getText();
	        parser.nextTag();
	    }
	    return result;
	}	

	private int readInt(XmlPullParser parser) throws IOException, XmlPullParserException {
	    int result = 0;
	    if (parser.next() == XmlPullParser.TEXT) {
	        result = Integer.valueOf(parser.getText());
	        parser.nextTag();
	    }
	    return result;
	}	

	private double readDouble(XmlPullParser parser) throws IOException, XmlPullParserException {
	    double result = 0;
	    if (parser.next() == XmlPullParser.TEXT) {
	        result = Double.valueOf(parser.getText());
	        parser.nextTag();
	    }
	    return result;
	}	
	
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
