# Fork Dependencies: Why We Have Forks

Last updated: 2026-02-17

## Overview

Prism depends on `wgpu4k`, which in turn depends on `wgpu4k-native` and `webgpu-ktypes`. All three upstream libraries have critical bugs on **Android API 35+** (Android 15/16) that have not been merged upstream. We maintain forks of all three under the `hyeons-lab` GitHub org with fixes applied.

### Dependency Chain

```
prism
  └── wgpu4k (hyeons-lab/wgpu4k)
        ├── wgpu4k-native (hyeons-lab/wgpu4k-native)
        └── webgpu-ktypes (hyeons-lab/webgpu-ktypes)
```

All forks use `com.hyeons-lab` as the Maven group ID (upstream uses `io.ygdrasil`) to avoid classpath collisions.

## Fork: wgpu4k-native

**Upstream:** [wgpu4k/wgpu4k-native](https://github.com/wgpu4k/wgpu4k-native)
**Fork:** [hyeons-lab/wgpu4k-native](https://github.com/hyeons-lab/wgpu4k-native)
**Branch:** `fix/android-api35-package-relocation`

### Problem: Panama FFI shim package collision

wgpu4k-native ships JNA-backed shim classes (`MemorySegment`, `SegmentAllocator`, `ValueLayout`, `GroupLayout`, `NativeString`) under the `java.lang.foreign` package to mimic the JDK Panama FFI API on Android, where Panama isn't available.

Starting with Android API 35, the **real** `java.lang.foreign` package is present on the boot classpath. The boot classpath always takes priority over app-bundled classes, so the real `java.lang.foreign.MemorySegment` **interface** shadows the shim `MemorySegment` **class**. At runtime this causes:

```
InstantiationError: java.lang.foreign.MemorySegment
```

You cannot instantiate an interface.

### Fix

Relocate all 5 shim files from `java.lang.foreign` to `com.hyeonslab.foreign`. Update all imports in:
- `ffi/FFI.kt`
- `ffi/MemoryAllocator.android.kt`
- `Structures.android.kt`
- `generator/AndroidStructureGenerator.kt` (code generator template)

### Additional changes

- Kotlin 2.2.21 → 2.3.0, KSP updated to match
- JVM toolchain JDK 24 → 25 across all modules
- `allWarningsAsErrors` disabled — Kotlin 2.3.0 flags new warnings in generated `Structures.kt` that can't be fixed without regenerating
- Maven group changed to `com.hyeons-lab`

---

## Fork: wgpu4k

**Upstream:** [wgpu4k/wgpu4k](https://github.com/wgpu4k/wgpu4k)
**Fork:** [hyeons-lab/wgpu4k](https://github.com/hyeons-lab/wgpu4k)
**Branch:** `fix/android-api35-wgpu4k-native-snapshot`

### Problem 1: ByteBuffer.address() hidden API blocked

`Queue.native.android.kt` used reflection to get the native memory address of a direct ByteBuffer:

```kotlin
val addressMethod = ByteBuffer::class.java.getDeclaredMethod("address")
addressMethod.isAccessible = true
(addressMethod.invoke(this) as Long?)!!
```

Android API 35+ blocks access to this hidden API, throwing `RuntimeException` at runtime.

### Fix

Replace reflection with JNA's public API:

```kotlin
Pointer.nativeValue(Native.getDirectBufferPointer(this)).toULong()
```

### Problem 2: Dependency coordinates

wgpu4k depends on `io.ygdrasil:wgpu4k-native` and `io.ygdrasil:webgpu-ktypes`, which are the unfixed upstream artifacts.

### Fix

Switch all dependency coordinates to `com.hyeons-lab:*` SNAPSHOT versions that contain the fixes.

### Additional changes

- Remove explicit `WGPUInstanceBackend.Vulkan` in `androidContextRenderer()` — let wgpu auto-detect the backend
- Gradle 9.1.0 → 9.2.0
- Maven group changed to `com.hyeons-lab`

---

## Fork: webgpu-ktypes

**Upstream:** [nicemicro/webgpu-ktypes](https://github.com/nicemicro/webgpu-ktypes) (originally wgpu4k/webgpu-ktypes)
**Fork:** [hyeons-lab/webgpu-ktypes](https://github.com/hyeons-lab/webgpu-ktypes)
**Branch:** `fix/android-api35-package-relocation`

### Problem: ByteBuffer byte order silently corrupts GPU data

`ByteBuffer.allocateDirect()` defaults to **BIG_ENDIAN** byte order regardless of platform. On little-endian ARM64 Android (all modern Android hardware), every multi-byte value — floats, ints, shorts, doubles — written via `ArrayBuffer.of()` gets byte-swapped silently.

The data appears correct in the Kotlin layer but arrives garbled at the GPU when passed through wgpu's native FFI boundary. This manifests as:
- Corrupted vertex positions and normals
- Wrong colors (RGBA channels get shuffled)
- Invalid index buffer data causing rendering artifacts or crashes
- Incorrect uniform values producing wrong transforms

In practice this showed up as a **white screen** — the geometry was so garbled nothing rendered visibly.

### Fix

Add `.order(ByteOrder.nativeOrder())` after every `ByteBuffer.allocateDirect()` call in the Android `ArrayBuffer` factory methods. Affected methods:
- `allocate(sizeInBytes)`
- `of(ShortArray)`, `of(IntArray)`, `of(FloatArray)`, `of(DoubleArray)`
- `of(UShortArray)`, `of(UIntArray)`

`ByteArray` and `UByteArray` variants are unaffected (single-byte elements have no endianness).

### Additional changes

- Gradle 9.1.0 → 9.2.0
- Maven group changed to `com.hyeons-lab`

---

## Summary Table

| Repo | Fix | Runtime Error | Android API |
|------|-----|---------------|-------------|
| wgpu4k-native | Relocate `java.lang.foreign` shims → `com.hyeonslab.foreign` | `InstantiationError` | 35+ |
| wgpu4k | Replace `ByteBuffer.getDeclaredMethod("address")` with JNA | `RuntimeException` | 35+ |
| webgpu-ktypes | Add `ByteOrder.nativeOrder()` to `allocateDirect()` calls | Silent data corruption (white screen) | All (ARM64) |

## When Can We Drop the Forks?

When upstream merges equivalent fixes and publishes new releases. Track these upstream issues/PRs and periodically check. Once upstream artifacts contain the fixes, switch prism's dependency coordinates back to `io.ygdrasil:*` and remove `mavenLocal()` / `com.hyeons-lab` content filters from `settings.gradle.kts`.
