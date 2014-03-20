/*******************************************************************************
 * Copyright 2013 Esri
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/
package com.esri.squadleader.view;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemSelectedListener;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnPanListener;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.core.geometry.AngularUnit;
import com.esri.core.geometry.AreaUnit;
import com.esri.core.geometry.CoordinateConversion;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.Graphic;
import com.esri.militaryapps.controller.ChemLightController;
import com.esri.militaryapps.controller.LocationController.LocationMode;
import com.esri.militaryapps.controller.LocationListener;
import com.esri.militaryapps.controller.MessageController;
import com.esri.militaryapps.controller.PositionReportController;
import com.esri.militaryapps.controller.SpotReportController;
import com.esri.militaryapps.model.LayerInfo;
import com.esri.militaryapps.model.Location;
import com.esri.militaryapps.model.SpotReport;
import com.esri.squadleader.R;
import com.esri.squadleader.controller.AdvancedSymbolController;
import com.esri.squadleader.controller.LocationController;
import com.esri.squadleader.controller.MapController;
import com.esri.squadleader.controller.MessageListener;
import com.esri.squadleader.model.BasemapLayer;
import com.esri.squadleader.util.Utilities;
import com.esri.squadleader.view.AddLayerFromWebDialogFragment.AddLayerListener;
import com.esri.squadleader.view.GoToMgrsDialogFragment.GoToMgrsHelper;
import com.ipaulpro.afilechooser.utils.FileUtils;

import com.esri.squadleader.util.GeometryUtil;

import com.esri.squadleader.view.DimensionPantallaDialogFragment.DimensionPantallaHelper;
import com.esri.squadleader.view.BufferPantallaDialogFragment.BufferPantallaHelper;



/**
 * The main activity for the Squad Leader application. Typically this displays a map with various other
 * controls.
 */
public class SquadLeaderActivity extends ActionBarActivity
        implements AddLayerListener, GoToMgrsHelper, DimensionPantallaHelper, BufferPantallaHelper {
    
    private static final String TAG = SquadLeaderActivity.class.getSimpleName();
    private static final double MILLISECONDS_PER_HOUR = 1000 * 60 * 60;
    
    /**
     * A unique ID for the GPX file chooser.
     */
    private static final int REQUEST_CHOOSER = 30046;
    
    /**
     * A unique ID for getting a result from the settings activity.
     */
    private static final int SETTINGS_ACTIVITY = 5862;
    
    /**
     * A unique ID for getting a result from the spot report activity.
     */
    private static final int SPOT_REPORT_ACTIVITY = 15504;
    
    private final Handler locationChangeHandler = new Handler() {
        
        private final SpatialReference SR = SpatialReference.create(4326);
        
        private Location previousLocation = null;
        private CoordinateConversion	conversionCoordenadas = new CoordinateConversion();
       	private SpatialReference spt = SpatialReference.create(SpatialReference.WKID_WGS84);
        
        @Override
        public void handleMessage(Message msg) {
            if (null != msg) {
                Location location = (Location) msg.obj;
                mapController.rumbo = (float) location.getHeading();
                try {
                    TextView locationView = (TextView) findViewById(R.id.textView_displayLocation);
                    // 1 - MGRS	2 - GEO		3 - UTM
                    
                    String txtLocation = "Posicion";
                    if (modoCoordenadas == 1){
                    	txtLocation = mapController.pointToMgrs(new Point(location.getLongitude(), location.getLatitude()), SR);
                    	locationView.setText(getString(R.string.display_location) + txtLocation);
                    }else if (modoCoordenadas == 2){
                    	txtLocation = "Long: " + Double.toString(location.getLongitude()) + " Lat: " + Double.toString(location.getLatitude());
                    	locationView.setText(txtLocation);
                    }else{
                     	txtLocation = "UTM: " + conversionCoordenadas.pointToUtm(new Point(location.getLongitude(),location.getLatitude()), spt, CoordinateConversion.UTMConversionMode.NORTH_SOUTH_LATITUDE_INDICATORS, true);
                    	locationView.setText(txtLocation);
                    }
                } catch (Throwable t) {
                    Log.i(TAG, "Couldn't set location text", t);
                }
                try {
                    double speedMph = location.getSpeedMph();
                    if (0 == Double.compare(speedMph, 0.0)
                            && null != previousLocation
                            && !mapController.getLocationController().getMode().equals(LocationMode.LOCATION_SERVICE)) {
                        //Calculate speed
                        double distanceInMiles = Utilities.calculateDistanceInMeters(previousLocation, location) / Utilities.METERS_PER_MILE;
                        double timeInHours = (location.getTimestamp().getTimeInMillis() - previousLocation.getTimestamp().getTimeInMillis()) /  MILLISECONDS_PER_HOUR;
                        speedMph = distanceInMiles / timeInHours;
                    }
                    ((TextView) findViewById(R.id.textView_displaySpeed)).setText(
                            getString(R.string.display_speed) + Double.toString(Math.round(10.0 * speedMph) / 10.0) + " mph");
                } catch (Throwable t) {
                    Log.i(TAG, "Couldn't set speed text", t);
                }
                try {
                    String headingString = LocationController.headingToString(location.getHeading(), angularUnitPreference, 0);
                    ((TextView) findViewById(R.id.textView_displayHeading)).setText(getString(R.string.display_heading) + headingString);
                } catch (Throwable t) {
                    Log.i(TAG, "Couldn't set heading text", t);
                }
                previousLocation = location;
            }
        };
    };
    
    private final OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener() {
        
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(getString(R.string.pref_angularUnits))) {
                try {
                    int angularUnitWkid = Integer.parseInt(sharedPreferences.getString(key, "0"));
                    angularUnitPreference = (AngularUnit) AngularUnit.create(angularUnitWkid);
                } catch (Throwable t) {
                    Log.i(TAG, "Couldn't get " + getString(R.string.pref_angularUnits) + " value", t);
                }
            } else if (key.equals(getString(R.string.pref_messagePort))) {
                boolean needToReset = true;
                try {
                    final int newPort = Integer.parseInt(sharedPreferences.getString(key, Integer.toString(messagePortPreference)));
                    if (1023 < newPort && 65536 > newPort && newPort != messagePortPreference) {
                        messagePortPreference = newPort;
                        changePort(newPort);
                        needToReset = false;
                    }
                } catch (Throwable t) {
                    Log.i(TAG, "Couldn't get " + getString(R.string.pref_messagePort) + " value; sticking with default of " + messagePortPreference, t);
                } finally {
                    if (needToReset) {
                        Editor editor = sharedPreferences.edit();
                        editor.putString(key, Integer.toString(messagePortPreference));
                        editor.commit();
                    }
                }
            } else if (key.equals(getString(R.string.pref_positionReportPeriod))) {
                try {
                    positionReportsPeriodPreference = Integer.parseInt(sharedPreferences.getString(key, Integer.toString(positionReportsPeriodPreference)));
                    positionReportController.setPeriod(positionReportsPeriodPreference);
                    int newPeriod = positionReportController.getPeriod();
                    if (newPeriod != positionReportsPeriodPreference) {
                        sharedPreferences.edit().putString(getString(R.string.pref_positionReportPeriod), Integer.toString(newPeriod)).commit();
                        positionReportsPeriodPreference = newPeriod;
                    }
                } catch (Throwable t) {
                    Log.i(TAG, "Couldn't get " + key + " value", t);
                }
            } else if (key.equals(getString(R.string.pref_positionReports))) {
                try {
                    positionReportsPreference = sharedPreferences.getBoolean(key, false);
                    positionReportController.setEnabled(positionReportsPreference);
                } catch (Throwable t) {
                    Log.i(TAG, "Couldn't get " + key + " value", t);
                }
            } else if (key.equals(getString(R.string.pref_uniqueId))) {
                try {
                    uniqueIdPreference = sharedPreferences.getString(key, uniqueIdPreference);
                    positionReportController.setUniqueId(uniqueIdPreference);
                } catch (Throwable t) {
                    Log.i(TAG, "Couldn't get " + key + " value", t);
                }
            } else if (key.equals(getString(R.string.pref_username))) {
                try {
                    usernamePreference = sharedPreferences.getString(key, usernamePreference);
                    positionReportController.setUsername(usernamePreference);
                } catch (Throwable t) {
                    Log.i(TAG, "Couldn't get " + key + " value", t);
                }
            }
        }
    };
    
    private final MessageController messageController;
    private final ChemLightController chemLightController;
    private final RadioGroup.OnCheckedChangeListener chemLightCheckedChangeListener;
    
    private MapController mapController = null;
    private NorthArrowView northArrowView = null;
    private SpotReportController spotReportController = null;
    private AdvancedSymbolController mil2525cController = null;
    private PositionReportController positionReportController;
    private AddLayerFromWebDialogFragment addLayerFromWebDialogFragment = null;
    private GoToMgrsDialogFragment goToMgrsDialogFragment = null;
    private boolean wasFollowMeBeforeMgrs = false;
    private final Timer clockTimer = new Timer(true);
    private TimerTask clockTimerTask = null;
    private AngularUnit angularUnitPreference = null;
    private int messagePortPreference = 45678;
    private boolean positionReportsPreference = false;
    private int positionReportsPeriodPreference = 1000;
    private String usernamePreference = "Squad Leader";
    private String vehicleTypePreference = "Dismounted";
    private String uniqueIdPreference = UUID.randomUUID().toString();
    private String sicPreference = "SFGPEWRR-------";
    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////
    private DimensionPantallaDialogFragment dimensionPantallaDialogFragment = null;
    private BufferPantallaDialogFragment bufferPantallaDialogFragment = null;
    
    private boolean consultarCoordenadasActivo = false;
    private boolean midiendoDistancias = false;
    private boolean midiendoAreas = false;
    private boolean consultandoUnidades = false;
    private boolean consultandoBuffer = false;
    private boolean simulandoActivo = true;

    public int modoCoordenadas = 1;   // 1 - MGRS	2 - GEO		3 - UTM 

	public static final LinearUnit LINEARUNIT_METER = (LinearUnit) Unit.create(LinearUnit.Code.METER);
	public static final AreaUnit AREAUNIT_SQUARE_METER = (AreaUnit) Unit.create(AreaUnit.Code.SQUARE_METER);
	GraphicsLayer firstGeomLayer = null;
	Geometry firstGeometry = null;
	TextView resultText = null;
	GEOMETRY_TYPE firstGeoType = GEOMETRY_TYPE.polyline;
	Button resetButton = null;
	boolean isStartPointSet1 = false;
	volatile int countTap = 0;
	double measure = 0;
	double value = 0;
	int resId = 0;
	int current_distance_unit = LinearUnit.Code.METER;
	int current_area_unit = AreaUnit.Code.SQUARE_METER;
	enum GEOMETRY_TYPE {point, polyline, polygon}
	protected int[] distance_units = new int[] { LinearUnit.Code.METER,
			LinearUnit.Code.MILE_US, LinearUnit.Code.YARD,
			LinearUnit.Code.FOOT, LinearUnit.Code.KILOMETER, LinearUnit.Code.MILE_STATUTE };
	protected int[] area_units = new int[] { AreaUnit.Code.SQUARE_METER,
			AreaUnit.Code.ACRE, AreaUnit.Code.SQUARE_MILE_US,
			AreaUnit.Code.SQUARE_YARD, AreaUnit.Code.SQUARE_KILOMETER, AreaUnit.Code.SQUARE_MILE_STATUTE };
    
    /////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    public SquadLeaderActivity() throws SocketException {
        super();
        chemLightCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                for (int j = 0; j < group.getChildCount(); j++) {
                    final ToggleButton view = (ToggleButton) group.getChildAt(j);
                    view.setChecked(view.getId() == checkedId);
                }
            }
        };
        
        messageController = new MessageController(messagePortPreference);
        chemLightController = new ChemLightController(messageController);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(SquadLeaderActivity.this);
        try {
            int wkid = Integer.parseInt(sp.getString(getString(R.string.pref_angularUnits), Integer.toString(AngularUnit.Code.DEGREE)));
            angularUnitPreference = (AngularUnit) AngularUnit.create(wkid);
        } catch (Throwable t) {
            Log.d(TAG, "Couldn't get preference", t);
        }
        try {
            messagePortPreference = Integer.parseInt(sp.getString(getString(R.string.pref_messagePort), Integer.toString(messagePortPreference)));
            changePort(messagePortPreference);
            messageController.startReceiving();
        } catch (Throwable t) {
            Log.d(TAG, "Couldn't get preference", t);
        }
        try {
            positionReportsPreference = sp.getBoolean(getString(R.string.pref_positionReports), false);
        } catch (Throwable t) {
            Log.d(TAG, "Couldn't get preference", t);
        }
        try {
            positionReportsPeriodPreference = Integer.parseInt(sp.getString(
                    getString(R.string.pref_positionReportPeriod),
                    Integer.toString(positionReportsPeriodPreference)));
            if (0 >= positionReportsPeriodPreference) {
                positionReportsPeriodPreference = PositionReportController.DEFAULT_PERIOD;
                sp.edit().putString(getString(R.string.pref_positionReportPeriod), Integer.toString(positionReportsPeriodPreference)).commit();
            }
        } catch (Throwable t) {
            Log.d(TAG, "Couldn't get preference", t);
        }
        try {
            usernamePreference = sp.getString(getString(R.string.pref_username), usernamePreference);
        } catch (Throwable t) {
            Log.d(TAG, "Couldn't get preference", t);
        }
        try {
            vehicleTypePreference = sp.getString(getString(R.string.pref_vehicleType), vehicleTypePreference);
        } catch (Throwable t) {
            Log.d(TAG, "Couldn't get preference", t);
        }
        try {
            uniqueIdPreference = sp.getString(getString(R.string.pref_uniqueId), uniqueIdPreference);
            //Make sure this one gets set in case we just generated it
            sp.edit().putString(getString(R.string.pref_uniqueId), uniqueIdPreference).commit();
        } catch (Throwable t) {
            Log.d(TAG, "Couldn't get preference", t);
        }
        try {
            sicPreference = sp.getString(getString(R.string.pref_sic), sicPreference);
        } catch (Throwable t) {
            Log.d(TAG, "Couldn't get preference", t);
        }

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        
//        //TODO implement Geo URIs
//        Uri intentData = getIntent().getData();
//        if (null != intentData) {
//            //intentData should be a Geo URI with a location to which we should navigate
//        }
        
        setContentView(R.layout.main);
        adjustLayoutForOrientation(getResources().getConfiguration().orientation);

        final MapView mapView = (MapView) findViewById(R.id.map);
        
        mapView.setOnPanListener(new OnPanListener() {
            
            private static final long serialVersionUID = 0x58d30af8d168f63aL;

            @Override
            public void prePointerUp(float fromx, float fromy, float tox, float toy) {}
            
            @Override
            public void prePointerMove(float fromx, float fromy, float tox, float toy) {
                setFollowMe(false);
            }
            
            @Override
            public void postPointerUp(float fromx, float fromy, float tox, float toy) {}
            
            @Override
            public void postPointerMove(float fromx, float fromy, float tox, float toy) {}
            
        });

        mapController = new MapController(mapView, getAssets(), new LayerErrorListener(this));
        northArrowView = (NorthArrowView) findViewById(R.id.northArrowView);
        northArrowView.setMapController(mapController);
        northArrowView.startRotation();
        try {
            mil2525cController = new AdvancedSymbolController(
                    mapController,
                    getAssets(),
                    getString(R.string.sym_dict_dirname),
                    getResources().getDrawable(R.drawable.ic_spot_report));
            mapController.setAdvancedSymbologyController(mil2525cController);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Couldn't find file while loading AdvancedSymbolController", e);
        }
        
        spotReportController = new SpotReportController(mapController, messageController);
        
        positionReportController = new PositionReportController(
                mapController.getLocationController(),
                messageController,
                usernamePreference,
                vehicleTypePreference,
                uniqueIdPreference,
                sicPreference);
        positionReportController.setPeriod(positionReportsPeriodPreference);
        positionReportController.setEnabled(positionReportsPreference);

        mapController.getLocationController().addListener(new LocationListener() {
            
            @Override
            public void onLocationChanged(final Location location) {
                if (null != location) {
                    //Do this in a thread in case we need to calculate the speed
                    new Thread() {
                        public void run() {
                            Message msg = new Message();
                            msg.obj = location;
                            locationChangeHandler.sendMessage(msg);
                        }
                    }.start();
                }
            }
        });
        
        messageController.addListener(new MessageListener(mil2525cController));

        clockTimerTask = new TimerTask() {
            
            private final Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    try {
                        if (null != msg.obj) {
                            ((TextView) findViewById(R.id.textView_displayTime)).setText(getString(R.string.display_time) + msg.obj);
                        }
                    } catch (Throwable t) {
                        Log.i(TAG, "Couldn't update time", t);
                    }
                }
            };
            
            @Override
            public void run() {                
                if (null != mapController) {
                    Message msg = new Message();
                    msg.obj = Utilities.DATE_FORMAT_MILITARY_ZULU.format(new Date());
                    handler.sendMessage(msg);
                }
            }
            
        };
        clockTimer.schedule(clockTimerTask, 0, Utilities.ANIMATION_PERIOD_MS);
        
        ((RadioGroup) findViewById(R.id.radioGroup_chemLightButtons)).setOnCheckedChangeListener(chemLightCheckedChangeListener);
        
        ///////////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////
        // Para cuando se de al rectangulo cambie el modo de ver coordenadas
        View displayView = findViewById(R.id.tableLayout_display);
        displayView.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				  // 1 - MGRS	2 - GEO		3 - UTM 
				modoCoordenadas++;
				modoCoordenadas = (modoCoordenadas >3 ? 1 : modoCoordenadas);
			}
		});
        
        // Para las medidas en el mapa
		resultText = (TextView) findViewById(R.id.result);
		resetButton = (Button) findViewById(R.id.reset);
		resetButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doReset();
			}
		});
        ///////////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////

    }
    ///////////   FIN    ONCREATE  /////////////
    ////////////////////////////////////////////
    ////////////////////////////////////////////
    ////////////////////////////////////////////
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustLayoutForOrientation(newConfig.orientation);
    }
    
    private void adjustLayoutForOrientation(int orientation) {
        View displayView = findViewById(R.id.tableLayout_display);
        if (displayView.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) displayView.getLayoutParams();
            switch (orientation) {
                case Configuration.ORIENTATION_LANDSCAPE: {
                    params.addRule(RelativeLayout.RIGHT_OF, R.id.toggleButton_grid);
                    params.addRule(RelativeLayout.LEFT_OF, R.id.toggleButton_followMe);
                    params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.imageButton_zoomOut);
                    params.addRule(RelativeLayout.ABOVE, -1);
                    break;
                }
                case Configuration.ORIENTATION_PORTRAIT:
                default: {
                    params.addRule(RelativeLayout.RIGHT_OF, -1);
                    params.addRule(RelativeLayout.LEFT_OF, R.id.imageButton_zoomIn);
                    params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.imageButton_zoomIn);
                    params.addRule(RelativeLayout.ABOVE, R.id.toggleButton_grid);
                }
            }
            displayView.setLayoutParams(params);
        }
    }
    
    private boolean isFollowMe() {
        ToggleButton followMeButton = (ToggleButton) findViewById(R.id.toggleButton_followMe);
        if (null != followMeButton) {
            return followMeButton.isChecked();
        } else {
            return false;
        }
    }
    
    private void setFollowMe(boolean isFollowMe) {
        ToggleButton followMeButton = (ToggleButton) findViewById(R.id.toggleButton_followMe);
        if (null != followMeButton) {
            if (isFollowMe != followMeButton.isChecked()) {
                followMeButton.performClick();
            }
        }
    }
    
    public MapController getMapController() {
        return mapController;
    }

    @Override
    public void beforePanToMgrs(String mgrs) {
        wasFollowMeBeforeMgrs = isFollowMe();
        setFollowMe(false);
    }

    @Override
    public void onPanToMgrsError(String mgrs) {
        if (wasFollowMeBeforeMgrs) {
            setFollowMe(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mapController.pause();
        northArrowView.stopRotation();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mapController.unpause();
        northArrowView.startRotation();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

    	TableLayout tablaLayoutMedir = (TableLayout) findViewById(R.id.tableLayout_medir);
    	ImageButton botonBaseMap = (ImageButton) findViewById(R.id.imageButton_openBasemapPanel);
    	ToggleButton botonSPOT = (ToggleButton) findViewById(R.id.toggleButton_spotReport);
    	
        switch (item.getItemId()) {
            case R.id.add_layer_from_web:
                //Present Add Layer from Web dialog
                if (null == addLayerFromWebDialogFragment) {
                    addLayerFromWebDialogFragment = new AddLayerFromWebDialogFragment();
                }
                addLayerFromWebDialogFragment.show(getSupportFragmentManager(), getString(R.string.add_layer_from_web_fragment_tag));
                return true;
            case R.id.go_to_mgrs:
                //Present Go to MGRS dialog
                if (null == goToMgrsDialogFragment) {
                    goToMgrsDialogFragment = new GoToMgrsDialogFragment();
                }
                goToMgrsDialogFragment.show(getSupportFragmentManager(), getString(R.string.go_to_mgrs_fragment_tag));
                return true;
            case R.id.set_location_mode:
                //Present Set Location Mode dialog
            	// AQUI. Añado el pausar y Continuar
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                String txtPausarSeguir = (simulandoActivo ? "Pausar Simulación":"Continuar Simulación");
                builder.setTitle(R.string.set_location_mode)
                        .setNegativeButton(R.string.cancel, null)
                        .setSingleChoiceItems(
                                new String[] {
                                        getString(R.string.option_location_service),
                                        getString(R.string.option_simulation_builtin),
                                        getString(R.string.option_simulation_file),
                                        txtPausarSeguir},
                                mapController.getLocationController().getMode() == LocationMode.LOCATION_SERVICE ? 0 : 
                                    null == mapController.getLocationController().getGpxFile() ? 1 : 2,
                                new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    if (2 == which) {
                                        //Present file chooser
                                        Intent getContentIntent = FileUtils.createGetContentIntent();
                                        Intent intent = Intent.createChooser(getContentIntent, "Select a file");
                                        startActivityForResult(intent, REQUEST_CHOOSER);
                                    } else if (which == 3){
                                    	if (simulandoActivo){
		                                    mapController.getLocationController().pause();
		                                    simulandoActivo = false;
                                    	} else {
                                    		mapController.getLocationController().unpause();
                                    		simulandoActivo = true;
                                    	}
                                    }else {
                                        mapController.getLocationController().setGpxFile(null);
                                        mapController.getLocationController().setMode(
                                                0 == which ? LocationMode.LOCATION_SERVICE : LocationMode.SIMULATOR,
                                                true);
                                    }                                   
                                } catch (Exception e) {
                                    Log.d(TAG, "Couldn't set location mode", e);
                                } finally {
                                    dialog.dismiss();
                                }
                            }
                            
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, SETTINGS_ACTIVITY);
                return true;
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
            case R.id.dimension_pantalla:
            	// establecer tamaño zoom en pantalla
                if (null == dimensionPantallaDialogFragment) {
                	dimensionPantallaDialogFragment = new DimensionPantallaDialogFragment();
                }
                dimensionPantallaDialogFragment.show(getSupportFragmentManager(), getString(R.string.dimension_pantalla_fragment_tag));            	
            	return true;
            case R.id.consultar_coordenada:
            	// consultar Coordenadas pinchando en el mapa
                final AlertDialog.Builder builderPto = new AlertDialog.Builder(this);
                if (consultarCoordenadasActivo){
                    mapController.setOnSingleTapListener(null);
                    consultarCoordenadasActivo =false;                    
                }else{
                    consultarCoordenadasActivo =true;              
	                mapController.setOnSingleTapListener(new OnSingleTapListener() {                    
	                    @Override
	                    public void onSingleTap(final float x, final float y) {
	                    	Point point = mapController.toMapPointObject((int) x, (int) y);                    	
	                		Log.d("single tap on screen:", "[" + x + "," + y + "]");
	                		Log.d("single tap on map:", "[" + point.getX() + "," + point.getY() + "]");
	                		Point geo, wm, utm;
	                		String mostrarCoordenadas = "-";
	                        CoordinateConversion	conversionCoordenadas = new CoordinateConversion();
	                        SpatialReference spt = SpatialReference.create(SpatialReference.WKID_WGS84);        
	                        SpatialReference sptWM = SpatialReference.create(3857);// Web Mercator (3857)  
	                        if (mapController.getSpatialReference().isWGS84()){//geo
	                			geo = new Point(point.getX(), point.getY());
	                			String coordUtm =conversionCoordenadas.pointToUtm(geo, spt, CoordinateConversion.UTMConversionMode.NORTH_SOUTH_LATITUDE_INDICATORS, true);
	                			mostrarCoordenadas = "GEOGRAFICAS WGS84:\nLat = " + Double.toString(point.getY())+  "   Long = " + Double.toString(point.getX());
	                			mostrarCoordenadas += "\nMGRS:\n" + conversionCoordenadas.pointToMgrs(geo, spt, CoordinateConversion.MGRSConversionMode.NEW_180_IN_ZONE1, 5, false, true);
	                			mostrarCoordenadas += "\nUTM WGS84:\n" + coordUtm;
	                 		}else if (mapController.getSpatialReference().isAnyWebMercator()){//wm 
	                 			wm = new Point(point.getX(), point.getY()); 
	                 			String coordGeo = conversionCoordenadas.pointToDecimalDegrees(wm, sptWM, 6);
	                 			mostrarCoordenadas = "GEOGRAFICAS WGS84:\n" + coordGeo;
	                			mostrarCoordenadas += "\nUTM WGS84\n" + "-";                 			
	                 		}else{//utm
	                 			utm = new Point(point.getX(), point.getY());                 			
	                 			/*String coordGeo = conversionCoordenadas.utmToPoint(txtUTM, spt, CoordinateConversion.UTMConversionMode.NORTH_SOUTH_LATITUDE_INDICATORS, true);
	                 			mostrarCoordenadas = "GEOGRAFICAS WGS84:\n" + coordGeo;*/
	                			mostrarCoordenadas = "";
	                			mostrarCoordenadas += "\nUTM WGS84\nX = " + Double.toString(point.getX())+  "   Y = " + Double.toString(point.getY());
	                	                 		}
	                        builderPto.setTitle("COORDENADAS");
	                        builderPto.setMessage(mostrarCoordenadas);
	                        builderPto.setCancelable(true);
	                        builderPto.create();
	                        builderPto.show();	 
	                        
	                    }
	                });
                }
            	return true;
            case R.id.medir_distancias:
                if (midiendoDistancias){
                    mapController.setOnSingleTapListener(null);
                    midiendoDistancias =false;
                    tablaLayoutMedir.setVisibility(View.INVISIBLE);
                    botonBaseMap.setVisibility(View.VISIBLE);
                    botonSPOT.setVisibility(View.VISIBLE);
                }else{
                    tablaLayoutMedir.setVisibility(View.VISIBLE);
                    botonBaseMap.setVisibility(View.INVISIBLE);
                    botonSPOT.setVisibility(View.INVISIBLE);
                	midiendoDistancias =true;  
                	if (firstGeomLayer == null ){
                		firstGeomLayer = new GraphicsLayer();
                		mapController.addLayer(firstGeomLayer);}
                	firstGeomLayer.removeAll();
					firstGeoType = GEOMETRY_TYPE.polyline;
					changeSpinnerUnits();
					doReset();
	                mapController.setOnSingleTapListener(new OnSingleTapListener() {                    
	                    @Override
	                    public void onSingleTap(final float x, final float y) {	                        
        					try {
        						singleTapAct(x, y);
        					} catch (Exception ex) {
        						ex.printStackTrace();
        					}
	                    }
	                });
                }
            	return true;
            case R.id.medir_areas:
                if (midiendoAreas){
                    mapController.setOnSingleTapListener(null);
                    midiendoAreas =false;
                    tablaLayoutMedir.setVisibility(View.INVISIBLE);
                    botonBaseMap.setVisibility(View.VISIBLE);
                    botonSPOT.setVisibility(View.VISIBLE);
                }else{
                    tablaLayoutMedir.setVisibility(View.VISIBLE);
                    botonBaseMap.setVisibility(View.INVISIBLE);
                    botonSPOT.setVisibility(View.INVISIBLE);
                	midiendoAreas =true;              
                	if (firstGeomLayer == null ){
                		firstGeomLayer = new GraphicsLayer();
                		mapController.addLayer(firstGeomLayer);}
                	firstGeomLayer.removeAll();
					firstGeoType = GEOMETRY_TYPE.polygon;
					changeSpinnerUnits();
					doReset();
	                mapController.setOnSingleTapListener(new OnSingleTapListener() {                    
	                    @Override
	                    public void onSingleTap(final float x, final float y) {	                        
        					try {
        						singleTapAct(x, y);
        					} catch (Exception ex) {
        						ex.printStackTrace();
        					}
	                    }
	                });
                }
            	return true;
            case R.id.consultar:
                if (consultandoUnidades){
                    mapController.setOnSingleTapListener(null);
                    consultandoUnidades =false;
                }else{
                	consultandoUnidades =true;              
	                mapController.setOnSingleTapListener(new OnSingleTapListener() {                    
	                    @Override
	                    public void onSingleTap(final float x, final float y) {	                        
        					try {
        						singleTapActConsultar(x, y);
        					} catch (Exception ex) {
        						ex.printStackTrace();
        					}
	                    }
	                });
                }                
            	return true;
            case R.id.buscar_cercanos:
               	// buscar elementos cercanos
                if (consultandoBuffer){
                    mapController.setOnSingleTapListener(null);
                    consultandoBuffer =false;
                    mapController.ptosDentroBufferGraphicLayer.removeAll();
                    mapController.bufferGraphicLayer.removeAll();
                }else{
                	consultandoBuffer =true; 
                	modoCoordenadas = 2;
                    mapController.ptosDentroBufferGraphicLayer.removeAll();
                    mapController.bufferGraphicLayer.removeAll();
	                mapController.setOnSingleTapListener(new OnSingleTapListener() {                    
	                    @Override
	                    public void onSingleTap(final float x, final float y) {	                        
        					try {
        						singleTapActBuffer(x, y);
        					} catch (Exception ex) {
        						ex.printStackTrace();
        					}
	                    }
	                });
                }
            	return true;
            case R.id.imprimir:
            	// es una PseudoImpresión. Previsualización
            	//RelativeLayout rview = (RelativeLayout) findViewById(R.id.relative_layout_main); 
            	MapView mapa = (MapView)findViewById(R.id.map);
            	//View v =  rview.getRootView();
            	//v.setDrawingCacheEnabled(true);
            	mapa.setDrawingCacheEnabled(true);
       			Bitmap b = mapa.getDrawingMapCache(5, 5, mapa.getWidth()-10, mapa.getHeight()-10);
            	mapa.setDrawingCacheEnabled(false);
            	//Log.d(TAG, "width = " + mapa.getWidth() + " ht = " + mapa.getHeight());
	                builder = new AlertDialog.Builder(SquadLeaderActivity.this);
	                LayoutInflater inflater2 = SquadLeaderActivity.this.getLayoutInflater();
	                View vista = inflater2.inflate(R.layout.impresion, null);
	            	ImageView img = (ImageView) vista.findViewById(R.id.imageView1);
	            	img.setImageBitmap(b);
	            	TextView txtFecha = (TextView)vista.findViewById(R.id.textView4);
	                //GregorianCalendar now = new GregorianCalendar();
	                //now.setTimeZone(TimeZone.getTimeZone("UTC"));
	                //txtFecha.setText(now.toString());
	            	java.util.Date fecha = new Date();	            	
	                txtFecha.setText(fecha.toString());
	            	TextView txtEscala = (TextView)vista.findViewById(R.id.textView5);
	                txtEscala.setText("Escala:  1 / " + Double.toString(mapa.getScale()) );
	            	TextView txtDescripcion = (TextView)vista.findViewById(R.id.textView6);
	            	txtDescripcion.setText("Descripción del mapa que se está previsualizando");
	            	builder.setView(vista);            
	                builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {				
	    				@Override
	    				public void onClick(DialogInterface dialog, int which) {
	    					// Se Sale sin más					
	    				}
	    			});
	                AlertDialog dialog2 = builder.create();
	                dialog2.show();
  	
            	String extr = Environment.getExternalStorageDirectory().toString();
            	File myPath = new File(extr, "pantallazo1.jpg");
            	FileOutputStream fos = null;
            	try {
            	    fos = new FileOutputStream(myPath);
            	    b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            	    fos.flush();
            	    fos.close();
            	    MediaStore.Images.Media.insertImage(getContentResolver(), b, "Screen", "screen");
            	} catch (FileNotFoundException e) {
            	    // TODO Auto-generated catch block
            	    e.printStackTrace();
            	} catch (Exception e) {
            	    // TODO Auto-generated catch block
            	    e.printStackTrace();
            	}
            	
            	
            	return true;
               //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * Called when an activity called by this activity returns a result. This method was initially
     * added to handle the result of choosing a GPX file for the LocationSimulator.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        switch (requestCode) {
        case REQUEST_CHOOSER:
            if (resultCode == RESULT_OK) {  
                final Uri uri = data.getData();
                File file = new File(FileUtils.getPath(this, uri));
                mapController.getLocationController().setGpxFile(file);
                try {
                    mapController.getLocationController().setMode(LocationMode.SIMULATOR, true);
                } catch (Exception e) {
                    Log.d(TAG, "Could not start simulator", e);
                }
            }
            break;
        case SETTINGS_ACTIVITY:
            if (null != data && data.getBooleanExtra(getString(R.string.pref_resetApp), false)) {
                try {
                    mapController.reset();
                } catch (Throwable t) {
                    Log.e(TAG, "Could not reset map", t);
                }
            }
            break;
        case SPOT_REPORT_ACTIVITY:
            if (null != data && null != data.getExtras()) {
                final SpotReport spotReport = (SpotReport) data.getExtras().get(getPackageName() + "." + SpotReportActivity.SPOT_REPORT_EXTRA_NAME);
                if (null != spotReport) {
                    new Thread() {
                        
                        @Override
                        public void run() {
                            String mgrs = (String) data.getExtras().get(getPackageName() + "." + SpotReportActivity.MGRS_EXTRA_NAME);
                            if (null != mgrs) {
                                Point pt = mapController.mgrsToPoint(mgrs);
                                if (null != pt) {
                                    spotReport.setLocationX(pt.getX());
                                    spotReport.setLocationY(pt.getY());
                                    spotReport.setLocationWkid(mapController.getSpatialReference().getID());
                                }
                            }                
                            try {
                                spotReportController.sendSpotReport(spotReport, usernamePreference);
                            } catch (Exception e) {
                                Log.e(TAG, "Could not send spot report", e);
                                //TODO notify user?
                            }
                        }
                    }.start();
                }
            }
            break;
        default:
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    public void imageButton_zoomIn_clicked(View view) {
	mapController.zoomIn();
    }
    
    public void imageButton_zoomOut_clicked(View view) {
	mapController.zoomOut();
    }
    
    public void imageButton_openBasemapPanel_clicked(final View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choose_basemap)
                .setNegativeButton(R.string.cancel, null);
        List<BasemapLayer> basemapLayers = mapController.getBasemapLayers();
        String[] basemapLayerNames = new String[basemapLayers.size()];
        for (int i = 0; i < basemapLayers.size(); i++) {
            basemapLayerNames[i] = basemapLayers.get(i).getLayer().getName();
        }
        builder.setSingleChoiceItems(
                basemapLayerNames,
                mapController.getVisibleBasemapLayerIndex(),
                new DialogInterface.OnClickListener() {
            
            public void onClick(DialogInterface dialog, int which) {
                mapController.setVisibleBasemapLayerIndex(which);
                dialog.dismiss();
            }
            
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    public void toggleButton_status911_clicked(final View view) {
        positionReportController.setStatus911(((ToggleButton) view).isChecked());
    }

    public void onValidLayerInfos(LayerInfo[] layerInfos) {
        for (int i = layerInfos.length - 1; i >= 0; i--) {
            mapController.addLayer(layerInfos[i]);
        }
    }
    
    public void toggleButton_grid_clicked(final View view) {
        mapController.setGridVisible(((ToggleButton) view).isChecked());
    }
    
    public void northArrowView_clicked(View view) {
        mapController.setRotation(0);
    }

    public void toggleButton_followMe_clicked(final View view) {
        mapController.setAutoPan(((ToggleButton) view).isChecked());
    }
    
    public void toggleButton_chemLightRed_clicked(final View view) {
        listenForChemLightTap(view, Color.RED);
    }

    public void toggleButton_chemLightYellow_clicked(final View view) {
        listenForChemLightTap(view, Color.YELLOW);
    }

    public void toggleButton_chemLightGreen_clicked(final View view) {
        listenForChemLightTap(view, Color.GREEN);
    }

    public void toggleButton_chemLightBlue_clicked(final View view) {
        listenForChemLightTap(view, Color.BLUE);
    }
    
    private void listenForChemLightTap(View button, final int color) {
        if (null != button && null != button.getParent() && button.getParent() instanceof RadioGroup) {
            ((RadioGroup) button.getParent()).check(button.getId());
            ((ToggleButton) findViewById(R.id.toggleButton_spotReport)).setChecked(false);
        }
        if (null != button && button instanceof ToggleButton && ((ToggleButton) button).isChecked()) {
            mapController.setOnSingleTapListener(new OnSingleTapListener() {
                
                @Override
                public void onSingleTap(final float x, final float y) {
                    new Thread() {
                        public void run() {
                            final double[] mapPoint = mapController.toMapPoint((int) x, (int) y);
                            if (null != mapPoint) {
                                chemLightController.sendChemLight(mapPoint[0], mapPoint[1], mapController.getSpatialReference().getID(), color);
                            } else {
                                Log.i(TAG, "Couldn't convert chem light to map coordinates");
                            }
                        };
                    }.start();
                }
            });
        } else {
            mapController.setOnSingleTapListener(null);
        }
    }
    
    public void toggleButton_spotReport_clicked(final View button) {
        ((RadioGroup) findViewById(R.id.radioGroup_chemLightButtons)).clearCheck();
        if (null != button && button instanceof ToggleButton && ((ToggleButton) button).isChecked()) {
            mapController.setOnSingleTapListener(new OnSingleTapListener() {
                
                @Override
                public void onSingleTap(final float x, final float y) {
                    Point pt = mapController.toMapPointObject((int) x, (int) y);
                    Intent intent = new Intent(SquadLeaderActivity.this, SpotReportActivity.class);
                    if (null != pt) {
                        intent.putExtra(getPackageName() + "." + SpotReportActivity.MGRS_EXTRA_NAME, mapController.pointToMgrs(pt));
                    }
                    startActivityForResult(intent, SPOT_REPORT_ACTIVITY);
                }
            });
        } else {
            mapController.setOnSingleTapListener(null);
        }
    }
    
    private void changePort(int newPort) {
        messageController.setPort(newPort);
    }
    
    
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////   I N I C I O      M E D I D A S      /////////////////////////////

	void doReset() {
		firstGeometry = null;
		firstGeomLayer.removeAll();
		measure = 0;
		value = 0;
		resultText.setText(Double.toString(value));
	}

	void changeSpinnerUnits() {

		if (firstGeoType == GEOMETRY_TYPE.polyline) {
			resId = R.array.DistanceUnits;
		} else {
			resId = R.array.AreaUnits;
		}

		// Create a spinner with the drop down values specified in
		// values->queryparameters.xml
		Spinner measureUnits = (Spinner) findViewById(R.id.spinner1);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, resId, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
		measureUnits.setAdapter(adapter);

		measureUnits.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				switch (pos) {
				// distance - Miles, area - Acres		
				//m  y m2
				case 0:
					if (firstGeoType == GEOMETRY_TYPE.polyline) {
						current_distance_unit = distance_units[0];
						doConvert(current_distance_unit, GEOMETRY_TYPE.polyline);
					} else {
						current_area_unit = area_units[0];
						doConvert(current_area_unit, GEOMETRY_TYPE.polygon);
					}
					break;
				// distance - Yards, area - Square Miles 	
					//millas  y acres
				case 1:
					if (firstGeoType == GEOMETRY_TYPE.polyline) {
						current_distance_unit = distance_units[1];
						doConvert(current_distance_unit, GEOMETRY_TYPE.polyline);
					} else {
						current_area_unit = area_units[1];
						doConvert(current_area_unit, GEOMETRY_TYPE.polygon);
					}
					break;
				// distance - Feet, area - Square Yards		
					//yardas		millas2
				case 2:
					if (firstGeoType == GEOMETRY_TYPE.polyline) {
						current_distance_unit = distance_units[2];
						doConvert(current_distance_unit, GEOMETRY_TYPE.polyline);
					} else {
						current_area_unit = area_units[2];
						doConvert(current_area_unit, GEOMETRY_TYPE.polygon);
					}
					break;
				// distance - Kilometers, area - Square Kilometers		
					//foot	Yardas2
				case 3:
					if (firstGeoType == GEOMETRY_TYPE.polyline) {
						current_distance_unit = distance_units[3];
						doConvert(current_distance_unit, GEOMETRY_TYPE.polyline);
					} else {
						current_area_unit = area_units[3];
						doConvert(current_area_unit, GEOMETRY_TYPE.polygon);
					}
					break;
				// distance - Meters, area - Square Meters				
					//KM		KM2
				case 4:
					if (firstGeoType == GEOMETRY_TYPE.polyline) {
						current_distance_unit = distance_units[4];
						doConvert(current_distance_unit, GEOMETRY_TYPE.polyline);
					} else {
						current_area_unit = area_units[4];
						doConvert(current_area_unit, GEOMETRY_TYPE.polygon);
					}
					break;
				// distancia - millas nauticas 		areas - millas nauticas cuadradas
				case 5:
					if (firstGeoType == GEOMETRY_TYPE.polyline) {
						current_distance_unit = distance_units[5];
						doConvert(current_distance_unit, GEOMETRY_TYPE.polyline);
					} else {
						current_area_unit = area_units[5];
						doConvert(current_area_unit, GEOMETRY_TYPE.polygon);
					}					
				default:
					break;
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Do nothing
			}
		});
	}

	void doConvert(int toUnit, GEOMETRY_TYPE GeoType) {
		// All the measurement is done in meters and sq meters. No need to
		// convert units
		if (toUnit == LinearUnit.Code.METER
				|| toUnit == AreaUnit.Code.SQUARE_METER) {
			// only two digits after the decimal
			//value = Double.valueOf(twoDForm.format(value));
			String numero = String.format("%.2f", measure);
			value = Double.parseDouble(numero.replace(',', '.'));
			resultText.setText(Double.toString(value));
			return;
		}
		// Calculate the value of measure in other units
		if (GeoType == GEOMETRY_TYPE.polyline) {
			if (toUnit == LinearUnit.Code.MILE_STATUTE)
				value = measure / 1852.0;
			else
				value = Unit.convertUnits(measure, LINEARUNIT_METER, Unit.create(toUnit));
			//value = Double.valueOf(twoDForm.format(value));
			String numero = String.format("%.2f", value);
			value = Double.parseDouble(numero.replace(',', '.'));
		} else if (GeoType == GEOMETRY_TYPE.polygon) {
			if (toUnit == AreaUnit.Code.SQUARE_MILE_STATUTE)
				value = measure / (1852.0 * 1852.0);
			else
				value = Unit.convertUnits(measure, AREAUNIT_SQUARE_METER, Unit.create(toUnit));
			//value = Double.valueOf(twoDForm.format(value));
			String numero = String.format("%.2f", value);
			value = Double.parseDouble(numero.replace(',', '.'));
		}
		// Display result in textview
		resultText.setText(Double.toString(value));
	}

	void singleTapAct(float x, float y) throws Exception {
		countTap++;
		Point point = mapController.toMapPointObject((int)x, (int)y);
		Log.d("single tap on screen:", "[" + x + "," + y + "]");
		Log.d("single tap on map:", "[" + point.getX() + "," + point.getY()
				+ "]");

		if (firstGeometry == null) {
			if (firstGeoType == GEOMETRY_TYPE.point) {
				firstGeometry = point;

			} else if (firstGeoType == GEOMETRY_TYPE.polygon) {
				firstGeometry = new Polygon();
				((MultiPath) firstGeometry).startPath(point);
				isStartPointSet1 = true;
				Log.d("geometry step " + countTap, GeometryEngine.geometryToJson(mapController.getSpatialReference(), firstGeometry));

			} else if (firstGeoType == GEOMETRY_TYPE.polyline) {
				isStartPointSet1 = true;
				firstGeometry = new Polyline();
				((MultiPath) firstGeometry).startPath(point);
			}

		}

		if (firstGeoType == null)
			return;
		int color1 = Color.BLUE;
		drawGeomOnGraphicLyr(firstGeometry, firstGeomLayer, point,
				firstGeoType, color1, isStartPointSet1);
		Log.d("geometry step " + countTap, GeometryEngine.geometryToJson(mapController.getSpatialReference(), firstGeometry));

	}

	void drawGeomOnGraphicLyr(Geometry geometryToDraw, GraphicsLayer glayer,
			Point point, GEOMETRY_TYPE geoTypeToDraw, int color,
			boolean startPointSet) {

		if (geoTypeToDraw == GEOMETRY_TYPE.point) {
			geometryToDraw = point;

		} else {

			if (startPointSet) {

				if (geoTypeToDraw == GEOMETRY_TYPE.polygon) {
					((Polygon) geometryToDraw).lineTo(point);
					// Simplify the geometry and project to spatial ref with
					// WKID for World Cylindrical Equal Area 54034
					Geometry geometry = GeometryEngine.simplify(geometryToDraw,	mapController.getSpatialReference());
					Geometry g2 = GeometryEngine.project(geometry, mapController.getSpatialReference(),
							SpatialReference.create(54034));
					// Get the area for the polygon
					measure = Math.abs(g2.calculateArea2D());
					if (measure != 0.0)
						doConvert(current_area_unit, firstGeoType);

				} else if (geoTypeToDraw == GEOMETRY_TYPE.polyline) {
					((Polyline) geometryToDraw).lineTo(point);
					// Get the geodesic length for the polyline
					measure = GeometryEngine.geodesicLength(geometryToDraw,	mapController.getSpatialReference(), LINEARUNIT_METER);
					if (measure != 0.0)
						doConvert(current_distance_unit, firstGeoType);
				}

			}
		}

		Geometry[] geoms = new Geometry[1];
		geoms[0] = geometryToDraw;

		try {
			glayer.removeAll();
			GeometryUtil.highlightGeometriesWithColor(geoms, glayer, color);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	//////////////////////  F I N     M E D I D A S  /////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////       C O N S U L T A R    ////////////////////////////////////
	
	void singleTapActConsultar(float x, float y) throws Exception {
		//countTap++;
		Point point = mapController.toMapPointObject((int)x, (int)y);
		Log.d("single tap on screen:", "[" + x + "," + y + "]");
		Log.d("single tap on map:", "[" + point.getX() + "," + point.getY()	+ "]");
	
		int[] identificadores = mil2525cController.spotReportLayer.getGraphicIDs(x, y, 5);
		int[] identificadores2 = mapController.locationGraphicsLayer.getGraphicIDs(x, y, 25);
		Graphic graficoSeleccionado = null;
		String Atributos ="";
		
		if (identificadores.length >0){
			Atributos = "SPOT\n";
			graficoSeleccionado = mil2525cController.spotReportLayer.getGraphic(identificadores[0]);
		}
		else if (identificadores2.length >0){
			Atributos = "UNIDAD\n";
			graficoSeleccionado = mapController.locationGraphicsLayer.getGraphic(identificadores2[0]);
		}else
			graficoSeleccionado = null;
		
		if (graficoSeleccionado == null)
			return;
		String[] mNombres = graficoSeleccionado.getAttributeNames();
		String valor = "-";
		for (int i=0 ; i < mNombres.length ; i ++){
			if (mNombres[i].toString().equals("") || mNombres[i].toString().equals("Shape") || mNombres[i].toString().equals("OBJECTID"))
				continue;
			Atributos += mNombres[i].toString() + "   :   " ;
			Object oj = graficoSeleccionado.getAttributeValue(mNombres[i].toString());
			if (oj == null)
				valor = "-";
			else
				valor = oj.toString();
			Atributos += valor + "\n";
		}
        AlertDialog.Builder builderPto = new AlertDialog.Builder(this);
        builderPto.setTitle("ATRIBUTOS");
        builderPto.setMessage(Atributos);
        builderPto.setCancelable(true);
        builderPto.create();
        builderPto.show();	 
	}
	
	
	
	
	//////////////////////  F I N     C O N S U L T A R  /////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	////////////////////// B U S Q U E D A      E S P A C I A L  /////////////////////////

	void singleTapActBuffer(float x, float y) throws Exception {
		//countTap++;
		Point pointManual = mapController.toMapPointObject((int)x, (int)y);
    	
		// de momento solo para geograficas
		//txtLocation = "Long: " + Double.toString(location.getLongitude()) + " Lat: " + Double.toString(location.getLatitude());
		TextView locationView = (TextView) findViewById(R.id.textView_displayLocation);

		String[] arrays = locationView.getText().toString().split(" ");
		Point pointGPS = null;
		if (arrays.length == 4)
			pointGPS = new Point(Double.parseDouble(arrays[1]), Double.parseDouble(arrays[3]));

		if (null == bufferPantallaDialogFragment) {
			bufferPantallaDialogFragment = new BufferPantallaDialogFragment();
		}
		bufferPantallaDialogFragment.puntoManual = pointManual;
		bufferPantallaDialogFragment.puntoGPS = pointGPS;
		bufferPantallaDialogFragment.show(getSupportFragmentManager(), getString(R.string.buffer_pantalla_fragment_tag));            	
	}
}
