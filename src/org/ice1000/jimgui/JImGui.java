package org.ice1000.jimgui;

import java.io.Closeable;

/**
 * @author ice1000
 * @since v0.1
 */
@SuppressWarnings("WeakerAccess")
public class JImGui implements AutoCloseable, Closeable {
	private long nativeObjectPtr;

	public JImGui() {
		JniLoader.load();
		nativeObjectPtr = allocateNativeObjects();
	}

	@Override
	public void close() {
		deallocateNativeObjects(nativeObjectPtr);
	}

	public void demoMainLoop() {
		demoMainLoop(nativeObjectPtr);
	}

	public boolean glfwWindowShouldClose() {
		return glfwWindowShouldClose(nativeObjectPtr);
	}

	/** @return see {@link JImGui#nativeObjectPtr} */
	private static native long allocateNativeObjects();

	/** @param nativeObjectPtr see {@link JImGui#nativeObjectPtr} */
	private static native void deallocateNativeObjects(long nativeObjectPtr);

	private static native void demoMainLoop(long nativeObjectPtr);

	private static native boolean glfwWindowShouldClose(long nativeObjectPtr);
}
