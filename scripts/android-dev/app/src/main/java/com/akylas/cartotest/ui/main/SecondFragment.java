package com.akylas.cartotest.ui.main;

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
import com.carto.datasources.MultiTileDataSource;
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
import com.carto.layers.VectorLayer;
import com.carto.layers.VectorTileLayer;
import com.carto.projections.EPSG4326;
import com.carto.projections.Projection;
import com.carto.rastertiles.MapBoxElevationDataDecoder;
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

    public static SecondFragment newInstance() {
        return new SecondFragment();
    }

    private static final int REQUEST_PERMISSIONS_CODE_WRITE_STORAGE = 1435;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_CODE_WRITE_STORAGE) {
            if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                proceedWithSdCard(this.getView());
            }
        }
    }
    void addHillshadeLayer(View view) {
        MBTilesTileDataSource hillshadeSourceFrance = null;
        MBTilesTileDataSource hillshadeSourceWorld = null;
        MultiTileDataSource  dataSource = new MultiTileDataSource();
        try {
            hillshadeSourceFrance = new MBTilesTileDataSource( "/storage/1C05-0202/alpimaps_mbtiles/france/terrain_25m_webp.etiles");
            hillshadeSourceWorld = new MBTilesTileDataSource( "/storage/1C05-0202/alpimaps_mbtiles/world_terrain.etiles");
//            hillshadeSource = this.hillshadeSource = new HTTPTileDataSource(5, 11, "http://192.168.1.45:8080/data/BDALTIV2_75M_rvb/{z}/{x}/{y}.png");
            //        HTTPTileDataSource hillshadeSource =   new HTTPTileDataSource(1, 15, "https://api.mapbox.com/v4/mapbox.terrain-rgb/{z}/{x}/{y}.pngraw?access_token=pk.eyJ1IjoiYWt5bGFzIiwiYSI6IkVJVFl2OXMifQ.TGtrEmByO3-99hA0EI44Ew");
        } catch (Exception e) {
            e.printStackTrace();
        }
        dataSource.add(hillshadeSourceFrance);
        dataSource.add(hillshadeSourceWorld);
        final MapBoxElevationDataDecoder elevationDecoder = new MapBoxElevationDataDecoder();
        final HillshadeRasterTileLayer layer = hillshadeLayer = new HillshadeRasterTileLayer(dataSource, elevationDecoder);
        layer.setContrast(1.0f);
        layer.setHeightScale(0.0625f);
        layer.setVisibleZoomRange(new MapRange(0, 14));
        layer.setIlluminationMapRotationEnabled(true);
        layer.setIlluminationDirection(new MapVec(-1, 0, 0));
//        layer.setTileFilterMode(RasterTileFilterMode.RASTER_TILE_FILTER_MODE_BICUBIC);
        layer.setHighlightColor(new Color((short) 125, (short) 216, (short) 79, (short) 255));
        layer.setShadowColor(new Color((short) 176, (short) 145, (short) 91, (short) 255));
        layer.setAccentColor(new Color((short) 34, (short) 67, (short) 252, (short) 255));
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

//        layer.setZoomLevelBias(1);
//        layer.setOpacity(0.5f);
        mapView.getLayers().add(layer);
    }
    void proceedWithSdCard(View view) {

        MultiTileDataSource  dataSource = new MultiTileDataSource();
        MBTilesTileDataSource sourceFrance = null;
        MBTilesTileDataSource sourceWorld = null;
        MBVectorTileDecoder decoder = null;
        try {
            sourceFrance = new MBTilesTileDataSource( "/storage/1C05-0202/alpimaps_mbtiles/france/routes.mbtiles");
            sourceWorld = new MBTilesTileDataSource( "/storage/1C05-0202/alpimaps_mbtiles/world.mbtiles");
            final File file = new File("/storage/1C05-0202/alpimaps_mbtiles/inner.zip");
            final FileInputStream stream = new java.io.FileInputStream(file);
            final DataInputStream dataInputStream = new java.io.DataInputStream(stream);
            final byte[] bytes = new byte[(int)file.length()];
            dataInputStream.readFully(bytes);
            decoder = new MBVectorTileDecoder(new CompiledStyleSet(new ZippedAssetPackage(new com.carto.core.BinaryData(bytes))));
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataSource.add(sourceFrance, "wzAzMzDAwMBwwwXFfBcVXAzAxE8BcVfMxXFXMBFfxVVVwMzMBMVwMB8RVPHFV9QQ1xDUPwMDFfMRwFVzwFXMVxFVUz/MQD1BAPXENQTMQAP1dA1xV9AxExFc1NQDXNQDUxAAAPD/ww3BV8FxVfDAcVwVcwMMFwXwxV8BwVV/FVVV/DMBXwXFV8DBcMFVX/AcEXBV8MED0Q0QH9ENED0Q0AN1fVNQDV1dU/VNQA9EAP1dU11AAB9/RDRAw0QN1dEfBDRcEDfDRcEEdw0QfRR0QP1111FXdXV0DXV1T1TUNAPXRNQA/MQD1FMQDdXRM1EN0DUAP9Q0AAAP3V1DUPUAPXUNA3QAAAAAAAAA%");
        dataSource.add(sourceWorld);
        VectorTileLayer backlayer  = new VectorTileLayer(dataSource, decoder);
//        backlayer.setMaxOverzoomLevel(1);
        mapView.getLayers().add(backlayer);

//        addHillshadeLayer(view);

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
//                testValhalla(hillshadeLayer, options);
                testVectoTileSearch(backlayer, options);
                // Code here executes on main thread after user presses button
//                if (options.getRenderProjectionMode() == RenderProjectionMode.RENDER_PROJECTION_MODE_SPHERICAL) {
//                    options.setRenderProjectionMode(RenderProjectionMode.RENDER_PROJECTION_MODE_PLANAR);
//                } else {
//                    options.setRenderProjectionMode(RenderProjectionMode.RENDER_PROJECTION_MODE_SPHERICAL);
//                }
            }
        });

//        testValhalla(hillshadeLayer, options);


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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
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
            vector.add(new MapPos(5.720614, 45.174683));
            vector.add(new MapPos(5.726890, 45.201224));
            RoutingRequest request = new RoutingRequest(projection, vector);
            request.setCustomParameter("costing_options", Variant.fromString("{\"pedestrian\":{\"driveway_factor\":10,\"max_hiking_difficulty\":6,\"shortest\":false,\"step_penalty\":0,\"use_ferry\":0,\"use_hills\":1,\"use_roads\":0,\"use_tracks\":1,\"walking_speed\":4}},\"directions_options\":{\"language\":\"en\"}}"));
            runValhallaInThread(routingService, request, "pedestrian", localSource);
            request = new RoutingRequest(projection, vector);
            request.setCustomParameter("costing_options", Variant.fromString("{\"pedestrian\":{\"driveway_factor\":10,\"max_hiking_difficulty\":6,\"shortest\":false,\"step_penalty\":1,\"use_ferry\":0,\"use_hills\":0,\"use_roads\":0,\"use_tracks\":1,\"walking_speed\":4}},\"directions_options\":{\"language\":\"en\"}}"));
            runValhallaInThread(routingService, request, "pedestrian", localSource);
            request = new RoutingRequest(projection, vector);
            request.setCustomParameter("costing_options", Variant.fromString("{\"pedestrian\":{\"driveway_factor\":10,\"max_hiking_difficulty\":6,\"shortest\":true,\"step_penalty\":5,\"use_ferry\":0,\"use_hills\":1,\"use_roads\":0,\"use_tracks\":1,\"walking_speed\":4}},\"directions_options\":{\"language\":\"en\"}}"));
            runValhallaInThread(routingService, request, "pedestrian", localSource);
        } catch (IOException e) {
            e.printStackTrace();
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
