package cn.at.pwb

import android.app.Application
import cn.at.pwb.ext.notNullSingleValue
import cn.net.pwb.BuildConfig
import com.socks.library.KLog


/**
 * @author  pengwb
@date 2018/11/18
 */

open class App : Application() {


    var isGoSetInstallApk = false

    /**
     * 是否从app跳转到其他地方
     */
    var isJumpOther = false

    /**
     * 定义app状态
     */
    var appCount: Int = 0
    var isRunInBackground = false
    var channelType: Int = 0

    companion object {
        var instance by notNullSingleValue<App>()
    }

    override fun onCreate() {
        super.onCreate()
        KLog.init(BuildConfig.DEBUG)
    }


}