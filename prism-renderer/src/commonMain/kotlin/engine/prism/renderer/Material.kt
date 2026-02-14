package engine.prism.renderer

/**
 * Describes the visual properties of a surface for rendering.
 *
 * Follows a PBR metallic-roughness workflow. Optional textures override
 * the corresponding scalar/color parameters when bound.
 *
 * @param shader Shader module used to render this material. Null means use the renderer default.
 * @param baseColor Base albedo color multiplied with [albedoTexture] if present.
 * @param metallic Metalness factor in [0, 1]. 0 = dielectric, 1 = metal.
 * @param roughness Roughness factor in [0, 1]. 0 = mirror, 1 = fully rough.
 * @param albedoTexture Optional albedo/diffuse texture map.
 * @param normalTexture Optional tangent-space normal map.
 * @param label Optional debug label.
 */
data class Material(
    val shader: ShaderModule? = null,
    val baseColor: Color = Color.WHITE,
    val metallic: Float = 0f,
    val roughness: Float = 0.5f,
    val albedoTexture: Texture? = null,
    val normalTexture: Texture? = null,
    val label: String = "",
) {
    /** The render pipeline associated with this material, assigned by the renderer. */
    var pipeline: RenderPipeline? = null
}
