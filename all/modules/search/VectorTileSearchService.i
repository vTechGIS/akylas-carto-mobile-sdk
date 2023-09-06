#ifndef _VECTORTILESEARCHSERVICE_I
#define _VECTORTILESEARCHSERVICE_I

#pragma SWIG nowarn=325
#pragma SWIG nowarn=401

%module(directors="1") VectorTileSearchService

#ifdef _CARTO_SEARCH_SUPPORT

!proxy_imports(carto::VectorTileSearchService, core.StringVector, search.SearchRequest, datasources.TileDataSource, geometry.VectorTileFeatureCollection, vectortiles.VectorTileDecoder, projections.Projection)

%{
#include "search/VectorTileSearchService.h"
#include "components/Exceptions.h"
#include <memory>
%}

%include <std_shared_ptr.i>
%include <cartoswig.i>
%include <std_vector.i>

%import "geometry/VectorTileFeatureCollection.i"
%import "search/SearchRequest.i"
%import "datasources/TileDataSource.i"
%import "vectortiles/VectorTileDecoder.i"
%import "projections/Projection.i"
%import "core/StringVector.i"

!polymorphic_shared_ptr(carto::VectorTileSearchService, search.VectorTileSearchService)

%attributestring(carto::VectorTileSearchService, std::shared_ptr<carto::TileDataSource>, DataSource, getDataSource)
%attributestring(carto::VectorTileSearchService, std::shared_ptr<carto::VectorTileDecoder>, TileDecoder, getTileDecoder)
%attribute(carto::VectorTileSearchService, int, MinZoom, getMinZoom, setMinZoom)
%attribute(carto::VectorTileSearchService, int, MaxZoom, getMaxZoom, setMaxZoom)
%attribute(carto::VectorTileSearchService, int, MaxResults, getMaxResults, setMaxResults)
%attribute(carto::VectorTileSearchService, bool, SortByDistance, getSortByDistance, setSortByDistance)
%attribute(carto::VectorTileSearchService, bool, PreventDuplicates, getPreventDuplicates, setPreventDuplicates)
%attributeval(carto::VectorTileSearchService, %arg(std::vector<std::string>), Layers, getLayers, setLayers)
%std_exceptions(carto::VectorTileSearchService::VectorTileSearchService)
%std_exceptions(carto::VectorTileSearchService::findFeatures)

%feature("director") carto::VectorTileSearchService;

%include "search/VectorTileSearchService.h"

#endif

#endif
