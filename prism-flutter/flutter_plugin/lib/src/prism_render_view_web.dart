import 'dart:ui_web' as ui_web;

import 'package:flutter/material.dart';
import 'package:web/web.dart' as web;

import 'prism_engine.dart';
import 'prism_web_plugin.dart';

class PrismRenderView extends StatefulWidget {
  final PrismEngine engine;

  const PrismRenderView({
    super.key,
    required this.engine,
  });

  @override
  State<PrismRenderView> createState() => _PrismRenderViewState();
}

class _PrismRenderViewState extends State<PrismRenderView> {
  static int _nextId = 0;
  late final String _canvasId;
  late final String _viewType;
  bool _initialized = false;
  web.HTMLCanvasElement? _canvas;

  @override
  void initState() {
    super.initState();
    final viewId = _nextId++;
    _canvasId = 'prism-canvas-$viewId';
    _viewType = 'prism-render-view-$viewId';

    ui_web.platformViewRegistry.registerViewFactory(_viewType, (int id) {
      final canvas =
          web.document.createElement('canvas') as web.HTMLCanvasElement;
      canvas.id = _canvasId;
      canvas.style.width = '100%';
      canvas.style.height = '100%';
      _canvas = canvas;
      return canvas;
    });
  }

  Future<void> _initEngine(int width, int height) async {
    if (_initialized) return;
    _initialized = true;

    final canvas = _canvas;
    if (canvas != null) {
      canvas.width = width;
      canvas.height = height;
    }

    widget.engine.attachCanvas(_canvasId);
    await PrismWebEngine.ensureWasmLoaded('prism-flutter.mjs');
    PrismWebEngine.init(_canvasId);
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth.toInt();
        final height = constraints.maxHeight.toInt();
        return HtmlElementView(
          viewType: _viewType,
          onPlatformViewCreated: (_) => _initEngine(
            width > 0 ? width : 800,
            height > 0 ? height : 600,
          ),
        );
      },
    );
  }

  // Shutdown is NOT called here — the PrismEngine owner (parent widget) is
  // responsible for calling engine.shutdown() in its own dispose().
}
