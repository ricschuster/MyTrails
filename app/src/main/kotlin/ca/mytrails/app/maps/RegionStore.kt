package ca.mytrails.app.maps

import java.io.File

/** One `.pmtiles` file in the regions folder. [bounds] is null when the file cannot be read as PMTiles. */
data class Region(
    val file: File,
    val sizeBytes: Long,
    val bounds: RegionBounds?,
) {
    val name: String get() = file.nameWithoutExtension
    val isReadable: Boolean get() = bounds != null

    /** A `<name>-terrain.pmtiles` file holds Terrarium elevation tiles for hillshade, not trails. */
    val isTerrain: Boolean get() = name.endsWith(RegionStore.TERRAIN_SUFFIX, ignoreCase = true)
}

object RegionStore {
    const val DIRECTORY = "regions"
    const val EXTENSION = "pmtiles"
    const val TERRAIN_SUFFIX = "-terrain"

    /** Lists region files in [dir], sorted by name. A missing folder is an empty list. */
    fun scan(dir: File): List<Region> =
        dir
            .listFiles { f -> f.isFile && f.extension.equals(EXTENSION, ignoreCase = true) }
            .orEmpty()
            .sortedBy { it.name.lowercase() }
            .map { Region(it, it.length(), PmtilesHeader.read(it)) }
}
