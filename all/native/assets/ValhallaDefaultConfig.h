static const char valhalla_default_config[] = R"({
  "additional_data": {
    "elevation": ""
  },
  "httpd": {
    "service": {
      "drain_seconds": 28,
      "timeout_seconds": 28,
      "interrupt": "ipc:///tmp/interrupt",
      "listen": "tcp://*:8002",
      "loopback": "ipc:///tmp/loopback",
      "shutdown_seconds": 1
    }
  },
  "loki": {
    "actions": [
      "locate",
      "route",
      "height",
      "sources_to_targets",
      "optimized_route",
      "isochrone",
      "trace_route",
      "trace_attributes",
      "transit_available",
      "expansion",
      "centroid",
      "status"
    ],
    "logging": {
      "color": true,
      "file_name": "",
      "long_request": 100.0,
      "type": ""
    },
    "service": {
      "proxy": "ipc:///tmp/loki"
    },
    "service_defaults": {
      "heading_tolerance": 60,
      "minimum_reachability": 50,
      "node_snap_tolerance": 5,
      "radius": 0,
      "search_cutoff": 35000,
      "street_side_max_distance": 1000,
      "street_side_tolerance": 5
    },
    "use_connectivity": false
  },
  "meili": {
    "auto": {
      "search_radius": 50,
      "turn_penalty_factor": 200
    },
    "bicycle": {
      "turn_penalty_factor": 140
    },
    "customizable": [
      "mode",
      "search_radius",
      "turn_penalty_factor",
      "gps_accuracy",
      "interpolation_distance",
      "sigma_z",
      "beta",
      "max_route_distance_factor",
      "max_route_time_factor"
    ],
    "default": {
      "beta": 3,
      "breakage_distance": 2000,
      "geometry": false,
      "gps_accuracy": 5.0,
      "interpolation_distance": 10,
      "max_route_distance_factor": 5,
      "max_route_time_factor": 5,
      "max_search_radius": 100,
      "route": true,
      "search_radius": 50,
      "sigma_z": 4.07,
      "turn_penalty_factor": 0
    },
    "grid": {
      "cache_size": 100240,
      "size": 500
    },
    "logging": {
      "color": true,
      "file_name": "",
      "type": ""
    },
    "mode": "auto",
    "multimodal": {
      "turn_penalty_factor": 70
    },
    "pedestrian": {
      "search_radius": 50,
      "turn_penalty_factor": 100
    },
    "service": {
      "proxy": "ipc:///tmp/meili"
    },
    "verbose": false
  },
  "mjolnir": {
    "admin": "",
    "data_processing": {
      "allow_alt_name": false,
      "apply_country_overrides": true,
      "infer_internal_intersections": true,
      "infer_turn_channels": true,
      "scan_tar": false,
      "use_admin_db": false,
      "use_direction_on_ways": false,
      "use_rest_area": false,
      "use_urban_tag": false
    },
    "global_synchronized_cache": false,
    "hierarchy": true,
    "id_table_size": 1300000000,
    "import_bike_share_stations": false,
    "include_bicycle": true,
    "include_construction": false,
    "include_driveways": true,
    "include_driving": true,
    "include_pedestrian": true,
    "logging": {
      "color": true,
      "file_name": "",
      "type": ""
    },
    "lru_mem_cache_hard_control": false,
    "max_cache_size": 1000000000,
    "max_concurrent_reader_users": 1,
    "reclassify_links": true,
    "shortcuts": true,
    "tile_dir": "",
    "tile_extract": "",
    "timezone": "",
    "traffic_extract": "",
    "transit_dir": "",
    "transit_feeds_dir": "",
    "use_lru_mem_cache": false,
    "use_simple_mem_cache": false
  },
  "odin": {
    "logging": {
      "color": true,
      "file_name": "",
      "type": ""
    },
    "markup_formatter": {
      "markup_enabled": false,
      "phoneme_format": "<TEXTUAL_STRING> (<span class=<QUOTES>phoneme<QUOTES>>/<VERBAL_STRING>/</span>) "
    },
    "service": {
      "proxy": "ipc:///tmp/odin"
    }
  },
  "service_limits": {
    "auto": {
      "max_distance": 5000000.0,
      "max_locations": 20,
      "max_matrix_distance": 400000.0,
      "max_matrix_location_pairs": 2500
    },
    "bicycle": {
      "max_distance": 500000.0,
      "max_locations": 50,
      "max_matrix_distance": 200000.0,
      "max_matrix_location_pairs": 2500
    },
    "bikeshare": {
      "max_distance": 500000.0,
      "max_locations": 50,
      "max_matrix_distance": 200000.0,
      "max_matrix_location_pairs": 2500
    },
    "bus": {
      "max_distance": 5000000.0,
      "max_locations": 50,
      "max_matrix_distance": 400000.0,
      "max_matrix_location_pairs": 2500
    },
    "centroid": {
      "max_distance": 200000.0,
      "max_locations": 5
    },
    "isochrone": {
      "max_contours": 4,
      "max_distance": 25000.0,
      "max_distance_contour": 200,
      "max_locations": 1,
      "max_time_contour": 120
    },
    "max_alternates": 2,
    "max_exclude_locations": 50,
    "max_exclude_polygons_length": 10000,
    "max_radius": 200,
    "max_reachability": 100,
    "max_timedep_distance": 500000,
    "max_timedep_distance_matrix": 0,
    "motor_scooter": {
      "max_distance": 500000.0,
      "max_locations": 50,
      "max_matrix_distance": 200000.0,
      "max_matrix_location_pairs": 2500
    },
    "motorcycle": {
      "max_distance": 500000.0,
      "max_locations": 50,
      "max_matrix_distance": 200000.0,
      "max_matrix_location_pairs": 2500
    },
    "multimodal": {
      "max_distance": 500000.0,
      "max_locations": 50,
      "max_matrix_distance": 0.0,
      "max_matrix_location_pairs": 0
    },
    "pedestrian": {
      "max_distance": 250000.0,
      "max_locations": 50,
      "max_matrix_distance": 200000.0,
      "max_matrix_location_pairs": 2500,
      "max_transit_walking_distance": 10000,
      "min_transit_walking_distance": 1
    },
    "skadi": {
      "max_shape": 750000,
      "min_resample": 10.0
    },
    "status": {
      "allow_verbose": false
    },
    "taxi": {
      "max_distance": 5000000.0,
      "max_locations": 20,
      "max_matrix_distance": 400000.0,
      "max_matrix_location_pairs": 2500
    },
    "trace": {
      "max_alternates": 3,
      "max_alternates_shape": 100,
      "max_distance": 200000.0,
      "max_gps_accuracy": 100.0,
      "max_search_radius": 100.0,
      "max_shape": 16000
    },
    "transit": {
      "max_distance": 500000.0,
      "max_locations": 50,
      "max_matrix_distance": 200000.0,
      "max_matrix_location_pairs": 2500
    },
    "truck": {
      "max_distance": 5000000.0,
      "max_locations": 20,
      "max_matrix_distance": 400000.0,
      "max_matrix_location_pairs": 2500
    }
  },
  "thor": {
    "clear_reserved_memory": false,
    "extended_search": false,
    "logging": {
      "color": true,
      "file_name": "",
      "long_request": 110.0,
      "type": ""
    },
    "max_reserved_labels_count": 1000000,
    "service": {
      "proxy": "ipc:///tmp/thor"
    },
    "source_to_target_algorithm": "select_optimal"
  }
})";
