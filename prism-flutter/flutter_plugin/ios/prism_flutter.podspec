Pod::Spec.new do |s|
  s.name             = 'prism_flutter'
  s.version          = '0.1.0'
  s.summary          = 'Flutter plugin for the Prism 3D engine'
  s.homepage         = 'https://github.com/hyeons-lab/prism'
  s.license          = { :type => 'Apache-2.0' }
  s.author           = 'Prism Contributors'
  s.source           = { :path => '.' }

  s.source_files     = 'Classes/**/*'
  s.platform         = :ios, '15.0'
  s.swift_version    = '5.9'

  s.dependency 'Flutter'

  # PrismDemo XCFramework built via:
  #   ./gradlew :prism-demo-core:assemblePrismDemoDebugXCFramework
  s.vendored_frameworks = 'Frameworks/PrismDemo.xcframework'

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386',
  }
end
