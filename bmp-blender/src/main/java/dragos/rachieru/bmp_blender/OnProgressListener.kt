package dragos.rachieru.bmp_blender

interface OnProgressListener {
    fun onProgress(progress: Int, min: Int, max: Int)
}