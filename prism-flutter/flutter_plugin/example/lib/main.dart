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
  double _metallic = 0.0;
  double _roughness = 0.5;
  double _envIntensity = 1.0;
  bool _isPaused = false;
  double _fps = 0.0;
  Timer? _fpsTimer;

  @override
  void initState() {
    super.initState();
    _fpsTimer = Timer.periodic(const Duration(milliseconds: 500), (_) async {
      final state = await _engine.getState();
      if (mounted) {
        setState(() => _fps = (state['fps'] as num?)?.toDouble() ?? 0.0);
      }
    });
  }

  @override
  void dispose() {
    _fpsTimer?.cancel();
    unawaited(_engine.shutdown());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // Full-screen: no AppBar, body fills the viewport.
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
          // PBR controls — semi-transparent panel at the bottom.
          Positioned(
            bottom: 0,
            left: 0,
            right: 0,
            child: _ControlPanel(
              metallic: _metallic,
              roughness: _roughness,
              envIntensity: _envIntensity,
              isPaused: _isPaused,
              onMetallic: (v) {
                setState(() => _metallic = v);
                _engine.setMetallic(v);
              },
              onRoughness: (v) {
                setState(() => _roughness = v);
                _engine.setRoughness(v);
              },
              onEnvIntensity: (v) {
                setState(() => _envIntensity = v);
                _engine.setEnvIntensity(v);
              },
              onTogglePause: () {
                setState(() => _isPaused = !_isPaused);
                _engine.togglePause();
              },
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

class _ControlPanel extends StatelessWidget {
  final double metallic;
  final double roughness;
  final double envIntensity;
  final bool isPaused;
  final ValueChanged<double> onMetallic;
  final ValueChanged<double> onRoughness;
  final ValueChanged<double> onEnvIntensity;
  final VoidCallback onTogglePause;

  const _ControlPanel({
    required this.metallic,
    required this.roughness,
    required this.envIntensity,
    required this.isPaused,
    required this.onMetallic,
    required this.onRoughness,
    required this.onEnvIntensity,
    required this.onTogglePause,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.black54,
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          _PbrSlider(
            label: 'Metallic',
            value: metallic,
            min: 0,
            max: 1,
            onChanged: onMetallic,
          ),
          _PbrSlider(
            label: 'Roughness',
            value: roughness,
            min: 0,
            max: 1,
            onChanged: onRoughness,
          ),
          _PbrSlider(
            label: 'Env IBL',
            value: envIntensity,
            min: 0,
            max: 2,
            onChanged: onEnvIntensity,
          ),
          const SizedBox(height: 4),
          FilledButton.icon(
            onPressed: onTogglePause,
            icon: Icon(isPaused ? Icons.play_arrow : Icons.pause),
            label: Text(isPaused ? 'Resume' : 'Pause'),
          ),
        ],
      ),
    );
  }
}

class _PbrSlider extends StatelessWidget {
  final String label;
  final double value;
  final double min;
  final double max;
  final ValueChanged<double> onChanged;

  const _PbrSlider({
    required this.label,
    required this.value,
    required this.min,
    required this.max,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        SizedBox(
          width: 72,
          child: Text(
            label,
            style: const TextStyle(color: Colors.white, fontSize: 13),
          ),
        ),
        Expanded(
          child: SliderTheme(
            data: SliderTheme.of(context).copyWith(
              activeTrackColor: Colors.white70,
              thumbColor: Colors.white,
              inactiveTrackColor: Colors.white24,
              overlayColor: Colors.white24,
            ),
            child: Slider(value: value, min: min, max: max, onChanged: onChanged),
          ),
        ),
        SizedBox(
          width: 36,
          child: Text(
            value.toStringAsFixed(2),
            style: const TextStyle(color: Colors.white70, fontSize: 12),
            textAlign: TextAlign.right,
          ),
        ),
      ],
    );
  }
}
