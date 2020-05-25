/*
 * Copyright (c) 2016 CartoDB. All rights reserved.
 * Copying and using this code is allowed only according
 * to license terms, as given in https://cartodb.com/terms/
 */

#ifndef _CARTO_HILLSHADETILELAYER_H_
#define _CARTO_HILLSHADETILELAYER_H_

#include "layers/RasterTileLayer.h"
#include "graphics/Color.h"

#include <string>
#include <memory>

namespace carto {
    class HillshadeTileRenderer;

    namespace IlluminationAnchorType {
        enum IlluminationAnchorType {
            MAP,
            VIEWPORT
        };
    }
    /**
     * Specialized onine raster tile layer that draws Terrain RGB tiles.
     */
    class HillshadeTileLayer : public RasterTileLayer {
    public:
        HillshadeTileLayer(const std::shared_ptr<TileDataSource>& dataSource);
        virtual ~HillshadeTileLayer();

        // virtual void setComponents(const std::shared_ptr<CancelableThreadPool>& envelopeThreadPool,
        //                            const std::shared_ptr<CancelableThreadPool>& tileThreadPool,
        //                            const std::weak_ptr<Options>& options,
        //                            const std::weak_ptr<MapRenderer>& mapRenderer,
        //                            const std::weak_ptr<TouchHandler>& touchHandler);


        /**
         * Gets the current illumination direction
         * @return The current illumination direction for this layer in degrees.
         */
        int getIlluminationDirection() const;
        /**
         * Sets the illumination direction for this layer.
         * @param illumination direction in degrees.
         */
        void setIlluminationDirection(int direction);

        /**
         * Gets the current exaggeration
         * @return The current exaggeration .
         */
        float getExaggeration() const;
        /**
         * Sets the exaggeration for this layer.
         * @param exaggeration.
         */
        void setExaggeration(float exaggeration);

        /**
         * Returns the highlight color of this layer.
         * @return The highlight color of this layer..
         */
        Color getHighlightColor() const;
        /**
         * Sets the highlight color of this layer.
         * @param color The new highlight color for the layer. Note: if bitmap is defined, the color is multiplied with the bitmap.
         */
        void setHighlightColor(const Color& color);

        /**
         * Returns the accent color of this layer.
         * @return The accent color of this layer..
         */
        Color getAccentColor() const;
        /**
         * Sets the accent color of this layer.
         * @param color The new accent color for the layer. Note: if bitmap is defined, the color is multiplied with the bitmap.
         */
        void setAccentColor(const Color& color);

        /**
         * Returns the shadow color of this layer.
         * @return The shadow color of this layer..
         */
        Color getShadowColor() const;
        /**
         * Sets the shadow color of this layer.
         * @param color The new shadow color for the layer. Note: if bitmap is defined, the color is multiplied with the bitmap.
         */
        void setShadowColor(const Color& color);
        bool getInspect() const;
        void setInspect(const bool inspect);


    protected:
        virtual void calculateRayIntersectedElements(const cglib::ray3<double>& ray, const ViewState& viewState, std::vector<RayIntersectedElement>& results) const;
        virtual bool processClick(ClickType::ClickType clickType, const RayIntersectedElement& intersectedElement, const ViewState& viewState) const;
        virtual bool onDrawOffscreenFrame(float deltaSeconds, BillboardSorter& billboardSorter, const ViewState& viewState);
        virtual bool onDrawFrame(float deltaSeconds, BillboardSorter& billboardSorter, const ViewState& viewState);

//            std::shared_ptr<HillshadeTileRenderer> _tileRenderer;
    private:
        static const Color DEFAULT_HIGHLIGHT_COLOR;
        static const Color DEFAULT_SHADOW_COLOR;
        static const Color DEFAULT_ACCENT_COLOR;
        int _illuminationDirection;
        float _exaggeration;
        Color _highlightColor;
        Color _shadowColor;
        Color _accentColor;
        bool _inspect;
    };


}

#endif
