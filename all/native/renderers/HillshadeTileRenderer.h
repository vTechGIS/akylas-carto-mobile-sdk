/*
 * Copyright (c) 2016 CartoDB. All rights reserved.
 * Copying and using this code is allowed only according
 * to license terms, as given in https://cartodb.com/terms/
 */

#ifndef _CARTO_HILLSHADETILERENDERER_H_
#define _CARTO_HILLSHADETILERENDERER_H_

#include "graphics/Color.h"
#include "graphics/ViewState.h"
#include "renderers/utils/GLResource.h"
#include "renderers/TileRenderer.h"

#include <memory>
#include <mutex>
#include <map>
#include <tuple>
#include <vector>

#include <cglib/ray.h>

#include <vt/TileId.h>
#include <vt/Tile.h>
#include <vt/Bitmap.h>

namespace carto {
    class Options;
    class MapRenderer;
    class TileDrawData;
    class ViewState;
    class VTRenderer;
    class HillshadeTileLayer;
    namespace vt {
        class LabelCuller;
        class TileTransformer;
        class GLTileRenderer;
    }
    
    class HillshadeTileRenderer : public TileRenderer {
    public:
        HillshadeTileRenderer();
        virtual ~HillshadeTileRenderer();
    
//        void setComponents(const std::weak_ptr<Options>& options, const std::weak_ptr<MapRenderer>& mapRenderer, const std::weak_ptr<HillshadeTileLayer>& tileLayer);

        // bool onDrawFrame(float deltaSeconds, const ViewState& viewState);
        void setRotation(float rotation);
        void setMaZoom(float maxZoom);
        void setIlluminationDirection(float illuminationDirection);
        void setExaggeration(float exaggeration);
        void setHighlightColor(Color highlightColor);
        void setAccentColor(Color accentColor);
        void setShadowColor(Color shadowColor);
        void setInspect(bool inspect);
    private:
        bool initializeRenderer() override;

        float _rotation;
        float _maxZoom;
        float _illuminationDirection;
        float _exaggeration;
        Color _highlightColor;
        Color _accentColor;
        Color _shadowColor;

        bool _inspect;

        static const std::string LIGHTING_SHADER_2D;
        static const std::string LIGHTING_SHADER_3D;

    };
    
}

#endif
