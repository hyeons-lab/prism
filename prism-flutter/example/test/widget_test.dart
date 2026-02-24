// Smoke test for the Prism Flutter example app.
import 'package:flutter_test/flutter_test.dart';

import 'package:prism_flutter_example/main.dart';

void main() {
  testWidgets('App renders without crashing', (WidgetTester tester) async {
    await tester.pumpWidget(const PrismExampleApp());
    expect(find.text('Prism 3D Engine'), findsOneWidget);
  });
}
