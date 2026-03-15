package pt.hitv.core.model.movieInfo

data class Audio(
    val index: Int,
    val codecName: String,
    val codecLongName: String,
    val profile: String,
    val codecType: String,
    val codecTimeBase: String,
    val codecTagString: String,
    val codecTag: String,
    val sampleFmt: String,
    val sampleRate: Int,
    val channels: Int,
    val channelLayout: Double,
    val bitsPerSample: Int,
    val rFrameRate: String,
    val avgFrameRate: String,
    val timeBase: String,
    val startPts: Int,
    val startTime: Double,
    val durationTs: Int,
    val duration: Double,
    val bitRate: Int,
    val maxBitRate: Int,
    val nbFrames: Int,
    val disposition: Disposition,
    val tags: Tags
)
