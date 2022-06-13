#ifndef _LOCALPACKAGEMANAGERTILEDATASOURCE_I
#define _LOCALPACKAGEMANAGERTILEDATASOURCE_I

%module(directors="1") LocalPackageManagerTileDataSource

!proxy_imports(carto::LocalPackageManagerTileDataSource, core.MapTile, core.MapBounds, core.StringMap, datasources.TileDataSource, datasources.MBTilesTileDataSource, datasources.components.TileData)

%{
#include "datasources/LocalPackageManagerTileDataSource.h"
#include "components/Exceptions.h"
#include <memory>
%}

%include <std_shared_ptr.i>
%include <std_string.i>
%include <cartoswig.i>

%import "core/MapTile.i"
%import "core/StringMap.i"
%import "datasources/TileDataSource.i"
%import "datasources/MBTilesTileDataSource.i"
%import "datasources/components/TileData.i"

!polymorphic_shared_ptr(carto::LocalPackageManagerTileDataSource, datasources.LocalPackageManagerTileDataSource)

%std_exceptions(carto::LocalPackageManagerTileDataSource::LocalPackageManagerTileDataSource)
%std_exceptions(carto::LocalVectorDataSource::add)
// %std_exceptions(carto::LocalVectorDataSource::addAll)
%std_exceptions(carto::LocalVectorDataSource::remove)
// %std_exceptions(carto::LocalVectorDataSource::removeAll)

%ignore carto::LocalVectorDataSource::addAll;
%ignore carto::LocalVectorDataSource::removeAll;

%feature("director") carto::LocalPackageManagerTileDataSource;

%include "datasources/LocalPackageManagerTileDataSource.h"

#endif
