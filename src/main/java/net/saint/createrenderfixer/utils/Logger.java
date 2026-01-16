package net.saint.createrenderfixer.utils;

import org.apache.logging.log4j.LogManager;

import net.saint.createrenderfixer.Mod;

public final class Logger {

	// Properties

	private final org.apache.logging.log4j.Logger delegate;
	private final String prefix;

	// Init

	private Logger(String name) {
		this.delegate = LogManager.getLogger(name);
		this.prefix = "[" + Mod.MOD_NAME + "] ";
	}

	public static Logger create(String name) {
		return new Logger(name);
	}

	public static Logger create(Class<?> type) {
		return new Logger(type.getSimpleName());
	}

	// Logging

	public void trace(String message, Object... params) {
		withEnabledLogging(() -> delegate.trace(prefix + message, params));

	}

	public void debug(String message, Object... params) {
		withEnabledLogging(() -> delegate.debug(prefix + message, params));
	}

	public void info(String message, Object... params) {
		withEnabledLogging(() -> delegate.info(prefix + message, params));
	}

	public void warn(String message, Object... params) {
		withEnabledLogging(() -> delegate.warn(prefix + message, params));
	}

	public void error(String message, Object... params) {
		withEnabledLogging(() -> delegate.error(prefix + message, params));
	}

	public void error(String message, Throwable throwable) {
		withEnabledLogging(() -> delegate.error(prefix + message, throwable));
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
}
