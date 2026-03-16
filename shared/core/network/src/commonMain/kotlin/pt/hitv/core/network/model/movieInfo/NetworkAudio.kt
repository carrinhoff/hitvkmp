package pt.hitv.core.network.model.movieInfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkAudio(
    @SerialName("index") val index: Int = 0,
    @SerialName("codec_name") val codecName: String? = null,
    @SerialName("codec_long_name") val codecLongName: String? = null,
    @SerialName("profile") val profile: String? = null,
    @SerialName("codec_type") val codecType: String? = null,
    @SerialName("codec_time_base") val codecTimeBase: String? = null,
    @SerialName("codec_tag_string") val codecTagString: String? = null,
    @SerialName("codec_tag") val codecTag: String? = null,
    @SerialName("sample_fmt") val sampleFmt: String? = null,
    @SerialName("sample_rate") val sampleRate: Int = 0,
    @SerialName("channels") val channels: Int = 0,
    @SerialName("channel_layout") val channelLayout: Double = 0.0,
    @SerialName("bits_per_sample") val bitsPerSample: Int = 0,
    @SerialName("r_frame_rate") val rFrameRate: String? = null,
    @SerialName("avg_frame_rate") val avgFrameRate: String? = null,
    @SerialName("time_base") val timeBase: String? = null,
    @SerialName("start_pts") val startPts: Int = 0,
    @SerialName("start_time") val startTime: Double = 0.0,
    @SerialName("duration_ts") val durationTs: Int = 0,
    @SerialName("duration") val duration: Double = 0.0,
    @SerialName("bit_rate") val bitRate: Int = 0,
    @SerialName("max_bit_rate") val maxBitRate: Int = 0,
    @SerialName("nb_frames") val nbFrames: Int = 0,
    @SerialName("disposition") val disposition: NetworkDisposition? = null,
    @SerialName("tags") val tags: NetworkTags? = null
)
