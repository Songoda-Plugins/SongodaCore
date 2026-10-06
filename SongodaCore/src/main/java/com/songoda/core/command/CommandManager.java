package com.songoda.core.command;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.command.annotation.BaseCommand;
import com.songoda.core.command.annotation.Command;
import com.songoda.core.command.annotation.Current;
import com.songoda.core.command.annotation.Param;
import com.songoda.core.command.annotation.Permission;
import com.songoda.core.command.annotation.Sender;
import com.songoda.core.command.annotation.SubCommand;
import com.songoda.core.command.annotation.TabArgs;
import com.songoda.core.command.annotation.TabComplete;
import com.songoda.core.text.AdventureUtils;
import com.songoda.core.text.lang.Lang;
import com.songoda.core.spi.CommandMaps;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandManager {
    private SongodaPlugin plugin;
    private Map<Class<?>, ParameterResolver<?>> resolvers;
    private Map<String, Object> commandInstances; // Store command instances instead of executors
    private CommandExecutor sharedExecutor; // Singleton executor
    private TabCompleter sharedTabCompleter; // Add singleton tab completer

    public CommandManager() {

    }

    public void init(SongodaPlugin plugin) {
        this.plugin = plugin;
        this.resolvers = new HashMap<>();
        this.commandInstances = new HashMap<>();
        this.sharedExecutor = createSharedExecutor();
        this.sharedTabCompleter = createSharedTabCompleter(); // Initialize the singleton tab completer
    }

    /**
     * Registers a parameter resolver for converting string arguments to specific types
     */
    public void registerResolver(ParameterResolver<?> resolver) {
        if (resolver == null) {
            plugin.getLogger().warning("Attempted to register null resolver - ignoring");
            return;
        }

        Set<Class<?>> supportedTypes = resolver.getSupportedTypes();

        if (supportedTypes.isEmpty()) {
            plugin.getLogger().warning("Resolver " + resolver.getClass().getSimpleName() +
                    " did not register any types - it may not work correctly");
        }

        // Register all explicitly supported types
        for (Class<?> type : supportedTypes) {
            if (type != null) {
                resolvers.put(type, resolver);
                plugin.getLogger().info("Registered " + resolver.getClass().getSimpleName() +
                        " for type " + type.getSimpleName());
            } else {
                plugin.getLogger().warning("Resolver " + resolver.getClass().getSimpleName() +
                        " tried to register null type - ignoring");
            }
        }
    }

    /**
     * Registers a command class
     */
    public void registerCommand(Class<?> commandClass, Object instance) {
        Command commandAnnotation = commandClass.getAnnotation(Command.class);
        if (commandAnnotation == null) {
            plugin.getLogger().warning("Command annotation not found on class: " + commandClass.getName());
            return;
        }

        registerCommand(commandAnnotation.value(), commandAnnotation.aliases(), instance);
    }

    /**
     * Registers an annotated command handler under a runtime-configured command name.
     */
    public void registerCommand(String commandName, String[] aliases, Object instance) {
        if (commandName == null || commandName.isBlank() || instance == null) {
            plugin.getLogger().warning("Cannot register a dynamic command with a blank name or null handler");
            return;
        }

        PluginCommand command = plugin.getServer().getPluginCommand(commandName);
        boolean dynamic = false;

        if (command == null) {
            // Create a new command dynamically
            try {
                Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                constructor.setAccessible(true);
                command = constructor.newInstance(commandName, plugin);
                dynamic = true;
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create plugin command: " + commandName);
                return;
            }
        }

        // Set command aliases
        if (aliases.length > 0) {
            command.setAliases(Arrays.asList(aliases));
            plugin.getLogger().info("Registered aliases for command " + commandName + ": " +
                    String.join(", ", aliases));
        }

        if (dynamic) {
            CommandMaps.register(plugin, command);
        }

        // Store the command instance instead of creating a new executor
        commandInstances.put(commandName, instance);

        // Use the shared executor for all commands
        command.setExecutor(sharedExecutor);

        // Register tab completion
        registerTabCompletion(commandName, instance);

        plugin.getLogger().info("Registered command: " + commandName);
    }

    /**
     * Unregisters all commands
     */
    public void unregisterAll() {
        for (String commandName : commandInstances.keySet()) {
            PluginCommand command = plugin.getCommand(commandName);
            if (command != null) {
                command.setExecutor(null);
                command.setTabCompleter(null);
            }
        }
        commandInstances.clear();
    }

    /**
     * Creates a shared command executor that works for all command instances
     */
    private CommandExecutor createSharedExecutor() {
        return (sender, command, label, args) -> {
            // Get the command instance for this command name
            Object instance = commandInstances.get(command.getName());
            if (instance == null) {
                return false;
            }

            Method baseCommand = null;
            Map<Method, List<String>> subCommandPatterns = new HashMap<>();

            // Find base command and subcommands
            for (Method method : instance.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(BaseCommand.class)) {
                    baseCommand = method;
                } else if (method.isAnnotationPresent(SubCommand.class)) {
                    SubCommand subCommand = method.getAnnotation(SubCommand.class);
                    List<String> patterns = new ArrayList<>();
                    patterns.add(subCommand.value());

                    // Add aliases if they exist
                    if (subCommand.aliases() != null && subCommand.aliases().length > 0) {
                        patterns.addAll(Arrays.asList(subCommand.aliases()));
                    }

                    subCommandPatterns.put(method, patterns);
                }
            }

            // Execute base command if no args
            if (args.length == 0 && baseCommand != null) {
                return executeCommand(sender, instance, baseCommand, args);
            }

            // Try to match a subcommand - prefer the most specific pattern (literals over wildcards)
            Method bestMatch = null;
            int bestSpecificity = -1;
            for (Map.Entry<Method, List<String>> entry : subCommandPatterns.entrySet()) {
                for (String pattern : entry.getValue()) {
                    if (matchesSubCommand(pattern, args)) {
                        int specificity = getPatternSpecificity(pattern);
                        if (specificity > bestSpecificity) {
                            bestSpecificity = specificity;
                            bestMatch = entry.getKey();
                        }
                    }
                }
            }

            if (bestMatch != null) {
                return executeCommand(sender, instance, bestMatch, args);
            }

            // Fall back to base command
            if (baseCommand != null) {
                return executeCommand(sender, instance, baseCommand, args);
            }

            return false;
        };
    }

    /**
     * Registers tab completion for a command
     */
    private void registerTabCompletion(String commandName, Object instance) {
        PluginCommand command = plugin.getServer().getPluginCommand(commandName);
        if (command == null) return;

        // Use the shared tab completer for all commands
        command.setTabCompleter(sharedTabCompleter);
    }

    /**
     * Creates a shared tab completer that works for all command instances
     */
    private TabCompleter createSharedTabCompleter() {
        return (sender, cmd, alias, args) -> {
            // Get the command instance for this command name
            Object instance = commandInstances.get(cmd.getName());
            if (instance == null) {
                return Collections.emptyList();
            }

            List<String> completions = new ArrayList<>();

            Map<String, Method> tabCompleteMethods = new HashMap<>();
            Map<String, Method> paramTabCompleteMethods = new HashMap<>();

            // Find all tab complete methods for this command instance
            for (Method method : instance.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(TabComplete.class)) {
                    TabComplete tabComplete = method.getAnnotation(TabComplete.class);

                    if (!tabComplete.param().isEmpty()) {
                        // Parameter-based tab completion
                        paramTabCompleteMethods.put(tabComplete.param(), method);
                    } else {
                        // Command pattern-based tab completion
                        tabCompleteMethods.put(tabComplete.command(), method);
                    }
                }
            }

            if (tabCompleteMethods.isEmpty() && paramTabCompleteMethods.isEmpty()) {
                return Collections.emptyList();
            }

            // Get all subcommands for this command
            Map<String, Method> subCommands = new HashMap<>();
            for (Method method : instance.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(SubCommand.class)) {
                    SubCommand subCommand = method.getAnnotation(SubCommand.class);
                    subCommands.put(subCommand.value(), method);

                    // Also register aliases if they exist
                    if (subCommand.aliases() != null && subCommand.aliases().length > 0) {
                        for (String al : subCommand.aliases()) {
                            subCommands.put(al, method);
                        }
                    }
                }
            }

            // First try parameter-based completions
            if (!paramTabCompleteMethods.isEmpty()) {
                handleParameterBasedTabCompletions(sender, args, paramTabCompleteMethods, subCommands, instance, completions);
            }

            // Then try command pattern-based completions
            if (!tabCompleteMethods.isEmpty()) {
                handlePatternBasedTabCompletions(sender, args, tabCompleteMethods, instance, completions);
            }

            // Filter completions based on current input
            String currentArg = args.length > 0 ? args[args.length - 1] : "";
            return filterCompletions(completions, currentArg);
        };
    }

    /**
     * Handles parameter-based tab completions
     */
    private void handleParameterBasedTabCompletions(CommandSender sender, String[] args,
                                                    Map<String, Method> paramTabCompleteMethods,
                                                    Map<String, Method> subCommands, Object instance,
                                                    List<String> completions) {
        for (Map.Entry<String, Method> entry : subCommands.entrySet()) {
            String pattern = entry.getKey();
            Method method = entry.getValue();
            String[] patternParts = pattern.split(" ");

            // Skip if no permission
            if (!hasPermission(sender, instance.getClass(), method, true)) {
                continue;
            }

            // Skip patterns that don't match the current args
            if (!matchesPartialPattern(patternParts, args)) {
                continue;
            }

            // Find the parameter at the current arg index
            int argIndex = args.length - 1;

            // Make sure we don't go out of bounds
            if (argIndex < patternParts.length) {
                String patternPart = patternParts[argIndex];

                if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                    String paramName = patternPart.substring(1, patternPart.length() - 1);
                    //make sure we send the real parameter name for tab completion
                    if (paramName.contains("=")) {
                        paramName = paramName.split("=")[0];
                    }

                    // Check if we have a tab completer for this parameter
                    Method tabMethod = paramTabCompleteMethods.get(paramName);
                    if (tabMethod != null) {
                        try {
                            // Create an array with only the parameters the method expects
                            Object[] methodParams = createMatchingParameters(sender, tabMethod, args, argIndex);
                            Object result = tabMethod.invoke(instance, methodParams);
                            if (result instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> paramCompletions = (List<String>) result;
                                completions.addAll(paramCompletions);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("Error executing tab complete: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } else {
                    // It's a static part, suggest it if it matches the current input
                    String currentArg = args[argIndex];
                    if (patternPart.toLowerCase().startsWith(currentArg.toLowerCase())) {
                        completions.add(patternPart);
                    }
                }
            }
        }
    }

    /**
     * Handles pattern-based tab completions
     */
    private void handlePatternBasedTabCompletions(CommandSender sender, String[] args,
                                                  Map<String, Method> tabCompleteMethods,
                                                  Object instance, List<String> completions) {
        for (Map.Entry<String, Method> entry : tabCompleteMethods.entrySet()) {
            String pattern = entry.getKey();
            Method method = entry.getValue();

            // Skip if no permission
            if (!hasPermission(sender, instance.getClass(), method, true)) {
                continue;
            }

            if (pattern.isEmpty() || matchesTabCompletePattern(pattern, args)) {
                TabComplete tabComplete = method.getAnnotation(TabComplete.class);
                int argIndex = tabComplete.argIndex();

                // If argIndex is -1, use the last argument (or the first slot when none typed yet)
                if (argIndex == -1) {
                    argIndex = Math.max(0, args.length - 1);
                }

                // An empty pattern belongs to the command root and must not leak
                // suggestions into an already selected subcommand.
                boolean shouldInvoke = pattern.isEmpty()
                        ? args.length <= 1
                        : argIndex >= 0 && argIndex < args.length;
                if (shouldInvoke) {
                    try {
                        // Create an array with only the parameters the method expects
                        Object[] methodParams = createMatchingParameters(sender, method, args, argIndex);
                        Object result = method.invoke(instance, methodParams);
                        if (result instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> paramCompletions = (List<String>) result;
                            completions.addAll(paramCompletions);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error executing tab complete: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Creates an array of parameters that matches exactly what the method expects
     */
    private Object[] createMatchingParameters(CommandSender sender, Method method, String[] args, int argIndex) {
        Parameter[] parameters = method.getParameters();
        Object[] values = new Object[parameters.length];
        int stringArgIndex = 0; // tracks progression through args array for normal parameters

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (param.getType().equals(CommandSender.class)) {
                values[i] = sender;
            } else if (param.isAnnotationPresent(TabArgs.class)) {
                values[i] = args;
            } else if (param.isAnnotationPresent(Current.class)) {
                // Inject the "current tab argument" as a string.
                String current = (argIndex >= 0 && argIndex < args.length) ? args[argIndex] : "";
                values[i] = current;
            } else {
                // Handle other parameters, typically those resolved from args
                if (stringArgIndex < args.length) {
                    values[i] = resolveParameter(param.getType(), args[stringArgIndex++]);
                } else {
                    values[i] = null; // or provide default/fallback logic as appropriate
                }
            }
        }
        return values;
    }


    /**
     * Checks if a command pattern matches the current args for tab completion
     */
    private boolean matchesPartialPattern(String[] patternParts, String[] args) {
        // For partial matching, we need to check all args except the last one
        int argsToCheck = args.length - 1;

        if (argsToCheck > patternParts.length) {
            return false;
        }

        for (int i = 0; i < argsToCheck; i++) {
            String patternPart = patternParts[i];

            // If it's a parameter placeholder, it matches anything
            if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                continue;
            }

            // For static parts, they must match exactly
            if (!patternPart.equalsIgnoreCase(args[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a tab complete pattern matches the current args
     */
    private boolean matchesTabCompletePattern(String pattern, String[] args) {
        String[] patternParts = pattern.split(" ");

        // If we have more args than pattern parts, only match if the pattern ends with a parameter
        if (args.length > patternParts.length &&
                (patternParts.length == 0 || !patternParts[patternParts.length - 1].startsWith("{"))) {
            return false;
        }

        // Check each part of the pattern
        for (int i = 0; i < Math.min(patternParts.length, args.length); i++) {
            String patternPart = patternParts[i];

            // If it's a parameter, it matches anything
            if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                continue;
            }

            // Otherwise, it must match exactly
            if (!patternPart.equalsIgnoreCase(args[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Filters completions based on the current input: prefix match or substring match (case-insensitive).
     * Substring matches are included so ids like {@code red_crate} appear when the player types {@code crate}.
     */
    private List<String> filterCompletions(List<String> completions, String prefix) {
        if (prefix.isEmpty()) return completions;

        String needle = prefix.toLowerCase(Locale.ROOT);
        return completions.stream()
                .filter(s -> {
                    String haystack = s.toLowerCase(Locale.ROOT);
                    return haystack.startsWith(needle) || haystack.contains(needle);
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns a specificity score for a subcommand pattern.
     * Literal segments score highest, named parameters lower, and {@code {**}} lowest
     * so exact subcommands like {@code reset} win over catch-all wildcards.
     */
    private int getPatternSpecificity(String pattern) {
        String[] patternParts = pattern.split(" ");
        int score = 0;

        for (String part : patternParts) {
            if (part.startsWith("{") && part.endsWith("}")) {
                String paramContent = part.substring(1, part.length() - 1);
                if (paramContent.equals("**")) {
                    score += 1;
                } else {
                    score += 10;
                }
            } else {
                score += 1000;
            }
        }

        // Tie-breaker: prefer longer, more structured patterns
        return score * 100 + patternParts.length;
    }

    /**
     * Checks if a subcommand pattern matches the given args
     */
    private boolean matchesSubCommand(String pattern, String[] args) {
        String[] patternParts = pattern.split(" ");

        // Count required parts (non-parameter or parameters without default values)
        int requiredParts = 0;
        for (String part : patternParts) {
            if (!part.startsWith("{") || !part.endsWith("}") ||
                    (part.startsWith("{") && part.endsWith("}") && !part.contains("="))) {
                requiredParts++;
            }
        }

        // Handle wildcard pattern at the end
        boolean hasWildcard = patternParts.length > 0 && patternParts[patternParts.length - 1].equals("{**}");
        if (hasWildcard) {
            requiredParts--;
        }

        // Make sure we have at least the required number of args
        if (args.length < requiredParts) {
            return false;
        }

        // Check each part of the pattern against the args
        int argIndex = 0;
        for (int i = 0; i < patternParts.length; i++) {
            // Skip wildcard at the end
            if (i == patternParts.length - 1 && hasWildcard) {
                break;
            }

            String patternPart = patternParts[i];

            // If it's a parameter placeholder
            if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                boolean hasDefault = patternPart.contains("=");

                // If we've used all args and this parameter has a default, it's optional
                if (argIndex >= args.length && hasDefault) {
                    continue;
                }

                // If we still have args, use this one and increment arg index
                if (argIndex < args.length) {
                    argIndex++;
                }
                continue;
            }

            // For static text, it must match exactly
            if (argIndex >= args.length || !patternPart.equalsIgnoreCase(args[argIndex])) {
                return false;
            }

            argIndex++;
        }

        // Make sure we've consumed all args unless there's a wildcard
        return hasWildcard || argIndex >= args.length;
    }

    /**
     * Executes a command method
     */
    private boolean executeCommand(CommandSender sender, Object instance, Method method, String[] args) {
        try {
            // Check permissions first
            if (!hasPermission(sender, instance.getClass(), method, false)) {
                return true; // Return true to indicate we handled the command
            }

            // Check if the sender type is compatible with the first parameter
            Parameter[] parameters = method.getParameters();
            if (parameters.length > 0) {
                Parameter firstParam = parameters[0];
                if (firstParam.getAnnotation(Param.class) == null) {
                    Class<?> requiredType = firstParam.getType();

                    // Check sender type compatibility
                    if (!requiredType.isAssignableFrom(sender.getClass())) {
                        if (Player.class.isAssignableFrom(requiredType)) {
                            Lang.send(sender, "General.Player Only Command");
                        } else if (ConsoleCommandSender.class.isAssignableFrom(requiredType)) {
                            Lang.send(sender, "General.Console Only Command");
                        } else {
                            Lang.send(sender, "General.Invalid Command Sender");
                        }
                        return true; // Return true to indicate we handled the command
                    }
                }
            }

            Object[] resolvedParameters = resolveParameters(sender, method, args);
            if (resolvedParameters == null) return false;

            method.invoke(instance, resolvedParameters);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing command: " + e.getMessage());
            if (e instanceof InvocationTargetException) {
                ((InvocationTargetException) e).getTargetException().printStackTrace();
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Checks if a sender has permission to execute a command
     */
    private boolean hasPermission(CommandSender sender, Class<?> commandClass, Method method, boolean tabcomplete) {
        // Check method-level permission first (more specific)
        if (method.isAnnotationPresent(Permission.class)) {
            Permission permission = method.getAnnotation(Permission.class);
            if (!sender.hasPermission(permission.value())) {
                if (!tabcomplete) {
                    String message = permission.message();
                    if (message.isEmpty()) {
                        Lang.send(sender, "General.No Permission");
                    } else if (message.startsWith("lang:")) {
                        Lang.send(sender, message.substring("lang:".length()));
                    } else {
                        AdventureUtils.sendMessage(AdventureUtils.formatComponent(message), sender);
                    }
                }
                return false;
            }
        }

        // Then check class-level permission
        if (commandClass.isAnnotationPresent(Permission.class)) {
            Permission permission = commandClass.getAnnotation(Permission.class);
            if (!sender.hasPermission(permission.value())) {
                if (!tabcomplete) {
                    String message = permission.message();
                    if (message.isEmpty()) {
                        Lang.send(sender, "General.No Permission");
                    } else if (message.startsWith("lang:")) {
                        Lang.send(sender, message.substring("lang:".length()));
                    } else {
                        AdventureUtils.sendMessage(AdventureUtils.formatComponent(message), sender);
                    }
                }
                return false;
            }
        }

        return true;
    }

    /**
     * Resolves parameters for command execution
     */
    private Object[] resolveParameters(CommandSender sender, Method method, String[] args) {
        Parameter[] parameters = method.getParameters();
        Object[] resolvedParams = new Object[parameters.length];

        // Create a map to store default values from the pattern
        Map<String, String> defaultValues = extractDefaultValuesFromPattern(method);

        // Track which args have been used
        boolean[] usedArgs = new boolean[args.length];

        // First pass: handle @Sender and other special annotations
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            if (parameter.isAnnotationPresent(Sender.class)) {
                // Handle @Sender annotation
                if (parameter.getType().isAssignableFrom(sender.getClass())) {
                    resolvedParams[i] = sender;
                } else if (parameter.getType() == Player.class && sender instanceof Player) {
                    resolvedParams[i] = sender;
                } else {
                    // Sender type mismatch
                    return null;
                }
            }
        }

        // Mark static text positions in the pattern as used so the ** wildcard doesn't consume them
        if (method.isAnnotationPresent(SubCommand.class)) {
            SubCommand subCommand = method.getAnnotation(SubCommand.class);
            String[] patternParts = subCommand.value().split(" ");
            int argIdx = 0;
            for (String part : patternParts) {
                if (argIdx >= args.length) break;
                if (part.startsWith("{") && part.endsWith("}")) {
                    // parameter placeholder: skip one arg slot (unless it's **)
                    String pName = part.substring(1, part.length() - 1);
                    if (!pName.equals("**")) {
                        argIdx++;
                    }
                } else {
                    // static text: mark it as used so ** won't pick it up
                    if (part.equalsIgnoreCase(args[argIdx])) {
                        usedArgs[argIdx] = true;
                    }
                    argIdx++;
                }
            }
        }

        // Second pass: handle @Param annotations
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            if (parameter.isAnnotationPresent(Param.class)) {
                Param paramAnnotation = parameter.getAnnotation(Param.class);
                String paramName = paramAnnotation.value();

                // Handle wildcard parameter (captures all remaining args)
                if (paramName.equals("**")) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < args.length; j++) {
                        if (!usedArgs[j]) {
                            if (sb.length() > 0) sb.append(" ");
                            sb.append(args[j]);
                            usedArgs[j] = true;
                        }
                    }

                    resolvedParams[i] = resolveParameter(parameter.getType(), sb.toString());
                    continue;
                }

                // Find the parameter in the command pattern
                int paramIndex = findParamIndex(method, paramName, args);
                if (paramIndex >= 0 && paramIndex < args.length && !usedArgs[paramIndex]) {
                    resolvedParams[i] = resolveParameter(parameter.getType(), args[paramIndex]);
                    usedArgs[paramIndex] = true;
                } else {
                    // Parameter not found or already used
                    String defaultValue = defaultValues.get(paramName);
                    if (defaultValue != null) {
                        resolvedParams[i] = resolveParameter(parameter.getType(), defaultValue);
                    } else {
                        return null; // Parameter not found and no default value
                    }
                }
            } else if (resolvedParams[i] == null) {
                // For parameters without annotations, try to resolve by position
                for (int j = 0; j < args.length; j++) {
                    if (!usedArgs[j]) {
                        resolvedParams[i] = resolveParameter(parameter.getType(), args[j]);
                        usedArgs[j] = true;
                        break;
                    }
                }

                // If still null, try to use default values or null
                if (resolvedParams[i] == null) {
                    if (parameter.getType().isPrimitive()) {
                        // Use default values for primitives
                        if (parameter.getType() == int.class) resolvedParams[i] = 0;
                        else if (parameter.getType() == long.class) resolvedParams[i] = 0L;
                        else if (parameter.getType() == double.class) resolvedParams[i] = 0.0;
                        else if (parameter.getType() == float.class) resolvedParams[i] = 0.0f;
                        else if (parameter.getType() == boolean.class) resolvedParams[i] = false;
                        else if (parameter.getType() == char.class) resolvedParams[i] = '\0';
                        else if (parameter.getType() == byte.class) resolvedParams[i] = (byte) 0;
                        else if (parameter.getType() == short.class) resolvedParams[i] = (short) 0;
                    }
                }
            }
        }

        return resolvedParams;
    }

    // Add this new method to extract default values from pattern
    private Map<String, String> extractDefaultValuesFromPattern(Method method) {
        Map<String, String> defaultValues = new HashMap<>();

        if (method.isAnnotationPresent(SubCommand.class)) {
            SubCommand subCommand = method.getAnnotation(SubCommand.class);
            String pattern = subCommand.value();

            // Process main command pattern
            extractDefaultsFromPattern(pattern, defaultValues);

            // Process aliases
            for (String alias : subCommand.aliases()) {
                extractDefaultsFromPattern(alias, defaultValues);
            }
        }

        return defaultValues;
    }

    // Helper method to extract defaults from a single pattern
    private void extractDefaultsFromPattern(String pattern, Map<String, String> defaultValues) {
        String[] patternParts = pattern.split(" ");

        for (String part : patternParts) {
            if (part.startsWith("{") && part.endsWith("}")) {
                String paramContent = part.substring(1, part.length() - 1);

                if (paramContent.contains("=")) {
                    String[] parts = paramContent.split("=", 2);
                    String paramName = parts[0];
                    String defaultValue = parts[1];
                    defaultValues.put(paramName, defaultValue);
                }
            }
        }
    }

    /**
     * Finds the index of a parameter in the command args
     */
    private int findParamIndex(Method method, String paramName, String[] args) {
        // If the method has a SubCommand annotation, use its pattern to find the parameter
        if (method.isAnnotationPresent(SubCommand.class)) {
            SubCommand subCommand = method.getAnnotation(SubCommand.class);
            String pattern = subCommand.value();
            String[] patternParts = pattern.split(" ");

            // Track the argument index, accounting for both static text and parameters
            int argIndex = 0;
            for (int i = 0; i < patternParts.length; i++) {
                String part = patternParts[i];

                if (part.startsWith("{") && part.endsWith("}")) {
                    // This is a parameter placeholder
                    String name = part.substring(1, part.length() - 1);

                    // Extract just the parameter name if it has a default value
                    if (name.contains("=")) {
                        name = name.split("=", 2)[0];
                    }

                    if (name.equals(paramName)) {
                        // Found the parameter - return the argument index
                        // Check if this argument index exists in the provided args
                        if (argIndex < args.length) {
                            return argIndex;
                        } else {
                            // Parameter not provided in args, return -1 to use default
                            return -1;
                        }
                    }

                    // Increment argument index for each parameter we encounter
                    argIndex++;
                } else {
                    // This is static text - it consumes an argument position
                    // We need to increment argIndex to account for it
                    if (argIndex < args.length && part.equalsIgnoreCase(args[argIndex])) {
                        argIndex++;
                    }
                    // If static text doesn't match, the pattern matching would have failed earlier
                    // but we still need to account for it in the index calculation
                }
            }
        }

        // If not found or no SubCommand annotation, return -1
        return -1;
    }

    /**
     * Resolves a parameter value from a string
     */
    private Object resolveParameter(Class<?> type, String value) {
        // Handle null or empty values
        if (value == null || value.isEmpty()) {
            // For primitives, return default values
            if (type.isPrimitive()) {
                if (type == int.class) return 0;
                if (type == long.class) return 0L;
                if (type == double.class) return 0.0;
                if (type == float.class) return 0.0f;
                if (type == boolean.class) return false;
                if (type == char.class) return '\0';
                if (type == byte.class) return (byte) 0;
                if (type == short.class) return (short) 0;
            }
            return null;
        }

        // Try to find a resolver for this type
        ParameterResolver<?> resolver = resolvers.get(type);
        if (resolver != null && resolver.supports(type)) {
            return resolver.resolve(value);
        }

        // Built-in conversions for common types
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return type.isPrimitive() ? 0 : null;
            }
        } else if (type == double.class || type == Double.class) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return type.isPrimitive() ? 0.0 : null;
            }
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == long.class || type == Long.class) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return type.isPrimitive() ? 0L : null;
            }
        } else if (type == float.class || type == Float.class) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                return type.isPrimitive() ? 0.0f : null;
            }
        } else if (type == Player.class) {
            return plugin.getServer().getPlayer(value);
        }

        // For other types, return null
        return null;
    }
}
