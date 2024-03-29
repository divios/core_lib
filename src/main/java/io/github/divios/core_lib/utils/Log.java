/*
 * This file is part of helper, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package io.github.divios.core_lib.utils;

import io.github.divios.core_lib.Core_lib;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * Utility for quickly accessing a logger instance without using {@link Bukkit#getLogger()}
 */
public final class Log {

    private static final String INFO = ChatColor.COLOR_CHAR + "f";
    private static final String WARN = ChatColor.COLOR_CHAR + "e";
    private static final String SEVERE = ChatColor.COLOR_CHAR + "c";

    public static void info(String format, Object... objects) {
        info(String.format(format, objects));
    }

    public static void info(String s) {
        sendMsg(INFO, s);
    }

    public static void warn(String format, Object... objects) {
        warn(String.format(format, objects));
    }

    public static void warn(String s) {
        sendMsg(WARN, s);
    }

    public static void severe(String format, Object... objects) {
        severe(String.format(format, objects));
    }

    public static void severe(String s) {
        sendMsg(SEVERE, s);
    }

    private static void sendMsg(String prefix, String msg) {
        String finalMsg = String.format("%s[%s] %s", prefix, Core_lib.PLUGIN.getName(), msg);
        Bukkit.getConsoleSender().sendMessage(finalMsg);
    }

    private Log() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

}
