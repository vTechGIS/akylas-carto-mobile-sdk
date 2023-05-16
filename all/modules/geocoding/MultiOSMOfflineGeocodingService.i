#ifndef _OSMOFFLINEGEOCODINGSERVICE_I
#define _OSMOFFLINEGEOCODINGSERVICE_I

#pragma SWIG nowarn=325
#pragma SWIG nowarn=401

%module(directors="1") MultiOSMOfflineGeocodingService

#if defined(_CARTO_GEOCODING_SUPPORT) && defined(_CARTO_OFFLINE_SUPPORT)

!proxy_imports(carto::MultiOSMOfflineGeocodingService, geocoding.GeocodingService, geocoding.GeocodingRequest, geocoding.GeocodingResult, projections.Projection)

%{
#include "geocoding/MultiOSMOfflineGeocodingService.h"
#include "components/Exceptions.h"
#include <memory>
%}

%include <std_shared_ptr.i>
%include <cartoswig.i>

%import "geocoding/GeocodingService.i"
%import "geocoding/GeocodingRequest.i"
%import "geocoding/GeocodingResult.i"

!polymorphic_shared_ptr(carto::MultiOSMOfflineGeocodingService, geocoding.MultiOSMOfflineGeocodingService)

%std_io_exceptions(carto::MultiOSMOfflineGeocodingService::MultiOSMOfflineGeocodingService)
%std_io_exceptions(carto::MultiOSMOfflineGeocodingService::calculateAddresses)

%feature("director") carto::MultiOSMOfflineGeocodingService;

%include "geocoding/MultiOSMOfflineGeocodingService.h"

#endif

#endif
