data class PlaylistMetadata(val programId: String, val bandwidth: String) {
    override fun toString(): String = "${bandwidth.toIntOrNull()?.let {it / 1024}}k"
}

class MasterPlaylist(val version: Number, val metadataMap: Map<PlaylistMetadata, String>) : Playlist() {
    override fun synthesize(): String {
        return "wip"
    }
}