package net.saint.createrenderfixer.utils;

import org.apache.logging.log4j.LogManager;

import net.saint.createrenderfixer.Mod;

/**
 * Lightweight wrapper around Log4j that automatically prefixes all log messages with the mod name
 * so callers don't have to repeat it.
 */
public final class Logger {

	private final org.apache.logging.log4j.Logger delegate;
	private final String prefix;

	private Logger(String name) {
		this.delegate = LogManager.getLogger(name);
		this.prefix = "[" + Mod.MOD_NAME + "] ";
	}

	/**
	 * Create a logger with the provided name.
	 */
	public static Logger create(String name) {
		return new Logger(name);
	}

	/**
	 * Create a logger named after a class (simple name).
	 */
	public static Logger create(Class<?> type) {
		return new Logger(type.getSimpleName());
	}

	public void trace(String message, Object... params) {
		delegate.trace(prefix + message, params);
	}

	public void debug(String message, Object... params) {
		delegate.debug(prefix + message, params);
	}

	public void info(String message, Object... params) {
		delegate.info(prefix + message, params);
	}

	public void warn(String message, Object... params) {
		delegate.warn(prefix + message, params);
	}

	public void error(String message, Object... params) {
		delegate.error(prefix + message, params);
	}

	public void error(String message, Throwable throwable) {
		delegate.error(prefix + message, throwable);
	}

	public boolean isDebugEnabled() {
		return delegate.isDebugEnabled();
	}
}
