package com.github.libretube.helpers

object ProxyHelper {

    /**
     * Decide whether the proxy should be used or not for a given stream URL based on user preferences.
     * In local-only mode, this simply returns the url unmodified.
     */
    fun rewriteUrlUsingProxyPreference(url: String): String {
        return url
    }

    /**
     * Convert a proxied Piped url to a YouTube url that's not proxied.
     * In local-only mode, this simply returns the url unmodified.
     */
    fun unwrapUrl(url: String): String {
        return url
    }
}
