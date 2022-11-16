package com.akylas.cartotest.ui.main;

import java.util.Timer;
import java.util.TimerTask;
import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import com.carto.components.RenderProjectionMode;
import com.carto.core.MapPos;
import com.carto.core.MapPosVector;
import com.carto.core.MapPosVectorVector;
import com.carto.core.MapRange;
import com.carto.core.MapVec;
import com.carto.core.StringVector;
import com.carto.core.Variant;
import com.carto.datasources.HTTPTileDataSource;
import com.carto.datasources.LocalVectorDataSource;
import com.carto.datasources.MBTilesTileDataSource;
import com.carto.datasources.TileDataSource;
import com.carto.geometry.Feature;
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
import com.carto.routing.RouteMatchingRequest;
import com.carto.routing.RouteMatchingResult;
import com.carto.routing.RoutingRequest;
import com.carto.routing.RoutingResult;
import com.carto.routing.ValhallaOfflineRoutingService;
import com.carto.search.SearchRequest;
import com.carto.search.VectorTileSearchService;
import com.carto.styles.CompiledStyleSet;
import com.carto.styles.LineStyleBuilder;
import com.carto.ui.MapClickInfo;
import com.carto.ui.MapEventListener;
import com.carto.ui.MapView;
import com.carto.utils.ZippedAssetPackage;
import com.carto.vectorelements.Line;
import com.carto.vectortiles.MBVectorTileDecoder;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.fragment.app.Fragment;

public class SecondFragment extends Fragment {
    private final String TAG = "SecondFragment";


    class MathRouteTask extends TimerTask
    {
        MathRouteTask(ValhallaOfflineRoutingService routingService, Projection projection, String profile) {
            super();
            this.profile = profile;
            this.projection = projection;
            this.routingService = routingService;
        }
        ValhallaOfflineRoutingService routingService;
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
    MapView mapView;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_CODE_WRITE_STORAGE) {
            if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                proceedWithSdCard(this.getView());
            }
        }
    }

    void proceedWithSdCard(View view) {

        MBTilesTileDataSource sourceFrance = null;
        MBVectorTileDecoder decoder = null;
        try {
            sourceFrance = new MBTilesTileDataSource( "/storage/1C05-0202/alpimaps_mbtiles/france/france_full.mbtiles");
            final File file = new File("/storage/1C05-0202/alpimaps_mbtiles/osm.zip");
            final FileInputStream stream = new java.io.FileInputStream(file);
            final DataInputStream dataInputStream = new java.io.DataInputStream(stream);
            final byte[] bytes = new byte[(int)file.length()];
            dataInputStream.readFully(bytes);
            decoder = new MBVectorTileDecoder(new CompiledStyleSet(new ZippedAssetPackage(new com.carto.core.BinaryData(bytes))));
        } catch (IOException e) {
            e.printStackTrace();
        }
        VectorTileLayer backlayer  = new VectorTileLayer(sourceFrance, decoder);
//        backlayer.setMaxOverzoomLevel(1);
        mapView.getLayers().add(backlayer);

        final TextView textZoom = (TextView) view.findViewById(R.id.zoomText); // initiate the Seek bar
        mapView.setMapEventListener(new MapEventListener() {
            @Override
            public void onMapMoved() {
                super.onMapMoved();
                Log.d(TAG, "onMapMoved " + mapView.getFocusPos());
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
                Log.d(TAG, "onMapClicked " + clickPos);
//                Log.d(TAG, "elevation " + layer.getElevation(new MapPos(5.722772489758224, 45.182362864932706)));
            }
        });

        final Options options = mapView.getOptions();

        final Button modeButton = (Button) view.findViewById(R.id.modeButton); // initiate the Seek bar
        modeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                testValhalla(hillshadeLayer, options);
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


    public void runValhallaInThread(final ValhallaOfflineRoutingService routingService, final RoutingRequest request, String profile, final LocalVectorDataSource localSource) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    RoutingResult result = routingService.calculateRoute(request);
//                    Log.d(TAG,"rawresult "+ result.getRawResult());

                    MapPosVector pointsWithAltitude = new MapPosVector();
                    if (result != null) {
                        MapPosVector points = result.getPoints();
                        Log.d(TAG, "showing route " + points.size());
                        LineStyleBuilder builder = new LineStyleBuilder();
                        builder.setWidth(4);
                        builder.setColor(new Color((short) 255, (short) 0, (short) 0, (short) 255));
                        Line line = new Line(points, builder.buildStyle());
                        localSource.add(line);
                    }
                    RouteMatchingRequest matchrequest = new RouteMatchingRequest(request.getProjection(), result.getPoints(), 1);
                    matchrequest.setCustomParameter("shape_match", new Variant("edge_walk"));
                    matchrequest.setCustomParameter("filters", Variant.fromString("{ \"attributes\": [\"edge.surface\", \"edge.road_class\", \"edge.sac_scale\", \"edge.use\", \"edge.length\"], \"action\": \"include\" }"));
                    RouteMatchingResult matchresult = routingService.matchRoute(matchrequest);
//                    largeLog(TAG,"matchresult "+ matchresult.getRawResult());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void matchRouteTest(final ValhallaOfflineRoutingService routingService, Projection projection, String profile) {
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
//                    largeLog(TAG,"matchresult "+ matchresult.getRawResult());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void testMatchRoute(Options options) {
        Projection projection = options.getBaseProjection();
        ValhallaOfflineRoutingService routingService;
        try {
            routingService = new ValhallaOfflineRoutingService("/storage/1C05-0202/alpimaps_mbtiles/france.vtiles");
            LocalVectorDataSource localSource = new LocalVectorDataSource(projection);
            Timer timer = new Timer();
            TimerTask task = new MathRouteTask(routingService, options.getBaseProjection(), "pedestrian");
            timer.schedule(task, 0, 500);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testValhalla(HillshadeRasterTileLayer layer, Options options) {
        Projection projection = options.getBaseProjection();
        ValhallaOfflineRoutingService routingService;
        try {
            routingService = new ValhallaOfflineRoutingService("/storage/1C05-0202/alpimaps_mbtiles/france.vtiles");
            LocalVectorDataSource localSource = new LocalVectorDataSource(projection);
            VectorLayer vectorLayer = new VectorLayer(localSource);
            mapView.getLayers().add(vectorLayer);
            MapPosVector vector = new MapPosVector();
            vector.add(new MapPos(5.720390081405645, 45.18661183942357));
            vector.add(new MapPos(5.733468532562251, 45.21845651921567));
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
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        Log.d(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.second_fragment, container, false);

//        MapView.registerLicense("XTUMwQ0ZRQ0RLZEM4Z1dMdkc1TDZkZy83RlN3Z0V2aTB5d0lVSlEwbGZNZjV5bDJLMnlPWXFJYWpVWmhuQWtZPQoKYXBwVG9rZW49OWYwZjBhMDgtZGQ1Mi00NjVkLTg5N2YtMTg0MDYzODQxMDBiCnBhY2thZ2VOYW1lPWNvbS5ha3lsYXMuY2FydG90ZXN0Cm9ubGluZUxpY2Vuc2U9MQpwcm9kdWN0cz1zZGstYW5kcm9pZC00LioKd2F0ZXJtYXJrPWNhcnRvZGIK", this.getContext());

        com.carto.utils.Log.setShowInfo(true);
        com.carto.utils.Log.setShowDebug(true);
        com.carto.utils.Log.setShowWarn(true);
        com.carto.utils.Log.setShowError(true);

        final MapView mapView = this.mapView = (MapView) view.findViewById(R.id.mapView);

        //        MapView.registerLicense("XTUMwQ0ZRQ0RLZEM4Z1dMdkc1TDZkZy83RlN3Z0V2aTB5d0lVSlEwbGZNZjV5bDJLMnlPWXFJYWpVWmhuQWtZPQoKYXBwVG9rZW49OWYwZjBhMDgtZGQ1Mi00NjVkLTg5N2YtMTg0MDYzODQxMDBiCnBhY2thZ2VOYW1lPWNvbS5ha3lsYXMuY2FydG90ZXN0Cm9ubGluZUxpY2Vuc2U9MQpwcm9kdWN0cz1zZGstYW5kcm9pZC00LioKd2F0ZXJtYXJrPWNhcnRvZGIK", this.getContext());
//        HTTPTileDataSource source = new HTTPTileDataSource(1, 20, "http://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png");
//        StringVector subdomains = new StringVector();
//        subdomains.add("a");
//        subdomains.add("b");
//        subdomains.add("c");
//        source.setSubdomains(subdomains);
//        final RasterTileLayer backlayer = new RasterTileLayer(source);
////        final CartoOnlineVectorTileLayer backlayer = new CartoOnlineVectorTileLayer(CartoBaseMapStyle.CARTO_BASEMAP_STYLE_POSITRON);
//        final AppCompatSeekBar opacitySeekBar = (AppCompatSeekBar) view.findViewById(R.id.opacitySeekBar); // initiate the Seek bar
//        final TextView textOpacity = (TextView) view.findViewById(R.id.textOpacity); // initiate the Seek bar
//        opacitySeekBar.setProgress(100);
//        textOpacity.setText(backlayer.getOpacity() + "");
//        opacitySeekBar.setOnSeekBarChangeListener(new AppCompatSeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
//                backlayer.setOpacity(i / 100.0f);
//                textOpacity.setText(backlayer.getOpacity() + "");
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


        final EPSG4326 projection = new EPSG4326();

        final Options options = mapView.getOptions();
        options.setZoomGestures(true);
        options.setRestrictedPanning(true);
        options.setSeamlessPanning(true);
        options.setRotatable(true);
//        options.setRenderProjectionMode(RenderProjectionMode.RENDER_PROJECTION_MODE_SPHERICAL);
        options.setPanningMode(PanningMode.PANNING_MODE_STICKY);
        options.setBaseProjection(projection);
//        mapView.getLayers().add(backlayer);
        mapView.setFocusPos(new MapPos(5.7562, 45.175), 0);
        mapView.setZoom(13.4f, 0);
        com.carto.utils.Log.setShowInfo(true);
        com.carto.utils.Log.setShowDebug(true);
        com.carto.utils.Log.setShowWarn(true);
        com.carto.utils.Log.setShowError(true);
        checkStoragePermission(view);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        Animator result = super.onCreateAnimator(transit, enter, nextAnim);
        Log.d(TAG, "onCreateAnimator " + result);
        return result;
    }

}