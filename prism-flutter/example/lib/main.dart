import 'package:flutter/material.dart';
import 'package:prism_flutter/prism_flutter.dart';
import 'package:prism_flutter/prism_sdk.dart';

void main() {
  runApp(const PrismExampleApp());
}

class PrismExampleApp extends StatelessWidget {
  const PrismExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Prism Engine Demo',
      theme: ThemeData(
        colorSchemeSeed: Colors.deepPurple,
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
  late final Engine _engine;
  bool _isPaused = false;

  @override
  void initState() {
    super.initState();
    _engine = Engine(const EngineConfig(appName: 'PrismDemo', targetFps: 60));
  }

  @override
  void dispose() {
    _engine.destroy();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Prism 3D Engine')),
      body: Column(
        children: [
          Expanded(
            child: PrismRenderView(engineHandle: _engine.handle),
          ),
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              children: [
                Text(
                  'dt: ${_engine.time.deltaTime.toStringAsFixed(4)} s  '
                  'total: ${_engine.time.totalTime.toStringAsFixed(2)} s',
                ),
                const SizedBox(height: 8),
                FilledButton.icon(
                  onPressed: () => setState(() => _isPaused = !_isPaused),
                  icon: Icon(_isPaused ? Icons.play_arrow : Icons.pause),
                  label: Text(_isPaused ? 'Resume' : 'Pause'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
