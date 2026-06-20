package com.vaka.app

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * Activity de captura del escáner QR forzada a modo portrait.
 * Hereda de CaptureActivity de ZXing y la orientación se fija en el manifest.
 */
class CaptureActivityPortrait : CaptureActivity()
