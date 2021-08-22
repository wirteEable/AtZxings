package cn.at.pwb.ext

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.LocationManager
import android.os.PowerManager
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import cn.at.pwb.App
import cn.at.pwb.utils.Permission
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.hjq.permissions.OnPermission
import com.hjq.permissions.XXPermissions
import com.socks.library.KLog
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * @author  pengwb
 * @date 2018/11/17
 */
private class NotNullSingleValue<T> : ReadWriteProperty<Any?, T> {

    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: throw IllegalStateException("not initialized")
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (this.value == null) {
            this.value = value
        } else {
            throw IllegalStateException("already initialized")
        }
    }
}

fun <T> notNullSingleValue(): ReadWriteProperty<Any?, T> = NotNullSingleValue()

/**
 * 获取颜色
 * */
fun Context.getCompatColor(color: Int) = ContextCompat.getColor(this, color)

fun Context.isGpsOpen(): Boolean {
    val locationManager: LocationManager =
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
    val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
    val network =
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    return gps || network
}


/**
 * inflater
 * */
val Context.inflater: LayoutInflater get() = LayoutInflater.from(this)

fun Context.inflateLayout(
    @LayoutRes layoutId: Int,
    parent: ViewGroup? = null,
    attachToRoot: Boolean = false
) = inflater.inflate(layoutId, parent, attachToRoot)


//根据手机的分辨率从 px(像素) 的单位 转成为 dp
fun Int.px2dp(): Int {
    val scale = App.instance.resources.displayMetrics.density
    return (this / scale + 0.5f).toInt()
}

/**
 * 获取当天的最后时间
 *
 * @param date
 * @return
 */
fun Date.getDayEndTime(): Long {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar[Calendar.HOUR_OF_DAY] = 23
    calendar[Calendar.MINUTE] = 59
    calendar[Calendar.SECOND] = 59
    return calendar.timeInMillis
}


/**
 * 隐藏输入法
 * */
fun Activity.hideSoftInput() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
}

/**
 * 动态隐藏软键盘
 *
 * @param context 上下文
 * @param view    视图
 */
fun hideSoftInput(context: Context, view: View) {
    view.clearFocus()
    val imm =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            ?: return
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

/**
 * 根据EditText所在坐标和用户点击的坐标相对比，来判断是否隐藏键盘
 */
fun isShouldHideKeyboard(v: View?, event: MotionEvent): Boolean {
    if (v != null && v is EditText) {
        val l = intArrayOf(0, 0)
        v.getLocationInWindow(l)
        val left = l[0]
        val top = l[1]
        val bottom = top + v.getHeight()
        val right = left + v.getWidth()
        return !(event.rawX > left && event.rawX < right && event.rawY > top && event.rawY < bottom)
    }
    return false
}


fun Fragment.hideSoftInput() {
    this?.hideSoftInput()
}

/**
 * logTagName 获取
 * */
fun Any.getStringTag(): String {
    return this.javaClass.name
}


/**
 * 当前版本名称
 */
fun packageVersionName(): String {
    val manager = App.instance.packageManager
    var name: String = ""
    try {
        val info = manager.getPackageInfo(App.instance.packageName, 0)
        name = info.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return name
}


/**
 * 当前版本号
 *
 * @param context
 */
fun packageVersionCode(): Int {
    val manager = App.instance.packageManager
    var code = 0
    try {
        val info = manager.getPackageInfo(App.instance.packageName, 0)
        code = info.versionCode
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return code
}

fun Context.isAvilibleApk(packageName: String): Boolean {
    var isAvilible = false
    val pinfo: List<PackageInfo> = packageManager?.getInstalledPackages(0) as List<PackageInfo>
    for (p: PackageInfo in pinfo) {
        if (p.packageName == packageName) {
            isAvilible = true
            break
        }
    }
    return isAvilible
}

fun SimpleDateFormat.getFormatTime(time: Long): String {
    return this.format(Date(time))
}

fun String.isPhoneNum(): Boolean {
    val REGEX_MOBILE_EXACT =
        "^((13[0-9])|(14[5,7])|(15[0-3,5-9])|(16[6])|(17[0,1,3,5-8])|(18[0-9])|(19[1,8,9]))\\d{8}$"
    return this.isNotEmpty() && Pattern.matches(REGEX_MOBILE_EXACT, this)
}

fun String.isHttpLink(): Boolean {
    val REGEX_HTTPLINK_EXACT =
        "((http|ftp|https)://)(([a-zA-Z0-9\\._-]+\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(:[0-9]{1,4})*(/[a-zA-Z0-9\\&%_\\./-~-]*)?|(([a-zA-Z0-9\\._-]+\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(:[0-9]{1,4})*(/[a-zA-Z0-9\\&%_\\./-~-]*)?"
    return this.isNotEmpty() && Pattern.matches(REGEX_HTTPLINK_EXACT, this)
}

/**
 * 验证函数优化版
 */
fun isEmail(email: String): Boolean {
    if (null == email || "" == email) {
        return false
    }
    val emailPattern =
        Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*")
    val matcher: Matcher = emailPattern.matcher(email)
    return matcher.matches()
}

/**
 * 获取屏幕宽高（px）
 * */
val Context.screenWidthPx: Int get() = displayMetrics().widthPixels
val Context.screenHeightPy: Int get() = displayMetrics().heightPixels

fun Context.displayMetrics(): DisplayMetrics {
    return resources.displayMetrics
}

fun Float.dp2px() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this,
    Resources.getSystem().displayMetrics
)

fun Int.dp2px() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics
).toInt()

fun dp2px(context: Context, dpValue: Int): Int {
    return (dpValue * context.resources.displayMetrics.density + 0.5).toInt()
}

/**
 * 聊天界面的时间格式
 *
 * @param timestamp 时间戳
 * @return
 */
fun Long.chatMsgTimeFormat(): String? {
    val pattern: String
    val now = Calendar.getInstance()
    val base = Calendar.getInstance()
    base.timeInMillis = this
    val diffYear = now[Calendar.YEAR] - base[Calendar.YEAR]
    val diffMonth = now[Calendar.MONTH] - base[Calendar.MONTH]
    val diffDate = (now[Calendar.DAY_OF_YEAR]
            - base[Calendar.DAY_OF_YEAR])
    pattern = if (diffYear > 0) {
        "yyyy/MM/dd HH:mm"
    } else if (diffMonth > 0) {
        "MM/dd HH:mm"
    } else {
        if (diffDate == 0) {
            "HH:mm"
        } else if (diffDate == 1) {
            "昨天 HH:mm"
        } else if (diffDate == 2) {
            "前天 HH:mm"
        } else {
            "MM/dd HH:mm"
        }
    }
    val df = SimpleDateFormat(pattern)
    return df.format(this)
}

/**
 * 语音时间格式化
 */
fun Long.voiceTime(): String {
    val second = this % 60
    val minuteTemp = this / 60
    val s = if (second >= 10) second.toString() + "" else "0$second"
    return if (minuteTemp > 0) {
        val minute = minuteTemp % 60
        val s1 = if (minute >= 10) minute.toString() + "" else "0$minute"
        "$s1:$s"
    } else {
        "00:$s"
    }
}

@Suppress("UNREACHABLE_CODE")
fun Activity.checkLocation(success: () -> Unit, onError: () -> Unit?) {
    checkPermission(success, Permission.LOCATION)
}

@Suppress("UNREACHABLE_CODE")
fun Activity.checkPermission(
    success: () -> Unit,
    denied: () -> Unit,
    permission: Array<String>
) {
    if (XXPermissions.isHasPermission(this, *permission)) {
        success()
    } else {
        XXPermissions.with(this) //.constantRequest() //可设置被拒绝后继续申请，直到用户授权或者永久拒绝
            //.permission(Permission.SYSTEM_ALERT_WINDOW, Permission.REQUEST_INSTALL_PACKAGES) //支持请求6.0悬浮窗权限8.0请求安装权限
            //不指定权限则自动获取清单中的危险权限
            .permission(*permission)
            .request(object : OnPermission {
                override fun hasPermission(
                    granted: List<String>,
                    isAll: Boolean
                ) {
                    if (isAll) {
                        success()
                    } else {
                        val denied: ArrayList<String> = arrayListOf()
                        permission.forEach {
                            if (!granted.contains(it)) {
                                denied.add(it)
                            }
                        }
                        denied()
                    }
                }

                override fun noPermission(
                    denied: List<String>,
                    quick: Boolean
                ) {
                    denied()
                }
            })
    }
}

@Suppress("UNREACHABLE_CODE")
fun Activity.checkPermission(
    success: () -> Unit,
    permission: Array<String>
) {
    checkPermission(success, ({

    }), permission)
}

/**
 * 唤醒手机屏幕
 */
fun Context.wakeUp() {
    // 获取电源管理器对象
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    val screenOn = pm.isScreenOn
    if (!screenOn) {
        // 获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
        @SuppressLint("InvalidWakeLockTag") val wl = pm.newWakeLock(
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright"
        )
        wl.acquire(10 * 60 * 1000L /*10 minutes*/) // 点亮屏幕
        wl.release() // 释放
    }
}


/**
 * 是否可以截屏
 *
 * @param canScreen true 可以截屏，false 禁止截屏
 */
fun Activity.canScreenShot(canScreen: Boolean) {
    val flags = WindowManager.LayoutParams.FLAG_SECURE
    if (canScreen) {
        this.window.clearFlags(flags)
    } else {
        this.window.addFlags(flags)
    }
}

fun Context.checkPackInfo(packName: String): Boolean {
    return if (TextUtils.isEmpty(packName)) {
        false
    } else try {
        val info: ApplicationInfo = this.packageManager
            .getApplicationInfo(packName, PackageManager.GET_UNINSTALLED_PACKAGES)
        return null != info
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        false
    }
}

fun Context.getPackageContext(packageName: String): Context? {
    var pkgContext: Context? = null
    if (this.packageName == packageName) {
        pkgContext = this
    } else {
        // 创建第三方应用的上下文环境
        try {
            pkgContext = this.createPackageContext(
                packageName, Context.CONTEXT_IGNORE_SECURITY
                        or Context.CONTEXT_INCLUDE_CODE
            )
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }
    return pkgContext
}

@SuppressLint("WrongConstant")
fun Context.getAppOpenIntentByPackageName(
    packageName: String
): Intent? {
    //Activity完整名
    var mainAct: String? = null
    //根据包名寻找
    val pkgMag = this.packageManager
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    intent.flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK
    val list: List<ResolveInfo> = pkgMag.queryIntentActivities(
        intent,
        PackageManager.GET_ACTIVITIES
    )
    for (element in list) {
        val info: ResolveInfo = element
        if (info.activityInfo.packageName == packageName) {
            mainAct = info.activityInfo.name
            KLog.e("sgl", "mainAct========${mainAct}")
            break
        }
    }
    if (TextUtils.isEmpty(mainAct)) {
        return null
    }
    intent.component = ComponentName(packageName, mainAct!!)
    return intent
}


fun Context.isAppInstalled(packageName: String): Boolean {
    val manager: PackageManager = this.packageManager
    return manager.getLaunchIntentForPackage(packageName) != null
}

/**
 * 生成二维码图片
 *
 * @param text
 * @param w
 * @param h
 * @param logo
 * @return
 */
fun Context.createQrImage(
    text: String?,
    w: Int,
    h: Int,
    logo: Bitmap?
): Bitmap? {
    if (TextUtils.isEmpty(text)) {
        return null
    }
    try {
        val scaleLogo = getScaleLogo(logo, w, h)
        var offsetX = w / 2
        var offsetY = h / 2
        var scaleWidth = 0
        var scaleHeight = 0
        if (scaleLogo != null) {
            scaleWidth = scaleLogo.width
            scaleHeight = scaleLogo.height
            offsetX = (w - scaleWidth) / 2
            offsetY = (h - scaleHeight) / 2
        }
        val hints =
            Hashtable<EncodeHintType, Any?>()
        hints[EncodeHintType.CHARACTER_SET] = "utf-8"
        //容错级别
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        //设置空白边距的宽度
        hints[EncodeHintType.MARGIN] = 0
        val bitMatrix = QRCodeWriter()
            .encode(text, BarcodeFormat.QR_CODE, w, h, hints)
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (x >= offsetX && x < offsetX + scaleWidth && y >= offsetY && y < offsetY + scaleHeight) {
                    var pixel = scaleLogo!!.getPixel(x - offsetX, y - offsetY)
                    if (pixel == 0) {
                        pixel = if (bitMatrix[x, y]) {
                            -0x1000000
                        } else {
                            -0x1
                        }
                    }
                    pixels[y * w + x] = pixel
                } else {
                    if (bitMatrix[x, y]) {
                        pixels[y * w + x] = -0x1000000
                    } else {
                        pixels[y * w + x] = -0x1
                    }
                }
            }
        }
        val bitmap = Bitmap.createBitmap(
            w, h,
            Bitmap.Config.ARGB_8888
        )
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    } catch (e: WriterException) {
        e.printStackTrace()
    }
    return null
}

fun getScaleLogo(
    logo: Bitmap?,
    w: Int,
    h: Int
): Bitmap? {
    if (logo == null) {
        return null
    }
    val matrix = Matrix()
    val scaleFactor =
        Math.min(w * 1.0f / 5 / logo.width, h * 1.0f / 5 / logo.height)
    matrix.postScale(scaleFactor, scaleFactor)
    return Bitmap.createBitmap(
        logo,
        0,
        0,
        logo.width,
        logo.height,
        matrix,
        true
    )
}


fun Context.toast(sId: Int) {
    Toast.makeText(this, getString(sId), Toast.LENGTH_SHORT).show()
}

fun Context.toast(s: String) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}

