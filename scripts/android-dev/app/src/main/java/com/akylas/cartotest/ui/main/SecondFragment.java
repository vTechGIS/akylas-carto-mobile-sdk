package com.akylas.cartotest.ui.main;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
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
import android.widget.Toast;

import com.akylas.cartotest.R;
import com.carto.components.Options;
import com.carto.components.PanningMode;
import com.carto.components.RenderProjectionMode;
import com.carto.core.IntVector;
import com.carto.core.DoubleVector;
import com.carto.core.MapPos;
import com.carto.core.MapPosVector;
import com.carto.core.MapRange;
import com.carto.core.MapVec;
import com.carto.core.StringVector;
import com.carto.core.Variant;
import com.carto.core.VariantObjectBuilder;
import com.carto.datasources.HTTPTileDataSource;
import com.carto.datasources.LocalVectorDataSource;
import com.carto.datasources.MBTilesTileDataSource;
import com.carto.datasources.TileDataSource;
import com.carto.geometry.VectorTileFeatureCollection;
import com.carto.graphics.Color;
import com.carto.layers.CartoBaseMapStyle;
import com.carto.layers.CartoOnlineRasterTileLayer;
import com.carto.layers.CartoOnlineVectorTileLayer;
import com.carto.layers.HillshadeRasterTileLayer;
import com.carto.layers.RasterTileLayer;
import com.carto.layers.TileLayer;
import com.carto.layers.VectorLayer;
import com.carto.layers.VectorTileLayer;
import com.carto.projections.EPSG4326;
import com.carto.projections.Projection;
import com.carto.rastertiles.ElevationDecoder;
import com.carto.rastertiles.MapBoxElevationDataDecoder;
import com.carto.routing.RoutingRequest;
import com.carto.routing.RoutingResult;
import com.carto.routing.ValhallaOfflineRoutingService;
import com.carto.routing.ValhallaOnlineRoutingService;
import com.carto.search.SearchRequest;
import com.carto.search.VectorTileSearchService;
import com.carto.styles.CompiledStyleSet;
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
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class SecondFragment extends Fragment {
    private final String TAG = "SecondFragment";

    public static SecondFragment newInstance() {
        return new SecondFragment();
    }

    private static final int REQUEST_PERMISSIONS_CODE_WRITE_STORAGE = 1435;
    MapView mapView;
    TileDataSource hillshadeSource;
    TileLayer backlayer;
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
        hillshadeLayer.setNormalMapLightingShader(activated? "uniform vec4 u_shadowColor;\n" +
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

    void proceedWithSdCard(View view) {
        TileDataSource hillshadeSource = null;
        try {
//            hillshadeSource = this.hillshadeSource = new MBTilesTileDataSource(5, 11, "/storage/10E7-1004/alpimaps_mbtiles/BDALTIV2_75M_rvb_2.etiles");
            hillshadeSource = this.hillshadeSource = new HTTPTileDataSource(5, 11, "http://192.168.1.45:8080/data/BDALTIV2_75M_rvb/{z}/{x}/{y}.png");
            //        HTTPTileDataSource hillshadeSource =   new HTTPTileDataSource(1, 15, "https://api.mapbox.com/v4/mapbox.terrain-rgb/{z}/{x}/{y}.pngraw?access_token=pk.eyJ1IjoiYWt5bGFzIiwiYSI6IkVJVFl2OXMifQ.TGtrEmByO3-99hA0EI44Ew");
        } catch (Exception e) {
            e.printStackTrace();
        }
        final MapBoxElevationDataDecoder decoder = elevationDecoder = new MapBoxElevationDataDecoder();
        final HillshadeRasterTileLayer layer = hillshadeLayer = new HillshadeRasterTileLayer(hillshadeSource, decoder);
        layer.setContrast(1.0f);
        layer.setHeightScale(0.0625f);
        layer.setVisibleZoomRange(new MapRange(5, 16));
        layer.setIlluminationMapRotationEnabled(true);
        layer.setIlluminationDirection(new MapVec(-1,0, 0));
        layer.setHighlightColor(new Color((short) 125, (short) 216, (short) 79, (short) 255));
        layer.setShadowColor(new Color((short) 176, (short) 145, (short) 91, (short) 255));
        layer.setAccentColor(new Color((short) 34, (short) 67, (short) 252, (short) 255));
        toggleSlopes(true);
        final CheckBox slopesCheckBox = (CheckBox) view.findViewById(R.id.slopesCheckBox); // initiate the Seek bar
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

        final double degrees = (Math.acos(layer.getIlluminationDirection().getY()) * 180 / Math.PI) ;
        illuminationDirectionSeekBar.setProgress((int)degrees);
        textIlluminationDirection.setText((int)degrees + "");
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
                Color color  = layer.getHighlightColor();
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
                Color color  = layer.getShadowColor();
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

        final TextView textZoom = (TextView) view.findViewById(R.id.zoomText); // initiate the Seek bar
        mapView.setMapEventListener(new MapEventListener() {
            @Override
            public void onMapMoved() {
                super.onMapMoved();
                Log.d(TAG, "onMapMoved " + mapView.getFocusPos());
                textZoom.setText(String.format("%.2f", mapView.getZoom()));
            }

            @Override
            public void onMapClicked(MapClickInfo mapClickInfo) {
                super.onMapClicked(mapClickInfo);
                MapPos clickPos = mapClickInfo.getClickPos();
                Log.d(TAG, "onMapClicked " + clickPos);
                Log.d(TAG, "elevation " + layer.getElevation(clickPos));
            }
        });

        final Options options = mapView.getOptions();

        final Button modeButton = (Button) view.findViewById(R.id.modeButton); // initiate the Seek bar
        modeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread() {
                    public void run() {
                testValhalla(hillshadeLayer, options);
                    }
                }.start();
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
    public void testVectoTileSearch(final VectorTileLayer layer, final Options options, final MapPos location ) {

        final VectorTileSearchService searchService = new VectorTileSearchService(layer.getDataSource(), layer.getTileDecoder());
        searchService.setMaxZoom(14);
        searchService.setMinZoom(14);
        final SearchRequest request = new SearchRequest();
        request.setRegexFilter(".*dame.*");
        request.setProjection(options.getBaseProjection());
        request.setSearchRadius(500);
        request.setGeometry(new com.carto.geometry.PointGeometry(location));
        new Thread() {
            public void run() {
                final VectorTileFeatureCollection result = searchService.findFeatures(request);
                new Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "findFeatures done " + result.getFeatureCount());
                    }

                });
            }
        }.start();
    }

    public void testValhalla(HillshadeRasterTileLayer layer, Options options) {
        Projection projection = options.getBaseProjection();
        ValhallaOfflineRoutingService routingService;
        try {
            routingService = new ValhallaOfflineRoutingService("/storage/10E7-1004/alpimaps_mbtiles/france.vtiles");
//            routingService = new ValhallaOnlineRoutingService("toto");
//            routingService.setCustomServiceURL("http://192.168.1.45:8081/{service}");
            routingService.setProfile("pedestrian");

            LocalVectorDataSource localSource = new LocalVectorDataSource(projection);
            VectorLayer vectorLayer = new VectorLayer(localSource);
            mapView.getLayers().add(vectorLayer);
            MapPosVector vector = new MapPosVector();
            vector.add(new MapPos(5.73018952319828, 45.19395768156221));
            vector.add(new MapPos(5.725817787850285, 45.19885075467135));
            RoutingRequest request = new RoutingRequest(projection, vector);
            request.setCustomParameter("costing_options", Variant.fromString("{\"pedestrian\":{\"use_hills\":0,\"use_roads\":0,\"max_hiking_difficulty\":6}}"));
            RoutingResult result = routingService.calculateRoute(request);
            MapPosVector pointsWithAltitude = new MapPosVector();
            if (result != null) {
                MapPosVector points = result.getPoints();
                Log.d(TAG, "showing route " + points.size());
                DoubleVector elevations = layer.getElevations(points);
                for (int i = (int) (elevations.size() - 1); i >= 0; i--) {
                    MapPos point = points.get(i);
//                    Log.d(TAG, "elevations2 " + i + "  " + point.getX() + "  " +  point.getY() + "  " +  elevations.get(i));
//                    int computedElevation = layer.getElevation(point);
                    pointsWithAltitude.add(new MapPos(point.getX(), point.getY(), elevations.get(i)));
                }
                LineStyleBuilder builder = new LineStyleBuilder();
                builder.setWidth(4);
                builder.setColor(new Color((short) 255, (short) 0, (short) 0, (short) 255));
                Line line = new Line(pointsWithAltitude, builder.buildStyle());
                localSource.add(line);
            }
//            Log.d(TAG, "elevations " + elevations.toString());


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


        final MapView mapView = this.mapView = (MapView) view.findViewById(R.id.mapView);


//        TileDataSource source = null;
//        MBVectorTileDecoder decoder = null;
//        try {
//            source = new MBTilesTileDataSource( "/storage/10E7-1004/alpimaps_mbtiles/Rhone_alpes/tiles.mbtiles");
//            final File file = new File("/storage/10E7-1004/alpimaps_mbtiles/osm.zip");
//            final FileInputStream stream = new java.io.FileInputStream(file);
//            final DataInputStream dataInputStream = new java.io.DataInputStream(stream);
//            final byte[] bytes = new byte[(int)file.length()];
//            dataInputStream.readFully(bytes);
//            decoder = new MBVectorTileDecoder(new CompiledStyleSet(new ZippedAssetPackage(new com.carto.core.BinaryData(bytes))));
//            decoder.setStyleParameter("routes", "1");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        backlayer  = new VectorTileLayer(source, decoder);


        //        MapView.registerLicense("XTUMwQ0ZRQ0RLZEM4Z1dMdkc1TDZkZy83RlN3Z0V2aTB5d0lVSlEwbGZNZjV5bDJLMnlPWXFJYWpVWmhuQWtZPQoKYXBwVG9rZW49OWYwZjBhMDgtZGQ1Mi00NjVkLTg5N2YtMTg0MDYzODQxMDBiCnBhY2thZ2VOYW1lPWNvbS5ha3lsYXMuY2FydG90ZXN0Cm9ubGluZUxpY2Vuc2U9MQpwcm9kdWN0cz1zZGstYW5kcm9pZC00LioKd2F0ZXJtYXJrPWNhcnRvZGIK", this.getContext());
        HTTPTileDataSource source =  new HTTPTileDataSource(1, 20, "http://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png");
//        HTTPTileDataSource source = new HTTPTileDataSource(1, 20, "https://1.base.maps.cit.api.here.com/maptile/2.1/maptile/newest/normal.day.grey/{z}/{x}/{y}/512/png8?app_id=9QKPJz6sIj9MkeeUmpfc&app_code=iD7QuqOFDMJS_nNtlKdp1A");
        StringVector subdomains = new StringVector();
        subdomains.add("a");
        subdomains.add("b");
        subdomains.add("c");
        source.setSubdomains(subdomains);
        final RasterTileLayer backlayer = new RasterTileLayer(source);
//        final CartoOnlineVectorTileLayer backlayer = new CartoOnlineVectorTileLayer(CartoBaseMapStyle.CARTO_BASEMAP_STYLE_POSITRON);
        final AppCompatSeekBar opacitySeekBar = (AppCompatSeekBar) view.findViewById(R.id.opacitySeekBar); // initiate the Seek bar
        final TextView textOpacity = (TextView) view.findViewById(R.id.textOpacity); // initiate the Seek bar
        opacitySeekBar.setProgress(100);
        textOpacity.setText(backlayer.getOpacity() + "");
        opacitySeekBar.setOnSeekBarChangeListener(new AppCompatSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                backlayer.setOpacity(i / 100.0f);
                textOpacity.setText(backlayer.getOpacity() + "");
                mapView.requestRender();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });


        final EPSG4326 projection = new EPSG4326();

        final Options options = mapView.getOptions();

        options.setZoomGestures(true);
        options.setRestrictedPanning(true);
        options.setSeamlessPanning(true);
        options.setRotatable(true);
        options.setRenderProjectionMode(RenderProjectionMode.RENDER_PROJECTION_MODE_SPHERICAL);
        options.setPanningMode(PanningMode.PANNING_MODE_STICKY);
        options.setBaseProjection(projection);
        mapView.getLayers().add(backlayer);
        mapView.setFocusPos(new MapPos(5.7562, 45.175), 0);
        mapView.setZoom(13, 0);
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
