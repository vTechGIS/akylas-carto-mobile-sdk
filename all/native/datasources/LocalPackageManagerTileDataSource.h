/*
 * Copyright (c) 2016 CartoDB. All rights reserved.
 * Copying and using this code is allowed only according
 * to license terms, as given in https://cartodb.com/terms/
 */


#ifndef _CARTO_LOCALPACKAGEMANAGERTILEDATASOURCE_H_
#define _CARTO_LOCALPACKAGEMANAGERTILEDATASOURCE_H_

#include "datasources/TileDataSource.h"
#include "datasources/MBTilesTileDataSource.h"
#include "packagemanager/PackageTileMask.h"

#include <memory>
#include <mutex>
#include <vector>

namespace carto {

    /**
     * A tile data source that handles multiple data sources.
     */
    class LocalPackageManagerTileDataSource : public TileDataSource {
    public:
        /**
         * Constructs a PackageManagerTileDataSource object.
         * @param packageManager The package manager that is used to retrieve requested tiles.
         */
        explicit LocalPackageManagerTileDataSource();
        LocalPackageManagerTileDataSource(int maxOpenedPackages);
        virtual ~LocalPackageManagerTileDataSource();

        virtual std::shared_ptr<TileData> loadTile(const MapTile& mapTile);

        
        /**
         * Adds a new  data source to the  data source stack. The new  data source will be the last (and topmost)  data source.
         * @param datasource The data source to be added.
         */
        void add(const std::shared_ptr<MBTilesTileDataSource>& datasource);
        /**
         * Adds a a list of datasources to the  data source stack. The new datasources will be the last (and topmost) datasources.
         * @param datasources The  data source list to be added.
         */
        void addAll(const std::vector<std::shared_ptr<MBTilesTileDataSource> >& datasources);

        /**
         * Removes a data source from the sources stack.
         * @param datasource The data source to be removed.
         * @return True if the  data source was removed. False otherwise ( data source was not found).
         */
        bool remove(const std::shared_ptr<MBTilesTileDataSource>& datasource);
        /**
         * Removes a list of datasources from the  data source stack.
         * @param datasources The list of datasources to be removed.
         * @return True if all  data source were removed. False otherwise (some datasources were not found).
         */
        bool removeAll(const std::vector<std::shared_ptr<MBTilesTileDataSource> >& datasources);

    protected:
         enum PackageChangeType {
            PACKAGES_UPDATED,
            PACKAGES_ADDED,
            PACKAGES_DELETED
        };

        mutable std::optional<int> _maxOpenedPackages;

        mutable std::vector<std::pair<std::shared_ptr<PackageTileMask>, std::shared_ptr<MBTilesTileDataSource> > > _dataSources;
        mutable std::vector<std::pair<std::shared_ptr<PackageTileMask>, std::shared_ptr<MBTilesTileDataSource> > > _cachedOpenDataSources;

        mutable std::mutex _mutex;

        void onPackagesChanged(PackageChangeType changeType);
    };

}

#endif
