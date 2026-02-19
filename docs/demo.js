// Prism Engine — WebGPU PBR Sphere Grid Demo
// 5×5 sphere grid with Cook-Torrance BRDF
// X-axis: metallic 0→1 (left to right)
// Y-axis: roughness 0.05→0.95 (smooth at top, rough at bottom)

// ── Math helpers ──────────────────────────────────────────────────────────────

function perspective(fovY, aspect, near, far) {
  const f = 1.0 / Math.tan(fovY * 0.5);
  const nf = 1 / (near - far);
  // column-major
  // prettier-ignore
  return new Float32Array([
    f / aspect, 0, 0, 0,
    0, f, 0, 0,
    0, 0, (far + near) * nf, -1,
    0, 0, 2 * far * near * nf, 0,
  ]);
}

function lookAt(eye, center, up) {
  const zx = eye[0] - center[0], zy = eye[1] - center[1], zz = eye[2] - center[2];
  let l = 1 / Math.hypot(zx, zy, zz);
  const fz = [zx * l, zy * l, zz * l];
  const sx = up[1] * fz[2] - up[2] * fz[1];
  const sy = up[2] * fz[0] - up[0] * fz[2];
  const sz = up[0] * fz[1] - up[1] * fz[0];
  l = 1 / Math.hypot(sx, sy, sz);
  const fs = [sx * l, sy * l, sz * l];
  const ux = fz[1] * fs[2] - fz[2] * fs[1];
  const uy = fz[2] * fs[0] - fz[0] * fs[2];
  const uz = fz[0] * fs[1] - fz[1] * fs[0];
  // column-major
  // prettier-ignore
  return new Float32Array([
    fs[0], ux, fz[0], 0,
    fs[1], uy, fz[1], 0,
    fs[2], uz, fz[2], 0,
    -(fs[0]*eye[0]+fs[1]*eye[1]+fs[2]*eye[2]),
    -(ux*eye[0]+uy*eye[1]+uz*eye[2]),
    -(fz[0]*eye[0]+fz[1]*eye[1]+fz[2]*eye[2]),
    1,
  ]);
}

function mul4(a, b) {
  const out = new Float32Array(16);
  for (let i = 0; i < 4; i++) {
    for (let j = 0; j < 4; j++) {
      out[j*4+i] = a[i]*b[j*4] + a[4+i]*b[j*4+1] + a[8+i]*b[j*4+2] + a[12+i]*b[j*4+3];
    }
  }
  return out;
}

function translate4(x, y, z) {
  // prettier-ignore
  return new Float32Array([
    1,0,0,0, 0,1,0,0, 0,0,1,0, x,y,z,1,
  ]);
}

// ── Sphere geometry ───────────────────────────────────────────────────────────

function makeSphere(rings, segs, radius) {
  const verts = [];
  const idx = [];
  for (let r = 0; r <= rings; r++) {
    const theta = r * Math.PI / rings;
    for (let s = 0; s <= segs; s++) {
      const phi = s * 2 * Math.PI / segs;
      const x = Math.sin(theta) * Math.cos(phi);
      const y = Math.cos(theta);
      const z = Math.sin(theta) * Math.sin(phi);
      verts.push(x * radius, y * radius, z * radius, x, y, z);
    }
  }
  for (let r = 0; r < rings; r++) {
    for (let s = 0; s < segs; s++) {
      const a = r * (segs + 1) + s;
      const b = a + segs + 1;
      idx.push(a, b, a + 1, b, b + 1, a + 1);
    }
  }
  return { verts: new Float32Array(verts), idx: new Uint32Array(idx) };
}

// ── WGSL Shaders ──────────────────────────────────────────────────────────────

const WGSL = /* wgsl */ `
const PI: f32 = 3.14159265358979;

// Group 0: per-frame data (camera + 2 lights)
struct Frame {
    viewProj     : mat4x4f,  // offset 0,   64B
    cameraPos    : vec3f,    // offset 64,  12B
    _pad0        : f32,      // offset 76,   4B
    light0Pos    : vec3f,    // offset 80,  12B
    _pad1        : f32,      // offset 92,   4B
    light0Color  : vec3f,    // offset 96,  12B
    light0Int    : f32,      // offset 108,  4B
    light1Pos    : vec3f,    // offset 112, 12B
    _pad2        : f32,      // offset 124,  4B
    light1Color  : vec3f,    // offset 128, 12B
    light1Int    : f32,      // offset 140,  4B
};                           // total 144B

@group(0) @binding(0) var<uniform> frame: Frame;

// Group 1: per-sphere data
struct Sphere {
    model     : mat4x4f,  // offset 0,   64B
    metallic  : f32,      // offset 64,   4B
    roughness : f32,      // offset 68,   4B
    _pad0     : f32,      // offset 72,   4B
    _pad1     : f32,      // offset 76,   4B
    albedo    : vec3f,    // offset 80,  12B
    _pad2     : f32,      // offset 92,   4B
};                        // total 96B

@group(1) @binding(0) var<uniform> sphere: Sphere;

// ── Vertex shader ─────────────────────────────────────────────────────────────

struct VsOut {
    @builtin(position) clip : vec4f,
    @location(0)       wPos : vec3f,
    @location(1)       wNrm : vec3f,
}

@vertex fn vs(
    @location(0) pos : vec3f,
    @location(1) nrm : vec3f,
) -> VsOut {
    let wPos = (sphere.model * vec4f(pos, 1.0)).xyz;
    // normal matrix = inverse-transpose; for uniform scale a simple model-matrix mul suffices
    let wNrm = normalize((sphere.model * vec4f(nrm, 0.0)).xyz);
    var out: VsOut;
    out.clip = frame.viewProj * vec4f(wPos, 1.0);
    out.wPos = wPos;
    out.wNrm = wNrm;
    return out;
}

// ── PBR helpers ───────────────────────────────────────────────────────────────

fn D_GGX(NdotH: f32, roughness: f32) -> f32 {
    let a  = roughness * roughness;
    let a2 = a * a;
    let d  = NdotH * NdotH * (a2 - 1.0) + 1.0;
    return a2 / max(PI * d * d, 1e-7);
}

fn G_Schlick(n: f32, roughness: f32) -> f32 {
    let r = roughness + 1.0;
    let k = r * r / 8.0;
    return n / max(n * (1.0 - k) + k, 1e-7);
}

fn G_Smith(NdotV: f32, NdotL: f32, roughness: f32) -> f32 {
    return G_Schlick(max(NdotV, 0.001), roughness)
         * G_Schlick(max(NdotL, 0.001), roughness);
}

fn F_Schlick(cosTheta: f32, F0: vec3f) -> vec3f {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

fn cookTorrance(
    N: vec3f, V: vec3f, L: vec3f,
    albedo: vec3f, metallic: f32, roughness: f32,
    lightColor: vec3f, lightIntensity: f32,
) -> vec3f {
    let H      = normalize(V + L);
    let NdotL  = max(dot(N, L), 0.0);
    let NdotV  = max(dot(N, V), 0.001);
    let NdotH  = max(dot(N, H), 0.0);
    let HdotV  = max(dot(H, V), 0.0);

    let F0     = mix(vec3f(0.04), albedo, metallic);
    let D      = D_GGX(NdotH, roughness);
    let G      = G_Smith(NdotV, NdotL, roughness);
    let F      = F_Schlick(HdotV, F0);
    let spec   = D * G * F / max(4.0 * NdotV * NdotL, 1e-7);

    let kD     = (1.0 - F) * (1.0 - metallic);
    let diffuse = kD * albedo / PI;

    return (diffuse + spec) * lightColor * lightIntensity * NdotL;
}

// ── Fragment shader ───────────────────────────────────────────────────────────

@fragment fn fs(in: VsOut) -> @location(0) vec4f {
    let N = normalize(in.wNrm);
    let V = normalize(frame.cameraPos - in.wPos);

    let albedo    = sphere.albedo;
    let metallic  = sphere.metallic;
    let roughness = max(sphere.roughness, 0.04); // avoid pure mirror at r=0

    let L0 = normalize(frame.light0Pos - in.wPos);
    let d0 = length(frame.light0Pos - in.wPos);
    let att0 = 1.0 / max(d0 * d0 * 0.04, 1.0);

    let L1 = normalize(frame.light1Pos - in.wPos);
    let d1 = length(frame.light1Pos - in.wPos);
    let att1 = 1.0 / max(d1 * d1 * 0.04, 1.0);

    var Lo = cookTorrance(N, V, L0, albedo, metallic, roughness,
                          frame.light0Color, frame.light0Int * att0);
    Lo    += cookTorrance(N, V, L1, albedo, metallic, roughness,
                          frame.light1Color, frame.light1Int * att1);

    // Simple ambient (no IBL in demo.js — use a fraction of the albedo)
    let ambient = albedo * 0.03;
    var color = Lo + ambient;

    // Gamma correction (sRGB output)
    color = pow(max(color, vec3f(0.0)), vec3f(1.0 / 2.2));
    return vec4f(color, 1.0);
}
`;

// ── Demo entry point ──────────────────────────────────────────────────────────

export async function initDemo(canvas) {
  if (!navigator.gpu) return false;

  const adapter = await navigator.gpu.requestAdapter();
  if (!adapter) return false;

  const device = await adapter.requestDevice();
  const context = canvas.getContext("webgpu");
  const format = navigator.gpu.getPreferredCanvasFormat();
  context.configure({ device, format, alphaMode: "opaque" });

  // ── Sphere mesh ──────────────────────────────────────────────────────────────

  const { verts, idx } = makeSphere(24, 24, 0.42);

  const vertexBuf = device.createBuffer({
    size: verts.byteLength,
    usage: GPUBufferUsage.VERTEX | GPUBufferUsage.COPY_DST,
  });
  device.queue.writeBuffer(vertexBuf, 0, verts);

  const indexBuf = device.createBuffer({
    size: idx.byteLength,
    usage: GPUBufferUsage.INDEX | GPUBufferUsage.COPY_DST,
  });
  device.queue.writeBuffer(indexBuf, 0, idx);
  const indexCount = idx.length;

  // ── Frame uniform buffer (144B) ──────────────────────────────────────────────
  // Layout: viewProj(64) + camPos(12)+pad(4) + 2×[pos(12)+pad(4)+color(12)+int(4)]

  const FRAME_SIZE = 144;
  const frameBuf = device.createBuffer({
    size: FRAME_SIZE,
    usage: GPUBufferUsage.UNIFORM | GPUBufferUsage.COPY_DST,
  });
  const frameData = new Float32Array(FRAME_SIZE / 4);

  // Light 0: warm white, upper-left
  const L0_POS = [5.5, 6.0, 6.0];
  const L0_COL = [1.0, 0.88, 0.72];
  const L0_INT = 120.0;
  // Light 1: cool blue-white, lower-right
  const L1_POS = [-4.0, -2.0, 7.0];
  const L1_COL = [0.55, 0.72, 1.0];
  const L1_INT = 80.0;

  // ── Per-sphere uniform buffers (96B each) ────────────────────────────────────
  // Layout: model(64) + metallic(4)+roughness(4)+pad(4)+pad(4) + albedo(12)+pad(4)

  const SPHERE_SIZE = 96;
  const GRID = 5;
  const SPACING = 2.0;
  const HALF = (GRID - 1) * SPACING * 0.5; // center the grid

  // Base color: silver-white dielectric
  const BASE = [0.95, 0.93, 0.88];

  const sphereBufs = [];
  for (let row = 0; row < GRID; row++) {
    for (let col = 0; col < GRID; col++) {
      const metallic  = col / (GRID - 1);            // 0→1 left to right
      const roughness = 0.05 + row / (GRID - 1) * 0.9; // 0.05→0.95 top to bottom

      const x = -HALF + col * SPACING;
      const y =  HALF - row * SPACING;

      const data = new Float32Array(SPHERE_SIZE / 4);
      data.set(translate4(x, y, 0.0), 0);
      data[16] = metallic;
      data[17] = roughness;
      // data[18], data[19]: padding
      data[20] = BASE[0];
      data[21] = BASE[1];
      data[22] = BASE[2];
      // data[23]: padding

      const buf = device.createBuffer({
        size: SPHERE_SIZE,
        usage: GPUBufferUsage.UNIFORM | GPUBufferUsage.COPY_DST,
      });
      device.queue.writeBuffer(buf, 0, data);
      sphereBufs.push(buf);
    }
  }

  // ── Bind group layouts + pipeline ────────────────────────────────────────────

  const frameBGL = device.createBindGroupLayout({
    entries: [{
      binding: 0,
      visibility: GPUShaderStage.VERTEX | GPUShaderStage.FRAGMENT,
      buffer: { type: "uniform" },
    }],
  });

  const sphereBGL = device.createBindGroupLayout({
    entries: [{
      binding: 0,
      visibility: GPUShaderStage.VERTEX | GPUShaderStage.FRAGMENT,
      buffer: { type: "uniform" },
    }],
  });

  const shader = device.createShaderModule({ code: WGSL });

  const pipeline = device.createRenderPipeline({
    layout: device.createPipelineLayout({ bindGroupLayouts: [frameBGL, sphereBGL] }),
    vertex: {
      module: shader, entryPoint: "vs",
      buffers: [{
        arrayStride: 24, // 6 floats × 4B
        attributes: [
          { shaderLocation: 0, offset: 0,  format: "float32x3" }, // position
          { shaderLocation: 1, offset: 12, format: "float32x3" }, // normal
        ],
      }],
    },
    fragment: { module: shader, entryPoint: "fs", targets: [{ format }] },
    depthStencil: { format: "depth24plus", depthWriteEnabled: true, depthCompare: "less" },
    primitive: { topology: "triangle-list", cullMode: "back" },
  });

  // ── Bind groups ───────────────────────────────────────────────────────────────

  const frameBG = device.createBindGroup({
    layout: frameBGL,
    entries: [{ binding: 0, resource: { buffer: frameBuf } }],
  });

  const sphereBGs = sphereBufs.map(buf =>
    device.createBindGroup({
      layout: sphereBGL,
      entries: [{ binding: 0, resource: { buffer: buf } }],
    })
  );

  // ── Depth texture (recreated on resize) ──────────────────────────────────────

  let depthTex = device.createTexture({
    size: [canvas.width, canvas.height],
    format: "depth24plus",
    usage: GPUTextureUsage.RENDER_ATTACHMENT,
  });

  // ── Orbit camera ──────────────────────────────────────────────────────────────

  let azimuth   = 0.4;  // radians, positive = right
  let elevation = 0.35; // radians, positive = up
  const DIST    = 13.5;

  let dragging = false, lastX = 0, lastY = 0;

  const onPointerDown = e => {
    dragging = true;
    lastX = e.clientX; lastY = e.clientY;
    canvas.setPointerCapture(e.pointerId);
    canvas.style.cursor = "grabbing";
  };
  const onPointerMove = e => {
    if (!dragging) return;
    azimuth   -= (e.clientX - lastX) * 0.006;
    elevation += (e.clientY - lastY) * 0.006;
    elevation  = Math.max(-1.4, Math.min(1.4, elevation));
    lastX = e.clientX; lastY = e.clientY;
  };
  const onPointerUp = e => {
    dragging = false;
    canvas.releasePointerCapture(e.pointerId);
    canvas.style.cursor = "grab";
  };

  canvas.addEventListener("pointerdown", onPointerDown);
  canvas.addEventListener("pointermove", onPointerMove);
  canvas.addEventListener("pointerup",   onPointerUp);
  canvas.addEventListener("pointercancel", onPointerUp);
  canvas.style.cursor      = "grab";
  canvas.style.touchAction = "none";

  // ── Render loop ───────────────────────────────────────────────────────────────

  let lastW = canvas.width, lastH = canvas.height;
  let animId;

  function frame() {
    animId = requestAnimationFrame(frame);

    // Handle resize
    const dpr = window.devicePixelRatio || 1;
    const w = Math.floor(canvas.clientWidth * dpr);
    const h = Math.floor(canvas.clientHeight * dpr);
    if (w !== lastW || h !== lastH) {
      canvas.width = w; canvas.height = h;
      depthTex.destroy();
      depthTex = device.createTexture({
        size: [w, h],
        format: "depth24plus",
        usage: GPUTextureUsage.RENDER_ATTACHMENT,
      });
      lastW = w; lastH = h;
    }

    // Camera position (spherical)
    const camX = Math.sin(azimuth) * Math.cos(elevation) * DIST;
    const camY = Math.sin(elevation) * DIST;
    const camZ = Math.cos(azimuth) * Math.cos(elevation) * DIST;
    const eye = [camX, camY, camZ];

    const proj = perspective(0.785, canvas.width / canvas.height, 0.5, 100);
    const view = lookAt(eye, [0, 0, 0], [0, 1, 0]);
    const vp   = mul4(proj, view);

    frameData.set(vp, 0);
    frameData[16] = eye[0]; frameData[17] = eye[1]; frameData[18] = eye[2];
    // [19] pad
    frameData[20] = L0_POS[0]; frameData[21] = L0_POS[1]; frameData[22] = L0_POS[2];
    // [23] pad
    frameData[24] = L0_COL[0]; frameData[25] = L0_COL[1]; frameData[26] = L0_COL[2];
    frameData[27] = L0_INT;
    frameData[28] = L1_POS[0]; frameData[29] = L1_POS[1]; frameData[30] = L1_POS[2];
    // [31] pad
    frameData[32] = L1_COL[0]; frameData[33] = L1_COL[1]; frameData[34] = L1_COL[2];
    frameData[35] = L1_INT;
    device.queue.writeBuffer(frameBuf, 0, frameData);

    const encoder = device.createCommandEncoder();
    const pass = encoder.beginRenderPass({
      colorAttachments: [{
        view: context.getCurrentTexture().createView(),
        loadOp: "clear",
        storeOp: "store",
        clearValue: { r: 0.05, g: 0.055, b: 0.07, a: 1 },
      }],
      depthStencilAttachment: {
        view: depthTex.createView(),
        depthLoadOp: "clear",
        depthStoreOp: "store",
        depthClearValue: 1.0,
      },
    });

    pass.setPipeline(pipeline);
    pass.setBindGroup(0, frameBG);
    pass.setVertexBuffer(0, vertexBuf);
    pass.setIndexBuffer(indexBuf, "uint32");

    for (let i = 0; i < sphereBGs.length; i++) {
      pass.setBindGroup(1, sphereBGs[i]);
      pass.drawIndexed(indexCount);
    }
    pass.end();

    device.queue.submit([encoder.finish()]);
  }

  animId = requestAnimationFrame(frame);

  return function cleanup() {
    cancelAnimationFrame(animId);
    canvas.removeEventListener("pointerdown", onPointerDown);
    canvas.removeEventListener("pointermove", onPointerMove);
    canvas.removeEventListener("pointerup",   onPointerUp);
    canvas.removeEventListener("pointercancel", onPointerUp);
    frameBuf.destroy();
    sphereBufs.forEach(b => b.destroy());
    vertexBuf.destroy();
    indexBuf.destroy();
    depthTex.destroy();
    device.destroy();
  };
}
