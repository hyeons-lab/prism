// TODO(ffibindings): export 'prism_engine_ffi.dart' for desktop once the
// native library is bundled per-platform. dart.library.ffi is true on all
// native targets (including Android/iOS), so we need a finer-grained
// condition or runtime dispatch before switching away from the channel impl.
export 'prism_engine_channel.dart'
    if (dart.library.js_interop) 'prism_engine_web.dart';
