#include "HillshadeTileRenderer.h"
#include "components/Options.h"
#include "components/ThreadWorker.h"
#include "graphics/ViewState.h"
#include "projections/ProjectionSurface.h"
#include "projections/PlanarProjectionSurface.h"
#include "renderers/MapRenderer.h"
#include "renderers/drawdatas/TileDrawData.h"
#include "renderers/utils/GLResourceManager.h"
#include "renderers/utils/VTRenderer.h"
#include "layers/HillshadeTileLayer.h"
#include "utils/Log.h"
#include "utils/Const.h"

#include <vt/Label.h>
#include <vt/LabelCuller.h>
#include <vt/TileTransformer.h>
#include <vt/GLTileRenderer.h>
#include <vt/GLExtensions.h>

#include <cglib/mat.h>

namespace carto {

    HillshadeTileRenderer::HillshadeTileRenderer(): TileRenderer() ,
                                                    _inspect(false)
    {
    }

    HillshadeTileRenderer::~HillshadeTileRenderer() {
    }

//    void HillshadeTileRenderer::setComponents(const std::weak_ptr<Options>& options, const std::weak_ptr<MapRenderer>& mapRenderer, const std::weak_ptr<HillshadeTileLayer>& tileLayer) {
//        _tileLayer = tileLayer;
//        TileRenderer::setComponents(options, mapRenderer);
//    }

    void HillshadeTileRenderer::setRotation(float rotation) {
        std::lock_guard<std::mutex> lock(_mutex);
        _rotation = rotation;
    }


    void HillshadeTileRenderer::setMaZoom(float maxZoom) {
        std::lock_guard<std::mutex> lock(_mutex);
        _maxZoom = maxZoom;
    }
    void HillshadeTileRenderer::setIlluminationDirection(float illuminationDirection) {
        std::lock_guard<std::mutex> lock(_mutex);
        _illuminationDirection = illuminationDirection;
    }
    void HillshadeTileRenderer::setExaggeration(float exaggeration) {
        std::lock_guard<std::mutex> lock(_mutex);
        _exaggeration = exaggeration;
    }
    void HillshadeTileRenderer::setHighlightColor(Color highlightColor) {
        std::lock_guard<std::mutex> lock(_mutex);
        _highlightColor = highlightColor;
    }
    void HillshadeTileRenderer::setAccentColor(Color accentColor) {
        std::lock_guard<std::mutex> lock(_mutex);
        _accentColor = accentColor;
    }
    void HillshadeTileRenderer::setShadowColor(Color shadowColor) {
        std::lock_guard<std::mutex> lock(_mutex);
        _shadowColor = shadowColor;
    }
    void HillshadeTileRenderer::setInspect(bool inspect) {
        std::lock_guard<std::mutex> lock(_mutex);
        _inspect = inspect;
    }
    bool HillshadeTileRenderer::initializeRenderer() {
        if (_vtRenderer && _vtRenderer->isValid()) {
            return true;
        }

        std::shared_ptr<MapRenderer> mapRenderer = _mapRenderer.lock();
        if (!mapRenderer) {
            return false; // safety check, should never happen
        }

        boost::optional<vt::GLTileRenderer::LightingShader> lightingShader2D;
//        if (!std::dynamic_pointer_cast<PlanarProjectionSurface>(mapRenderer->getProjectionSurface())) {
//        if (!_inspect) {
            lightingShader2D = vt::GLTileRenderer::LightingShader(false, HillshadeTileRenderer::LIGHTING_SHADER_2D, [this](GLuint shaderProgram, const vt::ViewState& viewState) {
                glUniform3fv(glGetUniformLocation(shaderProgram, "u_viewDir"), 1, _viewDir.data());

                 glUniform4f(glGetUniformLocation(shaderProgram, "u_highlight"), _highlightColor.getR() / 255.0f, _highlightColor.getG() / 255.0f, _highlightColor.getB() / 255.0f, _highlightColor.getA() / 255.0f);
                 glUniform4f(glGetUniformLocation(shaderProgram, "u_accent"), _accentColor.getR() / 255.0f, _accentColor.getG() / 255.0f, _accentColor.getB() / 255.0f, _accentColor.getA() / 255.0f);
                 glUniform4f(glGetUniformLocation(shaderProgram, "u_shadow"), _shadowColor.getR() / 255.0f, _shadowColor.getG() / 255.0f, _shadowColor.getB() / 255.0f, _shadowColor.getA() / 255.0f);

                // if (evaluated.get<HillshadeIlluminationAnchor>() == HillshadeIlluminationAnchorType::Viewport) azimuthal = azimuthal - parameters.state.getBearing();
                // first parameter is HillshadeExaggeration
                float azimuthal = _illuminationDirection * Const::DEG_TO_RAD;
                azimuthal = azimuthal - _rotation * Const::DEG_TO_RAD;
//                std::string message =  "HillshadeTileRenderer: rendering tile renderer " + std::to_string(viewState.zoom) + ", " + std::to_string(std::floor(viewState.zoom));
//                Log::Debug(message.c_str());
                glUniform2f(glGetUniformLocation(shaderProgram, "u_light"), _exaggeration, azimuthal);
                glUniform2f(glGetUniformLocation(shaderProgram, "u_dimension"), 514, 514);
//                glUniform4f(glGetUniformLocation(shaderProgram, "u_unpack"), 6553.6, 25.6, 0.1, 10000.0);
                glUniform4f(glGetUniformLocation(shaderProgram, "u_unpack"), 256.0, 1.0, 1.0 / 256.0, 32768.0);
                glUniform1f(glGetUniformLocation(shaderProgram, "u_zoom"), std::floor(viewState.zoom));
                glUniform1f(glGetUniformLocation(shaderProgram, "u_maxzoom"), 14);
//                glUniform1f(glGetUniformLocation(shaderProgram, "u_maxzoom"), _maxZoom);
                glUniform1i(glGetUniformLocation(shaderProgram, "u_inspect"), _inspect);
            });
//        }
        boost::optional<vt::GLTileRenderer::LightingShader> lightingShader3D = vt::GLTileRenderer::LightingShader(true, LIGHTING_SHADER_3D, [this](GLuint shaderProgram, const vt::ViewState& viewState) {
            if (auto options = _options.lock()) {
                const Color& ambientLightColor = options->getAmbientLightColor();
                glUniform4f(glGetUniformLocation(shaderProgram, "u_ambientColor"), ambientLightColor.getR() / 255.0f, ambientLightColor.getG() / 255.0f, ambientLightColor.getB() / 255.0f, ambientLightColor.getA() / 255.0f);
                const Color& mainLightColor = options->getMainLightColor();
                glUniform4f(glGetUniformLocation(shaderProgram, "u_lightColor"), mainLightColor.getR() / 255.0f, mainLightColor.getG() / 255.0f, mainLightColor.getB() / 255.0f, mainLightColor.getA() / 255.0f);
                glUniform3fv(glGetUniformLocation(shaderProgram, "u_lightDir"), 1, _mainLightDir.data());
                glUniform3fv(glGetUniformLocation(shaderProgram, "u_viewDir"), 1, _viewDir.data());
            }
        });

        _vtRenderer = mapRenderer->getGLResourceManager()->create<VTRenderer>(_tileTransformer, lightingShader2D, lightingShader3D);
        if (std::shared_ptr<vt::GLTileRenderer> tileRenderer = _vtRenderer->getTileRenderer()) {
            tileRenderer->setVisibleTiles(_tiles, _horizontalLayerOffset == 0);
        }

        return _vtRenderer && _vtRenderer->isValid();
    }

    const std::string HillshadeTileRenderer::LIGHTING_SHADER_2D = R"GLSL(
        uniform vec3 u_viewDir;
        uniform vec2 u_light;
        uniform vec4 u_shadow;
        uniform vec4 u_highlight;
        uniform vec4 u_accent;
        uniform bool u_inspect;

        uniform vec2 u_dimension;
        uniform float u_zoom;
        uniform float u_maxzoom;
        uniform vec4 u_unpack;

         #define PI 3.141592653589793

        #ifdef GL_ES
        precision highp float;
        #endif

        float getElevationFromData(vec4 pixel, float bias) {
            // Convert encoded elevation value to meters
            vec4 data = pixel * 255.0;
            data.a = -1.0;
            return dot(data, u_unpack) / 4.0;
        }
        float getElevation(sampler2D uBitmap, vec2 coord, float bias) {
            return getElevationFromData(texture2D(uBitmap, coord), bias);
        }

        vec4 computeElevationPixel(vec2 vUV, vec4 pixel, vec3 normal, vec2 uLatrange, sampler2D uBitmap) {
            vec2 epsilon = 1.0 / u_dimension;
            // queried pixels:
            // +-----------+
            // |   |   |   |
            // | a | b | c |
            // |   |   |   |
            // +-----------+
            // |   |   |   |
            // | d | e | f |
            // |   |   |   |
            // +-----------+
            // |   |   |   |
            // | g | h | i |
            // |   |   |   |
            // +-----------+
            float a = getElevation(uBitmap, vUV + vec2(-epsilon.x, -epsilon.y), 0.0);
            float b = getElevation(uBitmap, vUV + vec2(0, -epsilon.y), 0.0);
            float c = getElevation(uBitmap, vUV + vec2(epsilon.x, -epsilon.y), 0.0);
            float d = getElevation(uBitmap, vUV + vec2(-epsilon.x, 0), 0.0);
            float e = getElevationFromData(pixel, 0.0);
            float f = getElevation(uBitmap, vUV + vec2(epsilon.x, 0), 0.0);
            float g = getElevation(uBitmap, vUV + vec2(-epsilon.x, epsilon.y), 0.0);
            float h = getElevation(uBitmap, vUV + vec2(0, epsilon.y), 0.0);
            float i = getElevation(uBitmap, vUV + vec2(epsilon.x, epsilon.y), 0.0);
            // here we divide the x and y slopes by 8 * pixel size
            // where pixel size (aka meters/pixel) is:
            // circumference of the world / (pixels per tile * number of tiles)
            // which is equivalent to: 8 * 40075016.6855785 / (512 * pow(2, u_zoom))
            // which can be reduced to: pow(2, 19.25619978527 - u_zoom)
            // we want to vertically exaggerate the hillshading though, because otherwise
            // it is barely noticeable at low zooms. to do this, we multiply this by some
            // scale factor pow(2, (u_zoom - u_maxzoom) * a) where a is an arbitrary value
            // Here we use a=0.3 which works out to the expression below. see
            // nickidlugash's awesome breakdown for more info
            // https://github.com/mapbox/mapbox-gl-js/pull/5286#discussion_r148419556
            float exaggeration = u_zoom < 2.0 ? 0.4 : u_zoom < 4.5 ? 0.35 : 0.3;
            vec2 deriv = vec2(
                (c + f + f + i) - (a + d + d + g),
                (g + h + h + i) - (a + b + b + c)
            ) /  pow(2.0, (u_zoom - u_maxzoom) * exaggeration + 19.2562 - u_zoom);
            return clamp(vec4(
                deriv.x / 2.0 + 0.5,
                deriv.y / 2.0 + 0.5,
                1.0,
                1.0), 0.0, 1.0);
        }

        vec4 applyLighting(vec2 vUV, vec4 color, vec3 normal, vec2 uLatrange, sampler2D uBitmap) {
            float lighting = max(0.0, dot(normal, u_viewDir)) * 0.5 + 0.5;
            if (u_inspect){
               return vec4(color.rgb * lighting, color.a);
            }
            vec4 pixel = computeElevationPixel( vUV, color, normal, uLatrange, uBitmap);
            return pixel;
            vec2 deriv = ((pixel.rg * 2.0) - 1.0);
            // We divide the slope by a scale factor based on the cosin of the pixel's approximate latitude
            // to account for mercator projection distortion. see #4807 for details
            float scaleFactor = cos(radians((uLatrange[0] - uLatrange[1]) * (1.0 - vUV.y) + uLatrange[1]));
            // We also multiply the slope by an arbitrary z-factor of 1.25
            float slope = atan(1.25 * length(deriv) / scaleFactor);
            float aspect = deriv.x != 0.0 ? atan(deriv.y, -deriv.x) : PI / 2.0 * (deriv.y > 0.0 ? 1.0 : -1.0);
            float intensity = u_light.x;
            // We add PI to make this property match the global light object, which adds PI/2 to the light's azimuthal
            // position property to account for 0deg corresponding to north/the top of the viewport in the style spec
            // and the original shader was written to accept (-illuminationDirection - 90) as the azimuthal.
            float azimuth = u_light.y + PI;
            // We scale the slope exponentially based on intensity, using a calculation similar to
            // the exponential interpolation function in the style spec:
            // https://github.com/mapbox/mapbox-gl-js/blob/master/src/style-spec/expression/definitions/interpolate.js#L217-L228
            // so that higher intensity values create more opaque hillshading.
            float base = 1.875 - intensity * 1.75;
            float maxValue = 0.5 * PI;
            float scaledSlope = intensity != 0.5 ? ((pow(base, slope) - 1.0) / (pow(base, maxValue) - 1.0)) * maxValue : slope;
            // The accent color is calculated with the cosine of the slope while the shade color is calculated with the sine
            // so that the accent color's rate of change eases in while the shade color's eases out.
            float accent = cos(scaledSlope);
            // We multiply both the accent and shade color by a clamped intensity value
            // so that intensities >= 0.5 do not additionally affect the color values
            // while intensity values < 0.5 make the overall color more transparent.
            vec4 accent_color = (1.0 - accent) * u_accent * clamp(intensity * 2.0, 0.0, 1.0);
            float shade = abs(mod((aspect + azimuth) / PI + 0.5, 2.0) - 1.0);
            vec4 shade_color = mix(u_shadow, u_highlight, shade) * sin(scaledSlope) * clamp(intensity * 2.0, 0.0, 1.0);
            vec4 result_color = accent_color * (1.0 - shade_color.a) + shade_color;
            return vec4(result_color.rgb * lighting, result_color.a);
        }
    )GLSL";

    const std::string HillshadeTileRenderer::LIGHTING_SHADER_3D = R"GLSL(
        uniform vec4 u_ambientColor;
        uniform vec4 u_lightColor;
        uniform vec3 u_lightDir;
        uniform vec3 u_viewDir;

        vec4 applyLighting(vec4 color, vec3 normal, float height, bool sideVertex, vec2 uLatrange) {
            if (sideVertex) {
                vec3 dimmedColor = color.rgb * (1.0 - 0.5 / (1.0 + height * height));
                vec3 lighting = max(0.0, dot(normal, u_lightDir)) * u_lightColor.rgb + u_ambientColor.rgb;
                return vec4(dimmedColor.rgb * lighting, color.a);
            } else {
                float lighting = max(0.0, dot(normal, u_viewDir)) * 0.5 + 0.5;
                return vec4(color.rgb * lighting, color.a);
            }
        }
    )GLSL";

}
