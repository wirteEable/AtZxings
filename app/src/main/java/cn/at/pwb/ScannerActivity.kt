package cn.at.pwb

import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowManager
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import cn.at.pwb.ext.checkPermission
import cn.at.pwb.ext.toast
import cn.at.pwb.utils.Permission
import cn.net.pwb.R
import com.google.zxing.Result
import com.google.zxing.client.result.ParsedResult
import com.mylhyl.zxing.scanner.OnScannerCompletionListener
import com.mylhyl.zxing.scanner.ScannerView

/**
 *
 * 扫一扫
 *
 * */
class ScannerActivity : AppCompatActivity(), OnScannerCompletionListener {

    lateinit var scannerView: ScannerView
    lateinit var lightBtn: CheckBox
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)


        initView()
        initListener()
    }

    private fun initListener() {
        lightBtn = findViewById(R.id.cb_light_toggle)
        lightBtn.setOnCheckedChangeListener { compoundButton, b ->
            toogleLight(b)
        }
    }

    private fun toogleLight(b: Boolean) {
        lightBtn.isChecked = b
        lightBtn.setText((if (b) R.string.scan_flashlight_off else R.string.scan_flashlight_on))
        scannerView.toggleLight(b)
    }

    private fun initView() {
        //扫描界面视图
        scannerView = findViewById(R.id.scanner_view)
        scannerView.setMediaResId(R.raw.beep)
        scannerView.setDrawText(this.getString(R.string.scan_tip), true)
        scannerView.setLaserFrameBoundColor(ContextCompat.getColor(this, R.color.teal_200))
        scannerView.setLaserLineResId(R.drawable.scan_light)
        scannerView.setOnScannerCompletionListener(this)
        scannerView.setLaserFrameCornerWidth(3)
        scannerView.setLaserFrameCornerLength(25)
        scannerView.setOnScannerCompletionListener(this)
    }

    override fun onStart() {
        super.onStart()
        this.checkPermission({}, {}, Permission.CAMERAS)
    }

    override fun onResume() {
        scannerView.onResume()
        super.onResume()
    }

    override fun onPause() {
        scannerView.onPause()
        toogleLight(false)
        super.onPause()
    }

    override fun onScannerCompletion(
        rawResult: Result?,
        parsedResult: ParsedResult?,
        barcode: Bitmap?
    ) {
        scannerView.restartPreviewAfterDelay(1000)
        if (null == rawResult || TextUtils.isEmpty(rawResult.text)) {
            toast(R.string.failure_of_qrcode_parsing)
            return
        }
        toast(rawResult.text)
    }

}