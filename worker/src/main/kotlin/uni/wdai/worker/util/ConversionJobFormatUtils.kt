package uni.wdai.worker.util

import uni.wdai.model.document.ConversionJob.Format

val Format.ffmpegName
    get() = when(this) {
        Format.Mp4 -> "mp4"
        Format.Mp3 -> "mp3"
        Format.Aac -> "aac"
    }