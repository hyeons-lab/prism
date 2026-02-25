// Unit tests for the channel-backend PrismEngine (Android path).
//
// These are pure Dart unit tests — no native library, no widget framework.
// They specifically guard against regressions in the interface contract:
//
//   - Missing PrismEngineInterface members (caught at compile time by
//     `implements`, but unit tests provide an explicit, human-readable
//     failure message during flutter test).
//   - Incorrect stub values that break the demo UI (e.g. isRendererReady
//     returning false causes the loading spinner to never hide on Android).
import 'package:flutter_test/flutter_test.dart';
import 'package:prism_flutter/src/prism_engine_channel.dart' as channel;
import 'package:prism_flutter/src/prism_engine_interface.dart';

void main() {
  group('channel PrismEngine — PrismEngineInterface contract', () {
    late channel.PrismEngine engine;

    setUp(() => engine = channel.PrismEngine());

    // ── Regression: missing `int get handle` caused Android compile failure ──

    test('handle returns 0 (Android has no native handle)', () {
      expect(engine.handle, 0);
    });

    // ── Regression: isRendererReady == false kept spinner on screen forever ──

    test('isRendererReady returns true so loading overlay hides on Android',
        () {
      // Android drives the render loop natively; Flutter-level "renderer
      // ready" state is always true (no GLB loading via Flutter on Android).
      expect(engine.isRendererReady, isTrue);
    });

    // ── Sanity: remaining interface stubs return safe defaults ──

    test('fps returns 0.0', () => expect(engine.fps, 0.0));

    test('implements PrismEngineInterface', () {
      // Statically enforced by `implements`, but an explicit runtime cast
      // makes the test failure message clear if the class ever loses the
      // declaration.
      expect(engine, isA<PrismEngineInterface>());
    });
  });
}
