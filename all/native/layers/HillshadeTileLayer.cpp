#include "HillshadeTileLayer.h"
#include "layers/VectorTileEventListener.h"
#include "renderers/MapRenderer.h"
#include "renderers/HillshadeTileRenderer.h"
#include "vectortiles/TorqueTileDecoder.h"
#include "projections/Projection.h"
#include "renderers/components/RayIntersectedElement.h"
#include "layers/RasterTileEventListener.h"
#include "utils/Log.h"
#include "projections/ProjectionSurface.h"

#include "ui/HillshadeTileClickInfo.h"

#include <vt/Tile.h>


namespace {

    std::array<std::uint8_t, 4> readTileBitmapColor(const carto::vt::TileBitmap& bitmap, int x, int y) {
        x = std::max(0, std::min(x, bitmap.getWidth() - 1));
        y = bitmap.getHeight() - 1 - std::max(0, std::min(y, bitmap.getHeight() - 1));

        switch (bitmap.getFormat()) {
        case carto::vt::TileBitmap::Format::GRAYSCALE:
            {
                std::uint8_t val = bitmap.getData()[y * bitmap.getWidth() + x];
                return std::array<std::uint8_t, 4> { { val, val, val, 255 } };
            }
        case carto::vt::TileBitmap::Format::RGB:
            {
                const std::uint8_t* valPtr = &bitmap.getData()[(y * bitmap.getWidth() + x) * 3];
                return std::array<std::uint8_t, 4> { { valPtr[0], valPtr[1], valPtr[2], 255 } };
            }
        case carto::vt::TileBitmap::Format::RGBA:
            {
                const std::uint8_t* valPtr = &bitmap.getData()[(y * bitmap.getWidth() + x) * 4];
                return std::array<std::uint8_t, 4> { { valPtr[0], valPtr[1], valPtr[2], valPtr[3] } };
            }
        default:
            break;
        }
        return std::array<std::uint8_t, 4> { { 0, 0, 0, 0 } };
    }

    std::array<std::uint8_t, 4> readTileBitmapColor(const carto::vt::TileBitmap& bitmap, float x, float y) {
        std::array<float, 4> result { { 0, 0, 0, 0 } };
        for (int dy = 0; dy < 2; dy++) {
            for (int dx = 0; dx < 2; dx++) {
                int x0 = static_cast<int>(std::floor(x));
                int y0 = static_cast<int>(std::floor(y));

                std::array<std::uint8_t, 4> color = readTileBitmapColor(bitmap, x0 + dx, y0 + dy);
                for (int i = 0; i < 4; i++) {
                    result[i] += color[i] * (dx == 0 ? x0 + 1.0f - x : x - x0) * (dy == 0 ? y0 + 1.0f - y : y - y0);
                }
            }
        }
        return std::array<std::uint8_t, 4> { { static_cast<std::uint8_t>(result[0]), static_cast<std::uint8_t>(result[1]), static_cast<std::uint8_t>(result[2]), static_cast<std::uint8_t>(result[3]) } };
    }
    float elevationFromColor(const std::array<std::uint8_t, 4> colors) {
        return (-10000 + (colors[0] * 256 * 256 + colors[1] * 256 + colors[2]) * 0.1);
    }
}

namespace carto {

    HillshadeTileLayer::HillshadeTileLayer(const std::shared_ptr<TileDataSource>& dataSource) :
        RasterTileLayer(dataSource, std::make_shared<HillshadeTileRenderer>()),
        _shadowColor(DEFAULT_SHADOW_COLOR),
        _highlightColor(DEFAULT_HIGHLIGHT_COLOR),
        _accentColor(DEFAULT_ACCENT_COLOR),
        _illuminationDirection(335),
        _exaggeration(0.5),
        _inspect(false)
    {
    }


    const Color HillshadeTileLayer::DEFAULT_HIGHLIGHT_COLOR = Color(255, 255, 255, 255);
    const Color HillshadeTileLayer::DEFAULT_SHADOW_COLOR = Color(0, 0, 0, 255);
    const Color HillshadeTileLayer::DEFAULT_ACCENT_COLOR = Color(0, 0, 0, 255);
    
    HillshadeTileLayer::~HillshadeTileLayer() {
    }

//     void HillshadeTileLayer::setComponents(const std::shared_ptr<CancelableThreadPool>& envelopeThreadPool,
//                                   const std::shared_ptr<CancelableThreadPool>& tileThreadPool,
//                                   const std::weak_ptr<Options>& options,
//                                   const std::weak_ptr<MapRenderer>& mapRenderer,
//                                   const std::weak_ptr<TouchHandler>& touchHandler)
//     {
//         Layer::setComponents(envelopeThreadPool, tileThreadPool, options, mapRenderer, touchHandler);
// //        std::shared_ptr<HillshadeTileRenderer> tileRenderer = std::static_pointer_cast<HillshadeTileRenderer>(_tileRenderer);

//         _tileRenderer->setComponents(options, mapRenderer);

//         // To reduce memory usage, release all the caches now
//         std::lock_guard<std::recursive_mutex> lock(_mutex);
//         clearTileCaches(true);
//         _projectionSurface.reset();
//         _glResourceManager.reset();
//     }


    bool HillshadeTileLayer::onDrawOffscreenFrame(float deltaSeconds, BillboardSorter& billboardSorter, const ViewState& viewState) {
        // this is where we would draw tiles
    }
    bool HillshadeTileLayer::onDrawFrame(float deltaSeconds, BillboardSorter& billboardSorter, const ViewState& viewState) {
        updateTileLoadListener();

        std::shared_ptr<HillshadeTileRenderer> tileRenderer = std::static_pointer_cast<HillshadeTileRenderer>(_tileRenderer);
        tileRenderer->setRotation(viewState.getRotation());
        tileRenderer->setMaZoom(getDataSource()->getMaxZoom());
        tileRenderer->setIlluminationDirection(_illuminationDirection);
        tileRenderer->setExaggeration(_exaggeration);
        tileRenderer->setAccentColor(_accentColor);
        tileRenderer->setHighlightColor(_highlightColor);
        tileRenderer->setShadowColor(_shadowColor);
        tileRenderer->setInspect(_inspect);

        if (auto mapRenderer = getMapRenderer()) {
            float opacity = getOpacity();

            if (opacity < 1.0f) {
                mapRenderer->clearAndBindScreenFBO(Color(0, 0, 0, 0), false, false);
            }

            _tileRenderer->setInteractionMode(getRasterTileEventListener() ? true : false);
            _tileRenderer->setSubTileBlending(false);
            bool refresh = _tileRenderer->onDrawFrame(deltaSeconds, viewState);

            if (opacity < 1.0f) {
                mapRenderer->blendAndUnbindScreenFBO(opacity);
            }

            return refresh;
        }
        return false;
    }

    void HillshadeTileLayer::calculateRayIntersectedElements(const cglib::ray3<double>& ray, const ViewState& viewState, std::vector<RayIntersectedElement>& results) const {
        std::shared_ptr<RasterTileEventListener> eventListener = getRasterTileEventListener();

        if (eventListener) {
            std::vector<std::tuple<vt::TileId, double, vt::TileBitmap, cglib::vec2<float> > > hitResults;
            _tileRenderer->calculateRayIntersectedBitmaps(ray, viewState, hitResults);

            for (const std::tuple<vt::TileId, double, vt::TileBitmap, cglib::vec2<float> >& hitResult : hitResults) {
                vt::TileId vtTileId = std::get<0>(hitResult);
                double t = std::get<1>(hitResult);
                const vt::TileBitmap& tileBitmap = std::get<2>(hitResult);
                cglib::vec2<float> tilePos = std::get<3>(hitResult);

                if (tileBitmap.getData().empty() || tileBitmap.getWidth() < 1 || tileBitmap.getHeight() < 1) {
                    Log::Warnf("RasterTileLayer::calculateRayIntersectedElements: Bitmap data not available");
                    continue;
                }

                std::lock_guard<std::recursive_mutex> lock(_mutex);

                float x = tilePos(0) * tileBitmap.getWidth();
                float y = tilePos(1) * tileBitmap.getHeight();
                std::array<std::uint8_t, 4> interpolatedComponents = readTileBitmapColor(tileBitmap, x - 0.5f, y - 0.5f);
                float interpolatedElevation = elevationFromColor(interpolatedComponents);

                int nx = static_cast<int>(std::floor(x));
                int ny = static_cast<int>(std::floor(y));
                std::array<std::uint8_t, 4> nearestComponents = readTileBitmapColor(tileBitmap, nx, ny);
                float nearestElevation = elevationFromColor(nearestComponents);

                auto pixelInfo = std::make_shared<std::tuple<MapTile, float, float> >(MapTile(vtTileId.x, vtTileId.y, vtTileId.zoom, _frameNr), nearestElevation, interpolatedElevation);
                std::shared_ptr<Layer> thisLayer = std::const_pointer_cast<Layer>(shared_from_this());
                results.push_back(RayIntersectedElement(pixelInfo, thisLayer, ray(t), ray(t), false));
            }
        }

        TileLayer::calculateRayIntersectedElements(ray, viewState, results);
    }

    bool HillshadeTileLayer::processClick(ClickType::ClickType clickType, const RayIntersectedElement& intersectedElement, const ViewState& viewState) const {
        std::shared_ptr<ProjectionSurface> projectionSurface = viewState.getProjectionSurface();
        if (!projectionSurface) {
            return false;
        }
        
        std::shared_ptr<RasterTileEventListener> eventListener = getRasterTileEventListener();

        // TODO: create new listener for hillshades
        // if (eventListener) {
        //     if (auto pixelInfo = intersectedElement.getElement<std::tuple<MapTile, float, float> >()) {
        //         const MapTile& mapTile = std::get<0>(*pixelInfo);
        //         const float& nearestElevation = std::get<1>(*pixelInfo);
        //         const float& interpolatedElevation = std::get<2>(*pixelInfo);
        //         MapPos hitPos = _dataSource->getProjection()->fromInternal(projectionSurface->calculateMapPos(intersectedElement.getHitPos()));

        //         auto clickInfo = std::make_shared<HillshadeTileClickInfo>(clickType, hitPos, mapTile, nearestElevation, interpolatedElevation, intersectedElement.getLayer());
        //         return eventListener->onRasterTileClicked(clickInfo);
        //     }
        // }

        return RasterTileLayer::processClick(clickType, intersectedElement, viewState);
    }

    int HillshadeTileLayer::getIlluminationDirection() const {
        std::lock_guard<std::recursive_mutex> lock(_mutex);
        return _illuminationDirection;
    }

    void HillshadeTileLayer::setIlluminationDirection(int direction) {
        {
            std::lock_guard<std::recursive_mutex> lock(_mutex);
            _illuminationDirection = direction;
        }
        refresh();
    }
    float HillshadeTileLayer::getExaggeration() const {
        std::lock_guard<std::recursive_mutex> lock(_mutex);
        return _exaggeration;
    }

    void HillshadeTileLayer::setExaggeration(float exaggeration) {
        {
            std::lock_guard<std::recursive_mutex> lock(_mutex);
            _exaggeration = exaggeration;
        }
        refresh();
    }

    Color HillshadeTileLayer::getHighlightColor() const {
        std::lock_guard<std::recursive_mutex> lock(_mutex);
        return _highlightColor;
    }

    void HillshadeTileLayer::setHighlightColor(const Color& color) {
        {
            std::lock_guard<std::recursive_mutex> lock(_mutex);
            _highlightColor = color;
        }
        redraw();
    }

    Color HillshadeTileLayer::getAccentColor() const {
        std::lock_guard<std::recursive_mutex> lock(_mutex);
        return _accentColor;
    }

    void HillshadeTileLayer::setAccentColor(const Color& color) {
        {
            std::lock_guard<std::recursive_mutex> lock(_mutex);
            _accentColor = color;
        }
        redraw();
    }

    Color HillshadeTileLayer::getShadowColor() const {
        std::lock_guard<std::recursive_mutex> lock(_mutex);
        return _shadowColor;
    }

    void HillshadeTileLayer::setShadowColor(const Color& color) {
        {
            std::lock_guard<std::recursive_mutex> lock(_mutex);
            _shadowColor = color;
        }
        redraw();
    }
    bool HillshadeTileLayer::getInspect() const {
        std::lock_guard<std::recursive_mutex> lock(_mutex);
        return _inspect;
    }

    void HillshadeTileLayer::setInspect(const bool inspect) {
        {
            std::lock_guard<std::recursive_mutex> lock(_mutex);
            _inspect = inspect;
        }
        redraw();
    }
}
