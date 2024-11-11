## How to contribute:

* [Add issues](https://github.com/CartoDB/mobile-sdk/issues) for found bugs and new feature ideas. Check first if issue for specific thing does not already exist
* See [README.md](https://github.com/CartoDB/mobile-sdk/blob/master/README.md) for compilation guides
* Use Pull Requests to contribute with own code additions

Additional developer channels and more detailed guidelines will be added soon.

## Submitting contributions

Before opening a pull request (or submitting a contribution) you will need to sign a Contributor License Agreement (CLA) before making a submission, [learn more here](https://carto.com/contributing).

After that, there are several rules you should follow when a new pull request is created:

- Title has to be descriptive. If you are fixing a bug don't use the ticket title or number.
- Explain what you have achieved in the description and choose a reviewer (it has to be a CARTO team member) of your code. If you have doubts, just ask for one.

## Development environments

* [iOS instructions](https://github.com/CartoDB/mobile-sdk/tree/master/scripts/ios-dev)
* [Android instructions](https://github.com/CartoDB/mobile-sdk/tree/master/scripts/android-dev)

## Directories

* all/native - C++ sources for all platforms, main part of the SDK
* all/modules - Swig configuration files for all platforms
* android - Android-specific (C++, Java, C#) code, Swig wrapper config and helpers
* ios - iOS-specific (C++, Objective-C, C#) code, Swig wrapper config and helpers
* winphone - Windows Phone-specific (C++, C#) code, Swig wrapper config and helpers
* dotnet - generic C# code shared by Xamarin and Windows Phone builds
* assets - images and style files sources used for map rendering. Converted to C++ headers
* docs - documentation, published in CARTO docs site
* scripts - scripts for building SDK (using Python, CMake, Swig and other tools)


## valhalla

If you need to update the Valhalla submodule you might need to update the proto files.
```
cd libs-external/protobuf/protobuf
make -j$(nproc)
./src/protoc --proto_path ../../valhalla/valhalla/proto/ --cpp_out ../../valhalla/proto/valhalla/proto/ api.proto common.proto directions.proto incidents.proto info.proto options.proto sign.proto status.proto transit.proto transit_fetch.proto trip.proto matrix.proto isochrone.proto expansion.proto
```
You might also need to update locales
```
cd libs-external/valhalla/valhalla
cmake -DMSVC= -P cmake/ValhallaBin2Header.cmake path_to_wanted_valhalla_json_locales/ ../config/valhalla/odin/locales.h --locales