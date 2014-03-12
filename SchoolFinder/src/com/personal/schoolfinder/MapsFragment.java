package com.personal.schoolfinder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.personal.schoolfinder.GreatSchoolsNearbyXMLParser.School;

public class MapsFragment extends Fragment {
	
	public static final String TAG = MapsFragment.class.getSimpleName();

    public static final String NAV_MENU_ITEM = "nav_menu_item";
    private GoogleMap googleMap;
    private Marker customMarker;
    private String url;
    
    private GreatSchoolsNearbyXMLParser greatSchoolsNearbyXMLParser = new GreatSchoolsNearbyXMLParser();
    private List<School> schools = new ArrayList<GreatSchoolsNearbyXMLParser.School>();    

    public MapsFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
	public void onResume() {
    	super.onResume();
    	
		LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

    	Criteria criteria = new Criteria();
    	String provider = locationManager.getBestProvider(criteria, true);

    	Location location = locationManager.getLastKnownLocation(provider);
    	double latitude = location.getLatitude();
    	double longitude = location.getLongitude();	
    	
    	// TODO zoom map based on last(?) marker
    	LatLng latLng = new LatLng(latitude, longitude);        	        	
    	googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    	googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
		
    	// TODO get location updates
    	
    	State state = State.CALIFORNIA; 
    	Geocoder geoCoder = new Geocoder(getActivity());
    	try {
			List<Address> addresses = geoCoder.getFromLocation(latitude, longitude, 1);
			if (addresses.size() > 0) {
				state = State.valueOfName(addresses.get(0).getAdminArea());					
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
    	// TODO redo search if zoom changes
    	// TODO be smart about redoing search if nothing has changed from before
    	
    	// Get the setting for private schools
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    	boolean hidePrivateSchools = sharedPref.getBoolean("hide_private_schools", true);
    	String schoolType = "public";
    	if (!hidePrivateSchools) {
    		schoolType += "-private";
    	}
    	
		// TODO don't use SharedPrefs for levelCode - use in memory instead
    	// Get the setting for levelCode
    	String levelCode = sharedPref.getString("level_code", null);
    	
//      String url = "http://api.greatschools.org/schools/nearby?key=jt2vzgktkiqdklnavhizpaxx&state=CA&lat=37.758862&lon=-122.411406";
        url = "http://api.greatschools.org/schools/nearby?key=jt2vzgktkiqdklnavhizpaxx&" +
        		"state=" + state.getAbbreviation() +"&" +
        		"lat=" + latitude + "&" +
        		"lon=" + longitude + "&" +
        		"&schoolType=" + schoolType +
        		"&minimumSchools=5" +
        		"&radius=10" +
        		"&limit=10";
        
        if (levelCode != null) {
        	url += "&levelCode=" + levelCode;
        }
        
        Log.d(TAG, "onResume: " + url);        
        
        if (googleMap != null) {
        	googleMap.clear();
        	
            RequestQueue queue = Volley.newRequestQueue(getActivity());
            StringRequest request = new StringRequest(Request.Method.GET, url,
            		new Response.Listener<String>() {

						@Override
						public void onResponse(String response) {
							Log.d(TAG, response);
							drawMapMarkers(response);
						}
					},
					new Response.ErrorListener() {

						@Override
						public void onErrorResponse(VolleyError error) {
							Toast.makeText(getActivity(), R.string.error_getting_schools, Toast.LENGTH_LONG).show();
						}
					});
        	queue.add(request);
        }
		
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_maps, container, false);
                
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        if (status != ConnectionResult.SUCCESS) {
        	int requestCode = 10;
        	Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, getActivity(), requestCode);
        	dialog.show();        	
        } else {
        	MapFragment fm = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        	googleMap = fm.getMap();
        	googleMap.setMyLocationEnabled(true);
        	googleMap.setInfoWindowAdapter(new CustomInfoWindow());        	
        }
                
        int i = getArguments().getInt(NAV_MENU_ITEM);
        String nav_menu_item = getResources().getStringArray(R.array.nav_menu_array)[i];
        getActivity().setTitle(nav_menu_item);
        
        return rootView;
    }
    
    private void drawMapMarkers(String response) {
		InputStream in = new ByteArrayInputStream(response.getBytes());
		try {
			schools = greatSchoolsNearbyXMLParser.parse(in);
			Log.d(TAG, "Number of schools = " + schools.size());
			
			for (School school : schools) {
				LatLng position = new LatLng(school.lat, school.lon);
				
				View marker = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker, null);
				TextView ratingTextView = (TextView) marker.findViewById(R.id.num_txt);
				ratingTextView.setText(school.gsRating + "");

				customMarker = googleMap.addMarker(new MarkerOptions()
								.position(position)
								.title(school.schoolName)
								.snippet(school.address)
								.icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(getActivity(), marker))));
				
				if (school.schoolName.equals(schools.get(0).schoolName)) {
					customMarker.showInfoWindow();
				}
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}            	
    }
    
	// Convert a view to bitmap
	public static Bitmap createDrawableFromView(Context context, View view) {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		view.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
		view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
		view.buildDrawingCache();
		Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
 
		Canvas canvas = new Canvas(bitmap);
		view.draw(canvas);
 
		return bitmap;
	}  
	
	public class CustomInfoWindow implements InfoWindowAdapter {
		
		private final View customWindow = getActivity().getLayoutInflater().inflate(R.layout.custom_info_window, null);

		@Override
		public View getInfoContents(Marker marker) {
			return null;
		}

		@Override
		public View getInfoWindow(Marker marker) {			
			TextView schoolName = (TextView) customWindow.findViewById(R.id.schoolName);
			schoolName.setText(marker.getTitle());

			TextView address = (TextView) customWindow.findViewById(R.id.schoolAddress);
			address.setText(marker.getSnippet());

			return customWindow;
		}

	}	

}
