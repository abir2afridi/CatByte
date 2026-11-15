package com.example.data.export

import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ExportException

/**
 * Encoder capability queries and the failure classification that decides
 * whether an export is worth retrying with the default encoder.
 */

/**
 * Query MediaCodecList for encoders supporting the given resolution/fps.
 * Kept as a thin wrapper over [CodecCapabilityChecker] so existing callers
 * and tests keep working. Returns ordered mime types (HEVC before AVC).
 */
internal fun querySupportedCodecMimeTypes(width: Int, height: Int, fps: Int): List<String> =
    CodecCapabilityChecker.querySupportedCodecs(width, height, fps).map { it.mime }

/** True when the failure looks like the device encoder refusing our codec. */
@UnstableApi
internal fun isEncoderRelatedFailure(failure: Throwable?): Boolean {
    if (failure == null) return true
    if (failure !is ExportException) return false
    return failure.errorCode == ExportException.ERROR_CODE_ENCODER_INIT_FAILED ||
        failure.errorCode == ExportException.ERROR_CODE_ENCODING_FAILED ||
        failure.errorCode == ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED
}
