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
}

object RegionStore {
    const val DIRECTORY = "regions"
    const val EXTENSION = "pmtiles"

    /** Lists region files in [dir], sorted by name. A missing folder is an empty list. */
    fun scan(dir: File): List<Region> =
        dir
            .listFiles { f -> f.isFile && f.extension.equals(EXTENSION, ignoreCase = true) }
            .orEmpty()
            .sortedBy { it.name.lowercase() }
            .map { Region(it, it.length(), PmtilesHeader.read(it)) }
}
