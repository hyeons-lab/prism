import 'dart:async';
import 'dart:js_interop';

import 'package:web/web.dart' as web;

import 'generated/prism_js_bindings.dart';

/// Web implementation of PrismEngine that calls WASM-exported JS functions
/// instead of using platform method channels.
///
/// Each engine instance is identified by a canvasId, allowing multiple
/// PrismRenderView widgets to coexist on the same page without conflicting.
class PrismWebEngine {
  static bool _wasmLoaded = false;
  static String? _loadedModuleUrl;
  static Future<void>? _wasmLoadingFuture;

  /// canvasId → engine handle returned by [prismCreateEngine].
  static final Map<String, String> _handles = {};

  /// Load the Kotlin/WASM module and expose its @JsExport functions globally.
  ///
  /// [moduleUrl] is the path to the `.mjs` entry point relative to the web root
  /// (e.g., 'prism-flutter.mjs'). The WASM binary must be co-located.
  ///
  /// Only the first call triggers loading; subsequent calls with the same URL
  /// return immediately. Concurrent callers before loading completes await the
  /// same in-flight [Future] so the module is only imported once.
  /// A call with a different URL logs a warning and returns without re-loading
  /// (hot-swap is not supported).
  static Future<void> ensureWasmLoaded(String moduleUrl) {
    if (_wasmLoaded) {
      if (_loadedModuleUrl != null && _loadedModuleUrl != moduleUrl) {
        web.console.warn(
            'Prism WASM already loaded from $_loadedModuleUrl; '
                    'ignoring request for $moduleUrl'
                .toJS);
      }
      return Future.value();
    }

    // If a load is already in flight, return the same Future so concurrent
    // callers all await the same operation rather than injecting duplicate scripts.
    if (_wasmLoadingFuture != null) {
      return _wasmLoadingFuture!;
    }

    final completer = Completer<void>();
    _wasmLoadingFuture = completer.future;

    // Build the names array from the generated list — Dart is the single
    // source of truth; no hand-written names to keep in sync.
    final namesJson = prismJsExportNames.map((n) => '"$n"').join(', ');

    // Create an inline ES module script that:
    // 1. Dynamically imports the Kotlin/WASM entry point
    // 2. Instantiates the WASM module
    // 3. Exposes @JsExport functions on window for Dart @JS() bindings
    final script =
        web.document.createElement('script') as web.HTMLScriptElement;
    script.type = 'module';
    script.text = '''
      try {
        const mod = await import("./$moduleUrl");
        if (typeof mod.default === "function") {
          await mod.default();
        }
        const names = [$namesJson];
        for (const name of names) {
          if (typeof mod[name] === "function") {
            window[name] = mod[name];
          }
        }
        window.dispatchEvent(new CustomEvent("prism-wasm-ready"));
      } catch (e) {
        console.error("Prism WASM load error:", e);
        window.dispatchEvent(new CustomEvent("prism-wasm-error",
            { detail: String(e) }));
      }
    ''';

    late final JSFunction readyListener;
    late final JSFunction errorListener;

    void cleanup() {
      web.window.removeEventListener('prism-wasm-ready', readyListener);
      web.window.removeEventListener('prism-wasm-error', errorListener);
    }

    readyListener = ((web.Event e) {
      _wasmLoaded = true;
      _loadedModuleUrl = moduleUrl;
      cleanup();
      if (!completer.isCompleted) completer.complete();
    }).toJS;
    errorListener = ((web.Event e) {
      _wasmLoadingFuture = null; // allow retry on next call
      cleanup();
      if (!completer.isCompleted) {
        completer
            .completeError('Failed to load Prism WASM module: $moduleUrl');
      }
    }).toJS;

    web.window.addEventListener('prism-wasm-ready', readyListener);
    web.window.addEventListener('prism-wasm-error', errorListener);

    // Fail-safe: if neither event fires within 15 s, complete with an error and allow retry.
    Future<void>.delayed(const Duration(seconds: 15), () {
      if (!completer.isCompleted) {
        _wasmLoadingFuture = null;
        cleanup();
        completer.completeError(
            'Prism WASM module load timed out after 15 seconds');
      }
    });

    web.document.head!.appendChild(script);
    return _wasmLoadingFuture!;
  }

  /// Create and initialize an engine instance for [canvasId].
  static void init(String canvasId) {
    final handle = prismCreateEngine('Prism', 60.0);
    prismEngineInitialize(handle);
    _handles[canvasId] = handle;
  }

  static Future<void> togglePause(String canvasId) async {
    // No direct toggle-pause in the new API — no-op until added upstream.
  }

  static Future<Map<String, dynamic>> getState(String canvasId) async {
    if (!_wasmLoaded) return {};
    final h = _handles[canvasId];
    if (h == null) return {};
    return {
      'initialized': prismEngineIsAlive(h),
      'deltaTime': prismEngineGetDeltaTime(h),
      'totalTime': prismEngineGetTotalTime(h),
    };
  }

  static Future<bool> isInitialized(String canvasId) async {
    if (!_wasmLoaded) return false;
    final h = _handles[canvasId];
    if (h == null) return false;
    return prismEngineIsAlive(h);
  }

  static Future<void> shutdown(String canvasId) async {
    final h = _handles.remove(canvasId);
    if (h != null) prismDestroyEngine(h);
  }
}
