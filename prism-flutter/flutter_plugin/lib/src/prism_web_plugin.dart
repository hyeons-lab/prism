import 'dart:async';
import 'dart:convert';
import 'dart:js_interop';

import 'package:web/web.dart' as web;

/// JS interop bindings to the Prism WASM module's exported functions.
@JS('prismInit')
external void _prismInit(String canvasId);

@JS('prismSetRotationSpeed')
external void _prismSetRotationSpeed(double speed);

@JS('prismTogglePause')
external void _prismTogglePause();

@JS('prismSetCubeColor')
external void _prismSetCubeColor(double r, double g, double b);

@JS('prismGetState')
external String _prismGetState();

@JS('prismIsInitialized')
external bool _prismIsInitialized();

@JS('prismShutdown')
external void _prismShutdown();

/// Web implementation of PrismEngine that calls WASM-exported JS functions
/// instead of using platform method channels.
class PrismWebEngine {
  static bool _wasmLoaded = false;

  /// Load the Kotlin/WASM module and expose its @JsExport functions globally.
  ///
  /// [moduleUrl] is the path to the `.mjs` entry point relative to the web root
  /// (e.g., 'prism-flutter.mjs'). The WASM binary must be co-located.
  static Future<void> ensureWasmLoaded(String moduleUrl) async {
    if (_wasmLoaded) return;

    final completer = Completer<void>();

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
        const names = ["prismInit", "prismSetRotationSpeed", "prismTogglePause",
                        "prismSetCubeColor", "prismGetState", "prismIsInitialized",
                        "prismShutdown"];
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
      cleanup();
      if (!completer.isCompleted) completer.complete();
    }).toJS;
    errorListener = ((web.Event e) {
      cleanup();
      if (!completer.isCompleted) {
        completer
            .completeError('Failed to load Prism WASM module: $moduleUrl');
      }
    }).toJS;

    web.window.addEventListener('prism-wasm-ready', readyListener);
    web.window.addEventListener('prism-wasm-error', errorListener);

    web.document.head!.appendChild(script);
    return completer.future;
  }

  static void init(String canvasId) => _prismInit(canvasId);

  static Future<void> setRotationSpeed(double speed) async =>
      _prismSetRotationSpeed(speed);

  static Future<void> togglePause() async => _prismTogglePause();

  static Future<void> setCubeColor(double r, double g, double b) async =>
      _prismSetCubeColor(r, g, b);

  static Future<Map<String, dynamic>> getState() async {
    final json = _prismGetState();
    return jsonDecode(json) as Map<String, dynamic>;
  }

  static Future<bool> isInitialized() async => _prismIsInitialized();

  static Future<void> shutdown() async => _prismShutdown();
}
