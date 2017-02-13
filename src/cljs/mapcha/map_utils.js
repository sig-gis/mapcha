/*****************************************************************************
***
*** Create the map_utils object to act as a namespace for this file
***
*****************************************************************************/

var map_utils = {};

/*****************************************************************************
***
*** Lon/Lat Reprojection
***
*** Default map projection for OpenLayers is "Web Mercator"
*** (EPSG:3857), which is also the default for OpenStreetMap, Google
*** Maps, MapQuest, and Bing Maps.
***
*****************************************************************************/

map_utils.reproject_to_map = function (longitude, latitude) {
    return ol.proj.transform([Number(longitude), Number(latitude)],
                             "EPSG:4326",
                             "EPSG:3857");
};

map_utils.reproject_from_map = function (x, y) {
    return ol.proj.transform([Number(x), Number(y)],
                             "EPSG:3857",
                             "EPSG:4326");
};

map_utils.get_full_extent = function () {
    var llxy = map_utils.reproject_to_map(-180.0, -89.999999);
    var urxy = map_utils.reproject_to_map(180.0, 90.0);
    return [llxy[0], llxy[1], urxy[0], urxy[1]];
};

/*****************************************************************************
***
*** Create the default OpenLayers Map Object used on all pages
***
*****************************************************************************/

// This reference will hold the current page's OpenLayers map object
map_utils.map_ref = null;

map_utils.digital_globe_base_map = function (map_config) {
    var digital_globe_access_token     = "pk.eyJ1IjoiZGlnaXRhbGdsb2JlIiwiYS" +
                                         "I6ImNpcTJ3ZTlyZTAwOWNuam00ZWU3aTk" +
                                         "xdWIifQ.9OFrmevVe0YB2dJokKhhdA";
    var recent_imagery_url             = "digitalglobe.nal0g75k";
    var recent_imagery_and_streets_url = "digitalglobe.nal0mpda";
    var bing_maps_access_token         = "AlQPbThspGcsiCnczC-2QVOYU9u_PrteL" +
                                         "w6dxNQls99dmLXcr9-qWCM5J4Y2G-pS";
    var sig_geoserver_url              = "http://pyrite.sig-gis.com/geoserver/wms";

    // Declare each of the layer sources that will be shown in the map
    var source1 = new ol.source.XYZ({"url": "http://api.tiles.mapbox.com/v4/" +
                                            recent_imagery_url +
                                            "/{z}/{x}/{y}.png?access_token=" +
                                            digital_globe_access_token,
                                     "attribution": "© DigitalGlobe, Inc"});

    var source2 = new ol.source.XYZ({"url": "http://api.tiles.mapbox.com/v4/" +
                                            recent_imagery_and_streets_url +
                                            "/{z}/{x}/{y}.png?access_token=" +
                                            digital_globe_access_token,
                                     "attribution": "© DigitalGlobe, Inc"});

    var source3 = new ol.source.BingMaps({"key": bing_maps_access_token,
                                          "imagerySet": "Aerial",
                                          "maxZoom": 19});

    var source4 = new ol.source.BingMaps({"key": bing_maps_access_token,
                                          "imagerySet": "AerialWithLabels",
                                          "maxZoom": 19});

    var source5 = new ol.source.TileWMS({"url": sig_geoserver_url,
                                         "params": {"LAYERS": "servir:yr2002",
                                                    "TILED": true},
                                         "serverType": "geoserver"});

    // Wrap each source in a layer object
    var layer1 = new ol.layer.Tile({"title": "DigitalGlobeRecentImagery",
                                    "visible": false,
                                    "source": source1});

    var layer2 = new ol.layer.Tile({"title": "DigitalGlobeRecentImagery+Streets",
                                    "visible": true,
                                    "source": source2});

    var layer3 = new ol.layer.Tile({"title": "BingAerial",
                                    "visible": false,
                                    "source": source3});

    var layer4 = new ol.layer.Tile({"title": "BingAerialWithLabels",
                                    "visible": false,
                                    "source": source4});

    var layer5 = new ol.layer.Tile({"title": "NASASERVIRChipset2002",
                                    "visible": false,
                                    "extent": [10298030, 898184, 12094575, 2697289],
                                    "source": source5});

    // Add a scale line to the default map controls
    var controls = ol.control.defaults().extend([new ol.control.ScaleLine()]);

    // Create the map view using the passed in center_coords and zoom_level
    var view = new ol.View({"projection": "EPSG:3857",
                            "center": ol.proj.fromLonLat(map_config.center_coords),
                            "extent": map_utils.get_full_extent(),
                            "zoom": map_config.zoom_level});

    // Create the new OpenLayers map object
    var openlayers_map = new ol.Map({"target": map_config.div_name,
                                     "layers": [layer1, layer2, layer3,
                                                layer4, layer5],
                                     "controls": controls,
                                     "view": view});

    // Store the new OpenLayers map object in map_utils.map_ref and return it
    map_utils.map_ref = openlayers_map;
    return openlayers_map;
};

map_utils.current_imagery = "DigitalGlobeRecentImagery+Streets";

map_utils.set_current_imagery = function (new_imagery) {
    for (layer in map_utils.map_ref.getLayers().getArray()) {
        var title = layer.get("title");
        if (title == map_utils.current_imagery) {
            layer.setVisible(false);
        }
        if (title == new_imagery) {
            layer.setVisible(true);
        }
    }
    map_utils.current_imagery = new_imagery;
    return new_imagery;
};

map_utils.zoom_map_to_layer = function (layer) {
    var view = map_utils.map_ref.getView();
    var size = map_utils.map_ref.getSize();
    var extent = layer.getSource().getExtent();
    return view.fit(extent, size);
};

map_utils.styles =
    {"icon": new ol.style.Style(
        {"image": new ol.style.Icon({"src": "/favicon.ico"})}),

     "red_point": new ol.style.Style(
         {"image": new ol.style.Circle({"radius": 5,
                                        "fill": null,
                                        "stroke": new ol.style.Stroke(
                                            {"color": "#8b2323",
                                             "width": 2})})}),

     "blue_point": new ol.style.Style(
         {"image": new ol.style.Circle({"radius": 5,
                                        "fill": null,
                                        "stroke": new ol.style.Stroke(
                                            {"color": "#23238b",
                                             "width": 2})})}),

     "polygon": new ol.style.Style(
         {"fill": null,
          "stroke": new ol.style.Stroke(
              {"color": "#8b2323",
               "width": 3})})};

map_utils.current_boundary = null;

map_utils.draw_polygon = function (polygon) {
    var format = new ol.format.GeoJSON();
    var geometry = format.readGeometry(polygon).transform("EPSG:4326", "EPSG:3857");
    var feature = new ol.Feature({"geometry": geometry});
    var vector_source = new ol.source.Vector({"features": [feature]});
    var style = map_utils.styles["polygon"];
    var vector_layer = new ol.layer.Vector({"source": vector_source,
                                            "style": style});
    if (map_utils.current_boundary != null) {
        map_utils.map_ref.removeLayer(map_utils.current_boundary);
    }
    map_utils.current_boundary = vector_layer;
    map_utils.map_ref.addLayer(vector_layer);
    map_utils.zoom_map_to_layer(vector_layer);
    return map_utils.map_ref;
};

map_utils.current_buffer = null;

map_utils.remove_plot_layer = function () {
    if (map_utils.current_buffer != null) {
        map_utils.map_ref.removeLayer(map_utils.current_buffer);
        map_utils.current_buffer = null;
    }
    return null;
};

map_utils.draw_buffer = function (center, radius) {
    var format = new ol.format.GeoJSON();
    var geometry = format.readGeometry(center).transform("EPSG:4326", "EPSG:3857");
    var buffer = new ol.geom.Circle(geometry.getCoordinates(), radius);
    var feature = new ol.Feature({"geometry": buffer});
    var vector_source = new ol.source.Vector({"features": [feature]});
    var style = map_utils.styles["polygon"];
    var vector_layer = new ol.layer.Vector({"source": vector_source,
                                            "style": style});
    map_utils.remove_plot_layer();
    map_utils.current_buffer = vector_layer;
    map_utils.map_ref.addLayer(vector_layer);
    map_utils.zoom_map_to_layer(vector_layer);
    return map_utils.map_ref;
};
