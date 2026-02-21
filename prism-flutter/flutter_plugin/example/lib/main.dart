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
  bool _isPaused = false;
  double _fps = 0.0;
  Timer? _pollTimer;
  DateTime? _lastPauseToggle;

  @override
  void initState() {
    super.initState();
    _engine.initialize();
    _pollTimer = Timer.periodic(const Duration(milliseconds: 500), (_) async {
      if (!_isInitialized) {
        final ready = await _engine.isInitialized();
        if (ready && mounted) setState(() => _isInitialized = true);
      }
      final state = await _engine.getState();
      if (mounted) {
        setState(() {
          _fps = (state['fps'] as num?)?.toDouble() ?? 0.0;
          // Debounce: don't overwrite _isPaused from poll within 750 ms of a user toggle to
          // prevent the UI from flickering back to the pre-toggle state before the engine catches up.
          final sinceToggle = _lastPauseToggle != null
              ? DateTime.now().difference(_lastPauseToggle!)
              : const Duration(seconds: 999);
          if (sinceToggle > const Duration(milliseconds: 750)) {
            _isPaused = (state['isPaused'] as bool?) ?? false;
          }
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
            child: PrismRenderView(engine: _engine),
          ),
          // FPS indicator — top-right corner.
          Positioned(
            top: 16,
            right: 16,
            child: _FpsChip(fps: _fps),
          ),
          // Pause/resume button — bottom center.
          Positioned(
            bottom: 32,
            left: 0,
            right: 0,
            child: Center(
              child: FilledButton.icon(
                onPressed: () {
                  setState(() {
                    _isPaused = !_isPaused;
                    _lastPauseToggle = DateTime.now();
                  });
                  _engine.togglePause();
                },
                icon: Icon(_isPaused ? Icons.play_arrow : Icons.pause),
                label: Text(_isPaused ? 'Resume' : 'Pause'),
              ),
            ),
          ),
          // Loading overlay — shown while the engine initializes.
          if (!_isInitialized)
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
