package com.akylas.cartotest.ui.main;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.akylas.cartotest.R;
import com.carto.components.Options;
import com.carto.components.PanningMode;
import com.carto.core.BinaryData;
import com.carto.core.MapBounds;
import com.carto.core.MapPos;
import com.carto.core.MapPosVector;
import com.carto.core.MapPosVectorVector;
import com.carto.core.MapRange;
import com.carto.core.MapVec;
import com.carto.core.ScreenBounds;
import com.carto.core.StringMap;
import com.carto.core.StringVector;
import com.carto.core.Variant;
import com.carto.datasources.GeoJSONVectorTileDataSource;
import com.carto.datasources.HTTPTileDataSource;
import com.carto.datasources.MergedMBVTTileDataSource;
import com.carto.datasources.MultiTileDataSource;
import com.carto.datasources.LocalVectorDataSource;
import com.carto.datasources.MBTilesTileDataSource;
import com.carto.datasources.PersistentCacheTileDataSource;
import com.carto.datasources.TileDataSource;
import com.carto.geometry.Feature;
import com.carto.geometry.FeatureCollection;
import com.carto.geometry.GeoJSONGeometryReader;
import com.carto.geometry.Geometry;
import com.carto.geometry.LineGeometry;
import com.carto.geometry.MultiLineGeometry;
import com.carto.geometry.PolygonGeometry;
import com.carto.geometry.VectorTileFeatureCollection;
import com.carto.graphics.Color;
import com.carto.layers.HillshadeRasterTileLayer;
import com.carto.layers.RasterTileFilterMode;
import com.carto.layers.RasterTileLayer;
import com.carto.layers.TileSubstitutionPolicy;
import com.carto.layers.VectorLayer;
import com.carto.layers.VectorTileLayer;
import com.carto.projections.EPSG4326;
import com.carto.projections.Projection;
import com.carto.rastertiles.MapBoxElevationDataDecoder;
import com.carto.routing.RouteMatchingRequest;
import com.carto.routing.RouteMatchingResult;
import com.carto.routing.RoutingRequest;
import com.carto.routing.RoutingResult;
import com.carto.routing.MultiValhallaOfflineRoutingService;
import com.carto.search.SearchRequest;
import com.carto.search.VectorTileSearchService;
import com.carto.styles.CartoCSSStyleSet;
import com.carto.styles.CompiledStyleSet;
import com.carto.styles.LineJoinType;
import com.carto.styles.LineStyleBuilder;
import com.carto.ui.MapClickInfo;
import com.carto.ui.MapEventListener;
import com.carto.ui.MapView;
import com.carto.utils.AssetUtils;
import com.carto.utils.ZippedAssetPackage;
import com.carto.vectorelements.Line;
import com.carto.vectortiles.MBVectorTileDecoder;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.fragment.app.Fragment;

public class SecondFragment extends Fragment {
    private final String TAG = "SecondFragment";


    class MathRouteTask extends TimerTask
    {
        MathRouteTask(MultiValhallaOfflineRoutingService routingService, Projection projection, String profile) {
            super();
            this.profile = profile;
            this.projection = projection;
            this.routingService = routingService;
        }
        MultiValhallaOfflineRoutingService routingService;
        Projection projection;
        String profile;
        public void run()
        {
            matchRouteTest(this.routingService, this.projection, this.profile);
        }
    }

    public static SecondFragment newInstance() {
        return new SecondFragment();
    }

    private static final int REQUEST_PERMISSIONS_CODE_WRITE_STORAGE = 1435;
    private static final int REQUEST_PERMISSIONS_MANAGE_STORAGE = 1436;
    MapView mapView;
    MapBoxElevationDataDecoder elevationDecoder;
    HillshadeRasterTileLayer hillshadeLayer;

    // Function to check and request permission
    @SuppressLint("NewApi")
    public void checkStoragePermission(View view) {

        // Checking if permission is not granted
        if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSIONS_CODE_WRITE_STORAGE);

        } else {
            proceedWithSdCard(view);
        }
    }


    public void toggleSlopes(boolean activated) {
        hillshadeLayer.setExagerateHeightScaleEnabled(!activated);
        hillshadeLayer.setNormalMapLightingShader(activated ? "uniform vec4 u_shadowColor;\n" +
                "        uniform vec4 u_highlightColor;\n" +
                "        uniform vec4 u_accentColor;\n" +
                "        uniform vec3 u_lightDir;\n" +
                "        vec4 applyLighting(lowp vec4 color, mediump vec3 normal, mediump vec3 surfaceNormal, mediump float intensity) {\n" +
                "            mediump float lighting = max(0.0, dot(normal, u_lightDir));\n" +
                "            mediump float slope = acos(dot(normal, surfaceNormal)) *180.0 / 3.14159 * 1.2;\n" +
                "            if (slope >= 45.0) {return vec4(0.7568627450980392* 0.5, 0.5450980392156863* 0.5, 0.7176470588235294* 0.5, 0.5); }\n" +
                "            if (slope >= 40.0) {return vec4( 0.5, 0, 0, 0.5); }\n" +
                "            if (slope >= 35.0) {return vec4(0.9098039215686275* 0.5, 0.4627450980392157* 0.5, 0.2235294117647059* 0.5, 0.5); }\n" +
                "            if (slope >= 30.0) {return vec4(0.9411764705882353* 0.5, 0.9019607843137255* 0.5, 0.3058823529411765* 0.5, 0.5); }\n" +
                "            return vec4(0, 0, 0, 0.0);\n" +
                "        }\n" +
                " " : "");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_CODE_WRITE_STORAGE) {
            if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= 30) {
                    // If you have access to the external storage, do whatever you need
                    if (Environment.isExternalStorageManager()) {

                        // If you don't have access, launch a new activity to show the user the system's dialog
                        // to allow access to the external storage
                    } else {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, REQUEST_PERMISSIONS_MANAGE_STORAGE);
                    }
                } else {
                    proceedWithSdCard(this.getView());

                }
            }
        } else if (requestCode == REQUEST_PERMISSIONS_CODE_WRITE_STORAGE) {
            proceedWithSdCard(this.getView());

        }
    }
    void addHillshadeLayer(View view, String dataPath) {
        MBTilesTileDataSource hillshadeSourceFrance = null;
        MBTilesTileDataSource hillshadeSourceWorld = null;
        MultiTileDataSource  dataSource = new MultiTileDataSource();
        try {
            hillshadeSourceFrance = new MBTilesTileDataSource( dataPath+"/france_terrain.etiles");
            hillshadeSourceWorld = new MBTilesTileDataSource( dataPath+"/world_terrain.etiles");
//            hillshadeSource = this.hillshadeSource = new HTTPTileDataSource(5, 11, "http://192.168.1.45:8080/data/BDALTIV2_75M_rvb/{z}/{x}/{y}.png");
            //        HTTPTileDataSource hillshadeSource =   new HTTPTileDataSource(1, 15, "https://api.mapbox.com/v4/mapbox.terrain-rgb/{z}/{x}/{y}.pngraw?access_token=pk.eyJ1IjoiYWt5bGFzIiwiYSI6IkVJVFl2OXMifQ.TGtrEmByO3-99hA0EI44Ew");
        } catch (Exception e) {
            e.printStackTrace();
        }
//        hillshadeSourceWorld.setMaxOverzoomLevel(1);
        dataSource.add(hillshadeSourceFrance, "wzAzMzDAwMBwwwXFfBcVXAzAxE8BcVfMxXFXMBFfxVVVwMzMBMVwMB8RVPHFV9QQ1xDUPwMDFfMRwFVzwFXMVxFVUz/MQD1BAPXENQTMQAP1dA1xV9AxExFc1NQDXNQDUxAAAPD/ww3BV8FxVfDAcVwVcwMMFwXwxV8BwVV/FVVV/DMBXwXFV8DBcMFVX/AcEXBV8MED0Q0QH9ENED0Q0AN1fVNQDV1dU/VNQA9EAP1dU11AAB9/RDRAw0QN1dEfBDRcEDfDRcEEdw0QfRR0QP1111FXdXV0DXV1T1TUNAPXRNQA/MQD1FMQDdXRM1EN0DUAP9Q0AAAP3V1DUPUAPXUNA3QAAAAAAAAA%");
//        dataSource.add(hillshadeSourceWorld);
        final MapBoxElevationDataDecoder elevationDecoder = new MapBoxElevationDataDecoder();
        final HillshadeRasterTileLayer layer = hillshadeLayer = new HillshadeRasterTileLayer(hillshadeSourceFrance, elevationDecoder);
        layer.setPreloading(true);
        layer.setContrast( 0.52f);
        layer.setHeightScale(0.32f);
        layer.setVisibleZoomRange(new MapRange(0, 16));
        layer.setIlluminationMapRotationEnabled(true);
        layer.setIlluminationDirection(new MapVec(-1, 0, 0));
        layer.setTileSubstitutionPolicy(TileSubstitutionPolicy.TILE_SUBSTITUTION_POLICY_VISIBLE);
        layer.setTileFilterMode(RasterTileFilterMode.RASTER_TILE_FILTER_MODE_BILINEAR);
        layer.setHighlightColor(new Color((short) 0, (short) 0, (short) 0, (short) 170));
        layer.setShadowColor(new Color((short) 0, (short) 0, (short) 0, (short) 0));
        layer.setAccentColor(new Color((short) 0, (short) 0, (short) 0, (short) 170));

        mapView.getLayers().add(layer);
//        toggleSlopes(true);
        final CheckBox slopesCheckBox = view.findViewById(R.id.slopesCheckBox); // initiate the Seek bar
        slopesCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                toggleSlopes(b);
            }
        });
        final AppCompatSeekBar contrastSeekBar = (AppCompatSeekBar) view.findViewById(R.id.contrastSeekBar); // initiate the Seek bar
        final TextView textContrast = (TextView) view.findViewById(R.id.textContrast); // initiate the Seek bar
        contrastSeekBar.setProgress((int) (layer.getContrast() * 100.0f));
        textContrast.setText(layer.getContrast() + "");
        contrastSeekBar.setOnSeekBarChangeListener(new AppCompatSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                layer.setContrast(i / 100.0f);
                textContrast.setText(layer.getContrast() + "");
                mapView.requestRender();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });
        final AppCompatSeekBar illuminationDirectionSeekBar = (AppCompatSeekBar) view.findViewById(R.id.illuminationDirectionSeekBar); // initiate the Seek bar
        final TextView textIlluminationDirection = (TextView) view.findViewById(R.id.textIlluminationDirection); // initiate the Seek bar

        final double degrees = (Math.acos(layer.getIlluminationDirection().getY()) * 180 / Math.PI);
        illuminationDirectionSeekBar.setProgress((int) degrees);
        textIlluminationDirection.setText((int) degrees + "");
        illuminationDirectionSeekBar.setOnSeekBarChangeListener(new AppCompatSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                MapVec current = layer.getIlluminationDirection();
                double rad = i * Math.PI / 180;
                double sin = Math.sin(rad);
                double cos = Math.cos(rad);
                layer.setIlluminationDirection(new MapVec(sin, cos, current.getZ()));
                textIlluminationDirection.setText(i + "");
                mapView.requestRender();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });


        final AppCompatSeekBar heightScaleSeekBar = (AppCompatSeekBar) view.findViewById(R.id.heightScaleSeekBar); // initiate the Seek bar
        final TextView textHeightScale = (TextView) view.findViewById(R.id.textHeightScale); // initiate the Seek bar
        heightScaleSeekBar.setProgress((int) (layer.getHeightScale() * 100.0f));
        textHeightScale.setText(layer.getHeightScale() + "");
//        heightScaleSeekBar.setOnSeekBarChangeListener(new AppCompatSeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
//
//                layer.setHeightScale(i / 100.0f);
//                textHeightScale.setText(layer.getHeightScale() + "");
//                mapView.requestRender();
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//            }
//
//        });


        final AppCompatSeekBar highlightOpacitySeekBar = (AppCompatSeekBar) view.findViewById(R.id.highlightOpacitySeekBar); // initiate the Seek bar
        final TextView textHighlightOpacity = (TextView) view.findViewById(R.id.testHighlightOpacity); // initiate the Seek bar
        highlightOpacitySeekBar.setProgress(255);
        textHighlightOpacity.setText("255");
        highlightOpacitySeekBar.setOnSeekBarChangeListener(new AppCompatSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Color color = layer.getHighlightColor();
                Color highlightColor = new Color(color.getR(), color.getG(), color.getB(), (short) i);
                layer.setHighlightColor(highlightColor);
                textHighlightOpacity.setText(i + "");
                mapView.requestRender();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });
        final AppCompatSeekBar shadowOpacitySeekBar = (AppCompatSeekBar) view.findViewById(R.id.shadowOpacitySeekBar); // initiate the Seek bar
        final TextView textShadowOpacity = (TextView) view.findViewById(R.id.testShadowOpacity); // initiate the Seek bar
        shadowOpacitySeekBar.setProgress(255);
        textShadowOpacity.setText("255");
        shadowOpacitySeekBar.setOnSeekBarChangeListener(new AppCompatSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Color color = layer.getShadowColor();
                Color shadowColor = new Color(color.getR(), color.getG(), color.getB(), (short) i);
                layer.setShadowColor(shadowColor);
                textShadowOpacity.setText(i + "");
                mapView.requestRender();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });

    }

    void addMap(String dataPath) {
        MultiTileDataSource  dataSource = new MultiTileDataSource();
        MBTilesTileDataSource sourceFrance = null;
        MBTilesTileDataSource sourceItaly = null;
        MBTilesTileDataSource sourceFranceContours = null;
        MBTilesTileDataSource sourceWorld = null;
        MBVectorTileDecoder decoder = null;
        try {
            sourceFrance = new MBTilesTileDataSource( dataPath+"/france/france_full.mbtiles");
//            sourceItaly = new MBTilesTileDataSource( dataPath+"/italy/italy.mbtiles");
            sourceFranceContours = new MBTilesTileDataSource( dataPath+"/france/france_contours.mbtiles");
            sourceWorld = new MBTilesTileDataSource( dataPath+"/world.mbtiles");
            final File file = new File(dataPath+"/osm.zip");
            final FileInputStream stream = new java.io.FileInputStream(file);
            final DataInputStream dataInputStream = new java.io.DataInputStream(stream);
            final byte[] bytes = new byte[(int)file.length()];
            dataInputStream.readFully(bytes);
            decoder = new MBVectorTileDecoder(new CompiledStyleSet(new ZippedAssetPackage(new com.carto.core.BinaryData(bytes))));
        } catch (IOException e) {
            e.printStackTrace();
        }

        MergedMBVTTileDataSource mergedSource = new MergedMBVTTileDataSource(sourceFranceContours, sourceFrance);
        dataSource.add(mergedSource);
//        dataSource.add(sourceItaly);
        dataSource.add(sourceWorld);
        VectorTileLayer backlayer  = new VectorTileLayer(dataSource, decoder);
        mapView.getLayers().add(backlayer);
    }
    void addRoutes(String dataPath) {
        MultiTileDataSource  dataSource = new MultiTileDataSource();
        MBTilesTileDataSource sourceFrance = null;
        MBTilesTileDataSource sourceWorld = null;
        MBVectorTileDecoder decoder = null;
        try {
            sourceFrance = new MBTilesTileDataSource( dataPath+"/france/france_routes.mbtiles");
            sourceWorld = new MBTilesTileDataSource( dataPath+"/world_routes_9.mbtiles");
            final File file = new File(dataPath+"/inner.zip");
            final FileInputStream stream = new java.io.FileInputStream(file);
            final DataInputStream dataInputStream = new java.io.DataInputStream(stream);
            final byte[] bytes = new byte[(int)file.length()];
            dataInputStream.readFully(bytes);
            decoder = new MBVectorTileDecoder(new CompiledStyleSet(new ZippedAssetPackage(new com.carto.core.BinaryData(bytes))));
        } catch (IOException e) {
            e.printStackTrace();
        }

        dataSource.add(sourceWorld);
        dataSource.add(sourceFrance);
        VectorTileLayer backlayer  = new VectorTileLayer(dataSource, decoder);
        mapView.getLayers().add(backlayer);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void proceedWithSdCard(View view) {
        File externalPath = null;
        File[] externalPaths = getContext().getExternalFilesDirs(null);
        if (externalPaths != null && externalPaths.length > 1) {
            externalPath = externalPaths[externalPaths.length - 1];
        }
        if (externalPath == null) {
            externalPath = getContext().getExternalFilesDir(null);
        }
        String dataPath = Paths.get(externalPath.getAbsolutePath(), "../../../../alpimaps_mbtiles").normalize().toString();


        addMap(dataPath);
//        addRoutes(dataPath);
        addHillshadeLayer(view, dataPath);

//        try {
//            MBTilesTileDataSource dataSource = new MBTilesTileDataSource(dataPath+"/france/france_terrain.etiles");
//            RasterTileLayer layer = new RasterTileLayer(dataSource);
//            mapView.getLayers().add(layer);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        HTTPTileDataSource dataSource = new HTTPTileDataSource(0,19,"https://tile.openstreetmap.org/{z}/{x}/{y}.png");
//        StringMap headers = new StringMap();
//        headers.set("User-Agent", "test app");
//        dataSource.setHTTPHeaders(headers);
//        PersistentCacheTileDataSource pDataSource = new PersistentCacheTileDataSource(dataSource, getContext().getExternalFilesDir(null) + "/mapcache.db");
//        RasterTileLayer layer = new RasterTileLayer(pDataSource);
//        mapView.getLayers().add(layer);


//        testLineDrawing();
//        testLineDrawing2();
        final TextView textZoom = (TextView) view.findViewById(R.id.zoomText); // initiate the Seek bar
        mapView.setMapEventListener(new MapEventListener() {
            @Override
            public void onMapMoved() {
                super.onMapMoved();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textZoom.setText(String.format("z=%.2f", mapView.getZoom()));
                    }
                });
            }

            @Override
            public void onMapClicked(MapClickInfo mapClickInfo) {
                super.onMapClicked(mapClickInfo);
                MapPos clickPos = mapClickInfo.getClickPos();
//                Log.d(TAG, "elevation " + layer.getElevation(new MapPos(5.722772489758224, 45.182362864932706)));
            }
        });

        final Options options = mapView.getOptions();

        final Button modeButton = (Button) view.findViewById(R.id.modeButton); // initiate the Seek bar
        modeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                testValhalla(dataPath, options);
//                testMatchRoute(options);
//                testVectoTileSearch(backlayer, options);
                // Code here executes on main thread after user presses button
//                if (options.getRenderProjectionMode() == RenderProjectionMode.RENDER_PROJECTION_MODE_SPHERICAL) {
//                    options.setRenderProjectionMode(RenderProjectionMode.RENDER_PROJECTION_MODE_PLANAR);
//                } else {
//                    options.setRenderProjectionMode(RenderProjectionMode.RENDER_PROJECTION_MODE_SPHERICAL);
//                }
            }
        });
    }

    public void testLineDrawing() {

        String geojson = "{\"type\": \"FeatureCollection\", \"features\":[{\"type\":\"Feature\",\"id\":1668607642215,\"properties\":{\"name\":\"Bastille - 45.192, 5.726\",\"class\":\"pedestrian\",\"id\":1668607642215,\"zoomBounds\":{\"southwest\":{\"lat\":45.191462,\"lon\":5.719694,\"altitude\":0},\"northeast\":{\"lat\":45.198495,\"lon\":5.725541,\"altitude\":0}},\"route\":{\"costing_options\":{\"pedestrian\":{\"use_ferry\":0,\"shortest\":false,\"driveway_factor\":200,\"walkway_factor\":0.8,\"use_tracks\":1,\"sidewalk_factor\":10,\"max_hiking_difficulty\":3}},\"totalTime\":1452.0199999999995,\"totalDistance\":1994,\"type\":\"pedestrian\",\"subtype\":\"normal\"}},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[5.724991999999999,45.198263999999995],[5.724781,45.198305999999995],[5.724622999999999,45.198335],[5.724584999999999,45.198342],[5.724461,45.198384],[5.724422,45.198426],[5.724425,45.198446999999994],[5.72432,45.198468],[5.7242869999999995,45.198476],[5.724237,45.198479],[5.724199,45.198481],[5.7240899999999995,45.198488999999995],[5.724057999999999,45.198491999999995],[5.724011,45.198495],[5.724009,45.198485],[5.724072,45.198479999999996],[5.7240709999999995,45.19847],[5.723933,45.198478],[5.723930999999999,45.198467],[5.723980999999999,45.198463],[5.723978,45.198451999999996],[5.72391,45.198451999999996],[5.723897,45.19845],[5.723898,45.198442],[5.723902,45.198425],[5.723916,45.198375],[5.723934,45.198370999999995],[5.723947,45.198329],[5.723929,45.198327],[5.723917,45.198364],[5.723875,45.198358999999996],[5.723752999999999,45.198343],[5.723758,45.198324],[5.723777999999999,45.198251],[5.723794,45.198175],[5.723775,45.198163],[5.723751,45.198156999999995],[5.723688999999999,45.198175],[5.723624,45.198203],[5.7235759999999996,45.198215999999995],[5.7234989999999994,45.198217],[5.723252,45.198201999999995],[5.7232389999999995,45.198175],[5.723484,45.198088],[5.7234989999999994,45.198119999999996],[5.723387,45.198156999999995],[5.723345999999999,45.198114],[5.723256999999999,45.198051],[5.723043,45.198113],[5.722871,45.198204999999994],[5.722697,45.198249],[5.722523,45.198312],[5.722471,45.198361],[5.722366999999999,45.198356],[5.7223559999999996,45.198339999999995],[5.722421,45.198307],[5.722478,45.198291999999995],[5.722595999999999,45.19826],[5.722585,45.198246],[5.722563999999999,45.198226999999996],[5.72253,45.198195],[5.722509,45.198187999999995],[5.722351,45.198212999999996],[5.7223679999999995,45.198153],[5.722252,45.198141],[5.72222,45.198088],[5.722366,45.198060999999996],[5.722497,45.198046999999995],[5.722523,45.198026999999996],[5.722532999999999,45.197995999999996],[5.722532999999999,45.197987],[5.722532999999999,45.197981],[5.722594,45.197981],[5.722633999999999,45.197981999999996],[5.722633999999999,45.197981],[5.722624,45.197979],[5.722621,45.197976],[5.722636,45.197970999999995],[5.722601999999999,45.197961],[5.722631,45.197919],[5.722544,45.197950999999996],[5.7225909999999995,45.197888],[5.722498,45.197922999999996],[5.722549,45.197873],[5.722493999999999,45.197897999999995],[5.722448,45.197913],[5.722453,45.197899],[5.72251,45.197852],[5.722411999999999,45.197888],[5.722428,45.197858],[5.722449999999999,45.197828],[5.722411,45.197846999999996],[5.722392999999999,45.197859],[5.722364,45.197858],[5.7223559999999996,45.197838999999995],[5.722366999999999,45.197821999999995],[5.722421,45.197796],[5.722497,45.197755],[5.722506,45.197739999999996],[5.722512,45.197722],[5.722515,45.197705],[5.722499,45.1977],[5.722484,45.197708999999996],[5.722442,45.197725],[5.722403,45.197724],[5.722395,45.197705],[5.722404999999999,45.197649999999996],[5.722398,45.197581],[5.72241,45.19753],[5.7224319999999995,45.197483],[5.722421,45.197409],[5.722417,45.197331999999996],[5.722405999999999,45.197237],[5.722396,45.197148999999996],[5.722318,45.197112999999995],[5.7222219999999995,45.197075999999996],[5.722169,45.197125],[5.722085,45.197209],[5.722022,45.197269],[5.722009,45.197210999999996],[5.721998999999999,45.197136],[5.722036,45.197055],[5.722083,45.19701],[5.722144,45.196959],[5.7222159999999995,45.196928],[5.722327,45.196873],[5.722449,45.196833],[5.722517,45.196781],[5.722595999999999,45.196709999999996],[5.72264,45.196687999999995],[5.722669,45.196639999999995],[5.722707,45.196518999999995],[5.722725,45.196402],[5.722696,45.196284],[5.722702,45.196157],[5.722684,45.196053],[5.722614999999999,45.195842999999996],[5.722516,45.195705],[5.722417999999999,45.195591],[5.722369,45.195533],[5.722322999999999,45.195437],[5.722347999999999,45.195391],[5.72235,45.195353],[5.722313,45.195299],[5.722179,45.195167],[5.722080999999999,45.195015],[5.721939,45.194826],[5.721836,45.194733],[5.721722,45.194655],[5.721741,45.194632],[5.721741,45.194614],[5.721667,45.194519],[5.72166,45.194509],[5.721617,45.194525999999996],[5.721609,45.19452],[5.721653,45.1945],[5.721645,45.194489999999995],[5.7215929999999995,45.194514],[5.72122,45.194590999999996],[5.721182,45.194579],[5.721137,45.194576],[5.721083,45.194593999999995],[5.721041,45.194627999999994],[5.721,45.19464],[5.720738,45.194770999999996],[5.720686,45.194803],[5.720632,45.194778],[5.720625999999999,45.194773999999995],[5.7206209999999995,45.19477],[5.72052,45.194811],[5.720431,45.194809],[5.720349,45.194773],[5.720244999999999,45.194638999999995],[5.720203,45.194590999999996],[5.720161,45.194579],[5.720134,45.194562999999995],[5.720116,45.194541],[5.720111999999999,45.194488],[5.72009,45.194468],[5.7200359999999995,45.194455999999995],[5.720003,45.194438999999996],[5.7199979999999995,45.194403],[5.7199789999999995,45.194387],[5.7199279999999995,45.194331],[5.7198709999999995,45.194317999999996],[5.719755999999999,45.194291],[5.719694,45.194244999999995],[5.7197059999999995,45.194187],[5.719761999999999,45.194167],[5.71981,45.194213],[5.719823,45.194216],[5.719863999999999,45.194224],[5.719895999999999,45.194237],[5.719936,45.194199],[5.719949,45.194191],[5.7199409999999995,45.194181],[5.719945,45.194168],[5.719965999999999,45.194168],[5.720041,45.194181],[5.72011,45.194143],[5.720148,45.194072],[5.720155999999999,45.194005999999995],[5.720142999999999,45.193894],[5.720135,45.193822999999995],[5.720127,45.193774999999995],[5.720072,45.193737999999996],[5.720044,45.193673],[5.7200679999999995,45.193611],[5.720184,45.193559],[5.720178,45.193509],[5.720289999999999,45.193508],[5.720431,45.193506],[5.721293999999999,45.19354],[5.721585,45.193549],[5.721619,45.193549999999995],[5.723077,45.193596],[5.723552,45.193624],[5.72358,45.193591],[5.723805,45.193608999999995],[5.723789,45.193574],[5.723806,45.193525],[5.72385,45.193526999999996],[5.723913,45.193535999999995],[5.723964,45.193543999999996],[5.723967,45.19352],[5.72419,45.192761],[5.7241979999999995,45.192735],[5.724209,45.192727999999995],[5.7242239999999995,45.192671],[5.724235999999999,45.192616],[5.724412999999999,45.19265],[5.724696,45.192709],[5.724729,45.192715],[5.724765,45.192721999999996],[5.724876999999999,45.19231],[5.724895,45.192285999999996],[5.724959,45.192254],[5.7250179999999995,45.192218],[5.725121,45.191857999999996],[5.725156,45.191828],[5.725169999999999,45.191769],[5.725185,45.191704],[5.725249,45.191478],[5.725264,45.191466],[5.725541,45.191462]]}}]}";
        String cartoCss =
                "#items {\n" +
                        "  line-color: #374C70;\n" +
                        "  line-cap: round;\n" +
                        "  line-join: round;\n" +
                        "  line-width: 12;\n" +
                        "}";

        MBVectorTileDecoder decoder = new MBVectorTileDecoder(new CartoCSSStyleSet(cartoCss));

        GeoJSONVectorTileDataSource dataSource = new GeoJSONVectorTileDataSource(0,24);
        try {
            dataSource.createLayer("items");
            dataSource.setLayerGeoJSON(1,com.carto.core.Variant.fromString(geojson));
            VectorTileLayer mbLayer = new VectorTileLayer(dataSource,decoder);
            mapView.getLayers().add(mbLayer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mapView.setFocusPos(new MapPos(5.72476358599884, 45.19272038067931), 0);
        mapView.setZoom(15f, 0);
    }


    public void testLineDrawing2() {
        final Options options = mapView.getOptions();

        String geojson = "{\"type\": \"FeatureCollection\", \"features\":[{\"type\":\"Feature\",\"id\":1668607642215,\"properties\":{\"name\":\"Bastille - 45.192, 5.726\",\"class\":\"pedestrian\",\"id\":1668607642215,\"zoomBounds\":{\"southwest\":{\"lat\":45.191462,\"lon\":5.719694,\"altitude\":0},\"northeast\":{\"lat\":45.198495,\"lon\":5.725541,\"altitude\":0}},\"route\":{\"costing_options\":{\"pedestrian\":{\"use_ferry\":0,\"shortest\":false,\"driveway_factor\":200,\"walkway_factor\":0.8,\"use_tracks\":1,\"sidewalk_factor\":10,\"max_hiking_difficulty\":3}},\"totalTime\":1452.0199999999995,\"totalDistance\":1994,\"type\":\"pedestrian\",\"subtype\":\"normal\"}},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[5.724991999999999,45.198263999999995],[5.724781,45.198305999999995],[5.724622999999999,45.198335],[5.724584999999999,45.198342],[5.724461,45.198384],[5.724422,45.198426],[5.724425,45.198446999999994],[5.72432,45.198468],[5.7242869999999995,45.198476],[5.724237,45.198479],[5.724199,45.198481],[5.7240899999999995,45.198488999999995],[5.724057999999999,45.198491999999995],[5.724011,45.198495],[5.724009,45.198485],[5.724072,45.198479999999996],[5.7240709999999995,45.19847],[5.723933,45.198478],[5.723930999999999,45.198467],[5.723980999999999,45.198463],[5.723978,45.198451999999996],[5.72391,45.198451999999996],[5.723897,45.19845],[5.723898,45.198442],[5.723902,45.198425],[5.723916,45.198375],[5.723934,45.198370999999995],[5.723947,45.198329],[5.723929,45.198327],[5.723917,45.198364],[5.723875,45.198358999999996],[5.723752999999999,45.198343],[5.723758,45.198324],[5.723777999999999,45.198251],[5.723794,45.198175],[5.723775,45.198163],[5.723751,45.198156999999995],[5.723688999999999,45.198175],[5.723624,45.198203],[5.7235759999999996,45.198215999999995],[5.7234989999999994,45.198217],[5.723252,45.198201999999995],[5.7232389999999995,45.198175],[5.723484,45.198088],[5.7234989999999994,45.198119999999996],[5.723387,45.198156999999995],[5.723345999999999,45.198114],[5.723256999999999,45.198051],[5.723043,45.198113],[5.722871,45.198204999999994],[5.722697,45.198249],[5.722523,45.198312],[5.722471,45.198361],[5.722366999999999,45.198356],[5.7223559999999996,45.198339999999995],[5.722421,45.198307],[5.722478,45.198291999999995],[5.722595999999999,45.19826],[5.722585,45.198246],[5.722563999999999,45.198226999999996],[5.72253,45.198195],[5.722509,45.198187999999995],[5.722351,45.198212999999996],[5.7223679999999995,45.198153],[5.722252,45.198141],[5.72222,45.198088],[5.722366,45.198060999999996],[5.722497,45.198046999999995],[5.722523,45.198026999999996],[5.722532999999999,45.197995999999996],[5.722532999999999,45.197987],[5.722532999999999,45.197981],[5.722594,45.197981],[5.722633999999999,45.197981999999996],[5.722633999999999,45.197981],[5.722624,45.197979],[5.722621,45.197976],[5.722636,45.197970999999995],[5.722601999999999,45.197961],[5.722631,45.197919],[5.722544,45.197950999999996],[5.7225909999999995,45.197888],[5.722498,45.197922999999996],[5.722549,45.197873],[5.722493999999999,45.197897999999995],[5.722448,45.197913],[5.722453,45.197899],[5.72251,45.197852],[5.722411999999999,45.197888],[5.722428,45.197858],[5.722449999999999,45.197828],[5.722411,45.197846999999996],[5.722392999999999,45.197859],[5.722364,45.197858],[5.7223559999999996,45.197838999999995],[5.722366999999999,45.197821999999995],[5.722421,45.197796],[5.722497,45.197755],[5.722506,45.197739999999996],[5.722512,45.197722],[5.722515,45.197705],[5.722499,45.1977],[5.722484,45.197708999999996],[5.722442,45.197725],[5.722403,45.197724],[5.722395,45.197705],[5.722404999999999,45.197649999999996],[5.722398,45.197581],[5.72241,45.19753],[5.7224319999999995,45.197483],[5.722421,45.197409],[5.722417,45.197331999999996],[5.722405999999999,45.197237],[5.722396,45.197148999999996],[5.722318,45.197112999999995],[5.7222219999999995,45.197075999999996],[5.722169,45.197125],[5.722085,45.197209],[5.722022,45.197269],[5.722009,45.197210999999996],[5.721998999999999,45.197136],[5.722036,45.197055],[5.722083,45.19701],[5.722144,45.196959],[5.7222159999999995,45.196928],[5.722327,45.196873],[5.722449,45.196833],[5.722517,45.196781],[5.722595999999999,45.196709999999996],[5.72264,45.196687999999995],[5.722669,45.196639999999995],[5.722707,45.196518999999995],[5.722725,45.196402],[5.722696,45.196284],[5.722702,45.196157],[5.722684,45.196053],[5.722614999999999,45.195842999999996],[5.722516,45.195705],[5.722417999999999,45.195591],[5.722369,45.195533],[5.722322999999999,45.195437],[5.722347999999999,45.195391],[5.72235,45.195353],[5.722313,45.195299],[5.722179,45.195167],[5.722080999999999,45.195015],[5.721939,45.194826],[5.721836,45.194733],[5.721722,45.194655],[5.721741,45.194632],[5.721741,45.194614],[5.721667,45.194519],[5.72166,45.194509],[5.721617,45.194525999999996],[5.721609,45.19452],[5.721653,45.1945],[5.721645,45.194489999999995],[5.7215929999999995,45.194514],[5.72122,45.194590999999996],[5.721182,45.194579],[5.721137,45.194576],[5.721083,45.194593999999995],[5.721041,45.194627999999994],[5.721,45.19464],[5.720738,45.194770999999996],[5.720686,45.194803],[5.720632,45.194778],[5.720625999999999,45.194773999999995],[5.7206209999999995,45.19477],[5.72052,45.194811],[5.720431,45.194809],[5.720349,45.194773],[5.720244999999999,45.194638999999995],[5.720203,45.194590999999996],[5.720161,45.194579],[5.720134,45.194562999999995],[5.720116,45.194541],[5.720111999999999,45.194488],[5.72009,45.194468],[5.7200359999999995,45.194455999999995],[5.720003,45.194438999999996],[5.7199979999999995,45.194403],[5.7199789999999995,45.194387],[5.7199279999999995,45.194331],[5.7198709999999995,45.194317999999996],[5.719755999999999,45.194291],[5.719694,45.194244999999995],[5.7197059999999995,45.194187],[5.719761999999999,45.194167],[5.71981,45.194213],[5.719823,45.194216],[5.719863999999999,45.194224],[5.719895999999999,45.194237],[5.719936,45.194199],[5.719949,45.194191],[5.7199409999999995,45.194181],[5.719945,45.194168],[5.719965999999999,45.194168],[5.720041,45.194181],[5.72011,45.194143],[5.720148,45.194072],[5.720155999999999,45.194005999999995],[5.720142999999999,45.193894],[5.720135,45.193822999999995],[5.720127,45.193774999999995],[5.720072,45.193737999999996],[5.720044,45.193673],[5.7200679999999995,45.193611],[5.720184,45.193559],[5.720178,45.193509],[5.720289999999999,45.193508],[5.720431,45.193506],[5.721293999999999,45.19354],[5.721585,45.193549],[5.721619,45.193549999999995],[5.723077,45.193596],[5.723552,45.193624],[5.72358,45.193591],[5.723805,45.193608999999995],[5.723789,45.193574],[5.723806,45.193525],[5.72385,45.193526999999996],[5.723913,45.193535999999995],[5.723964,45.193543999999996],[5.723967,45.19352],[5.72419,45.192761],[5.7241979999999995,45.192735],[5.724209,45.192727999999995],[5.7242239999999995,45.192671],[5.724235999999999,45.192616],[5.724412999999999,45.19265],[5.724696,45.192709],[5.724729,45.192715],[5.724765,45.192721999999996],[5.724876999999999,45.19231],[5.724895,45.192285999999996],[5.724959,45.192254],[5.7250179999999995,45.192218],[5.725121,45.191857999999996],[5.725156,45.191828],[5.725169999999999,45.191769],[5.725185,45.191704],[5.725249,45.191478],[5.725264,45.191466],[5.725541,45.191462]]}}]}";

        GeoJSONGeometryReader reader = new GeoJSONGeometryReader();
        reader.setTargetProjection(options.getBaseProjection());
        FeatureCollection collection = reader.readFeatureCollection(geojson);

        GeoJSONVectorTileDataSource dataSource = new GeoJSONVectorTileDataSource(0,24);

        LineStyleBuilder builder = new LineStyleBuilder();
        builder.setWidth(9);
        builder.setColor(new Color((short) 255, (short)0,(short)0,(short)255));
        builder.setLineJoinType(LineJoinType.LINE_JOIN_TYPE_MITER);
        builder.setColor(new Color((short) 255, (short) 0, (short) 0, (short) 255));
        Line line = new Line((LineGeometry) collection.getFeature(0).getGeometry(), builder.buildStyle());

        LocalVectorDataSource localSource = new LocalVectorDataSource(options.getBaseProjection());
        VectorLayer vectorLayer = new VectorLayer(localSource);
        mapView.getLayers().add(vectorLayer);
        localSource.add(line);

        mapView.setFocusPos(new MapPos(5.72476358599884, 45.19272038067931), 0);
        mapView.setZoom(15f, 0);
    }


    public void testVectoTileSearch(final VectorTileLayer layer, final Options options) {

        final VectorTileSearchService searchService = new VectorTileSearchService(layer.getDataSource(), layer.getTileDecoder());
        searchService.setMaxZoom(14);
        searchService.setMinZoom(14);
        MapPosVector vector = new MapPosVector();
        vector.add(new MapPos(5.853,45.096));
        vector.add(new MapPos(6.009,45.225));
        PolygonGeometry boundsGeo = new PolygonGeometry(vector);
        final SearchRequest request = new SearchRequest();
        request.setFilterExpression("layer::name='route' AND osmid=-2535348");
        request.setGeometry(boundsGeo);
        request.setProjection(options.getBaseProjection());
        new Thread() {
            public void run() {
                final VectorTileFeatureCollection result = searchService.findFeatures(request);
                MapPosVectorVector points = new MapPosVectorVector();
                for (int i=0; i < result.getFeatureCount(); i++){
                    Feature feature = result.getFeature(i);
                    Geometry geometry = feature.getGeometry();
                    if (geometry instanceof MultiLineGeometry) {
                        for (int j=0; j < ((MultiLineGeometry)geometry).getGeometryCount(); j++) {
                            points.add(((MultiLineGeometry) geometry).getGeometry(j).getPoses());
                        }
                    } else {
                        points.add(((LineGeometry)geometry).getPoses());
                    }
                }
//                LineStyleBuilder builder = new LineStyleBuilder();
//                builder.setWidth(4);
//                builder.setColor(new Color((short) 255, (short) 0, (short) 0, (short) 255));
//                Line line = new Line(points, builder.buildStyle());
//                localSource.add(line);
                new Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "findFeatures done " + result.getFeatureCount());
                    }

                });
            }
        }.start();
    }


    public void runValhallaInThread(final MultiValhallaOfflineRoutingService routingService, final RoutingRequest request, String profile, final LocalVectorDataSource localSource) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    RoutingResult result = routingService.calculateRoute(request);
                    Log.d(TAG,"rawresult "+ result.getRawResult());

//                    MapPosVector pointsWithAltitude = new MapPosVector();
                    if (result != null) {
                        MapPosVector points = result.getPoints();
                        Log.d(TAG, "showing route " + points.size());
                        LineStyleBuilder builder = new LineStyleBuilder();
                        builder.setWidth(4);
                        builder.setColor(new Color((short) 255, (short) 0, (short) 0, (short) 255));
                        Line line = new Line(points, builder.buildStyle());

                        localSource.add(line);
                        MapBounds bounds = line.getGeometry().getBounds();
                        mapView.moveToFitBounds(bounds, new ScreenBounds(), false, 0);
                    }
//                    RouteMatchingRequest matchrequest = new RouteMatchingRequest(request.getProjection(), result.getPoints(), 1);
//                    matchrequest.setCustomParameter("shape_match", new Variant("edge_walk"));
//                    matchrequest.setCustomParameter("filters", Variant.fromString("{ \"attributes\": [\"edge.surface\", \"edge.road_class\", \"edge.sac_scale\", \"edge.use\", \"edge.length\"], \"action\": \"include\" }"));
//                    RouteMatchingResult matchresult = routingService.matchRoute(matchrequest);
//                    largeLog(TAG,"matchresult "+ matchresult.getRawResult());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void matchRouteTest(final MultiValhallaOfflineRoutingService routingService, Projection projection, String profile) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    routingService.setProfile(profile);
                    MapPosVector vector = new MapPosVector();
                    vector.add(new MapPos(5.721619, 45.193549999999995));
                    vector.add(new MapPos(5.721585, 45.193549));
                    RouteMatchingRequest matchrequest = new RouteMatchingRequest(projection, vector, 1);
                    matchrequest.setCustomParameter("shape_match", new Variant("edge_walk"));
                    matchrequest.setCustomParameter("filters", Variant.fromString("{ \"attributes\": [\"edge.surface\", \"edge.road_class\", \"edge.sac_scale\", \"edge.use\"], \"action\": \"include\" }"));
                    RouteMatchingResult matchresult = routingService.matchRoute(matchrequest);
                    largeLog(TAG,"matchresult "+ matchresult.getRawResult());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

        public void testMatchRoute(String dataPath, Options options) {
            Projection projection = options.getBaseProjection();
            MultiValhallaOfflineRoutingService routingService = new MultiValhallaOfflineRoutingService();
            routingService.add(dataPath+"/france.vtiles");
            LocalVectorDataSource localSource = new LocalVectorDataSource(projection);
            Timer timer = new Timer();
            TimerTask task = new MathRouteTask(routingService, options.getBaseProjection(), "pedestrian");
            timer.schedule(task, 0, 500);
        }

    public void testValhalla(String dataPath, Options options) {
        Projection projection = options.getBaseProjection();
        MultiValhallaOfflineRoutingService routingService = new MultiValhallaOfflineRoutingService();
//        routingService.add(dataPath+"/italy/italy.vtiles");
        routingService.add(dataPath+"/france/france.vtiles");
        LocalVectorDataSource localSource = new LocalVectorDataSource(projection);
        VectorLayer vectorLayer = new VectorLayer(localSource);
        mapView.getLayers().add(vectorLayer);
        MapPosVector vector = new MapPosVector();
        vector.add(new MapPos(5.7233, 45.1924));
        vector.add(new MapPos(5.7247, 45.1992));
        RoutingRequest request = new RoutingRequest(projection, vector);
        request.setCustomParameter("costing_options", Variant.fromString("{\"pedestrian\":{\"use_ferry\":0,\"shortest\":false,\"use_hills\":1,\"max_hiking_difficulty\":6,\"step_penalty\":10,\"driveway_factor\":200,\"use_roads\":0,\"use_tracks\":1,\"walking_speed\":4,\"sidewalk_factor\":10}}"));
        request.setCustomParameter("directions_options", Variant.fromString("{\"language\":\"en\"}"));
        runValhallaInThread(routingService, request, "pedestrian", localSource);
//            request = new RoutingRequest(projection, vector);
//            request.setCustomParameter("costing_options", Variant.fromString("{\"pedestrian\":{\"driveway_factor\":10,\"max_hiking_difficulty\":6,\"shortest\":false,\"step_penalty\":1,\"use_ferry\":0,\"use_hills\":0,\"use_roads\":0,\"use_tracks\":1,\"walking_speed\":4}},\"directions_options\":{\"language\":\"en\"}}"));
//            runValhallaInThread(routingService, request, "pedestrian", localSource);
//            request = new RoutingRequest(projection, vector);
//            request.setCustomParameter("costing_options", Variant.fromString("{\"pedestrian\":{\"driveway_factor\":10,\"max_hiking_difficulty\":6,\"shortest\":true,\"step_penalty\":5,\"use_ferry\":0,\"use_hills\":1,\"use_roads\":0,\"use_tracks\":1,\"walking_speed\":4}},\"directions_options\":{\"language\":\"en\"}}"));
//            runValhallaInThread(routingService, request, "pedestrian", localSource);
    }
    public static void largeLog(String tag, String content) {
        if (content.length() > 4000) {
            Log.d(tag, content.substring(0, 4000));
            largeLog(tag, content.substring(4000));
        } else {
            Log.d(tag, content);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.second_fragment, container, false);
        com.carto.utils.Log.setShowInfo(true);
        com.carto.utils.Log.setShowDebug(true);
        com.carto.utils.Log.setShowWarn(true);
        com.carto.utils.Log.setShowError(true);

        final MapView mapView = this.mapView = (MapView) view.findViewById(R.id.mapView);
        final EPSG4326 projection = new EPSG4326();
        final Options options = mapView.getOptions();
        options.setZoomGestures(true);
        options.setRestrictedPanning(true);
        options.setSeamlessPanning(true);
        options.setRotatable(true);
//        options.setTileThreadPoolSize(1);

//        options.setRenderProjectionMode(RenderProjectionMode.RENDER_PROJECTION_MODE_SPHERICAL);
        options.setPanningMode(PanningMode.PANNING_MODE_STICKY);
        options.setBaseProjection(projection);
        mapView.setFocusPos(new MapPos(5.7279, 45.1949), 0);
        mapView.setZoom(17f, 0);
        checkStoragePermission(view);

        return view;
    }

}
