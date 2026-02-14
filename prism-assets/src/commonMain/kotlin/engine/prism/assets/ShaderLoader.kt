package engine.prism.assets

import engine.prism.renderer.ShaderModule
import engine.prism.renderer.ShaderSource
import engine.prism.renderer.ShaderStage

class ShaderLoader : AssetLoader<ShaderModule> {
    override val supportedExtensions: List<String> = listOf("wgsl", "vert", "frag")

    override suspend fun load(path: String, data: ByteArray): ShaderModule {
        val source = data.decodeToString()
        // For WGSL shaders, the same source contains both vertex and fragment
        return ShaderModule(
            vertexSource = ShaderSource(source, ShaderStage.VERTEX, "vs_main"),
            fragmentSource = ShaderSource(source, ShaderStage.FRAGMENT, "fs_main"),
            label = path,
        )
    }
}
