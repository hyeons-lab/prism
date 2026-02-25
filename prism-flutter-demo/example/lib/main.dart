import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:prism_flutter/prism_flutter.dart';

void main() {
  runApp(const PrismDemoApp());
}

class PrismDemoApp extends StatelessWidget {
  const PrismDemoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Prism Flutter Demo',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorSchemeSeed: Colors.blue,
        useMaterial3: true,
      ),
      home: const PrismDemoPage(),
    );
  }
}

class PrismDemoPage extends StatefulWidget {
  const PrismDemoPage({super.key});

  @override
  State<PrismDemoPage> createState() => _PrismDemoPageState();
}

class _PrismDemoPageState extends State<PrismDemoPage>
    with SingleTickerProviderStateMixin {
  final _engine = PrismEngine();
  bool _isSceneReady = false;
  double _fps = 0.0;
  late final Ticker _ticker;

  static const _glbAsset = 'assets/DamagedHelmet.glb';

  @override
  void initState() {
    super.initState();
    unawaited(_setup());
    _ticker = createTicker(_onFrame)..start();
  }

  Future<void> _setup() async {
    await _engine.initialize();
    // Trigger a rebuild so PrismRenderView is created with the now-valid engine
    // handle. Without this the UiKitView/AppKitView is built with handle=0,
    // setupMtkView() is skipped, and prism_attach_metal_layer is never called.
    if (mounted) setState(() {});
    final path = await PrismEngine.resolveFlutterAssetPath(_glbAsset);
    if (path != null) _engine.loadGltfFromPath(path);
  }

  void _onFrame(Duration elapsed) {
    if (!mounted) return;
    final newFps = _engine.fps;
    final nowReady = _isSceneReady || _engine.isRendererReady;
    if (nowReady != _isSceneReady || (nowReady && (newFps - _fps).abs() >= 1.0)) {
      setState(() {
        _isSceneReady = nowReady;
        _fps = newFps;
      });
    }
  }

  @override
  void dispose() {
    _ticker.dispose();
    unawaited(_engine.shutdown());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final topPad = MediaQuery.of(context).padding.top;
    return Scaffold(
      body: Stack(
        children: [
          // 3D render view — only added once the engine handle is valid so that
          // the platform view (UiKitView / AppKitView) is created with the
          // correct handle and setupMtkView() is called on creation.
          if (_engine.handle != 0)
            Positioned.fill(
              child: PrismRenderView(engineHandle: _engine.handle),
            ),
          // FPS indicator — top-right corner, below Dynamic Island / status bar.
          Positioned(
            top: topPad + 8,
            right: 16,
            child: _FpsChip(fps: _fps),
          ),
          // Loading overlay — shown until the 3D scene is ready to render.
          if (!_isSceneReady)
            const Positioned.fill(
              child: ColoredBox(
                color: Colors.black,
                child: Center(
                  child: CircularProgressIndicator(),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _FpsChip extends StatelessWidget {
  final double fps;
  const _FpsChip({required this.fps});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.black54,
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        '${fps.toStringAsFixed(0)} fps',
        style: const TextStyle(
          color: Colors.white,
          fontSize: 13,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
