import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'prism_engine.dart';

class PrismRenderView extends StatelessWidget {
  final PrismEngine engine;

  const PrismRenderView({
    super.key,
    required this.engine,
  });

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      // Android: render loop is driven by Choreographer natively.
      // The engine handle is not used for creation — show unconditionally.
      return AndroidView(
        viewType: 'engine.prism.flutter/render_view_native',
        creationParams: {'engineHandle': engine.handle},
        creationParamsCodec: const StandardMessageCodec(),
        onPlatformViewCreated: _onPlatformViewCreated,
      );
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      final handle = engine.handle;
      // Defer creation until initialize() has supplied a valid handle so that
      // prism_attach_metal_layer is called with the correct engine pointer.
      if (handle == 0) return const SizedBox.shrink();
      return UiKitView(
        viewType: 'engine.prism.flutter/render_view',
        creationParams: {'engineHandle': handle},
        creationParamsCodec: const StandardMessageCodec(),
      );
    } else if (defaultTargetPlatform == TargetPlatform.macOS) {
      final handle = engine.handle;
      // Same deferred-creation guard as iOS.
      if (handle == 0) return const SizedBox.shrink();
      return AppKitView(
        viewType: 'engine.prism.flutter/render_view',
        // Pass the native engine handle so the Swift platform view can call
        // prism_attach_metal_layer / prism_render_frame via the C API.
        creationParams: {'engineHandle': handle},
        creationParamsCodec: const StandardMessageCodec(),
      );
    }
    return const Center(
      child: Text('Prism: render view not yet available on this platform'),
    );
  }

  // Android readiness: the native PrismSurface initialises itself when the
  // SurfaceHolder is available (surfaceCreated callback). The channel backend
  // returns isRendererReady == true immediately, so the Dart overlay hides
  // without needing a platform-view-created signal here.
  void _onPlatformViewCreated(int id) {}
}
