@file:OptIn(ExperimentalForeignApi::class)

package com.hyeonslab.prism.native

import androidNativeWindow.ANativeWindow_fromSurface
import kotlin.native.CName
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.android.JNIEnvVar
import platform.android.jobject

// Lives in the leaf source set because the androidNativeWindow cinterop is registered
// on the androidNativeArm64/X64 compilations only — shared androidNativeMain cannot see it.
@Suppress("UNUSED_PARAMETER")
@CName("Java_com_hyeonslab_prism_flutter_PrismAndroidNative_nAttachSurface")
fun jniAttachSurface(
  env: CPointer<JNIEnvVar>,
  cls: jobject,
  handle: Long,
  surface: jobject,
  w: Int,
  h: Int,
) {
  val window = ANativeWindow_fromSurface(env, surface) ?: return
  prismAttachAndroidSurface(handle, window, w, h)
}
