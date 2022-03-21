package com.ligz.routedrawing;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    MapView map = null;
    private MyLocationNewOverlay mLocationOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;
    private RequestQueue queue;
    private JsonObjectRequest requestMapRequest;
    private CompassOverlay mCompassOverlay;
    private Button btnPosition, btnDeletePosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        //inflate and create the map
        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setMultiTouchControls(true);

        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), map);
        this.mLocationOverlay.enableMyLocation();
        map.getOverlays().add(this.mLocationOverlay);

        this.mCompassOverlay = new CompassOverlay(ctx, new InternalCompassOrientationProvider(ctx), map);
        this.mCompassOverlay.enableCompass();
        map.getOverlays().add(this.mCompassOverlay);

        IMapController mapController = map.getController();
        mapController.setZoom(19.0);

        GeoPoint startPoint = new GeoPoint(20.14649016556056,-101.17566401392817);
        mapController.setCenter(startPoint);

        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), map);
        this.mLocationOverlay.enableMyLocation();
        map.getOverlays().add(this.mLocationOverlay);

        mRotationGestureOverlay = new RotationGestureOverlay(ctx, map);
        mRotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(this.mRotationGestureOverlay);

        //your items
        ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
        items.add(new OverlayItem("Title", "Description", new GeoPoint(20.14649016556056,-101.17566401392817))); // Lat/Lon decimal degrees

        //the overlay
        ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        //do something
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        return false;
                    }
                }, ctx);
        mOverlay.setFocusItemsOnTap(true);

        //map.getOverlays().add(mOverlay);

        //Añadir un marcador
        Marker startMarker = new Marker(map);

        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startMarker);

        btnPosition = findViewById(R.id.btnPos);
        btnDeletePosition = findViewById(R.id.btnDelete);

        btnPosition.setOnClickListener(view -> {
            GeoPoint geoPoint = new GeoPoint(20.14649016556056,-101.17566401392817);
            mapController.setCenter(geoPoint);
            obtenerRouteFromMapRequest();
        });

        btnDeletePosition.setOnClickListener(view -> {
            map.getOverlays().clear();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    protected void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    private void obtenerRouteFromMapRequest() {
        List<GeoPoint> puntosRuta = new ArrayList<>();
        queue =
                Volley.newRequestQueue(this);

        requestMapRequest =
                new JsonObjectRequest(
                        "http://www.mapquestapi.com/directions/v2/route?key=3DAfWsMIWwkEfW0XdH9hU6HufelHBhI1&from=20.14649016556056,-101.17566401392817&to=20.126496094732943,-101.19317063853653",
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.d("GIVO", "se ejecuto");
                                try {
                                    JSONArray indicaiones = response.getJSONObject("route")
                                            .getJSONArray("legs")
                                            .getJSONObject(0).
                                                    getJSONArray("maneuvers");


                                    for (int i = 0; i < indicaiones.length(); i++) {
                                        JSONObject indi = indicaiones.getJSONObject(i);
                                        String strlatlog = indi.getJSONObject("startPoint").get("lat").toString()
                                                + "," +
                                                indi.getJSONObject("startPoint").get("lng").toString();



                                        Log.d("GIVO", "se ejecuto: " + strlatlog);
                                        puntosRuta.add(new GeoPoint(Double.parseDouble(indi.getJSONObject("startPoint").get("lat").toString()),
                                                Double.parseDouble(indi.getJSONObject("startPoint").get("lng").toString())));

                                    }

                                    dibujarPolilinea(puntosRuta);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("GIVO", "se ejecuto CON ERROR");

                            }
                        }
                );

        queue.add(requestMapRequest);
        Log.d("GIVO", "se ejecuto antes que los puntos");
    }

    private void dibujarPolilinea(List<GeoPoint> geoPoints) {

        Polyline line = new Polyline();   //see note below!
        line.setPoints(geoPoints);
        line.setOnClickListener(new Polyline.OnClickListener() {
            @Override
            public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {
                Toast.makeText(mapView.getContext(), "polyline with " + polyline.getPoints().size() + "pts was tapped", Toast.LENGTH_LONG).show();
                return false;
            }
        });
        map.getOverlayManager().add(line);

        IMapController mapController = map.getController();
        mapController.setZoom(17.0);
        GeoPoint startPoint = new GeoPoint(20.14649016556056,-101.17566401392817);
        mapController.setCenter(startPoint);

    }
}