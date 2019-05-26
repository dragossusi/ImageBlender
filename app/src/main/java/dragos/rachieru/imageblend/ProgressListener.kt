package dragos.rachieru.imageblend

/**
 * ImageBlender
 *
 * @author Dragos
 * @since 26.05.2019
 */

typealias ProgressListener = (bytesRead: Long, contentLength: Long, done: Boolean) -> Unit