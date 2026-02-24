import 'dart:async';

import 'package:flutter/material.dart';
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

class _PrismDemoPageState extends State<PrismDemoPage> {
  final _engine = PrismEngine();
  bool _isInitialized = false;
  bool _isSceneReady = false;
  double _fps = 0.0;
  Timer? _pollTimer;

  static const _glbAsset = 'assets/DamagedHelmet.glb';

  @override
  void initState() {
    super.initState();
    _engine.initialize();
    // Resolve the bundle path once and attempt to load the GLB. The C API call
    // is a no-op if the Metal surface is not yet attached, so the poll timer
    // retries until isRendererReady returns true.
    PrismEngine.resolveFlutterAssetPath(_glbAsset).then((path) {
      if (path != null) _engine.loadGltfFromPath(path);
    });
    _pollTimer = Timer.periodic(const Duration(milliseconds: 500), (_) async {
      if (!_isInitialized) {
        final ready = await _engine.isInitialized();
        if (ready && mounted) setState(() => _isInitialized = true);
      }
      // Retry loadGltfFromPath until the renderer reports ready (surface may
      // not have been attached yet on the first attempt above).
      if (!_isSceneReady) {
        final sceneReady = _engine.isRendererReady;
        if (!sceneReady) {
          PrismEngine.resolveFlutterAssetPath(_glbAsset).then((path) {
            if (path != null) _engine.loadGltfFromPath(path);
          });
        } else if (mounted) {
          setState(() => _isSceneReady = true);
        }
      }
      final state = await _engine.getState();
      if (mounted) {
        setState(() {
          _fps = (state['fps'] as num?)?.toDouble() ?? 0.0;
        });
      }
    });
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    unawaited(_engine.shutdown());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          // 3D render view fills the entire screen.
          Positioned.fill(
            child: PrismRenderView(engineHandle: _engine.handle),
          ),
          // FPS indicator — top-right corner.
          Positioned(
            top: 16,
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
