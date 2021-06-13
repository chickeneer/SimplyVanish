package dev.chickeneer.simplyvanish.api.hooks;

/**
 * Supported hook methods.
 */
public enum HookPurpose {
    BEFORE_VANISH,
    AFTER_VANISH,
    BEFORE_REAPPEAR,
    AFTER_REAPPEAR,
    BEFORE_SETFLAGS,
    AFTER_SETFLAGS,
    ALLOW_UPDATE,
    ALLOW_SHOW,
    ALLOW_HIDE,
    LISTENER,
}
