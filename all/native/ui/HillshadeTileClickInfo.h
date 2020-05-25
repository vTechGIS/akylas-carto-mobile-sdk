
/*
 * Copyright (c) 2016 CartoDB. All rights reserved.
 * Copying and using this code is allowed only according
 * to license terms, as given in https://cartodb.com/terms/
 */

#ifndef _CARTO_HILLSHADETILECLICKINFO_H_
#define _CARTO_HILLSHADETILECLICKINFO_H_

#include "core/MapPos.h"
#include "core/MapTile.h"
#include "ui/ClickType.h"

#include <memory>
#include <string>

namespace carto {
    class Layer;
    
    /**
     * A container class that provides information about a click performed on an hillshade raster tile.
     */
    class HillshadeTileClickInfo {
    public:
        /**
         * Constructs a HillshadeTileClickInfo object from a click position, tile information and clicked feature.
         * @param clickType The click type (SINGLE, DUAL, etc)
         * @param clickPos The click position in the coordinate system of the data source.
         * @param mapTile The clicked tile id.
         * @param nearestElevation The elevation of the nearest pixel to the click position.
         * @param interpolatedElevation The interpolated elevation at the click position.
         * @param layer The layer of the raster tile on which the click was performed.
         */
        HillshadeTileClickInfo(ClickType::ClickType clickType, const MapPos& clickPos, const MapTile& mapTile, const float nearestElevation, const float interpolatedElevation, const std::shared_ptr<Layer>& layer);
        virtual ~HillshadeTileClickInfo();
    
        /**
         * Returns the click type.
         * @return The type of the click performed.
         */
        ClickType::ClickType getClickType() const;

        /**
         * Returns the click position.
         * @return The click position in the coordinate system of the data source.
         */
        const MapPos& getClickPos() const;
        
        /**
         * Returns the tile id of the clicked feature.
         * @return The tile id of the clicked feature.
         */
        const MapTile& getMapTile() const;

        /**
         * Returns the elevation of the nearest pixel to the click position.
         * @return The elevation of the nearest pixel to the click position.
         */
        const float getNearestElevation() const;

        /**
         * Returns the interpolated elevation at the click position.
         * @return The interpolated elevation at the click position.
         */
        const float getInterpolatedElevation() const;

        /**
         * Returns the layer of the raster tile.
         * @return The layer of the raster tile.
         */
        std::shared_ptr<Layer> getLayer() const;
    
    private:
        ClickType::ClickType _clickType;
        MapPos _clickPos;
        MapTile _mapTile;
    
        float _nearestElevation;
        float _interpolatedElevation;
        std::shared_ptr<Layer> _layer;
    };
    
}

#endif
