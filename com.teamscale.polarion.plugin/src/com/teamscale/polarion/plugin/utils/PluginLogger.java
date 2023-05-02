package com.teamscale.polarion.plugin.utils;

import com.polarion.core.util.logging.ILogger;
import com.polarion.core.util.logging.Logger;

public class PluginLogger implements ILogger {

  private static final ILogger logger = Logger.getLogger(PluginLogger.class);

  private static final String PREFIX = "[Teamscale Polarion Plugin] ";

  public void debug(Object message) {
    if (message instanceof String) {
      logger.debug(PREFIX + message);
    } else {
      logger.debug(message);
    }
  }

  public void debug(Object message, Throwable exception) {
    if (message instanceof String) {
      logger.debug(PREFIX + message, exception);
    } else {
      logger.debug(message, exception);
    }
  }

  public void error(Object message) {
    if (message instanceof String) {
      logger.error(PREFIX + message);
    } else {
      logger.error(message);
    }
  }

  public void error(Object message, Throwable exception) {
    if (message instanceof String) {
      logger.error(PREFIX + message, exception);
    } else {
      logger.error(message, exception);
    }
  }

  public void fatal(Object message) {
    if (message instanceof String) {
      logger.fatal(PREFIX + message);
    } else {
      logger.fatal(message);
    }
  }

  public void fatal(Object message, Throwable exception) {
    if (message instanceof String) {
      logger.fatal(PREFIX + message, exception);
    } else {
      logger.fatal(message, exception);
    }
  }

  public void info(Object message) {
    if (message instanceof String) {
      logger.info(PREFIX + message);
    } else {
      logger.info(message);
    }
  }

  public void info(Object message, Throwable exception) {
    if (message instanceof String) {
      logger.info(PREFIX + message, exception);
    } else {
      logger.info(message, exception);
    }
  }

  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  public boolean isFatalEnabled() {
    return logger.isFatalEnabled();
  }

  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  public void trace(Object message) {
    if (message instanceof String) {
      logger.trace(PREFIX + message);
    } else {
      logger.trace(message);
    }
  }

  public void trace(Object message, Throwable exception) {
    if (message instanceof String) {
      logger.trace(PREFIX + message, exception);
    } else {
      logger.trace(message, exception);
    }
  }

  public void warn(Object message) {
    if (message instanceof String) {
      logger.warn(PREFIX + message);
    } else {
      logger.warn(message);
    }
  }

  public void warn(Object message, Throwable exception) {
    if (message instanceof String) {
      logger.warn(PREFIX + message, exception);
    } else {
      logger.warn(message, exception);
    }
  }
}
