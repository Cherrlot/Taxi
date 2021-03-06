package com.bdstar.updatelib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * <pre>
 *     @author yangchong
 *     blog  : https://github.com/yangchong211
 *     time  : 2017/05/29
 *     desc  : 版本更新弹窗
 *     revise:
 * </pre>
 */
public class UpdateUtils {


    public static String APP_UPDATE_DOWN_APK_PATH = "apk" + File.separator + "downApk";
    private static String mApkName;
    static String getLocalApkDownSavePath(String apkName){
        mApkName = apkName;
        String saveApkPath= APP_UPDATE_DOWN_APK_PATH+ File.separator;
        String sdPath = getInnerSDCardPath();
        if (!isExistSDCard() || TextUtils.isEmpty(sdPath)) {
            ArrayList<String> sdPathList = getExtSDCardPath();
            if (sdPathList != null && sdPathList.size() > 0 && !TextUtils.isEmpty(sdPathList.get(0))) {
                sdPath = sdPathList.get(0);
            }
        }
        String saveApkDirs = sdPath+File.separator+saveApkPath;
        File file = new File(saveApkDirs);
        //判断文件夹是否存在，如果不存在就创建，否则不创建
        if (!file.exists()) {
            //通过file的mkdirs()方法创建目录中包含却不存在的文件夹
            //noinspection ResultOfMethodCallIgnored
            file.mkdirs();
        }
        saveApkPath = saveApkDirs + apkName+".apk";
        return saveApkPath;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void clearDownload(){
        if (mApkName!=null && mApkName.length()>0){
            String localApkDownSavePath = getLocalApkDownSavePath(mApkName);
            deleteFile(localApkDownSavePath);
        }
    }

    /**
     * 删除单个文件
     * @param fileName          要删除的文件的文件名
     * @return                  单个文件删除成功返回true，否则返回false
     */
    private static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                System.out.println("删除单个文件" + fileName + "成功！");
                return true;
            } else {
                System.out.println("删除单个文件" + fileName + "失败！");
                return false;
            }
        } else {
            System.out.println("删除单个文件失败：" + fileName + "不存在！");
            return false;
        }
    }

    /**
     * 判断是否有sd卡
     * @return                      是否有sd
     */
    private static boolean isExistSDCard() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }


    /**
     * 获取内置SD卡路径
     * @return                      路径
     */
    private static String getInnerSDCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    /**
     * 获取外置SD卡路径
     * @return 应该就一条记录或空
     */
    private static ArrayList<String> getExtSDCardPath() {
        ArrayList<String> lResult = new ArrayList<>();
        try {
            Runtime rt = Runtime.getRuntime();
            Process process = rt.exec("mount");
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("extSdCard")) {
                    String[] arr = line.split(" ");
                    String path = arr[1];
                    File file = new File(path);
                    if (file.isDirectory()) {
                        lResult.add(path);
                    }
                }
            }
            isr.close();
        } catch (Exception ignored) {
        }
        return lResult;
    }



    /*
     注意配置清单文件
     <provider
     android:name="android.support.v4.content.FileProvider"
     android:authorities="你的包名.fileprovider"
     android:exported="false"
     android:grantUriPermissions="true">
     <meta-data
     android:name="android.support.FILE_PROVIDER_PATHS"
     android:resource="@xml/file_paths" />
     </provider>
     */
    /**
     * 关于在代码中安装 APK 文件，在 Android N 以后，为了安卓系统为了安全考虑，不能直接访问软件
     * 需要使用 fileProvider 机制来访问、打开 APK 文件。
     * 普通安装
     * @param context                   上下文
     * @param apkPath                    path，文件路径
     */
    static boolean installNormal(Context context, String apkPath , String application_id) {
        if(apkPath==null){
            return false;
        }

        /*if (install(apkPath)){
            return true;
        }*/
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            File apkFile = new File(apkPath);
            // 由于没有在Activity环境下启动Activity,设置下面的标签
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //版本在7.0以上是不能直接通过uri访问的
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
                Uri apkUri = FileProvider.getUriForFile(context, application_id+".provider", apkFile);
                //添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                Uri uri = Uri.fromFile(apkFile);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            }
            context.startActivity(intent);
            ((Activity)context).finish();
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //1:判定app是否获取root权限
    public static boolean isRootPermission() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("ls /data/data/\n");
            os.writeBytes("exit\n");
            os.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            String result = builder.toString();
            if (result.contains("com.android.phone")) {
                return true;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    //2:安装apk
    public static boolean install(String apkPath) {
        boolean result = false;
        DataOutputStream dataOutputStream = null;
        BufferedReader errorStream = null;
        try {
            Process process = Runtime.getRuntime().exec("pm");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            String command = "install -r " + apkPath + "\n";
            dataOutputStream.write(command.getBytes(Charset.forName("utf-8")));
            dataOutputStream.flush();
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            process.waitFor();
            errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String msg = "";
            String line;
            while ((line = errorStream.readLine()) != null) {
                msg += line;
            }
            if (!msg.contains("Failure")) {
                result = true;
            }
        } catch (Exception e) {
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (errorStream != null) {
                    errorStream.close();
                }
            } catch (IOException e) {
            }
        }
        return result;
    }

    /*public boolean install(String apkPath) {
        boolean result = false;
        DataOutputStream dataOutputStream = null;
        BufferedReader errorStream = null;
        try {
            // 申请su权限
            Process process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());  
            // 执行pm install命令  
            String command = "pm install -r " + apkPath + "\n";  
            dataOutputStream.write(command.getBytes(Charset.forName("utf-8")));  
            dataOutputStream.flush();  
            dataOutputStream.writeBytes("exit\n");  
            dataOutputStream.flush();  
            process.waitFor();  
            errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));  
            String msg = "";  
            String line;  
            // 读取命令的执行结果  
            while ((line = errorStream.readLine()) != null) {  
                msg += line;  
            }  
            Log.d("TAG", "install msg is " + msg);  
            // 如果执行结果中包含Failure字样就认为是安装失败，否则就认为安装成功  
            if (!msg.contains("Failure")) {  
                result = true;  
            }  
        } catch (Exception e) {  
            Log.e("TAG", e.getMessage(), e);  
        } finally {  
            try {  
                if (dataOutputStream != null) {  
                    dataOutputStream.close();  
                }  
                if (errorStream != null) {  
                    errorStream.close();  
                }  
            } catch (IOException e) {  
                Log.e("TAG", e.getMessage(), e);  
            }  
        }  
        return result;  
    }*/

    /**
     * 下载状态
     * START            开始下载
     * UPLOADING.       下载中
     * FINISH           下载完成，可以安装
     * ERROR            下载错误
     * PAUSED           下载暂停中，继续
     */
    @Retention(RetentionPolicy.SOURCE)
    public @interface DownloadStatus {
        int START = 6;
        int UPLOADING = 7;
        int FINISH = 8;
        int ERROR = 9;
        int PAUSED = 10;
    }



}
