#ifndef _MBTILESTILEDATASOURCE_I
#define _MBTILESTILEDATASOURCE_I

%module(directors="1") OruxDBTileDataSource

#ifdef _CARTO_OFFLINE_SUPPORT

!proxy_imports(carto::OruxDBTileDataSource, core.MapTile, core.MapBounds, core.StringMap, datasources.TileDataSource, datasources.components.TileData)

%{
#include "datasources/OruxDBTileDataSource.h"
#include "components/Exceptions.h"
#include <memory>
%}

%include <std_shared_ptr.i>
%include <std_string.i>
%include <cartoswig.i>

%import "core/MapTile.i"
%import "core/StringMap.i"
%import "datasources/TileDataSource.i"
%import "datasources/components/TileData.i"

!enum(carto::MBTilesScheme::MBTilesScheme)
!polymorphic_shared_ptr(carto::OruxDBTileDataSource, datasources.OruxDBTileDataSource)

%std_io_exceptions(carto::OruxDBTileDataSource::OruxDBTileDataSource)

%feature("director") carto::OruxDBTileDataSource;

%include "datasources/OruxDBTileDataSource.h"

#endif

#endif
