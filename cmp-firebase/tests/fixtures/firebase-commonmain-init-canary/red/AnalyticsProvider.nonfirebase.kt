// RED fixture — the PRE-FIX bug: the Measurement-Protocol tier hard-returns NoOp
// and ignores the stashed FirebaseConfig, so events never flow on jvm/desktop/web
// even when the consumer supplied an MpConfig. The canary asserts this fixture
// does NOT read the MP config field (proving the guard catches the regression).
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics

actual fun provideAnalyticsHelper(): AnalyticsHelper = NoOpAnalyticsHelper
