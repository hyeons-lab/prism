// Prism Engine — WebGPU glTF DamagedHelmet Demo
// Loads DamagedHelmet.glb, parses glTF 2.0, renders with Cook-Torrance PBR
// and all five material textures (base color, metallic-roughness, normal,
// occlusion, emissive) via normal-mapped TBN shading.
//
// Model: DamagedHelmet — © Khronos Group, Inc. CC BY 4.0.
// https://github.com/KhronosGroup/glTF-Sample-Assets

// ── Math helpers ──────────────────────────────────────────────────────────────

function perspective(fovY, aspect, near, far) {
  const f = 1.0 / Math.tan(fovY * 0.5);
  const nf = 1 / (near - far);
  // prettier-ignore
  return new Float32Array([
    f / aspect, 0, 0, 0,
    0, f, 0, 0,
    0, 0, (far + near) * nf, -1,
    0, 0, 2 * far * near * nf, 0,
  ]);
}

function lookAt(eye, center, up) {
  const zx = eye[0]-center[0], zy = eye[1]-center[1], zz = eye[2]-center[2];
  let l = 1 / Math.hypot(zx, zy, zz);
  const fz = [zx*l, zy*l, zz*l];
  const sx = up[1]*fz[2]-up[2]*fz[1], sy = up[2]*fz[0]-up[0]*fz[2], sz = up[0]*fz[1]-up[1]*fz[0];
  l = 1 / Math.hypot(sx, sy, sz);
  const fs = [sx*l, sy*l, sz*l];
  const ux = fz[1]*fs[2]-fz[2]*fs[1], uy = fz[2]*fs[0]-fz[0]*fs[2], uz = fz[0]*fs[1]-fz[1]*fs[0];
  // prettier-ignore
  return new Float32Array([
    fs[0], ux, fz[0], 0, fs[1], uy, fz[1], 0, fs[2], uz, fz[2], 0,
    -(fs[0]*eye[0]+fs[1]*eye[1]+fs[2]*eye[2]),
    -(ux*eye[0]+uy*eye[1]+uz*eye[2]),
    -(fz[0]*eye[0]+fz[1]*eye[1]+fz[2]*eye[2]), 1,
  ]);
}

function mul4(a, b) {
  const out = new Float32Array(16);
  for (let i = 0; i < 4; i++)
    for (let j = 0; j < 4; j++)
      out[j*4+i] = a[i]*b[j*4]+a[4+i]*b[j*4+1]+a[8+i]*b[j*4+2]+a[12+i]*b[j*4+3];
  return out;
}

// Convert a glTF quaternion [x,y,z,w] to a column-major mat4x4 for WebGPU.
function quatToMat4([x, y, z, w]) {
  // prettier-ignore
  return new Float32Array([
    1-2*(y*y+z*z), 2*(x*y+w*z),   2*(x*z-w*y),   0,  // col 0
    2*(x*y-w*z),   1-2*(x*x+z*z), 2*(y*z+w*x),   0,  // col 1
    2*(x*z+w*y),   2*(y*z-w*x),   1-2*(x*x+y*y), 0,  // col 2
    0,             0,             0,             1,  // col 3
  ]);
}

// Read TRS transform from the glTF scene's root node and return a model matrix.
// Falls back to identity if no transform is defined.
function getNodeModelMatrix(json) {
  const scene = json.scenes?.[json.scene ?? 0];
  const node  = scene?.nodes?.length ? json.nodes[scene.nodes[0]] : null;
  if (!node) { const m = new Float32Array(16); m[0]=m[5]=m[10]=m[15]=1; return m; }
  if (node.matrix) return new Float32Array(node.matrix);
  let m = new Float32Array(16); m[0]=m[5]=m[10]=m[15]=1;
  // glTF TRS order: T * R * S  (applied right-to-left)
  if (node.scale) {
    const s = node.scale;
    const sm = new Float32Array(16); sm[0]=s[0]; sm[5]=s[1]; sm[10]=s[2]; sm[15]=1;
    m = mul4(m, sm);
  }
  if (node.rotation) m = mul4(m, quatToMat4(node.rotation));
  if (node.translation) {
    const t = node.translation;
    const tm = new Float32Array(16); tm[0]=tm[5]=tm[10]=tm[15]=1;
    tm[12]=t[0]; tm[13]=t[1]; tm[14]=t[2];
    m = mul4(tm, m);
  }
  return m;
}

// ── GLB parsing ───────────────────────────────────────────────────────────────

async function loadGlb(url) {
  const buf = await (await fetch(url)).arrayBuffer();
  const dv  = new DataView(buf);
  if (dv.getUint32(0, true) !== 0x46546C67) throw new Error("Not a GLB file");
  const jsonLen = dv.getUint32(12, true);
  const json    = JSON.parse(new TextDecoder().decode(new Uint8Array(buf, 20, jsonLen)));
  const binStart = 20 + jsonLen;
  const binLen   = dv.getUint32(binStart, true);
  const bin      = buf.slice(binStart + 8, binStart + 8 + binLen);
  return { json, bin };
}

function accessorData(json, bin, index) {
  if (index == null) return null;
  const acc = json.accessors[index];
  const bv  = json.bufferViews[acc.bufferView];
  const off = (bv.byteOffset ?? 0) + (acc.byteOffset ?? 0);
  const nc  = { SCALAR: 1, VEC2: 2, VEC3: 3, VEC4: 4, MAT4: 16 }[acc.type];
  const T   = {
    5121: Uint8Array, 5123: Uint16Array, 5125: Uint32Array,
    5122: Int16Array, 5126: Float32Array,
  }[acc.componentType];
  return new T(bin, off, acc.count * nc);
}

// ── Texture loading ───────────────────────────────────────────────────────────

async function loadGpuTex(device, json, bin, texIndex, srgb) {
  if (texIndex == null) return null;
  const src   = json.textures[texIndex].source;
  const img   = json.images[src];
  const bv    = json.bufferViews[img.bufferView];
  const bytes = new Uint8Array(bin, bv.byteOffset ?? 0, bv.byteLength);
  const blob  = new Blob([bytes], { type: img.mimeType ?? "image/jpeg" });
  const bmp   = await createImageBitmap(blob);
  const fmt   = srgb ? "rgba8unorm-srgb" : "rgba8unorm";
  const gpuTex = device.createTexture({
    size: [bmp.width, bmp.height],
    format: fmt,
    usage: GPUTextureUsage.TEXTURE_BINDING | GPUTextureUsage.COPY_DST | GPUTextureUsage.RENDER_ATTACHMENT,
  });
  device.queue.copyExternalImageToTexture({ source: bmp }, { texture: gpuTex }, [bmp.width, bmp.height]);
  bmp.close();
  return gpuTex;
}

function fallbackTex(device, fmt, r, g, b, a) {
  const tex = device.createTexture({
    size: [1, 1], format: fmt,
    usage: GPUTextureUsage.TEXTURE_BINDING | GPUTextureUsage.COPY_DST,
  });
  device.queue.writeTexture({ texture: tex }, new Uint8Array([r, g, b, a]), { bytesPerRow: 4 }, [1, 1]);
  return tex;
}

// ── Vertex interleaving ───────────────────────────────────────────────────────
// stride = 48 bytes: pos(12) + normal(12) + uv(8) + tangent(16)

function buildVerts(json, bin, prim) {
  const pos  = accessorData(json, bin, prim.attributes['POSITION']);
  const norm = accessorData(json, bin, prim.attributes['NORMAL']);
  const uv   = accessorData(json, bin, prim.attributes['TEXCOORD_0']);
  const tan  = accessorData(json, bin, prim.attributes['TANGENT']);
  const n    = pos.length / 3;
  const out  = new Float32Array(n * 12);
  for (let i = 0; i < n; i++) {
    const b = i * 12;
    out[b]    = pos[i*3];     out[b+1]  = pos[i*3+1];   out[b+2]  = pos[i*3+2];
    out[b+3]  = norm?norm[i*3]:0; out[b+4] = norm?norm[i*3+1]:1; out[b+5] = norm?norm[i*3+2]:0;
    out[b+6]  = uv?uv[i*2]:0; out[b+7]  = uv?uv[i*2+1]:0;
    out[b+8]  = tan?tan[i*4]:1; out[b+9] = tan?tan[i*4+1]:0;
    out[b+10] = tan?tan[i*4+2]:0; out[b+11] = tan?tan[i*4+3]:1;
  }
  return out;
}

// ── WGSL shader ───────────────────────────────────────────────────────────────

const WGSL = /* wgsl */`
const PI: f32 = 3.14159265358979;

// Group 0 — per-frame uniforms (144 B, mirrors demo.js layout)
struct Frame {
    viewProj  : mat4x4f,           // offset   0, 64B
    cameraPos : vec3f, _p0 : f32,  // offset  64, 16B
    light0Pos : vec3f, _p1 : f32,  // offset  80, 16B
    light0Col : vec3f, light0Int : f32,  // offset 96, 16B
    light1Pos : vec3f, _p2 : f32,  // offset 112, 16B
    light1Col : vec3f, light1Int : f32,  // offset 128, 16B
};
@group(0) @binding(0) var<uniform> frame : Frame;

// Group 1 — per-object transform (64 B)
struct Object { model : mat4x4f };
@group(1) @binding(0) var<uniform> obj : Object;

// Group 2 — material textures + sampler
@group(2) @binding(0) var smp          : sampler;
@group(2) @binding(1) var baseColorTex : texture_2d<f32>;   // sRGB → linear
@group(2) @binding(2) var mrTex        : texture_2d<f32>;   // G=rough, B=metal
@group(2) @binding(3) var normalTex    : texture_2d<f32>;   // tangent-space normal
@group(2) @binding(4) var occlusionTex : texture_2d<f32>;   // R=occlusion
@group(2) @binding(5) var emissiveTex  : texture_2d<f32>;   // sRGB → linear

struct VsOut {
    @builtin(position) clip  : vec4f,
    @location(0)       wPos  : vec3f,
    @location(1)       wNorm : vec3f,
    @location(2)       uv    : vec2f,
    @location(3)       wTan  : vec3f,
    @location(4)       wBit  : vec3f,
};

@vertex fn vs(
    @location(0) pos    : vec3f,
    @location(1) normal : vec3f,
    @location(2) uv     : vec2f,
    @location(3) tan    : vec4f,   // .w = handedness (+1 or -1)
) -> VsOut {
    let wPos = (obj.model * vec4f(pos, 1.0)).xyz;
    let wN   = normalize((obj.model * vec4f(normal, 0.0)).xyz);
    let wT   = normalize((obj.model * vec4f(tan.xyz, 0.0)).xyz);
    let wB   = cross(wN, wT) * tan.w;
    var out: VsOut;
    out.clip  = frame.viewProj * vec4f(wPos, 1.0);
    out.wPos  = wPos;
    out.wNorm = wN;
    out.uv    = uv;
    out.wTan  = wT;
    out.wBit  = wB;
    return out;
}

// ── Cook-Torrance PBR helpers ────────────────────────────────────────────────

fn D_GGX(NdotH: f32, a: f32) -> f32 {
    let a2 = a * a; let d = NdotH * NdotH * (a2 - 1.0) + 1.0;
    return a2 / max(PI * d * d, 1e-7);
}
fn G_Schlick(n: f32, k: f32) -> f32 { return n / max(n * (1.0 - k) + k, 1e-7); }
fn G_Smith(NdotV: f32, NdotL: f32, rough: f32) -> f32 {
    let k = (rough + 1.0) * (rough + 1.0) / 8.0;
    return G_Schlick(max(NdotV, 0.001), k) * G_Schlick(max(NdotL, 0.001), k);
}
fn F_Schlick(cos: f32, F0: vec3f) -> vec3f {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cos, 0.0, 1.0), 5.0);
}

fn brdf(
    N: vec3f, V: vec3f, L: vec3f,
    albedo: vec3f, metallic: f32, rough: f32,
    lCol: vec3f, lInt: f32,
) -> vec3f {
    let H     = normalize(V + L);
    let NdotL = max(dot(N, L), 0.0);
    let NdotV = max(dot(N, V), 0.001);
    let NdotH = max(dot(N, H), 0.0);
    let HdotV = max(dot(H, V), 0.0);
    let a  = rough * rough;
    let F0 = mix(vec3f(0.04), albedo, metallic);
    let D  = D_GGX(NdotH, a);
    let G  = G_Smith(NdotV, NdotL, rough);
    let F  = F_Schlick(HdotV, F0);
    let spec = D * G * F / max(4.0 * NdotV * NdotL, 1e-7);
    let kD   = (1.0 - F) * (1.0 - metallic);
    return (kD * albedo / PI + spec) * lCol * lInt * NdotL;
}

@fragment fn fs(in: VsOut) -> @location(0) vec4f {
    // Base color (rgba8unorm-srgb → sampler auto-linearizes)
    let bc = textureSample(baseColorTex, smp, in.uv);
    if (bc.a < 0.5) { discard; }   // alpha-mask cutout
    let albedo = bc.rgb;

    // Metallic-roughness (linear data: G=roughness, B=metallic)
    let mr       = textureSample(mrTex, smp, in.uv);
    let metallic = mr.b;
    let rough    = max(mr.g, 0.04);

    // Normal mapping — tangent-space normal → world space via TBN
    let ns  = textureSample(normalTex, smp, in.uv).rgb * 2.0 - 1.0;
    let TBN = mat3x3f(in.wTan, in.wBit, in.wNorm);
    let N   = normalize(TBN * ns);

    let V = normalize(frame.cameraPos - in.wPos);

    let L0  = normalize(frame.light0Pos - in.wPos);
    let d0  = length(frame.light0Pos - in.wPos);
    let att0 = 1.0 / max(d0 * d0 * 0.04, 1.0);

    let L1   = normalize(frame.light1Pos - in.wPos);
    let d1   = length(frame.light1Pos - in.wPos);
    let att1 = 1.0 / max(d1 * d1 * 0.04, 1.0);

    var Lo = brdf(N, V, L0, albedo, metallic, rough, frame.light0Col, frame.light0Int * att0);
    Lo    += brdf(N, V, L1, albedo, metallic, rough, frame.light1Col, frame.light1Int * att1);

    // Ambient occlusion + indirect ambient approximation
    let ao      = textureSample(occlusionTex, smp, in.uv).r;
    let ambient = albedo * 0.08 * ao;

    // Emissive (rgba8unorm-srgb → sampler auto-linearizes)
    let emissive = textureSample(emissiveTex, smp, in.uv).rgb;

    var c = Lo + ambient + emissive;

    // Khronos PBR Neutral tone mapping (same as Prism's WgpuRenderer)
    let sc  = 0.8 - 0.04;
    let pk  = max(max(c.r, c.g), c.b);
    if (pk > sc) {
        let v = 1.0 - sc;
        c *= (1.0 - v * v / (pk - sc + v)) / pk;
    }
    let pk2 = max(max(c.r, c.g), c.b);
    if (pk2 > 0.15) {
        let t = (pk2 - 0.15) / 0.85;
        c = mix(c, vec3f(pk2), t * t);
    }

    // sRGB gamma encode for display
    c = pow(max(c, vec3f(0.0)), vec3f(1.0 / 2.2));
    return vec4f(c, 1.0);
}
`;

// ── Demo entry point ──────────────────────────────────────────────────────────

export async function initGltfDemo(canvas) {
  if (!navigator.gpu) return false;
  const adapter = await navigator.gpu.requestAdapter();
  if (!adapter) return false;
  const device  = await adapter.requestDevice();
  const context = canvas.getContext("webgpu");
  const format  = navigator.gpu.getPreferredCanvasFormat();
  context.configure({ device, format, alphaMode: "opaque" });

  // ── Load GLB ─────────────────────────────────────────────────────────────────

  const { json, bin } = await loadGlb("./DamagedHelmet.glb");
  const prim = json.meshes[0].primitives[0];
  const mat  = json.materials[0];
  const pbr  = mat.pbrMetallicRoughness ?? {};

  // ── Vertex buffer ─────────────────────────────────────────────────────────────

  const verts   = buildVerts(json, bin, prim);
  const vertBuf = device.createBuffer({
    size: verts.byteLength,
    usage: GPUBufferUsage.VERTEX | GPUBufferUsage.COPY_DST,
  });
  device.queue.writeBuffer(vertBuf, 0, verts);

  // ── Index buffer ──────────────────────────────────────────────────────────────

  const indices  = accessorData(json, bin, prim.indices);
  const idxBuf   = device.createBuffer({
    size: Math.ceil(indices.byteLength / 4) * 4,
    usage: GPUBufferUsage.INDEX | GPUBufferUsage.COPY_DST,
  });
  device.queue.writeBuffer(idxBuf, 0, indices);
  const idxCount  = indices.length;
  const idxFormat = indices instanceof Uint32Array ? "uint32" : "uint16";

  // ── Textures ──────────────────────────────────────────────────────────────────

  const [baseColorTex, mrTex, normTex, occTex, emitTex] = await Promise.all([
    loadGpuTex(device, json, bin, pbr.baseColorTexture?.index,          true),
    loadGpuTex(device, json, bin, pbr.metallicRoughnessTexture?.index,  false),
    loadGpuTex(device, json, bin, mat.normalTexture?.index,             false),
    loadGpuTex(device, json, bin, mat.occlusionTexture?.index,          false),
    loadGpuTex(device, json, bin, mat.emissiveTexture?.index,           true),
  ]);

  const texBC   = baseColorTex ?? fallbackTex(device, "rgba8unorm-srgb", 200, 200, 200, 255);
  const texMR   = mrTex        ?? fallbackTex(device, "rgba8unorm",       0, 128,   0, 255);
  const texNorm = normTex      ?? fallbackTex(device, "rgba8unorm",     128, 128, 255, 255);
  const texOcc  = occTex       ?? fallbackTex(device, "rgba8unorm",     255, 255, 255, 255);
  const texEmit = emitTex      ?? fallbackTex(device, "rgba8unorm-srgb",  0,   0,   0, 255);

  // ── Uniform buffers ───────────────────────────────────────────────────────────

  const FRAME_SIZE = 144;
  const frameBuf  = device.createBuffer({
    size: FRAME_SIZE,
    usage: GPUBufferUsage.UNIFORM | GPUBufferUsage.COPY_DST,
  });
  const frameData = new Float32Array(FRAME_SIZE / 4);

  // Two point lights: warm key + cool fill
  const L0 = { pos: [3.0, 4.0, 4.0], col: [1.0, 0.90, 0.78], int: 12.0 };
  const L1 = { pos: [-4.0, -1.5, 3.5], col: [0.45, 0.58, 1.0], int: 8.0 };

  const objBuf = device.createBuffer({
    size: 64,
    usage: GPUBufferUsage.UNIFORM | GPUBufferUsage.COPY_DST,
  });
  // Apply the scene node's transform (DamagedHelmet has a 180° Y rotation).
  device.queue.writeBuffer(objBuf, 0, getNodeModelMatrix(json));

  // ── Bind group layouts ────────────────────────────────────────────────────────

  const frameBGL = device.createBindGroupLayout({ entries: [{
    binding: 0,
    visibility: GPUShaderStage.VERTEX | GPUShaderStage.FRAGMENT,
    buffer: { type: "uniform" },
  }]});
  const objBGL = device.createBindGroupLayout({ entries: [{
    binding: 0,
    visibility: GPUShaderStage.VERTEX,
    buffer: { type: "uniform" },
  }]});
  const texBGL = device.createBindGroupLayout({ entries: [
    { binding: 0, visibility: GPUShaderStage.FRAGMENT, sampler: { type: "filtering" } },
    { binding: 1, visibility: GPUShaderStage.FRAGMENT, texture: { sampleType: "float" } },
    { binding: 2, visibility: GPUShaderStage.FRAGMENT, texture: { sampleType: "float" } },
    { binding: 3, visibility: GPUShaderStage.FRAGMENT, texture: { sampleType: "float" } },
    { binding: 4, visibility: GPUShaderStage.FRAGMENT, texture: { sampleType: "float" } },
    { binding: 5, visibility: GPUShaderStage.FRAGMENT, texture: { sampleType: "float" } },
  ]});

  // ── Pipeline ──────────────────────────────────────────────────────────────────

  const shader = device.createShaderModule({ code: WGSL });
  const pipeline = device.createRenderPipeline({
    layout: device.createPipelineLayout({ bindGroupLayouts: [frameBGL, objBGL, texBGL] }),
    vertex: {
      module: shader, entryPoint: "vs",
      buffers: [{
        arrayStride: 48,
        attributes: [
          { shaderLocation: 0, offset:  0, format: "float32x3" }, // pos
          { shaderLocation: 1, offset: 12, format: "float32x3" }, // normal
          { shaderLocation: 2, offset: 24, format: "float32x2" }, // uv
          { shaderLocation: 3, offset: 32, format: "float32x4" }, // tangent
        ],
      }],
    },
    fragment: { module: shader, entryPoint: "fs", targets: [{ format }] },
    depthStencil: { format: "depth24plus", depthWriteEnabled: true, depthCompare: "less" },
    primitive: { topology: "triangle-list", cullMode: "back" },
  });

  // ── Bind groups ───────────────────────────────────────────────────────────────

  const sampler = device.createSampler({
    magFilter: "linear", minFilter: "linear", mipmapFilter: "linear",
    addressModeU: "repeat", addressModeV: "repeat",
  });

  const frameBG = device.createBindGroup({
    layout: frameBGL,
    entries: [{ binding: 0, resource: { buffer: frameBuf } }],
  });
  const objBG = device.createBindGroup({
    layout: objBGL,
    entries: [{ binding: 0, resource: { buffer: objBuf } }],
  });
  const texBG = device.createBindGroup({
    layout: texBGL,
    entries: [
      { binding: 0, resource: sampler },
      { binding: 1, resource: texBC.createView() },
      { binding: 2, resource: texMR.createView() },
      { binding: 3, resource: texNorm.createView() },
      { binding: 4, resource: texOcc.createView() },
      { binding: 5, resource: texEmit.createView() },
    ],
  });

  // ── Depth texture (recreated on resize) ──────────────────────────────────────

  let depthTex = device.createTexture({
    size: [canvas.width || 1, canvas.height || 1],
    format: "depth24plus",
    usage: GPUTextureUsage.RENDER_ATTACHMENT,
  });

  // ── Orbit camera ─────────────────────────────────────────────────────────────

  let azimuth = 0.2, elevation = 0.15, dist = 3.0;
  let dragging = false, lastX = 0, lastY = 0;

  const onDown = e => {
    dragging = true; lastX = e.clientX; lastY = e.clientY;
    canvas.setPointerCapture(e.pointerId);
    canvas.style.cursor = "grabbing";
  };
  const onMove = e => {
    if (!dragging) return;
    azimuth   -= (e.clientX - lastX) * 0.006;
    elevation += (e.clientY - lastY) * 0.006;
    elevation  = Math.max(-1.4, Math.min(1.4, elevation));
    lastX = e.clientX; lastY = e.clientY;
  };
  const onUp = e => {
    dragging = false;
    canvas.releasePointerCapture(e.pointerId);
    canvas.style.cursor = "grab";
  };
  const onWheel = e => {
    dist = Math.max(1.0, Math.min(8.0, dist + e.deltaY * 0.01));
    e.preventDefault();
  };

  canvas.addEventListener("pointerdown", onDown);
  canvas.addEventListener("pointermove", onMove);
  canvas.addEventListener("pointerup",   onUp);
  canvas.addEventListener("pointercancel", onUp);
  canvas.addEventListener("wheel", onWheel, { passive: false });
  canvas.style.cursor      = "grab";
  canvas.style.touchAction = "none";

  // ── Render loop ───────────────────────────────────────────────────────────────

  let lastW = 0, lastH = 0, animId;

  function frame() {
    animId = requestAnimationFrame(frame);

    const dpr = window.devicePixelRatio || 1;
    const w   = Math.floor(canvas.clientWidth * dpr) || 1;
    const h   = Math.floor(canvas.clientHeight * dpr) || 1;
    if (w !== lastW || h !== lastH) {
      canvas.width = w; canvas.height = h;
      depthTex.destroy();
      depthTex = device.createTexture({
        size: [w, h], format: "depth24plus",
        usage: GPUTextureUsage.RENDER_ATTACHMENT,
      });
      lastW = w; lastH = h;
    }

    const camX = Math.sin(azimuth) * Math.cos(elevation) * dist;
    const camY = Math.sin(elevation) * dist;
    const camZ = Math.cos(azimuth) * Math.cos(elevation) * dist;
    const eye  = [camX, camY, camZ];
    const proj = perspective(0.785, w / h, 0.05, 20.0);
    const view = lookAt(eye, [0, 0, 0], [0, 1, 0]);
    const vp   = mul4(proj, view);

    frameData.set(vp, 0);
    frameData[16] = eye[0]; frameData[17] = eye[1]; frameData[18] = eye[2];
    frameData[20] = L0.pos[0]; frameData[21] = L0.pos[1]; frameData[22] = L0.pos[2];
    frameData[24] = L0.col[0]; frameData[25] = L0.col[1]; frameData[26] = L0.col[2];
    frameData[27] = L0.int;
    frameData[28] = L1.pos[0]; frameData[29] = L1.pos[1]; frameData[30] = L1.pos[2];
    frameData[32] = L1.col[0]; frameData[33] = L1.col[1]; frameData[34] = L1.col[2];
    frameData[35] = L1.int;
    device.queue.writeBuffer(frameBuf, 0, frameData);

    const enc  = device.createCommandEncoder();
    const pass = enc.beginRenderPass({
      colorAttachments: [{
        view: context.getCurrentTexture().createView(),
        loadOp: "clear", storeOp: "store",
        clearValue: { r: 0.05, g: 0.055, b: 0.07, a: 1 },
      }],
      depthStencilAttachment: {
        view: depthTex.createView(),
        depthLoadOp: "clear", depthStoreOp: "store", depthClearValue: 1.0,
      },
    });
    pass.setPipeline(pipeline);
    pass.setBindGroup(0, frameBG);
    pass.setBindGroup(1, objBG);
    pass.setBindGroup(2, texBG);
    pass.setVertexBuffer(0, vertBuf);
    pass.setIndexBuffer(idxBuf, idxFormat);
    pass.drawIndexed(idxCount);
    pass.end();
    device.queue.submit([enc.finish()]);
  }

  animId = requestAnimationFrame(frame);

  return function cleanup() {
    cancelAnimationFrame(animId);
    canvas.removeEventListener("pointerdown", onDown);
    canvas.removeEventListener("pointermove", onMove);
    canvas.removeEventListener("pointerup",   onUp);
    canvas.removeEventListener("pointercancel", onUp);
    canvas.removeEventListener("wheel", onWheel);
    frameBuf.destroy(); objBuf.destroy();
    vertBuf.destroy(); idxBuf.destroy(); depthTex.destroy();
    device.destroy();
  };
}
