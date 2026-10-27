package net.seemless.transfers.placeholder;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.seemless.transfers.network.PlaceholderResolver;

/**
 * Universal runtime placeholder bridge for NeoForge.
 * Dynamically hooks into Text Placeholder API if present on the server,
 * providing real-time %seemless:sl <ip>_<port>% queries without hard compile or runtime dependencies.
 *
 * Authored by TD_Hunterrr. All Rights Reserved.
 */
public final class NeoForgePlaceholderHook {

    private NeoForgePlaceholderHook() {
    }

    /**
     * Attempts to register placeholders with any supported NeoForge placeholder engine.
     */
    public static void registerIfPresent() {
        hookTextPlaceholderApi();
    }

    private static void hookTextPlaceholderApi() {
        try {
            Class<?> placeholdersClass = Class.forName("eu.pb4.placeholders.api.Placeholders");
            Class<?> handlerClass = Class.forName("eu.pb4.placeholders.api.PlaceholderHandler");
            Class<?> resultClass = Class.forName("eu.pb4.placeholders.api.PlaceholderResult");

            // Locate value(Component) factory method
            Method valueMethod = null;
            for (Method m : resultClass.getMethods()) {
                if (m.getName().equals("value") && m.getParameterCount() == 1) {
                    valueMethod = m;
                    break;
                }
            }
            if (valueMethod == null) {
                return;
            }

            // Locate registerCommon or register method
            Method registerMethod = null;
            for (Method m : placeholdersClass.getMethods()) {
                if ((m.getName().equals("registerCommon") || m.getName().equals("register"))
                        && m.getParameterCount() == 2) {
                    registerMethod = m;
                    break;
                }
            }
            if (registerMethod == null) {
                return;
            }

            final Method finalValueMethod = valueMethod;

            registerHandler(registerMethod, handlerClass, finalValueMethod, "seemless", "sl", "online");
            registerHandler(registerMethod, handlerClass, finalValueMethod, "sl", "online", "online");
            registerHandler(registerMethod, handlerClass, finalValueMethod, "sl", "count", "online");
            registerHandler(registerMethod, handlerClass, finalValueMethod, "sl", "max", "max");
            registerHandler(registerMethod, handlerClass, finalValueMethod, "sl", "status", "status");
            registerHandler(registerMethod, handlerClass, finalValueMethod, "sl", "ping", "ping");

            System.out.println("[Seemless Transfers] Successfully hooked into NeoForge Text Placeholder API! Registered %seemless:sl <ip>_<port>%");
        } catch (Throwable ignored) {
            // Text Placeholder API is not installed or skipped
        }
    }

    private static void registerHandler(Method registerMethod, Class<?> handlerClass, Method valueMethod,
                                        String namespace, String path, String defaultType) {
        try {
            Object handlerProxy = Proxy.newProxyInstance(
                    handlerClass.getClassLoader(),
                    new Class<?>[]{handlerClass},
                    (proxy, method, args) -> {
                        if (method.getName().equals("onPlaceholderRequest") || method.getParameterCount() == 2) {
                            String argument = (args != null && args.length > 1 && args[1] != null) ? args[1].toString() : "";
                            String resolved = PlaceholderResolver.resolve(argument, defaultType);
                            Component text = Component.literal(resolved);
                            return valueMethod.invoke(null, text);
                        }
                        if (method.getName().equals("toString")) {
                            return "SeemlessPlaceholderHandler[" + namespace + ":" + path + "]";
                        }
                        return null;
                    }
            );

            Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
            registerMethod.invoke(null, id, handlerProxy);
        } catch (Throwable ignored) {
        }
    }
}