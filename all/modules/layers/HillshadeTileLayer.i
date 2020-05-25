#ifndef _HILLSHADETILELAYER_I
#define _HILLSHADETILELAYER_I

%module HillshadeTileLayer

!proxy_imports(carto::HillshadeTileLayer, datasources.TileDataSource, layers.RasterTileLayer, graphics.Color)

%{
#include "layers/HillshadeTileLayer.h"
#include "components/Exceptions.h"
#include <memory>
%}

%include <std_shared_ptr.i>
%include <cartoswig.i>

!polymorphic_shared_ptr(carto::HillshadeTileLayer, layers.HillshadeTileLayer)

%import "datasources/TileDataSource.i"
%import "layers/RasterTileLayer.i"
%import "graphics/Color.i"

%attribute(carto::HillshadeTileLayer, float, Exaggeration, getExaggeration, setExaggeration)
%attribute(carto::HillshadeTileLayer, int, IlluminationDirection, getIlluminationDirection, setIlluminationDirection)
%attributeval(carto::HillshadeTileLayer, carto::Color, ShadowColor, getShadowColor, setShadowColor)
%attributeval(carto::HillshadeTileLayer, carto::Color, AccentColor, getAccentColor, setAccentColor)
%attributeval(carto::HillshadeTileLayer, carto::Color, HighlightColor, getHighlightColor, setHighlightColor)
%attributeval(carto::HillshadeTileLayer, bool, Inspect, getInspect, setInspect)

%include "layers/HillshadeTileLayer.h"

#endif
