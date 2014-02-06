package com.personal.schoolfinder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.personal.schoolfinder.GreatSchoolsNearbyXMLParser.School;

public class MapsFragment extends Fragment {
	
	public static final String TAG = MapsFragment.class.getSimpleName();

    public static final String NAV_MENU_ITEM = "nav_menu_item";
    private GoogleMap googleMap;
    
    private GreatSchoolsNearbyXMLParser greatSchoolsNearbyXMLParser = new GreatSchoolsNearbyXMLParser();
    private List<School> schools = new ArrayList<GreatSchoolsNearbyXMLParser.School>();    

    public MapsFragment() {
        // Empty constructor required for fragment subclasses
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
        	getActivity();
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
        	
            RequestQueue queue = Volley.newRequestQueue(getActivity());
//          String url = "http://api.greatschools.org/schools/nearby?key=jt2vzgktkiqdklnavhizpaxx&state=CA&lat=37.758862&lon=-122.411406";
            String url = "http://api.greatschools.org/schools/nearby?key=jt2vzgktkiqdklnavhizpaxx&" +
            		"state=" + state.getAbbreviation() +"&" +
            		"lat=" + latitude + "&" +
            		"lon=" + longitude + "&" +
            		"limit=5";
            
            Log.d(TAG, url);
                        
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
							// TODO Auto-generated method stub
							
						}
					});
        	queue.add(request);
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
				// TODO pretty up the markers
				// TODO add a filter to hide private schools & schools without rating
				// TODO add filters for elementary, middle, high schools
				LatLng position = new LatLng(school.lat, school.lon);
				Marker marker = googleMap.addMarker(new MarkerOptions()
									.position(position)
									.title(school.schoolName)
									.snippet(String.valueOf(school.gsRating)));
				
				if (school.schoolName.equals(schools.get(0).schoolName)) {
					marker.showInfoWindow();
				}
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}            	
    }

}
