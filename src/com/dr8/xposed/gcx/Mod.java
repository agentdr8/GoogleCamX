package com.dr8.xposed.gcx;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import android.view.KeyEvent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Mod implements IXposedHookLoadPackage {

	private static final String target = "com.google.android.GoogleCamera";
	private static final String TAG = "GCX";
	private static boolean DEBUG = false;
	private static int zoomlvl = 0;
	private static int vzoomlvl = 0;

	private static final int zoommax = 59;
	private static final int vzoommax = 59;

	private static boolean firsttime;
	private static boolean vidrecording;
	
	private static void log(String msg) {
		XposedBridge.log(TAG + ": " + msg);
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

		if (!lpparam.packageName.equals(target)) {
			return;
		} else {
			
			if (DEBUG) log("Hooked GC package, looking for classes and methods");
			
			findAndHookMethod("com.android.camera.PhotoModule", lpparam.classLoader, "initializeFirstTime", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam mparam) throws Throwable {
					firsttime = (Boolean) getBooleanField(mparam.thisObject, "mFirstTimeInitialized");
					if (DEBUG) log("mFirstTimeInitialized is " + firsttime);
				}
			});
			
			findAndHookMethod("com.android.camera.VideoModule", lpparam.classLoader, "startVideoRecording", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam mparam) throws Throwable {
					vidrecording = (Boolean) getBooleanField(mparam.thisObject, "mMediaRecorderRecording");
					if (DEBUG) log("mMediaRecorderRecording is " + firsttime);
				}
			});
			
			findAndHookMethod("com.android.camera.PhotoModule", lpparam.classLoader, "onZoomChanged", int.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam mparam) throws Throwable {
					Integer result = (Integer) mparam.getResult();
					if (DEBUG) log("our zoom result is " + result);
					zoomlvl = result;
				}
			});
			
			findAndHookMethod("com.android.camera.VideoModule", lpparam.classLoader, "onZoomChanged", int.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam mparam) throws Throwable {
					Integer result = (Integer) mparam.getResult();
					if (DEBUG) log("our video zoom result is " + result);
					vzoomlvl = result;
				}
			});
			
			findAndHookMethod("com.android.camera.PhotoModule", lpparam.classLoader, "onKeyUp", int.class, KeyEvent.class, new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam mparam) throws Throwable {
					if (DEBUG) log("Hooked onKeyUp");
					Integer paramInt = (Integer) mparam.args[0];
					
					boolean bool = true;
					switch (paramInt) {
					default:
						bool = false;
						break;
					case 24:
					case 25:
						if (!firsttime) {
							bool = false;
						} else {
							break;
						}
					case 80:
						if (firsttime) {
							callMethod(mparam.thisObject, "onShutterButtonFocus", false);
						}
						break;
					}
					mparam.setResult(bool);
					return bool;
				}
			});

			findAndHookMethod("com.android.camera.VideoModule", lpparam.classLoader, "onKeyUp", int.class, KeyEvent.class, new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam mparam) throws Throwable {
					if (DEBUG) log("Hooked video onKeyUp");
					Integer paramInt = (Integer) mparam.args[0];
					
					boolean bool = true;
					switch (paramInt) {
					default:
						bool = false;
						break;
					case 27:
						callMethod(mparam.thisObject, "onShutterButtonClick");
						bool = true;
						break;
					case 82:
						bool = vidrecording;
					}
					mparam.setResult(bool);
					return bool;
				}
			});

			findAndHookMethod("com.android.camera.PhotoModule", lpparam.classLoader, "onKeyDown", int.class, KeyEvent.class, new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam mparam) throws Throwable {
					if (DEBUG) log("Hooked onKeyDown");
					Integer paramInt = (Integer) mparam.args[0];
					KeyEvent paramKey = (KeyEvent) mparam.args[1];
					
					boolean bool = true;
					switch (paramInt) {
					default:
						bool = false;
						break;
					case 23:
						if ((firsttime) && (paramKey.getRepeatCount() == 0)) {
							callMethod(mparam.thisObject, "onShutterButtonFocus", true);
							break;
						}
					case 24:
						if (DEBUG) log("Zoom in");
						if (zoomlvl < zoommax) {
							zoomlvl += 1;
							callMethod(mparam.thisObject, "onZoomChanged", zoomlvl);
						} else {
							zoomlvl = zoommax;
							callMethod(mparam.thisObject, "onZoomChanged", zoomlvl);
						}
						break;
					case 25:
						if (DEBUG) log("Zoom out");
						if (zoomlvl == 0) { 
							break;
						} else if (zoomlvl >= 1) {
							zoomlvl -= 1;
							callMethod(mparam.thisObject, "onZoomChanged", zoomlvl);
						}
						break;
					case 27:
						if ((firsttime) && (paramKey.getRepeatCount() == 0)) {
							callMethod(mparam.thisObject, "onShutterButtonClick");
						}
					}
					mparam.setResult(bool);
					return bool;
				}
			});

			findAndHookMethod("com.android.camera.VideoModule", lpparam.classLoader, "onKeyDown", int.class, KeyEvent.class, new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam mparam) throws Throwable {
					if (DEBUG) log("Hooked video onKeyDown");
					Integer paramInt = (Integer) mparam.args[0];
					KeyEvent paramKey = (KeyEvent) mparam.args[1];
					
					boolean bool = true;
					switch (paramInt) {
					default:
						bool = false;
						break;
					case 23:
						if (paramKey.getRepeatCount() == 0) {
							break;
						}
					case 24:
						if (DEBUG) log("Zoom video in");
						if (vzoomlvl < vzoommax) {
							vzoomlvl += 1;
							callMethod(mparam.thisObject, "onZoomChanged", vzoomlvl);
						} else {
							vzoomlvl = vzoommax;
							callMethod(mparam.thisObject, "onZoomChanged", vzoomlvl);
						}
						break;
					case 25:
						if (DEBUG) log("Zoom video out");
						if (vzoomlvl == 0) { 
							break;
						} else if (vzoomlvl >= 1) {
							vzoomlvl -= 1;
							callMethod(mparam.thisObject, "onZoomChanged", vzoomlvl);
						}
						break;
					case 27:
						if (paramKey.getRepeatCount() == 0) {
							callMethod(mparam.thisObject, "onShutterButtonClick");
						}
					case 82:
						bool = vidrecording;
						break;
					}
					mparam.setResult(bool);
					return bool;
				}
			});

		}
	}

}
