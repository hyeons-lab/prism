import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
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
      return AndroidView(
        viewType: 'engine.prism.flutter/render_view',
        onPlatformViewCreated: _onPlatformViewCreated,
      );
    } else if (defaultTargetPlatform == TargetPlatform.macOS) {
      return const AppKitView(
        viewType: 'engine.prism.flutter/render_view',
      );
    }
    return const Center(
      child: Text('Prism: render view not yet available on this platform'),
    );
  }

  void _onPlatformViewCreated(int id) {}
}
