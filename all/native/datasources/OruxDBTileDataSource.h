/*
 * Copyright (c) 2016 CartoDB. All rights reserved.
 * Copying and using this code is allowed only according
 * to license terms, as given in https://cartodb.com/terms/
 */

#ifndef _CARTO__ORUXDBDATASOURCE_H_
#define _CARTO__ORUXDBDATASOURCE_H_

#ifdef _CARTO_OFFLINE_SUPPORT

#include "datasources/TileDataSource.h"

#include <map>
#include <memory>
#include <optional>

namespace sqlite3pp {
    class database;
}
    
namespace carto {
    
    namespace OruxScheme {
        /**
         * MBTiles tile schemes.
         */
        enum OruxScheme {
            /**
             * The default scheme. Vertical coordinate is not flipped.
             */
            ORUX_SCHEME_TMS,
        
            /**
             * Alternative to TMS scheme. Vertical coordinate is flipped.
             */
            ORUX_SCHEME_XYZ
        };
    }
    
    /**
     * A tile data source that loads tiles from a local Sqlite database.
     * The database must contain table "tiles" with the following fields:
     * "zoom_level" (tile zoom level), "tile_column" (tile x coordinate),
     * "tile_row" (tile y coordinate), "tile_data" (compressed tile image).
     */
    class OruxDBTileDataSource : public TileDataSource {
    public:
        /**
         * Constructs a OruxDBTileDataSource object. TMS tile scheme is used,
         * Min and max zoom levels are automatically detected.
         * @param path The path to the local Sqlite database file.
         * @throws std::exception If the the file could not be opened.
         */
        explicit OruxDBTileDataSource(const std::string& path);
        
        /**
         * Constructs a OruxDBTileDataSource object. TMS tile scheme is used.
         * @param minZoom The minimum zoom level supported by this data source.
         * @param maxZoom The maximum zoom level supported by this data source.
         * @param path The path to the local Sqlite database file.
         * @throws std::exception If the the file could not be opened.
         */
        OruxDBTileDataSource(int minZoom, int maxZoom, const std::string& path);
    
        /**
         * Constructs a OruxDBTileDataSource object with specified tile scheme.
         * @param minZoom The minimum zoom level supported by this data source.
         * @param maxZoom The maximum zoom level supported by this data source.
         * @param path The path to the local Sqlite database file.
         * @param scheme Tile scheme to use.
         * @throws std::exception If the the file could not be opened.
         */
        OruxDBTileDataSource(int minZoom, int maxZoom, const std::string& path, OruxScheme::OruxScheme scheme);
    
        virtual ~OruxDBTileDataSource();
        
        /**
         * Get data source metadata information.
         * Possible parameters can be found in MBTiles specification.
         * @return Map containing meta data information (parameter names mapped to parameter values).
         */
        std::map<std::string, std::string> getMetaData() const;
        
        virtual int getMinZoom() const;

        virtual int getMaxZoom() const;

        virtual MapBounds getDataExtent() const;

        virtual std::shared_ptr<TileData> loadTile(const MapTile& mapTile);
    
    private:
        static std::unique_ptr<sqlite3pp::database> OpenDatabase(const std::string& path);

        bool loadZoomLevels(int& minZoom, int& maxZoom) const;
        bool loadDataExtent(MapBounds& mapBounds) const;

        OruxScheme::OruxScheme _scheme;
        std::unique_ptr<sqlite3pp::database> _database;
        mutable std::optional<int> _cachedMinZoom;
        mutable std::optional<int> _cachedMaxZoom;
        mutable std::optional<MapBounds> _cachedDataExtent;
        mutable std::recursive_mutex _mutex;
    };
    
}

#endif

#endif