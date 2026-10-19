package ca.mytrails.app.maps

/**
 * Builds the MapLibre style JSON. The base and layer templates are bundled assets; each readable
 * region adds one vector source and one copy of the layers. With no regions only the background shows.
 */
object MapStyle {
    private const val SOURCES_MARKER = "{{sources}}"
    private const val LAYERS_MARKER = "{{layers}}"
    private const val INDEX_MARKER = "{{r}}"
    private const val PATH_MARKER = "{{path}}"
    private const val SOURCE_TEMPLATE =
        """"region{{r}}": {"type": "vector", "url": "pmtiles://file://{{path}}", "maxzoom": 15, """ +
            """"attribution": "&copy; OpenStreetMap contributors"}"""

    /** [layers] is a JSON array whose ids and sources contain `{{r}}`, filled in per region. */
    fun build(
        base: String,
        layers: String,
        regions: List<Region>,
    ): String {
        val usable = regions.filter { it.isReadable }
        val sources =
            usable.indices.joinToString(",\n") { i ->
                SOURCE_TEMPLATE.replace(INDEX_MARKER, i.toString()).replace(PATH_MARKER, escape(usable[i].file.absolutePath))
            }
        val body = layers.trim().removePrefix("[").removeSuffix("]").trim()
        val regionLayers =
            usable.indices.joinToString("") { i -> ",\n" + body.replace(INDEX_MARKER, i.toString()) }
        return base.replace(SOURCES_MARKER, sources).replace(LAYERS_MARKER, regionLayers)
    }

    private fun escape(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
}
