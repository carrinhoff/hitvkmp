package pt.hitv.core.network.model.movieInfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkVideo(
    @SerialName("index") val index: Int = 0,
    @SerialName("codec_name") val codecName: String? = null,
    @SerialName("codec_long_name") val codecLongName: String? = null,
    @SerialName("profile") val profile: String? = null,
    @SerialName("codec_type") val codecType: String? = null,
    @SerialName("codec_time_base") val codecTimeBase: String? = null,
    @SerialName("codec_tag_string") val codecTagString: String? = null,
    @SerialName("codec_tag") val codecTag: String? = null,
    @SerialName("width") val width: Int = 0,
    @SerialName("height") val height: Int = 0,
    @SerialName("coded_width") val codedWidth: Int = 0,
    @SerialName("coded_height") val codedHeight: Int = 0,
    @SerialName("has_b_frames") val hasBFrames: Int = 0,
    @SerialName("sample_aspect_ratio") val sampleAspectRatio: String? = null,
    @SerialName("display_aspect_ratio") val displayAspectRatio: String? = null,
    @SerialName("pix_fmt") val pixFmt: String? = null,
    @SerialName("level") val level: Int = 0,
    @SerialName("chroma_location") val chromaLocation: String? = null,
    @SerialName("refs") val refs: Int = 0,
    @SerialName("is_avc") val isAvc: Boolean = false,
    @SerialName("nal_length_size") val nalLengthSize: Int = 0,
    @SerialName("r_frame_rate") val rFrameRate: String? = null,
    @SerialName("avg_frame_rate") val avgFrameRate: String? = null,
    @SerialName("time_base") val timeBase: String? = null,
    @SerialName("start_pts") val startPts: Int = 0,
    @SerialName("start_time") val startTime: Double = 0.0,
    @SerialName("duration_ts") val durationTs: Int = 0,
    @SerialName("duration") val duration: Double = 0.0,
    @SerialName("bit_rate") val bitRate: Int = 0,
    @SerialName("bits_per_raw_sample") val bitsPerRawSample: Int = 0,
    @SerialName("nb_frames") val nbFrames: Int = 0,
    @SerialName("disposition") val disposition: NetworkDisposition? = null,
    @SerialName("tags") val tags: NetworkTags? = null
)
