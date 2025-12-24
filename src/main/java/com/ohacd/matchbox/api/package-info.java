/**
 * Public API for Matchbox.
 *
 * <p>API policy:
 * <ul>
 *   <li>Use JetBrains annotations (`@NotNull` / `@Nullable`) for nullability on all public API surfaces.</li>
 *   <li>Use Javadoc `@since` on public classes and when introducing new public methods.</li>
 *   <li>Use `@Deprecated` (and Javadoc `@deprecated`) when removing or replacing behavior; supply a replacement if available.</li>
 *   <li>Use `@com.ohacd.matchbox.api.annotation.Experimental` for unstable APIs and `@com.ohacd.matchbox.api.annotation.Internal` for internal-only APIs.</li>
 * </ul>
 *
 * <p>This package contains the public-facing API types and should remain stable across patch releases where possible.
 *
 * @since 0.9.5
 */
package com.ohacd.matchbox.api;