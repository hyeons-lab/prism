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
      canvas.width = 800;
      canvas.height = 600;
      return canvas;
    });
  }

  Future<void> _initEngine() async {
    if (_initialized) return;
    _initialized = true;
    await PrismWebEngine.ensureWasmLoaded('prism-flutter.mjs');
    PrismWebEngine.init(_canvasId);
  }

  @override
  Widget build(BuildContext context) {
    return HtmlElementView(
      viewType: _viewType,
      onPlatformViewCreated: (_) => _initEngine(),
    );
  }

  @override
  void dispose() {
    widget.engine.shutdown();
    super.dispose();
  }
}
