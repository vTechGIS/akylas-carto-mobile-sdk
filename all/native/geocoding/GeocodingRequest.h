/*
 * Copyright (c) 2016 CartoDB. All rights reserved.
 * Copying and using this code is allowed only according
 * to license terms, as given in https://cartodb.com/terms/
 */

#ifndef _CARTO_GEOCODINGREQUEST_H_
#define _CARTO_GEOCODINGREQUEST_H_

#ifdef _CARTO_GEOCODING_SUPPORT

#include <memory>
#include <mutex>
#include <string>

#include "core/MapPos.h"
#include "core/Variant.h"

namespace carto {
    class Projection;

    /**
     * A geocoding request describing text-based query and other optional constraints.
     */
    class GeocodingRequest {
    public:
        /**
         * Constructs a new GeocodingRequest object from a projection and a string-based query.
         * @param projection The projection to use for the result and location data.
         * @param query The text-based query.
         */
        GeocodingRequest(const std::shared_ptr<Projection>& projection, const std::string& query);
        virtual ~GeocodingRequest();

        /**
         * Returns the text-based query of the request.
         * @return The text-based query of the request.
         */
        const std::string& getQuery() const;

        /**
         * Returns the projection of the request.
         * @return The projection of the request.
         */
        const std::shared_ptr<Projection>& getProjection() const;

        /**
         * Returns the location attribute of the request.
         * The matching address near the specified location (up to a specified radius) are preferred.
         * @return The location attribute of the request.
         */
        MapPos getLocation() const;
        /**
         * Sets the location attribute of the request.
         * The matching address near the specified location (up to a specified radius) are preferred.
         * @param pos The new location for the request.
         */
        void setLocation(const MapPos& pos);
        /**
         * Returns true if the location is explictly defined in the request.
         * @return True if the location is explictly defined in the request.
         */
        bool isLocationDefined() const;

        /**
         * Returns the location radius attribute of the request (in meters).
         * @return The location radius attribute of the request in meters.
         */
        float getLocationRadius() const;
        /**
         * Sets the location radius attribute of the request.
         * The default is 0, which means that location based ranking is not applied.
         * @param radius The location radius attribute of the request in meters.
         */
        void setLocationRadius(float radius);
        
        /**
         * Returns the set of custom parameters of the request as a variant.
         * @return The set of custom parameters as a variant. Can be empty.
         */
        Variant getCustomParameters() const;
        /**
         * Returns the custom parameter value of the request.
         * @param param The name of the parameter to return.
         * @return The value of the parameter. If the parameter does not exist, empty variant is returned.
         */
        Variant getCustomParameter(const std::string& param) const;
        /**
         * Sets a custom parameter value for the the request.
         * @param param The name of the parameter. For example, "ranking.location_sigma".
         * @param value The new value for the parameter.
         */
        void setCustomParameter(const std::string& param, const Variant& value);

        /**
         * Creates a string representation of this request object, useful for logging.
         * @return The string representation of this request object.
         */
        std::string toString() const;

    private:
        const std::string _query;
        const std::shared_ptr<Projection> _projection;
        MapPos _location;
        bool _locationDefined;
        float _locationRadius;
        Variant _customParams;

        mutable std::mutex _mutex;
    };
    
}

#endif

#endif
