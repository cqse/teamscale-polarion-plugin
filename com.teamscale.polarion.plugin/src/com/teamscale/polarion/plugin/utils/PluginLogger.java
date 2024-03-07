package com.teamscale.polarion.plugin.utils;

import com.polarion.core.util.logging.ILogger;
import com.polarion.core.util.logging.Logger;

/**
 * This class servers as a wrapper in order to provide some customization to the Teamscale plugin on
 * top of Polarion logger.
 */
public class PluginLogger implements ILogger {

  private final ILogger logger = Logger.getLogger(PluginLogger.class);

  private static final String PREFIX = "[Teamscale Polarion Plugin] ";

  /** Logs message - debug level */
  public void debug(Object message) {
    if (message instanceof String) {
      logger.debug(PREFIX + message);
    } else {
      logger.debug(message);
    }
  }

  /** Logs message with exception - debug level */
  public void debug(Object message, Throwable exception) {
    if (message instanceof String) {
      logger.debug(PREFIX + message, exception);
    } else {
      logger.debug(message, exception);
    }
  }

  /** Logs message - error level */
  public void error(Object message) {
    if (message instanceof String) {
      logger.error(PREFIX + message);
    } else {
      logger.error(message);
    }
  }

  /** Logs message with exception - error level */
  public void error(Object message, Throwable exception) {
    if (message instanceof String) {
      logger.error(PREFIX + message, exception);
    } else {
      logger.error(message, exception);
    }
  }

  /** Logs message - fatal level */
  public void fatal(Object message) {
    if (message instanceof String) {
      logger.fatal(PREFIX + message);
    } else {
      logger.fatal(message);
    }
  }

  /** Logs message with exception - fatal level */
  public void fatal(Object message, Throwable exception) {
    if (message instanceof String) {
      logger.fatal(PREFIX + message, exception);
    } else {
      logger.fatal(message, exception);
    }
  }

  /** Logs message - info level */
  public void info(Object message) {
    if (message instanceof String) {
      logger.info(PREFIX + message);
    } else {
      logger.info(message);
    }
  }

  /** Logs message with exception - info level */
  public void info(Object message, Throwable exception) {
    if (message instanceof String) {
      logger.info(PREFIX + message, exception);
    } else {
      logger.info(message, exception);
    }
  }

  /** Checks if debug logging level is enabled */
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  /** Checks if error logging level is enabled */
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  /** Checks if fatal logging level is enabled */
  public boolean isFatalEnabled() {
    return logger.isFatalEnabled();
  }

  /** Checks if info logging level is enabled */
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  /** Checks if trace logging level is enabled */
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  /** Checks if warn logging level is enabled */
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  /** Logs message - trace level */
  public void trace(Object message) {
    if (message instanceof String) {
      logger.trace(PREFIX + message);
    } else {
      logger.trace(message);
    }
  }

  /** Logs message with exception - trace level */
  public void trace(Object message, Throwable exception) {
    if (message instanceof String) {
      logger.trace(PREFIX + message, exception);
    } else {
      logger.trace(message, exception);
    }
  }

  /** Logs message - warn level */
  public void warn(Object message) {
    if (message instanceof String) {
      logger.warn(PREFIX + message);
    } else {
      logger.warn(message);
    }
  }

  /** Logs message with exception - warn level */
  public void warn(Object message, Throwable exception) {
    if (message instanceof String) {
      logger.warn(PREFIX + message, exception);
    } else {
      logger.warn(message, exception);
    }
  }
}
