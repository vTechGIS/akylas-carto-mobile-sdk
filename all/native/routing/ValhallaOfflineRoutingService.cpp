#if defined(_CARTO_ROUTING_SUPPORT) && defined(_CARTO_VALHALLA_ROUTING_SUPPORT) && defined(_CARTO_OFFLINE_SUPPORT)

#include "ValhallaOfflineRoutingService.h"
#include "components/Exceptions.h"
#include "routing/utils/ValhallaRoutingProxy.h"
#include "utils/Const.h"
#include "utils/Log.h"

#include <boost/algorithm/string.hpp>

#include <sqlite3pp.h>

namespace carto {

    ValhallaOfflineRoutingService::ValhallaOfflineRoutingService(const std::string& path) :
        _database(std::make_unique<sqlite3pp::database>()),
        _profile("pedestrian"),
        _configuration(ValhallaRoutingProxy::GetDefaultConfiguration()),
        _mutex()
    {
        if (_database->connect_v2(path.c_str(), SQLITE_OPEN_READONLY | SQLITE_OPEN_FULLMUTEX) != SQLITE_OK) {
            throw FileException("Failed to open routing database", path);
        }
        _database->execute("PRAGMA temp_store=MEMORY");
    }

    ValhallaOfflineRoutingService::~ValhallaOfflineRoutingService() {
    }

    Variant ValhallaOfflineRoutingService::getConfigurationParameter(const std::string& param) const {
        std::lock_guard<std::mutex> lock(_mutex);
        std::vector<std::string> keys;
        boost::split(keys, param, boost::is_any_of("."));
        picojson::value subValue = _configuration.toPicoJSON();
        for (const std::string& key : keys) {
            if (!subValue.is<picojson::object>()) {
                return Variant();
            }
            subValue = subValue.get(key);
        }
        return Variant::FromPicoJSON(subValue);
    }

    void ValhallaOfflineRoutingService::setConfigurationParameter(const std::string& param, const Variant& value) {
        std::lock_guard<std::mutex> lock(_mutex);
        std::vector<std::string> keys;
        boost::split(keys, param, boost::is_any_of("."));
        picojson::value config = _configuration.toPicoJSON();
        picojson::value* subValue = &config;
        for (const std::string& key : keys) {
            if (!subValue->is<picojson::object>()) {
                subValue->set(picojson::object());
            }
            subValue = &subValue->get<picojson::object>()[key];
        }
        *subValue = value.toPicoJSON();
        _configuration = Variant::FromPicoJSON(config);
    }

    std::string ValhallaOfflineRoutingService::getProfile() const {
        std::lock_guard<std::mutex> lock(_mutex);
        return _profile;
    }

    void ValhallaOfflineRoutingService::setProfile(const std::string& profile) {
        std::lock_guard<std::mutex> lock(_mutex);
        _profile = profile;
    }

    std::shared_ptr<RouteMatchingResult> ValhallaOfflineRoutingService::matchRoute(const std::shared_ptr<RouteMatchingRequest>& request) const {
        if (!request) {
            throw NullArgumentException("Null request");
        }

        std::string profile;
        Variant configuration;
        {
            std::lock_guard<std::mutex> lock(_mutex);

            profile = _profile;
            configuration = _configuration;
        }
        return ValhallaRoutingProxy::MatchRoute(std::vector<std::shared_ptr<sqlite3pp::database> > { _database }, profile, configuration, request);
    }

    std::shared_ptr<RoutingResult> ValhallaOfflineRoutingService::calculateRoute(const std::shared_ptr<RoutingRequest>& request) const {
        if (!request) {
            throw NullArgumentException("Null request");
        }

        std::string profile;
        Variant configuration;
        {
            std::lock_guard<std::mutex> lock(_mutex);

            profile = _profile;
            configuration = _configuration;
        }

        return ValhallaRoutingProxy::CalculateRoute(std::vector<std::shared_ptr<sqlite3pp::database> > { _database }, profile, configuration, request);
    }

    void ValhallaOfflineRoutingService::addLocale(const std::string& key, const std::string& json) const {
        std::lock_guard<std::mutex> lock(_mutex);
        ValhallaRoutingProxy::AddLocale(key, json);
    }
}

#endif
