#include "LocalPackageManagerTileDataSource.h"
#include "core/MapTile.h"
#include "components/Exceptions.h"
#include "utils/GeneralUtils.h"
#include "utils/Log.h"
#include "utils/Const.h"
#include "packagemanager/PackageTileMask.h"

#include <boost/lexical_cast.hpp>

#include <memory>

namespace carto
{

    LocalPackageManagerTileDataSource::LocalPackageManagerTileDataSource(int maxOpenedPackages) : TileDataSource(0, Const::MAX_SUPPORTED_ZOOM_LEVEL),
                                                                                                  _dataSources(),
                                                                                                  _cachedOpenDataSources(),
                                                                                                  _maxOpenedPackages(maxOpenedPackages),
                                                                                                  _mutex()
    {
    }
    LocalPackageManagerTileDataSource::LocalPackageManagerTileDataSource() : TileDataSource(0, Const::MAX_SUPPORTED_ZOOM_LEVEL),
                                                                             _dataSources(),
                                                                             _cachedOpenDataSources(),
                                                                             _maxOpenedPackages(4),
                                                                             _mutex()
    {
    }

    std::shared_ptr<TileData> LocalPackageManagerTileDataSource::loadTile(const MapTile &mapTile)
    {
        Log::Infof("LocalPackageManagerTileDataSource::loadTile: Loading %s", mapTile.toString().c_str());
        try
        {
            MapTile mapTileFlipped = mapTile.getFlipped();

            std::shared_ptr<TileData> tileData;
            std::lock_guard<std::mutex> lock(_mutex);

            // Fast path: try already open packages
            for (auto it = _cachedOpenDataSources.begin(); it != _cachedOpenDataSources.end(); it++)
            {
                std::shared_ptr<PackageTileMask> tileMask = it->first;
                if (tileMask)
                {
                    if (tileMask->getTileStatus(mapTileFlipped) == PackageTileStatus::PACKAGE_TILE_STATUS_MISSING)
                    {
                        continue;
                    }
                }

                tileData = it->second->loadTile(mapTileFlipped);
                if (tileData || tileMask)
                {
                    std::rotate(_cachedOpenDataSources.begin(), it, it + 1);
                    break;
                }
            }
            if (!tileData)
            {
                // Slow path: try other packages
                for (auto it = _dataSources.begin(); it != _dataSources.end(); it++)
                {
                    if (auto dataSource = std::dynamic_pointer_cast<MBTilesTileDataSource>(it->second))
                    {
                        std::shared_ptr<PackageTileMask> tileMask = it->first;
                        if (tileMask)
                        {
                            if (tileMask->getTileStatus(mapTileFlipped) == PackageTileStatus::PACKAGE_TILE_STATUS_MISSING)
                            {
                                continue;
                            }
                        }

                        tileData = dataSource->loadTile(mapTileFlipped);
                        if (tileData || tileMask)
                        {
                            _cachedOpenDataSources.insert(_cachedOpenDataSources.begin(), std::make_pair(tileMask, dataSource));
                            if (_cachedOpenDataSources.size() > _maxOpenedPackages)
                            {
                                _cachedOpenDataSources.pop_back();
                            }
                            break;
                        }
                    }
                }
            }

            if (!tileData)
            {
                if (mapTileFlipped.getZoom() > getMinZoom())
                {
                    Log::Infof("PackageManagerTileDataSource::loadTile: Tile data doesn't exist in the database, redirecting to parent");
                    tileData->setReplaceWithParent(true);
                }
                else
                {
                    Log::Infof("PackageManagerTileDataSource::loadTile: Tile data doesn't exist in the database");
                    return std::shared_ptr<TileData>();
                }
            }
            return tileData;
        }
        catch (const std::exception &ex)
        {
            Log::Errorf("PackageManagerTileDataSource::loadTile: Exception: %s", ex.what());
        }
        return std::shared_ptr<TileData>();
    }

    void LocalPackageManagerTileDataSource::onPackagesChanged(PackageChangeType changeType)
    {
        {
            std::lock_guard<std::mutex> lock(_mutex);
            _cachedOpenDataSources.clear();
        }
        notifyTilesChanged(changeType == PACKAGES_DELETED); // we need to remove tiles only if packages were deleted
    }

    void LocalPackageManagerTileDataSource::add(const std::shared_ptr<MBTilesTileDataSource> &dataSource)
    {
        addAll(std::vector<std::shared_ptr<MBTilesTileDataSource>>{dataSource});
    }

    void LocalPackageManagerTileDataSource::addAll(const std::vector<std::shared_ptr<MBTilesTileDataSource>> &dataSources)
    {
        if (!std::all_of(dataSources.begin(), dataSources.end(), [](const std::shared_ptr<MBTilesTileDataSource> &dataSource) -> bool
                         { return dataSource.get(); }))
        {
            throw NullArgumentException("Null dataSource");
        }

        {
            std::lock_guard<std::mutex> lock(_mutex);
            for (const std::shared_ptr<MBTilesTileDataSource> &dataSource : dataSources)
            {
                auto it = std::find_if(_dataSources.begin(), _dataSources.end(), [&dataSource](const std::pair<std::shared_ptr<PackageTileMask>, std::shared_ptr<MBTilesTileDataSource>> pair)
                                       { return pair.second == dataSource; });
                if (it == _dataSources.end())
                {
                    std::shared_ptr<PackageTileMask> tileMask;
                    std::string tileMaskStr = dataSource->getMetaData("tilemask");
                    if (!tileMaskStr.empty())
                    {
                        std::vector<std::string> parts = GeneralUtils::Split(tileMaskStr, ':');
                        if (!parts.empty())
                        {
                             int zoomLevel;
                            if (parts.size() > 1)
                            {
                                zoomLevel = boost::lexical_cast<int>(parts[1]);
                            }
                            else
                            {
                                zoomLevel = dataSource->getMaxZoom();
                            }
                            tileMask = std::make_shared<PackageTileMask>(parts[0], zoomLevel);
                        }
                    }
                    _dataSources.emplace_back(tileMask, dataSource);
                }
            }
        }
        onPackagesChanged(PACKAGES_ADDED);
    }

    bool LocalPackageManagerTileDataSource::remove(const std::shared_ptr<MBTilesTileDataSource> &dataSource)
    {
        return removeAll(std::vector<std::shared_ptr<MBTilesTileDataSource>>{dataSource});
    }

    bool LocalPackageManagerTileDataSource::removeAll(const std::vector<std::shared_ptr<MBTilesTileDataSource>> &dataSources)
    {
        if (!std::all_of(dataSources.begin(), dataSources.end(), [](const std::shared_ptr<MBTilesTileDataSource> &dataSource) -> bool
                         { return dataSource.get(); }))
        {
            throw NullArgumentException("Null dataSource");
        }

        bool removedAll = true;
        {
            std::lock_guard<std::mutex> lock(_mutex);
            for (const std::shared_ptr<MBTilesTileDataSource> &dataSource : dataSources)
            {
                auto it = std::remove_if(_dataSources.begin(), _dataSources.end(), [&dataSource](const std::pair<std::shared_ptr<PackageTileMask>, std::shared_ptr<MBTilesTileDataSource>> pair)
                                         { return pair.second == dataSource; });
                if (it == _dataSources.end())
                {
                    removedAll = false;
                    continue;
                }
                _dataSources.erase(it);
                // auto it = std::remove(_dataSources.begin(), _dataSources.end(), datasource);
                // if (it == _dataSources.end()) {
                // removedAll = false;
                // continue;
                // }
                // _dataSources.erase(it);
                // if (std::find(_dataSources.begin(), _dataSources.end(), layer) == _dataSources.end()) {
                // }
            }
        }
        onPackagesChanged(PACKAGES_DELETED);
        return removedAll;
    }

}
