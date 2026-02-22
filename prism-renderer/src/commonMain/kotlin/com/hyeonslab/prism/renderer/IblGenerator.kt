@file:Suppress("TooManyFunctions", "LargeClass")

package com.hyeonslab.prism.renderer

import io.ygdrasil.webgpu.ArrayBuffer
import io.ygdrasil.webgpu.Extent3D
import io.ygdrasil.webgpu.GPUDevice
import io.ygdrasil.webgpu.GPUTexture as WGPUTexture
import io.ygdrasil.webgpu.GPUTextureFormat
import io.ygdrasil.webgpu.GPUTextureUsage
import io.ygdrasil.webgpu.Origin3D
import io.ygdrasil.webgpu.TexelCopyBufferLayout
import io.ygdrasil.webgpu.TexelCopyTextureInfo
import io.ygdrasil.webgpu.TextureDescriptor as WGPUTextureDescriptor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * CPU-side IBL (Image-Based Lighting) texture generator for the PBR pipeline.
 *
 * Generates three textures used in [WgpuRenderer]'s environment bind group:
 * - **BRDF LUT**: Split-sum approximation lookup table for specular reflectance
 * - **Irradiance cubemap**: Pre-convolved diffuse environment (Lambertian irradiance)
 * - **Prefiltered environment cubemap**: GGX-importance-sampled specular environment
 *
 * A procedural sky gradient (blue zenith → white horizon → brown nadir) is used as the base
 * environment. All computation is CPU-side; results are uploaded to GPU textures via the device
 * queue.
 *
 * Call [generate] once during initialization and pass the resulting [IblTextures] to
 * [WgpuRenderer.initializeIbl].
 */
object IblGenerator {

  /** GPU textures produced by [generate]. */
  data class IblTextures(
    val brdfLutTexture: WGPUTexture,
    val irradianceTexture: WGPUTexture,
    val prefilteredTexture: WGPUTexture,
    /** Number of mip levels in [prefilteredTexture]. Used to set `maxMipLevel` in env uniforms. */
    val prefilteredMipLevels: Int,
  )

  /**
   * Generates all IBL textures for PBR rendering.
   *
   * @param device GPU device used to create and upload textures.
   * @param skySize Face size of the internal procedural sky (used for CPU sampling only).
   * @param irradianceSize Face size of the output irradiance cubemap.
   * @param prefilteredSize Face size of the output prefiltered environment cubemap.
   * @param prefilteredMipLevels Number of mip levels (roughness steps) in the prefiltered map.
   * @param brdfLutSize Resolution of the BRDF lookup table.
   * @param brdfLutSamples Monte Carlo samples per BRDF LUT texel. Higher = more accurate.
   * @return [IblTextures] with all GPU textures ready for binding.
   */
  @Suppress("LongParameterList")
  fun generate(
    device: GPUDevice,
    skySize: Int = 32,
    irradianceSize: Int = 16,
    prefilteredSize: Int = 32,
    prefilteredMipLevels: Int = 5,
    brdfLutSize: Int = 256,
    brdfLutSamples: Int = 256,
  ): IblTextures {
    // Generate sky gradient data on CPU (no GPU upload needed — used only for IBL convolution)
    val skyFloats = Array(6) { face -> skyFaceFloats(face, skySize) }

    // Irradiance: hemisphere convolution of the sky for diffuse ambient
    val irradiancePixels =
      Array(6) { face -> irradianceFacePixels(face, irradianceSize, skyFloats, skySize) }
    val irradianceTexture =
      uploadCubemap(device, irradianceSize, "Irradiance Map", irradiancePixels)

    // Prefiltered env: GGX-importance-sampled specular environment, one roughness level per mip
    val prefilteredTexture =
      uploadPrefilteredCubemap(device, prefilteredSize, prefilteredMipLevels, skyFloats, skySize)

    // BRDF LUT: split-sum integration table (NdotV × roughness)
    val brdfLutTexture = uploadBrdfLut(device, brdfLutSize, brdfLutSamples)

    return IblTextures(brdfLutTexture, irradianceTexture, prefilteredTexture, prefilteredMipLevels)
  }

  // ===== BRDF LUT =====

  /**
   * Computes the split-sum BRDF integration for one (NdotV, roughness) pair.
   *
   * Returns (scale, bias) where specularReflectance = F0 * scale + bias. Both values are in [0, 1].
   * Uses Hammersley quasi-random sequence with GGX importance sampling.
   */
  internal fun integrateBrdf(
    ndotV: Float,
    roughness: Float,
    numSamples: Int = 256,
  ): Pair<Float, Float> {
    val v = floatArrayOf(sqrt(1f - ndotV * ndotV), 0f, ndotV)
    var scale = 0f
    var bias = 0f

    for (i in 0 until numSamples) {
      val (xi0, xi1) = hammersley(i, numSamples)
      val h = importanceSampleGGX(xi0, xi1, roughness) // H in tangent space (N = Z)
      val vdotH = v[0] * h[0] + v[1] * h[1] + v[2] * h[2]
      val ndotL = (2f * vdotH * h[2] - v[2]).coerceAtLeast(0f) // L.z
      val ndotH = h[2].coerceAtLeast(0f)
      val vdotHClamped = vdotH.coerceAtLeast(0f)
      if (ndotL > 0f) {
        val g = geometrySmith(ndotV, ndotL, roughness)
        val gVis = g * vdotHClamped / (ndotH * ndotV + 1e-6f)
        val fc = (1f - vdotHClamped).pow(5f)
        scale += (1f - fc) * gVis
        bias += fc * gVis
      }
    }
    return Pair(scale / numSamples, bias / numSamples)
  }

  /** Computes the sky gradient color for a given normalized direction (in linear RGB). */
  internal fun skyColor(dx: Float, dy: Float, dz: Float): FloatArray {
    @Suppress("UNUSED_PARAMETER") val ignored = dz // horizontal direction doesn't change sky
    val t = (dy + 1f) * 0.5f // 0 = nadir, 0.5 = horizon, 1 = zenith
    return if (t > 0.5f) {
      val s = (t - 0.5f) * 2f // 0 = horizon, 1 = zenith
      floatArrayOf(lerp(0.85f, 0.18f, s), lerp(0.90f, 0.48f, s), lerp(1.00f, 0.95f, s))
    } else {
      val s = t * 2f // 0 = nadir, 1 = horizon
      floatArrayOf(lerp(0.25f, 0.85f, s), lerp(0.15f, 0.90f, s), lerp(0.05f, 1.00f, s))
    }
  }

  // ===== Private: GPU upload helpers =====

  @Suppress("LongMethod")
  private fun uploadBrdfLut(device: GPUDevice, size: Int, numSamples: Int): WGPUTexture {
    // RG16Float: 2 channels × 2 bytes each = 4 bytes/texel. R=scale, G=bias.
    val pixels = ByteArray(size * size * 4)
    for (y in 0 until size) {
      val roughness = (y.toFloat() + 0.5f) / size
      for (x in 0 until size) {
        val ndotV = (x.toFloat() + 0.5f) / size
        val (scale, bias) = integrateBrdf(ndotV, roughness, numSamples)
        val idx = (y * size + x) * 4
        writeFloat16(pixels, idx, scale.coerceIn(0f, 1f))
        writeFloat16(pixels, idx + 2, bias.coerceIn(0f, 1f))
      }
    }
    val texture =
      device.createTexture(
        WGPUTextureDescriptor(
          size = Extent3D(size.toUInt(), size.toUInt()),
          format = GPUTextureFormat.RG16Float,
          usage = GPUTextureUsage.TextureBinding or GPUTextureUsage.CopyDst,
          label = "BRDF LUT",
        )
      )
    device.queue.writeTexture(
      TexelCopyTextureInfo(texture = texture),
      ArrayBuffer.of(pixels),
      TexelCopyBufferLayout(bytesPerRow = (size * 4).toUInt(), rowsPerImage = size.toUInt()),
      Extent3D(size.toUInt(), size.toUInt()),
    )
    return texture
  }

  private fun uploadCubemap(
    device: GPUDevice,
    size: Int,
    label: String,
    facePixels: Array<ByteArray>,
  ): WGPUTexture {
    // RGBA16Float: 4 channels × 2 bytes each = 8 bytes/texel.
    val texture =
      device.createTexture(
        WGPUTextureDescriptor(
          size = Extent3D(size.toUInt(), size.toUInt(), 6u),
          format = GPUTextureFormat.RGBA16Float,
          usage = GPUTextureUsage.TextureBinding or GPUTextureUsage.CopyDst,
          label = label,
        )
      )
    for (face in 0 until 6) {
      device.queue.writeTexture(
        TexelCopyTextureInfo(texture = texture, origin = Origin3D(z = face.toUInt())),
        ArrayBuffer.of(facePixels[face]),
        TexelCopyBufferLayout(bytesPerRow = (size * 8).toUInt(), rowsPerImage = size.toUInt()),
        Extent3D(size.toUInt(), size.toUInt()),
      )
    }
    return texture
  }

  @Suppress("LongMethod")
  private fun uploadPrefilteredCubemap(
    device: GPUDevice,
    size: Int,
    mipLevels: Int,
    skyFloats: Array<FloatArray>,
    skySize: Int,
  ): WGPUTexture {
    val texture =
      device.createTexture(
        WGPUTextureDescriptor(
          size = Extent3D(size.toUInt(), size.toUInt(), 6u),
          format = GPUTextureFormat.RGBA16Float,
          usage = GPUTextureUsage.TextureBinding or GPUTextureUsage.CopyDst,
          mipLevelCount = mipLevels.toUInt(),
          label = "Prefiltered Env",
        )
      )
    for (mip in 0 until mipLevels) {
      val roughness = if (mipLevels > 1) mip.toFloat() / (mipLevels - 1).toFloat() else 0.5f
      val mipSize = max(1, size shr mip)
      for (face in 0 until 6) {
        val pixels = prefilteredFacePixels(face, mipSize, roughness, skyFloats, skySize)
        device.queue.writeTexture(
          TexelCopyTextureInfo(
            texture = texture,
            mipLevel = mip.toUInt(),
            origin = Origin3D(z = face.toUInt()),
          ),
          ArrayBuffer.of(pixels),
          // RGBA16Float: 4 channels × 2 bytes each = 8 bytes/texel
          TexelCopyBufferLayout(bytesPerRow = (mipSize * 8).toUInt(), rowsPerImage = mipSize.toUInt()),
          Extent3D(mipSize.toUInt(), mipSize.toUInt()),
        )
      }
    }
    return texture
  }

  // ===== Private: Sky gradient =====

  private fun skyFaceFloats(face: Int, size: Int): FloatArray {
    val pixels = FloatArray(size * size * 3)
    for (y in 0 until size) {
      for (x in 0 until size) {
        val u = (x.toFloat() + 0.5f) / size * 2f - 1f
        val v = (y.toFloat() + 0.5f) / size * 2f - 1f
        val dir = faceUvToDir(face, u, v)
        val c = skyColor(dir[0], dir[1], dir[2])
        val idx = (y * size + x) * 3
        pixels[idx] = c[0]
        pixels[idx + 1] = c[1]
        pixels[idx + 2] = c[2]
      }
    }
    return pixels
  }

  // ===== Private: Irradiance =====

  private fun irradianceFacePixels(
    face: Int,
    size: Int,
    skyFloats: Array<FloatArray>,
    skySize: Int,
  ): ByteArray {
    val numSamples = 64
    // RGBA16Float: 4 channels × 2 bytes each = 8 bytes/texel.
    val pixels = ByteArray(size * size * 8)
    for (y in 0 until size) {
      for (x in 0 until size) {
        val u = (x.toFloat() + 0.5f) / size * 2f - 1f
        val v = (y.toFloat() + 0.5f) / size * 2f - 1f
        val n = faceUvToDir(face, u, v)
        val (t, b) = tangentBasis(n)
        var ir = 0f
        var ig = 0f
        var ib = 0f
        for (i in 0 until numSamples) {
          val (xi0, xi1) = hammersley(i, numSamples)
          val phi = 2f * PI.toFloat() * xi0
          val cosTheta = xi1
          val sinTheta = sqrt(1f - cosTheta * cosTheta)
          val lx = cos(phi) * sinTheta
          val ly = sin(phi) * sinTheta
          val lz = cosTheta
          // Transform to world space
          val worldDir =
            floatArrayOf(
              t[0] * lx + b[0] * ly + n[0] * lz,
              t[1] * lx + b[1] * ly + n[1] * lz,
              t[2] * lx + b[2] * ly + n[2] * lz,
            )
          val c = sampleSky(worldDir, skyFloats, skySize)
          ir += c[0] * cosTheta
          ig += c[1] * cosTheta
          ib += c[2] * cosTheta
        }
        val scale = 2f * PI.toFloat() / numSamples
        val idx = (y * size + x) * 8
        writeFloat16(pixels, idx, ir * scale)
        writeFloat16(pixels, idx + 2, ig * scale)
        writeFloat16(pixels, idx + 4, ib * scale)
        writeFloat16(pixels, idx + 6, 1f)
      }
    }
    return pixels
  }

  // ===== Private: Prefiltered environment =====

  @Suppress("LongMethod")
  private fun prefilteredFacePixels(
    face: Int,
    size: Int,
    roughness: Float,
    skyFloats: Array<FloatArray>,
    skySize: Int,
  ): ByteArray {
    val numSamples = 64
    // RGBA16Float: 4 channels × 2 bytes each = 8 bytes/texel.
    val pixels = ByteArray(size * size * 8)
    for (y in 0 until size) {
      for (x in 0 until size) {
        val u = (x.toFloat() + 0.5f) / size * 2f - 1f
        val v = (y.toFloat() + 0.5f) / size * 2f - 1f
        val r = faceUvToDir(face, u, v) // R = V = N for prefilter
        val (t, b) = tangentBasis(r)
        var totalWeight = 0f
        var pr = 0f
        var pg = 0f
        var pb = 0f
        for (i in 0 until numSamples) {
          val (xi0, xi1) = hammersley(i, numSamples)
          val h = importanceSampleGGX(xi0, xi1, roughness)
          // Transform H to world space
          val wh =
            floatArrayOf(
              t[0] * h[0] + b[0] * h[1] + r[0] * h[2],
              t[1] * h[0] + b[1] * h[1] + r[1] * h[2],
              t[2] * h[0] + b[2] * h[1] + r[2] * h[2],
            )
          val vdotH = max(r[0] * wh[0] + r[1] * wh[1] + r[2] * wh[2], 0f)
          val lx = 2f * vdotH * wh[0] - r[0]
          val ly = 2f * vdotH * wh[1] - r[1]
          val lz = 2f * vdotH * wh[2] - r[2]
          val len = sqrt(lx * lx + ly * ly + lz * lz)
          if (len < 1e-6f) continue
          val l = floatArrayOf(lx / len, ly / len, lz / len)
          val ndotL = max(r[0] * l[0] + r[1] * l[1] + r[2] * l[2], 0f)
          if (ndotL > 0f) {
            val c = sampleSky(l, skyFloats, skySize)
            pr += c[0] * ndotL
            pg += c[1] * ndotL
            pb += c[2] * ndotL
            totalWeight += ndotL
          }
        }
        if (totalWeight > 0f) {
          pr /= totalWeight
          pg /= totalWeight
          pb /= totalWeight
        }
        val idx = (y * size + x) * 8
        writeFloat16(pixels, idx, pr)
        writeFloat16(pixels, idx + 2, pg)
        writeFloat16(pixels, idx + 4, pb)
        writeFloat16(pixels, idx + 6, 1f)
      }
    }
    return pixels
  }

  // ===== Private: Math helpers =====

  /**
   * Maps cubemap face + (u, v) in [-1, 1] to a normalized world-space direction.
   *
   * Uses the WebGPU cubemap face convention.
   */
  private fun faceUvToDir(face: Int, u: Float, v: Float): FloatArray {
    val dir =
      when (face) {
        0 -> floatArrayOf(1f, -v, -u) // +X
        1 -> floatArrayOf(-1f, -v, u) // -X
        2 -> floatArrayOf(u, 1f, v) // +Y
        3 -> floatArrayOf(u, -1f, -v) // -Y
        4 -> floatArrayOf(u, -v, 1f) // +Z
        else -> floatArrayOf(-u, -v, -1f) // -Z
      }
    val len = sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2])
    return floatArrayOf(dir[0] / len, dir[1] / len, dir[2] / len)
  }

  /**
   * Builds an orthonormal tangent basis (T, B) for the given normal N.
   *
   * Returns (T, B) as FloatArrays so the caller can transform tangent-space vectors to world space:
   * world = T*x + B*y + N*z.
   */
  private fun tangentBasis(n: FloatArray): Pair<FloatArray, FloatArray> {
    val up = if (abs(n[2]) < 0.999f) floatArrayOf(0f, 0f, 1f) else floatArrayOf(1f, 0f, 0f)
    val tx = n[1] * up[2] - n[2] * up[1]
    val ty = n[2] * up[0] - n[0] * up[2]
    val tz = n[0] * up[1] - n[1] * up[0]
    val tLen = sqrt(tx * tx + ty * ty + tz * tz)
    val t = floatArrayOf(tx / tLen, ty / tLen, tz / tLen)
    val bx = n[1] * t[2] - n[2] * t[1]
    val by = n[2] * t[0] - n[0] * t[2]
    val bz = n[0] * t[1] - n[1] * t[0]
    return Pair(t, floatArrayOf(bx, by, bz))
  }

  /**
   * Nearest-neighbour sample from CPU-side cubemap float data.
   *
   * Returns [r, g, b] at the texel corresponding to the given direction.
   */
  private fun sampleSky(
    dir: FloatArray,
    skyFacePixels: Array<FloatArray>,
    skySize: Int,
  ): FloatArray {
    val ax = abs(dir[0])
    val ay = abs(dir[1])
    val az = abs(dir[2])
    val (face, sc, tc) =
      when {
        ax >= ay && ax >= az ->
          if (dir[0] > 0) Triple(0, -dir[2] / ax, -dir[1] / ax)
          else Triple(1, dir[2] / ax, -dir[1] / ax)
        ay >= ax && ay >= az ->
          if (dir[1] > 0) Triple(2, dir[0] / ay, dir[2] / ay)
          else Triple(3, dir[0] / ay, -dir[2] / ay)
        else ->
          if (dir[2] > 0) Triple(4, dir[0] / az, -dir[1] / az)
          else Triple(5, -dir[0] / az, -dir[1] / az)
      }
    val u = ((sc + 1f) * 0.5f).coerceIn(0f, 1f)
    val v = ((tc + 1f) * 0.5f).coerceIn(0f, 1f)
    val px = (u * (skySize - 1)).toInt().coerceIn(0, skySize - 1)
    val py = (v * (skySize - 1)).toInt().coerceIn(0, skySize - 1)
    val idx = (py * skySize + px) * 3
    val data = skyFacePixels[face]
    return floatArrayOf(data[idx], data[idx + 1], data[idx + 2])
  }

  /** Hammersley quasi-random sequence point [i/n, Van der Corput(i)]. */
  private fun hammersley(i: Int, n: Int): Pair<Float, Float> {
    var bits = i
    bits = (bits shl 16) or (bits ushr 16)
    bits = ((bits and 0x55555555) shl 1) or ((bits ushr 1) and 0x55555555)
    bits = ((bits and 0x33333333) shl 2) or ((bits ushr 2) and 0x33333333)
    bits = ((bits and 0x0F0F0F0F) shl 4) or ((bits ushr 4) and 0x0F0F0F0F)
    bits = ((bits and 0x00FF00FF) shl 8) or ((bits ushr 8) and 0x00FF00FF)
    val r1 = bits.toUInt().toFloat() / 0x100000000UL.toFloat()
    return Pair(i.toFloat() / n, r1)
  }

  /**
   * GGX importance-sampled half-vector in tangent space (N aligned with +Z).
   *
   * @return [H.x, H.y, H.z] — a unit vector biased toward specular reflection for [roughness].
   */
  private fun importanceSampleGGX(xi0: Float, xi1: Float, roughness: Float): FloatArray {
    val a = roughness * roughness
    val phi = 2f * PI.toFloat() * xi0
    val cosTheta = sqrt((1f - xi1) / (1f + (a * a - 1f) * xi1))
    val sinTheta = sqrt(1f - cosTheta * cosTheta)
    return floatArrayOf(cos(phi) * sinTheta, sin(phi) * sinTheta, cosTheta)
  }

  private fun geometrySchlickGGX(ndotX: Float, roughness: Float): Float {
    val r = roughness + 1f
    val k = r * r / 8f
    return ndotX / (ndotX * (1f - k) + k)
  }

  private fun geometrySmith(ndotV: Float, ndotL: Float, roughness: Float): Float =
    geometrySchlickGGX(ndotV, roughness) * geometrySchlickGGX(ndotL, roughness)

  private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

  // ===== Private: Float16 encoding =====

  /**
   * Converts a 32-bit float to its IEEE 754 half-precision (float16) bit pattern.
   *
   * Handles normal values, zero, subnormals (mapped to zero), and overflow (mapped to Inf). Safe
   * for values in [0, 1] and the small positive HDR range used by IBL textures.
   */
  private fun float32ToFloat16Bits(f: Float): Int {
    val bits = f.toBits()
    val sign = (bits ushr 31) and 0x1
    val exp32 = (bits ushr 23) and 0xFF
    val mantissa32 = bits and 0x7FFFFF
    return when {
      exp32 == 0xFF -> (sign shl 15) or 0x7C00 or (if (mantissa32 != 0) 0x200 else 0) // Inf / NaN
      exp32 == 0 -> (sign shl 15) // zero / subnormal → zero
      else -> {
        val exp16 = exp32 - 127 + 15
        when {
          exp16 >= 31 -> (sign shl 15) or 0x7C00 // overflow → Inf
          exp16 <= 0 -> (sign shl 15) // underflow → zero
          else -> (sign shl 15) or (exp16 shl 10) or (mantissa32 ushr 13)
        }
      }
    }
  }

  /** Writes [f] as a little-endian float16 into [output] at byte [offset]. */
  private fun writeFloat16(output: ByteArray, offset: Int, f: Float) {
    val bits = float32ToFloat16Bits(f)
    output[offset] = (bits and 0xFF).toByte()
    output[offset + 1] = ((bits ushr 8) and 0xFF).toByte()
  }
}
