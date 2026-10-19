package ca.mytrails.app.maps

/**
 * Builds the MapLibre style JSON. The base and layer templates are bundled assets; each readable
 * region adds one vector source and one copy of the layers, and each readable terrain file adds a
 * hillshade under them. With no regions only the background shows.
 */
object MapStyle {
    private const val SOURCES_MARKER = "{{sources}}"
    private const val LAYERS_MARKER = "{{layers}}"
    private const val INDEX_MARKER = "{{r}}"
    private const val PATH_MARKER = "{{path}}"
    private const val HILLSHADE_MARKER = "{{hillshade}}"
    private const val DEM_TEMPLATE =
        """"terrain{{r}}": {"type": "raster-dem", "url": "pmtiles://file://{{path}}", "encoding": "terrarium", """ +
            """"tileSize": 512, "maxzoom": 15}"""
    private const val HILLSHADE_LAYER_TEMPLATE =
        """{"id": "hillshade{{r}}", "type": "hillshade", "source": "terrain{{r}}", """ +
            """"paint": {"hillshade-exaggeration": 0.5, "hillshade-shadow-color": "#5a5a5a", """ +
            """"hillshade-highlight-color": "#ffffff", "hillshade-accent-color": "#8a8a8a"}}"""
    private const val SOURCE_TEMPLATE =
        """"region{{r}}": {"type": "vector", "url": "pmtiles://file://{{path}}", "maxzoom": 15, """ +
            """"attribution": "&copy; OpenStreetMap contributors"}"""

    /** [layers] is a JSON array whose ids and sources contain `{{r}}`, filled in per region. */
    fun build(
        base: String,
        layers: String,
        regions: List<Region>,
    ): String {
        val readable = regions.filter { it.isReadable }
        val usable = readable.filterNot { it.isTerrain }
        val terrain = readable.filter { it.isTerrain }
        val vectorSources =
            usable.indices.map { i ->
                SOURCE_TEMPLATE.replace(INDEX_MARKER, i.toString()).replace(PATH_MARKER, escape(usable[i].file.absolutePath))
            }
        val demSources =
            terrain.indices.map { i ->
                DEM_TEMPLATE.replace(INDEX_MARKER, i.toString()).replace(PATH_MARKER, escape(terrain[i].file.absolutePath))
            }
        val sources = (vectorSources + demSources).joinToString(",\n")
        val hillshade =
            terrain.indices.joinToString("") { i -> ",\n" + HILLSHADE_LAYER_TEMPLATE.replace(INDEX_MARKER, i.toString()) }
        val body = layers.trim().removePrefix("[").removeSuffix("]").trim()
        val regionLayers =
            usable.indices.joinToString("") { i -> ",\n" + body.replace(INDEX_MARKER, i.toString()) }
        return base
            .replace(SOURCES_MARKER, sources)
            .replace(HILLSHADE_MARKER, hillshade)
            .replace(LAYERS_MARKER, regionLayers)
    }

    private fun escape(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
}
