#ifndef _OSMOFFLINEREVERSEGEOCODINGSERVICE_I
#define _OSMOFFLINEREVERSEGEOCODINGSERVICE_I

#pragma SWIG nowarn=325
#pragma SWIG nowarn=401

%module(directors="1") MultiOSMOfflineReverseGeocodingService

#if defined(_CARTO_GEOCODING_SUPPORT) && defined(_CARTO_OFFLINE_SUPPORT)

!proxy_imports(carto::MultiOSMOfflineReverseGeocodingService, geocoding.ReverseGeocodingService, geocoding.ReverseGeocodingRequest, geocoding.GeocodingResult, projections.Projection)

%{
#include "geocoding/MultiOSMOfflineReverseGeocodingService.h"
#include "components/Exceptions.h"
#include <memory>
%}

%include <std_shared_ptr.i>
%include <cartoswig.i>

%import "geocoding/ReverseGeocodingService.i"
%import "geocoding/ReverseGeocodingRequest.i"
%import "geocoding/GeocodingResult.i"

!polymorphic_shared_ptr(carto::MultiOSMOfflineReverseGeocodingService, geocoding.MultiOSMOfflineReverseGeocodingService)

%std_io_exceptions(carto::MultiOSMOfflineReverseGeocodingService::MultiOSMOfflineReverseGeocodingService)
%std_io_exceptions(carto::MultiOSMOfflineReverseGeocodingService::calculateAddresses)

%feature("director") carto::MultiOSMOfflineReverseGeocodingService;

%include "geocoding/MultiOSMOfflineReverseGeocodingService.h"

#endif

#endif
