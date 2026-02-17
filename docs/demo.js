// Prism Engine — WebGPU Spinning Cube Demo
// Renders a lit cube with Hyeons' Lab logo textured on each face

export async function initDemo(canvas, logoUrl) {
  if (!navigator.gpu) {
    return false;
  }

  const adapter = await navigator.gpu.requestAdapter();
  if (!adapter) return false;

  const device = await adapter.requestDevice();
  const context = canvas.getContext("webgpu");
  const format = navigator.gpu.getPreferredCanvasFormat();

  context.configure({ device, format, alphaMode: "premultiplied" });

  // ── Logo texture ──────────────────────────────────

  // Load the logo image, then composite it centered on a dark square
  const texSize = 512;
  const texCanvas = document.createElement("canvas");
  texCanvas.width = texSize;
  texCanvas.height = texSize;
  const ctx = texCanvas.getContext("2d");

  // Dark rusty orange background
  ctx.fillStyle = "#3a1e0a";
  ctx.fillRect(0, 0, texSize, texSize);

  // Subtle border
  const bw = 3;
  ctx.strokeStyle = "rgba(180, 90, 30, 0.5)";
  ctx.lineWidth = bw;
  ctx.strokeRect(bw / 2, bw / 2, texSize - bw, texSize - bw);

  // Try to load the external logo image
  try {
    const img = await new Promise((resolve, reject) => {
      const i = new Image();
      i.crossOrigin = "anonymous";
      i.onload = () => resolve(i);
      i.onerror = reject;
      i.src = logoUrl;
    });

    // Draw logo to a temp canvas, convert to white, then stamp onto face
    const tmpC = document.createElement("canvas");
    tmpC.width = texSize;
    tmpC.height = texSize;
    const tmpCtx = tmpC.getContext("2d");

    const maxDim = texSize * 0.8;
    const scale = Math.min(maxDim / img.width, maxDim / img.height);
    const w = img.width * scale;
    const h = img.height * scale;
    tmpCtx.drawImage(img, (texSize - w) / 2, (texSize - h) / 2, w, h);

    // Make all visible pixels white, keep alpha
    const imgData = tmpCtx.getImageData(0, 0, texSize, texSize);
    const px = imgData.data;
    for (let i = 0; i < px.length; i += 4) {
      if (px[i + 3] > 0) {
        px[i] = 255;     // R
        px[i + 1] = 255; // G
        px[i + 2] = 255; // B
      }
    }
    tmpCtx.putImageData(imgData, 0, 0);
    ctx.drawImage(tmpC, 0, 0);
  } catch {
    // Fallback: render text if image fails to load
    ctx.fillStyle = "#e6edf3";
    ctx.font =
      "bold 64px 'SF Mono', 'Cascadia Code', 'Fira Code', monospace";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillText("hyeons", texSize / 2, texSize / 2 - 20);
    ctx.fillStyle = "#58a6ff";
    ctx.font =
      "bold 48px 'SF Mono', 'Cascadia Code', 'Fira Code', monospace";
    ctx.fillText("_lab", texSize / 2, texSize / 2 + 40);
  }

  const texture = device.createTexture({
    size: [texSize, texSize],
    format: "rgba8unorm",
    usage:
      GPUTextureUsage.TEXTURE_BINDING |
      GPUTextureUsage.COPY_DST |
      GPUTextureUsage.RENDER_ATTACHMENT,
  });

  device.queue.copyExternalImageToTexture(
    { source: texCanvas },
    { texture },
    [texSize, texSize]
  );

  const sampler = device.createSampler({
    magFilter: "linear",
    minFilter: "linear",
    mipmapFilter: "linear",
  });

  // ── Cube geometry ─────────────────────────────────
  // pos(3) + normal(3) + uv(2) per vertex, 6 faces * 4 verts
  // prettier-ignore
  const vertices = new Float32Array([
    // Front face
    -1, -1,  1,   0,  0,  1,   0, 1,
     1, -1,  1,   0,  0,  1,   1, 1,
     1,  1,  1,   0,  0,  1,   1, 0,
    -1,  1,  1,   0,  0,  1,   0, 0,
    // Back face
     1, -1, -1,   0,  0, -1,   0, 1,
    -1, -1, -1,   0,  0, -1,   1, 1,
    -1,  1, -1,   0,  0, -1,   1, 0,
     1,  1, -1,   0,  0, -1,   0, 0,
    // Top face
    -1,  1,  1,   0,  1,  0,   0, 1,
     1,  1,  1,   0,  1,  0,   1, 1,
     1,  1, -1,   0,  1,  0,   1, 0,
    -1,  1, -1,   0,  1,  0,   0, 0,
    // Bottom face
    -1, -1, -1,   0, -1,  0,   0, 1,
     1, -1, -1,   0, -1,  0,   1, 1,
     1, -1,  1,   0, -1,  0,   1, 0,
    -1, -1,  1,   0, -1,  0,   0, 0,
    // Right face
     1, -1,  1,   1,  0,  0,   0, 1,
     1, -1, -1,   1,  0,  0,   1, 1,
     1,  1, -1,   1,  0,  0,   1, 0,
     1,  1,  1,   1,  0,  0,   0, 0,
    // Left face
    -1, -1, -1,  -1,  0,  0,   0, 1,
    -1, -1,  1,  -1,  0,  0,   1, 1,
    -1,  1,  1,  -1,  0,  0,   1, 0,
    -1,  1, -1,  -1,  0,  0,   0, 0,
  ]);

  // prettier-ignore
  const indices = new Uint16Array([
     0,  1,  2,  0,  2,  3,
     4,  5,  6,  4,  6,  7,
     8,  9, 10,  8, 10, 11,
    12, 13, 14, 12, 14, 15,
    16, 17, 18, 16, 18, 19,
    20, 21, 22, 20, 22, 23,
  ]);

  const vertexBuffer = device.createBuffer({
    size: vertices.byteLength,
    usage: GPUBufferUsage.VERTEX | GPUBufferUsage.COPY_DST,
  });
  device.queue.writeBuffer(vertexBuffer, 0, vertices);

  const indexBuffer = device.createBuffer({
    size: indices.byteLength,
    usage: GPUBufferUsage.INDEX | GPUBufferUsage.COPY_DST,
  });
  device.queue.writeBuffer(indexBuffer, 0, indices);

  // ── Shaders ───────────────────────────────────────

  const shaderCode = /* wgsl */ `
    struct Uniforms {
      mvp: mat4x4f,
      model: mat4x4f,
      lightDir: vec3f,
      _pad: f32,
      eyePos: vec3f,
      _pad2: f32,
    }

    @group(0) @binding(0) var<uniform> u: Uniforms;
    @group(0) @binding(1) var texSampler: sampler;
    @group(0) @binding(2) var texColor: texture_2d<f32>;

    struct VsOut {
      @builtin(position) pos: vec4f,
      @location(0) normal: vec3f,
      @location(1) uv: vec2f,
      @location(2) worldPos: vec3f,
    }

    @vertex fn vs(
      @location(0) position: vec3f,
      @location(1) normal: vec3f,
      @location(2) uv: vec2f,
    ) -> VsOut {
      var out: VsOut;
      out.pos = u.mvp * vec4f(position, 1.0);
      out.normal = (u.model * vec4f(normal, 0.0)).xyz;
      out.worldPos = (u.model * vec4f(position, 1.0)).xyz;
      out.uv = uv;
      return out;
    }

    @fragment fn fs(in: VsOut) -> @location(0) vec4f {
      let n = normalize(in.normal);
      let texel = textureSample(texColor, texSampler, in.uv);

      // Key light (upper-right-front)
      let light1 = normalize(u.lightDir);
      let diff1 = max(dot(n, light1), 0.0);

      // Fill light (lower-left, softer)
      let light2 = normalize(vec3f(-0.4, -0.3, 0.8));
      let diff2 = max(dot(n, light2), 0.0) * 0.35;

      // Rim light (behind, edge highlight)
      let viewDir = normalize(u.eyePos - in.worldPos);
      let rim = pow(1.0 - max(dot(n, viewDir), 0.0), 3.0) * 0.3;

      // Specular (Blinn-Phong)
      let halfDir = normalize(light1 + viewDir);
      let spec = pow(max(dot(n, halfDir), 0.0), 32.0) * 0.5;

      let ambient = 0.15;
      let lighting = ambient + diff1 * 0.7 + diff2 + rim;
      let color = texel.rgb * lighting + vec3f(1.0, 0.7, 0.4) * spec;

      return vec4f(color, texel.a);
    }
  `;

  const shaderModule = device.createShaderModule({ code: shaderCode });

  // ── Depth texture ─────────────────────────────────

  let depthTexture = device.createTexture({
    size: [canvas.width, canvas.height],
    format: "depth24plus",
    usage: GPUTextureUsage.RENDER_ATTACHMENT,
  });

  // ── Uniform buffer ────────────────────────────────

  const uniformBufferSize = 4 * 16 + 4 * 16 + 4 * 4 + 4 * 4; // mvp + model + lightDir+pad + eyePos+pad
  const uniformBuffer = device.createBuffer({
    size: uniformBufferSize,
    usage: GPUBufferUsage.UNIFORM | GPUBufferUsage.COPY_DST,
  });

  // ── Pipeline ──────────────────────────────────────

  const bindGroupLayout = device.createBindGroupLayout({
    entries: [
      {
        binding: 0,
        visibility: GPUShaderStage.VERTEX | GPUShaderStage.FRAGMENT,
        buffer: { type: "uniform" },
      },
      {
        binding: 1,
        visibility: GPUShaderStage.FRAGMENT,
        sampler: { type: "filtering" },
      },
      {
        binding: 2,
        visibility: GPUShaderStage.FRAGMENT,
        texture: { sampleType: "float" },
      },
    ],
  });

  const pipeline = device.createRenderPipeline({
    layout: device.createPipelineLayout({
      bindGroupLayouts: [bindGroupLayout],
    }),
    vertex: {
      module: shaderModule,
      entryPoint: "vs",
      buffers: [
        {
          arrayStride: 8 * 4,
          attributes: [
            { shaderLocation: 0, offset: 0, format: "float32x3" },
            { shaderLocation: 1, offset: 12, format: "float32x3" },
            { shaderLocation: 2, offset: 24, format: "float32x2" },
          ],
        },
      ],
    },
    fragment: {
      module: shaderModule,
      entryPoint: "fs",
      targets: [{ format }],
    },
    depthStencil: {
      format: "depth24plus",
      depthWriteEnabled: true,
      depthCompare: "less",
    },
    primitive: {
      topology: "triangle-list",
      cullMode: "back",
    },
  });

  const bindGroup = device.createBindGroup({
    layout: bindGroupLayout,
    entries: [
      { binding: 0, resource: { buffer: uniformBuffer } },
      { binding: 1, resource: sampler },
      { binding: 2, resource: texture.createView() },
    ],
  });

  // ── Math helpers ──────────────────────────────────

  function mat4Perspective(fovY, aspect, near, far) {
    const f = 1.0 / Math.tan(fovY / 2);
    const nf = 1 / (near - far);
    // prettier-ignore
    return new Float32Array([
      f / aspect, 0, 0, 0,
      0, f, 0, 0,
      0, 0, (far + near) * nf, -1,
      0, 0, 2 * far * near * nf, 0,
    ]);
  }

  function mat4LookAt(eye, center, up) {
    const zx = eye[0] - center[0],
      zy = eye[1] - center[1],
      zz = eye[2] - center[2];
    let len = 1 / Math.hypot(zx, zy, zz);
    const fz = [zx * len, zy * len, zz * len];
    const sx = up[1] * fz[2] - up[2] * fz[1];
    const sy = up[2] * fz[0] - up[0] * fz[2];
    const sz = up[0] * fz[1] - up[1] * fz[0];
    len = 1 / Math.hypot(sx, sy, sz);
    const fs = [sx * len, sy * len, sz * len];
    const ux = fz[1] * fs[2] - fz[2] * fs[1];
    const uy = fz[2] * fs[0] - fz[0] * fs[2];
    const uz = fz[0] * fs[1] - fz[1] * fs[0];
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

  function mat4RotateY(angle) {
    const c = Math.cos(angle),
      s = Math.sin(angle);
    // prettier-ignore
    return new Float32Array([
       c, 0, s, 0,
       0, 1, 0, 0,
      -s, 0, c, 0,
       0, 0, 0, 1,
    ]);
  }

  function mat4RotateX(angle) {
    const c = Math.cos(angle),
      s = Math.sin(angle);
    // prettier-ignore
    return new Float32Array([
      1, 0,  0, 0,
      0, c, -s, 0,
      0, s,  c, 0,
      0, 0,  0, 1,
    ]);
  }

  // Pre-allocated scratch buffers to avoid per-frame GC pressure
  const _mulTmp = new Float32Array(16);
  const _uniformData = new Float32Array(40);

  function mat4Mul(a, b, dst) {
    const out = dst || new Float32Array(16);
    for (let i = 0; i < 4; i++) {
      for (let j = 0; j < 4; j++) {
        out[j * 4 + i] =
          a[i] * b[j * 4] +
          a[4 + i] * b[j * 4 + 1] +
          a[8 + i] * b[j * 4 + 2] +
          a[12 + i] * b[j * 4 + 3];
      }
    }
    return out;
  }

  // ── Mouse / touch drag ─────────────────────────────

  // Accumulated rotation as a matrix (left-multiply for screen-space feel)
  // prettier-ignore
  let modelRotation = new Float32Array([
    1, 0, 0, 0,
    0, 1, 0, 0,
    0, 0, 1, 0,
    0, 0, 0, 1,
  ]);
  let dragging = false;
  let lastPointerX = 0;
  let lastPointerY = 0;
  let pendingDx = 0;
  let pendingDy = 0;
  const SENSITIVITY = 0.007;

  function onPointerDown(e) {
    dragging = true;
    lastPointerX = e.clientX;
    lastPointerY = e.clientY;
    canvas.setPointerCapture(e.pointerId);
    canvas.style.cursor = "grabbing";
  }

  function onPointerMove(e) {
    if (!dragging) return;
    pendingDx += -(e.clientX - lastPointerX) * SENSITIVITY;
    pendingDy += -(e.clientY - lastPointerY) * SENSITIVITY;
    lastPointerX = e.clientX;
    lastPointerY = e.clientY;
  }

  function onPointerUp(e) {
    dragging = false;
    canvas.releasePointerCapture(e.pointerId);
    canvas.style.cursor = "grab";
  }

  canvas.addEventListener("pointerdown", onPointerDown);
  canvas.addEventListener("pointermove", onPointerMove);
  canvas.addEventListener("pointerup", onPointerUp);
  canvas.addEventListener("pointercancel", onPointerUp);
  canvas.addEventListener("pointerleave", onPointerUp);
  canvas.style.cursor = "grab";
  canvas.style.touchAction = "none"; // prevent scroll on touch drag

  // Respect reduced motion preference
  const prefersReducedMotion = window.matchMedia(
    "(prefers-reduced-motion: reduce)"
  );

  // ── Render loop ───────────────────────────────────

  let lastW = canvas.width;
  let lastH = canvas.height;
  let animId;
  let lastTime = -1;

  function frame(t) {
    animId = requestAnimationFrame(frame);
    const dt = lastTime < 0 ? 0 : (t - lastTime) / 1000;
    lastTime = t;

    // Handle resize
    const dpr = window.devicePixelRatio || 1;
    const w = Math.floor(canvas.clientWidth * dpr);
    const h = Math.floor(canvas.clientHeight * dpr);
    if (w !== lastW || h !== lastH) {
      canvas.width = w;
      canvas.height = h;
      depthTexture.destroy();
      depthTexture = device.createTexture({
        size: [w, h],
        format: "depth24plus",
        usage: GPUTextureUsage.RENDER_ATTACHMENT,
      });
      lastW = w;
      lastH = h;
    }

    const aspect = canvas.width / canvas.height;

    // Apply drag deltas as screen-space rotations (left-multiply)
    if (pendingDx !== 0 || pendingDy !== 0) {
      const ry = mat4RotateY(pendingDx);
      const rx = mat4RotateX(pendingDy);
      mat4Mul(ry, rx, _mulTmp);
      modelRotation = mat4Mul(_mulTmp, modelRotation);
      pendingDx = 0;
      pendingDy = 0;
    }

    // Auto-rotate: gentle Y spin when not dragging (respects reduced motion)
    if (!dragging && !prefersReducedMotion.matches) {
      const autoRot = mat4RotateY(dt * 0.4);
      modelRotation = mat4Mul(autoRot, modelRotation);
    }

    const proj = mat4Perspective(Math.PI / 4, aspect, 0.1, 100);
    const view = mat4LookAt([0, 1.5, 5], [0, 0, 0], [0, 1, 0]);
    const model = modelRotation;
    mat4Mul(view, model, _mulTmp);
    const mvp = mat4Mul(proj, _mulTmp);

    _uniformData.set(mvp, 0);
    _uniformData.set(model, 16);
    _uniformData[32] = 0.5; _uniformData[33] = 0.7; _uniformData[34] = 1.0; // light dir
    _uniformData[36] = 0; _uniformData[37] = 1.5; _uniformData[38] = 5;     // eye pos
    device.queue.writeBuffer(uniformBuffer, 0, _uniformData);

    const encoder = device.createCommandEncoder();
    const pass = encoder.beginRenderPass({
      colorAttachments: [
        {
          view: context.getCurrentTexture().createView(),
          loadOp: "clear",
          storeOp: "store",
          clearValue: { r: 0.051, g: 0.067, b: 0.09, a: 1 },
        },
      ],
      depthStencilAttachment: {
        view: depthTexture.createView(),
        depthLoadOp: "clear",
        depthStoreOp: "store",
        depthClearValue: 1.0,
      },
    });

    pass.setPipeline(pipeline);
    pass.setBindGroup(0, bindGroup);
    pass.setVertexBuffer(0, vertexBuffer);
    pass.setIndexBuffer(indexBuffer, "uint16");
    pass.drawIndexed(36);
    pass.end();

    device.queue.submit([encoder.finish()]);
  }

  animId = requestAnimationFrame(frame);

  return function cleanup() {
    cancelAnimationFrame(animId);
    canvas.removeEventListener("pointerdown", onPointerDown);
    canvas.removeEventListener("pointermove", onPointerMove);
    canvas.removeEventListener("pointerup", onPointerUp);
    canvas.removeEventListener("pointercancel", onPointerUp);
    canvas.removeEventListener("pointerleave", onPointerUp);
    vertexBuffer.destroy();
    indexBuffer.destroy();
    uniformBuffer.destroy();
    texture.destroy();
    depthTexture.destroy();
    device.destroy();
  };
}
