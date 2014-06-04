/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.place;

import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.List;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Projection;
import com.ds.avare.storage.KmlPlacemarkParser;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.Helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

/***
 * Points Of Interest.
 * 
 * The POI is a user defined collection of locations with a name. The collection is read from a KML formatted
 * XML file at startup. If configured to display, the location is on the active chart when in visual display with
 * a centralized icon with the name on top and the distance/direction specified below
 * @author Ron
 *
 */
public class POI {
	Paint 			mPaint;		// Paint object used to do the display work
	List<KmlPlacemarkParser.Placemark> mPoints;	// Collection of points of interest
	GpsParams		mGpsParams;	// Current location
	float			mPix;
	float 			m2Pix;
	float 			m5Pix;
	float 			m8Pix;
	float 			m15Pix;
	float 			m25Pix;
	
	/***
	 * public constructor for user defined PointsOfInterest
	 * @param fileName Name of the coordinate file to read
	 */
	public POI(Context context, String fileName) {
		KmlPlacemarkParser parser = new KmlPlacemarkParser();
		try {
			FileInputStream  kmlStream = new FileInputStream(fileName);
			mPoints = parser.parse(kmlStream);
		} catch (Exception e) {

		} finally  {
		
		}
		
		// Allocate and initialize the paint object
		mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Style.STROKE);
	}

	// Calculate some of the dispay size constants
	public void setDipToPix(float dipToPix) {
        mPix = dipToPix;
        m2Pix = 2 * mPix;
        m5Pix = 5 * mPix;
        m8Pix = 8 * mPix;
        m15Pix = 15 * mPix;
        m25Pix = 25 * mPix;
	}
	
	/***
	 * Time to draw all of our points on the display
	 * 
	 * @param canvas Where to draw
	 * @param face Typeface to use
	 * @param origin Top/Left origin of the logical display
	 */
	public void draw(Canvas canvas, Typeface face, Origin origin) {
		
		// If there are no points to display, then just get out of here
		if(null == mPoints) {
			return;
		}
		
		// Loop through every point that we have
		//
		for(int idx = 0; idx < mPoints.size(); idx++) {

			// Get the placemark
			KmlPlacemarkParser.Placemark p = mPoints.get(idx);

			// Direction and distance from current location
			String dstBrg = whereAndHowFar(p);
			
			// Map the lat/lon to the x/y of the current canvas
			float x = (float) origin.getOffsetX(p.mLon);
			float y = (float) origin.getOffsetY(p.mLat);

			// Draw the filled circle, centered on the point
	        mPaint.setStyle(Style.FILL);
	        mPaint.setColor(Color.CYAN);
	        mPaint.setAlpha(0x9F);
	        canvas.drawCircle(x, y, (float) m8Pix, mPaint);

	        // A black ring around it to highlight it a bit
	        mPaint.setStyle(Style.STROKE);
	        mPaint.setColor(Color.BLACK);
	        mPaint.setStrokeWidth(m2Pix);
	        canvas.drawCircle(x, y, (float) m8Pix, mPaint);
	        
	        // Set the display text properties
	        mPaint.setStyle(Style.FILL);
	        mPaint.setTypeface(face);
	        mPaint.setTextSize(m15Pix);
	        mPaint.setColor(Color.WHITE);
	        mPaint.setShadowLayer(2, 3, 3,Color.BLACK );

	        // Draw the name above
	        drawShadowedText(canvas, mPix, mPaint, p.mName, x, y - m25Pix);
	        
	        // and the distance/brg below
	        drawShadowedText(canvas, mPix, mPaint, dstBrg, x, y + m25Pix);
		}
	}

	// Utility function to draw text with a curved corner rectangle background.
	// This will be replaced with the "helper" object in the refactored code
	//
    private void drawShadowedText(Canvas canvas, float mDipToPix, Paint paint, String text, float x, float y) {
    	Rect mTextSize = new Rect();	// This should not be happening during draw
    	RectF mShadowBox = new RectF();	// allocation of paint objects is too slow
    	Paint mShadowPaint = new Paint();	// should  be done statically at create time
    	
        final int XMARGIN = (int) (5 * mDipToPix);
        final int YMARGIN = (int) (5 * mDipToPix);
        final int SHADOWRECTRADIUS = (int) (5 * mDipToPix);
        paint.getTextBounds(text, 0, text.length(), mTextSize);
        
        mShadowBox.bottom = mTextSize.bottom + YMARGIN + y - (mTextSize.top / 2);
        mShadowBox.top    = mTextSize.top - YMARGIN + y - (mTextSize.top / 2);
        mShadowBox.left   = mTextSize.left - XMARGIN + x  - (mTextSize.right / 2);
        mShadowBox.right  = mTextSize.right + XMARGIN + x  - (mTextSize.right / 2);

        
        mShadowPaint.setColor(Color.DKGRAY);
        mShadowPaint.setAlpha(0x90);
        canvas.drawRoundRect(mShadowBox, SHADOWRECTRADIUS, SHADOWRECTRADIUS, mShadowPaint);
        canvas.drawText(text,  x - (mTextSize.right / 2), y - (mTextSize.top / 2), paint);
    }
    
    // Calculate the distance and bearing to the point from our current location
    //
    String whereAndHowFar(KmlPlacemarkParser.Placemark p) {
    	double brg = Projection.getStaticBearing(mGpsParams.getLongitude(), mGpsParams.getLatitude(), p.mLon, p.mLat);
    	double dst = Projection.getStaticDistance(mGpsParams.getLongitude(), mGpsParams.getLatitude(), p.mLon, p.mLat);
    	
    	brg = Helper.getMagneticHeading(brg, mGpsParams.getDeclinition());
    	return String.format("%03d %03d", (int) dst, (int) brg);
    }
    
    // Update our current location based upon the provided GPS settings
    //
    public void setGpsParams(GpsParams gpsParams) {
    	mGpsParams = gpsParams;
    }
    
    // Search our list for a name that closely matches what is passed in.
    public void search(String name, LinkedHashMap<String, String> params) {
    	if(null != mPoints) {
    		final String uName = name.toUpperCase();
    		for(int idx = 0; idx < mPoints.size(); idx++) {
    			KmlPlacemarkParser.Placemark p = mPoints.get(idx);
    			final String mName = p.mName.toUpperCase();
    			if (mName.startsWith(uName)) {
    		        StringPreference s = new StringPreference(Destination.KML, "TYPE", p.mName, p.mName);
    		        s.putInHash(params);
    			}
    		}
    	}
    }
    
    public KmlPlacemarkParser.Placemark getPlacemark(String name){
    	if(null != mPoints) {
    		final String uName = name.toUpperCase();
    		for(int idx = 0; idx < mPoints.size(); idx++) {
    			KmlPlacemarkParser.Placemark p = mPoints.get(idx);
    			final String mName = p.mName.toUpperCase();
    			if (mName.equals(uName)) {
    				return p;
    			}
    		}
    	}
    	return null;
    }
}
