data class PlaylistMetadata(val programId: String, val bandwidth: String, val resolution: String) {
    override fun toString(): String = "$resolution-${bandwidth.toIntOrNull()?.let {it / 1024}}"
}

class MasterPlaylist(val version: Number, val metadataMap: Map<PlaylistMetadata, String>) : Playlist() {
    override fun synthesize(): String {
        return "wip"
    }
}