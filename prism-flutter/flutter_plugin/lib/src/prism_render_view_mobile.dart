import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
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
      return AndroidView(
        viewType: 'engine.prism.flutter/render_view',
        onPlatformViewCreated: _onPlatformViewCreated,
      );
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      return UiKitView(
        viewType: 'engine.prism.flutter/render_view',
        onPlatformViewCreated: _onPlatformViewCreated,
      );
    }
    return const Center(
      child: Text('Prism: Platform not supported for rendering view'),
    );
  }

  void _onPlatformViewCreated(int id) {
    // Platform view created, engine will attach its rendering surface
  }
}
