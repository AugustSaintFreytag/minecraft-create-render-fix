package net.saint.createrenderfixer.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;

import net.saint.createrenderfixer.Mod;

public final class Logger {

	// Properties

	private final org.apache.logging.log4j.Logger delegate;

	// Init

	private Logger(String name) {
		this.delegate = LogManager.getLogger(name);
	}

	public static Logger create(String name) {
		return new Logger(name);
	}

	public static Logger create(Class<?> type) {
		return new Logger(type.getSimpleName());
	}

	// Logging

	public void trace(String message, Object... params) {
		withEnabledLogging(() -> delegate.trace(getPrefix() + message, params));
	}

	public void debug(String message, Object... params) {
		withEnabledLogging(() -> delegate.debug(getPrefix() + message, params));
	}

	public void info(String message, Object... params) {
		withEnabledLogging(() -> delegate.info(getPrefix() + message, params));
	}

	public void warn(String message, Object... params) {
		withEnabledLogging(() -> delegate.warn(getPrefix() + message, params));
	}

	public void error(String message, Object... params) {
		withEnabledLogging(() -> delegate.error(getPrefix() + message, params));
	}

	public void error(String message, Throwable throwable) {
		withEnabledLogging(() -> delegate.error(getPrefix() + message, throwable));
	}

	// Content

	private String getPrefix() {
		return getModPrefix() + " " + getEnvironmentPrefix() + " ";
	}

	private String getEnvironmentPrefix() {
		return "[" + getCurrentEnvironmentLabel() + "]";
	}

	private String getModPrefix() {
		return "[" + Mod.MOD_NAME + "]";
	}

	private static final Map<String, String> THREAD_NAME_MAP = new HashMap<>() {
		{
			this.put("Render thread", "Client");
			this.put("Netty Local Client IO", "Client/Network");
			this.put("Server thread", "Server");
			this.put("Netty Server IO", "Server (Network)");
		}
	};

	private static String getCurrentEnvironmentLabel() {
		var threadName = Thread.currentThread().getName();
		var remappedName = THREAD_NAME_MAP.computeIfAbsent(threadName, (key) -> ellipsizedLabel(key));

		return remappedName;
	}

	// Check

	private boolean isLoggingEnabled() {
		return Mod.CONFIG == null || Mod.CONFIG.enableLogging;
	}

	private void withEnabledLogging(Runnable action) {
		if (!isLoggingEnabled()) {
			return;
		}

		action.run();
	}

	// Utility

	private static String ellipsizedLabel(String label) {
		return ellipsizedLabel(label, 20);
	}

	private static String ellipsizedLabel(String label, int maxLength) {
		if (label.length() <= maxLength) {
			return label;
		}

		int leftLength = maxLength / 2 - 1;
		int rightLength = maxLength - leftLength - 1;

		return label.substring(0, leftLength) + "…" + label.substring(label.length() - rightLength);
	}

}
