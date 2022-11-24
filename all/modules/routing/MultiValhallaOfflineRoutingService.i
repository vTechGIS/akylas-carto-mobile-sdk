#ifndef _MULTIVALHALLAOFFLINEROUTINGSERVICE_I
#define _MULTIVALHALLAOFFLINEROUTINGSERVICE_I

%module(directors="1") MultiValhallaOfflineRoutingService

#if defined(_CARTO_ROUTING_SUPPORT) && defined(_CARTO_VALHALLA_ROUTING_SUPPORT)

!proxy_imports(carto::MultiValhallaOfflineRoutingService, core.Variant, routing.RoutingService, routing.RoutingRequest, routing.RoutingResult, routing.RouteMatchingRequest, routing.RouteMatchingResult)

%{
#include "routing/MultiValhallaOfflineRoutingService.h"
#include "components/Exceptions.h"
#include <memory>
%}

%include <std_shared_ptr.i>
%include <std_string.i>
%include <cartoswig.i>

%import "core/Variant.i"
%import "routing/RoutingService.i"

!polymorphic_shared_ptr(carto::MultiValhallaOfflineRoutingService, routing.MultiValhallaOfflineRoutingService)

%std_io_exceptions(carto::MultiValhallaOfflineRoutingService::matchRoute)
%std_io_exceptions(carto::MultiValhallaOfflineRoutingService::calculateRoute)

%feature("director") carto::MultiValhallaOfflineRoutingService;

%include "routing/MultiValhallaOfflineRoutingService.h"

#endif

#endif
