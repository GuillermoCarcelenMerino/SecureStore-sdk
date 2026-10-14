# R8/ProGuard rules shipped inside the SDK's AAR and applied automatically to every app that
# uses it (consumerProguardFiles in securestore/build.gradle.kts).
#
# Currently no rules are needed:
# - The SDK does not use reflection, serialization by class name or JNI, so R8 can safely rename
#   and remove whatever the app does not call. The public API the app calls is kept by that usage.
# - Android Keystore (java.security / javax.crypto / android.security.keystore) belongs to the
#   platform, not to the APK, so R8 never touches it.
# - DataStore and kotlinx.coroutines ship their own consumer rules in their AARs.
#
# Checked by building the sample app with R8 enabled (:app:assembleRelease).
# Add rules here only if a future change introduces reflection (for example, serializing objects
# by class name).
