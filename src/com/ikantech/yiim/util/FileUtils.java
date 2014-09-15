package com.ikantech.yiim.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;

import com.ikantech.support.util.YiBase64;
import com.ikantech.support.util.YiFileUtils;

public class FileUtils {
	private String mRootDir = null;

	private static FileUtils mFileUtils = null;

	public static FileUtils getInstance() {
		if (mFileUtils == null) {
			mFileUtils = new FileUtils();
		}
		return mFileUtils;
	}

	private FileUtils() {
		mRootDir = YiFileUtils.getStorePath() + "yiim/";
	}
	
	public String getStoreRootPath() {
		return mRootDir;
	}

	@SuppressLint("SimpleDateFormat")
	public synchronized String storeAudioFile(String user, String source)
			throws Exception {
		BufferedOutputStream outputStream = null;
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
					"yyyy_MM_dd_HH_mm_ss_SSS");
			String filename = mRootDir + StringUtils.escapeUserHost(user)
					+ File.separatorChar + "audio/";

			File file = new File(filename);
			if (!file.exists()) {
				file.mkdirs();
			}

			filename += simpleDateFormat.format(Calendar.getInstance()
					.getTime());
			filename += ".ik";

			byte[] buffer = YiBase64.decode(source.getBytes("ASCII"));

			file = new File(filename);
			if (!file.exists()) {
				file.createNewFile();
			}
			outputStream = new BufferedOutputStream(new FileOutputStream(
					filename));
			outputStream.write(buffer);
			return filename;
		} catch (Exception e) {
			throw e;
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
					outputStream = null;
				} catch (Exception e2) {
				}
			}
		}
	}

	/**
	 * 从用户相册中选择照片
	 */
	public static void doChoicePhoto(Activity activty, int requestCode) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("image/*");
		intent.putExtra("return-data", true);
		activty.startActivityForResult(intent, requestCode);
	}

	/**
	 * 从本地选择视频
	 */
	public static void doChoiceVideo(Activity activty, int requestCode) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("video/*");
		intent.putExtra("return-data", true);
		activty.startActivityForResult(intent, requestCode);
	}
}
