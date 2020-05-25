#include "HillshadeTileClickInfo.h"

namespace carto {

    HillshadeTileClickInfo::HillshadeTileClickInfo(ClickType::ClickType clickType, const MapPos& clickPos, const MapTile& mapTile,
                                                   const float nearestElevation, const float interpolatedElevation,
                                                   const std::shared_ptr<Layer>& layer) :
        _clickType(clickType),
        _clickPos(clickPos),
        _mapTile(mapTile),
        _nearestElevation(nearestElevation),
        _interpolatedElevation(interpolatedElevation),
        _layer(layer)
    {
    }
    
    HillshadeTileClickInfo::~HillshadeTileClickInfo() {
    }

    ClickType::ClickType HillshadeTileClickInfo::getClickType() const {
        return _clickType;
    }
    
    const MapPos& HillshadeTileClickInfo::getClickPos() const {
        return _clickPos;
    }
        
    const MapTile& HillshadeTileClickInfo::getMapTile() const {
        return _mapTile;
    }

    const float HillshadeTileClickInfo::getNearestElevation() const {
        return _nearestElevation;
    }
    
    const float HillshadeTileClickInfo::getInterpolatedElevation() const {
        return _interpolatedElevation;
    }
    
    std::shared_ptr<Layer> HillshadeTileClickInfo::getLayer() const {
        return _layer;
    }

}
