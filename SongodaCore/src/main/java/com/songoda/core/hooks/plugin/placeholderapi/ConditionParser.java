package com.songoda.core.hooks.plugin.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import com.songoda.core.SongodaPlugin;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;

public class ConditionParser {

    private final static boolean placeholderAPIEnabled;

    static {
        boolean enabled = false;
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            enabled = true;
        } catch (ClassNotFoundException e) {
            // PlaceholderAPI not found
        }
        placeholderAPIEnabled = enabled;
    }

    public static boolean isPlaceholderApiEnabled() {
        return placeholderAPIEnabled;
    }

    // Parse and evaluate a condition
    public static boolean evaluate(String condition, Player player) {
        return evaluateCondition(condition, player);
    }

    // Parse and evaluate a condition
    public static boolean evaluateCondition(String condition, Player player) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }

        if (condition.contains("%") && !placeholderAPIEnabled) {
            logWarning("PlaceholderAPI is not enabled. Placeholder condition '" + condition + "' cannot be evaluated.");
            return false;
        }

        try {
            List<String> tokens = tokenize(condition);
            List<String> postfix = toPostfix(tokens);
            return evaluatePostfix(postfix, player);
        } catch (Exception e) {
            logWarning("Error evaluating condition '" + condition + "': " + e.getMessage());
            return false;
        }
    }

    private static void logWarning(String message) {
        try {
            if (SongodaPlugin.getInstance() != null && SongodaPlugin.getInstance().getLogger() != null) {
                SongodaPlugin.getInstance().getLogger().warning("[ConditionParser] " + message);
                return;
            }
        } catch (Throwable ignored) {
        }
        Logger.getLogger("ConditionParser").warning(message);
    }

    // Tokenize the condition string
    private static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        int i = 0;

        while (i < input.length()) {
            char c = input.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Quoted string literals
            if (c == '\'' || c == '"') {
                char quote = c;
                i++;
                StringBuilder str = new StringBuilder();
                while (i < input.length() && input.charAt(i) != quote) {
                    if (input.charAt(i) == '\\' && i + 1 < input.length()) {
                        i++;
                    }
                    str.append(input.charAt(i));
                    i++;
                }
                if (i < input.length() && input.charAt(i) == quote) {
                    i++;
                }
                tokens.add("\"" + str + "\"");
                continue;
            }

            // Two-character operators
            if (i + 1 < input.length()) {
                String twoChar = input.substring(i, i + 2);
                if (twoChar.equals("==") || twoChar.equals("!=") || twoChar.equals(">=")
                        || twoChar.equals("<=") || twoChar.equals("&&") || twoChar.equals("||")) {
                    tokens.add(twoChar);
                    i += 2;
                    continue;
                }
            }

            // Single-character operators and parentheses
            if (c == '(' || c == ')' || c == '>' || c == '<' || c == '!') {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }

            // Check if '-' is a negative number prefix
            if (c == '-' && (tokens.isEmpty() || isOperatorOrLeftParen(tokens.get(tokens.size() - 1)))) {
                if (i + 1 < input.length() && (Character.isDigit(input.charAt(i + 1)) || input.charAt(i + 1) == '.')) {
                    StringBuilder num = new StringBuilder("-");
                    i++;
                    while (i < input.length() && (Character.isDigit(input.charAt(i)) || input.charAt(i) == '.')) {
                        num.append(input.charAt(i));
                        i++;
                    }
                    tokens.add(num.toString());
                    continue;
                }
            }

            // Regular operand (identifier, number, placeholder %...%)
            StringBuilder operand = new StringBuilder();
            while (i < input.length()) {
                char ch = input.charAt(i);
                if (Character.isWhitespace(ch) || ch == '(' || ch == ')' || ch == '\'' || ch == '"'
                        || ch == '<' || ch == '>' || ch == '=' || ch == '!' || ch == '&' || ch == '|') {
                    break;
                }
                operand.append(ch);
                i++;
            }

            if (!operand.isEmpty()) {
                tokens.add(operand.toString());
            }
        }

        return tokens;
    }

    private static boolean isOperator(String token) {
        return token.equals("||") || token.equals("&&") || token.equals("==")
                || token.equals("!=") || token.equals(">") || token.equals("<")
                || token.equals(">=") || token.equals("<=");
    }

    private static boolean isOperatorOrLeftParen(String token) {
        return token.equals("(") || isOperator(token) || token.equals("!");
    }

    private static int getPrecedence(String token) {
        return switch (token) {
            case "!" -> 5;
            case ">", "<", ">=", "<=" -> 4;
            case "==", "!=" -> 3;
            case "&&" -> 2;
            case "||" -> 1;
            default -> 0;
        };
    }

    // Convert tokens to postfix (Reverse Polish Notation)
    private static List<String> toPostfix(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> operators = new Stack<>();

        for (String token : tokens) {
            if (token.equals("(")) {
                operators.push(token);
            } else if (token.equals(")")) {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    output.add(operators.pop());
                }
                if (!operators.isEmpty()) {
                    operators.pop(); // Remove "(" from stack
                }
            } else if (isOperator(token) || token.equals("!")) {
                while (!operators.isEmpty() && getPrecedence(token) <= getPrecedence(operators.peek())) {
                    output.add(operators.pop());
                }
                operators.push(token);
            } else {
                output.add(token); // Operand
            }
        }

        while (!operators.isEmpty()) {
            output.add(operators.pop());
        }

        return output;
    }

    // Evaluate the postfix expression
    private static boolean evaluatePostfix(List<String> postfix, Player player) {
        Stack<Object> stack = new Stack<>();

        for (String token : postfix) {
            if (token.equals("!")) {
                if (stack.isEmpty()) {
                    throw new IllegalStateException("Invalid postfix expression: insufficient operands for operator !");
                }
                Object val = stack.pop();
                stack.push(!toBoolean(val));
                continue;
            }

            if (isOperator(token)) {
                if (stack.size() < 2) {
                    throw new IllegalStateException("Invalid postfix expression: insufficient operands for operator " + token);
                }

                Object b = stack.pop();
                Object a = stack.pop();
                stack.push(evaluateOperation(a, b, token));
                continue;
            }

            // Quoted string literal
            if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
                String strVal = token.substring(1, token.length() - 1);
                stack.push(strVal);
                continue;
            }

            // Resolve placeholder or treat as literal value
            String resolved = token;
            if (placeholderAPIEnabled && player != null && token.contains("%")) {
                resolved = PlaceholderAPI.setPlaceholders(player, token);
            }

            // Check if placeholder was unresolved
            if (resolved.contains("%") && resolved.matches(".*%[a-zA-Z0-9._]+%.*")) {
                String unparsedPlaceholder = resolved.replaceAll(".*?(%[a-zA-Z0-9._]+%).*", "$1");
                logWarning("Unable to parse placeholder " + unparsedPlaceholder + " for player " + (player != null ? player.getName() : "null"));
                return false;
            }

            if (resolved.equalsIgnoreCase("true") || resolved.equalsIgnoreCase("false")) {
                stack.push(Boolean.parseBoolean(resolved));
            } else {
                try {
                    stack.push(Double.parseDouble(resolved));
                } catch (NumberFormatException e) {
                    stack.push(resolved);
                }
            }
        }

        if (stack.size() != 1) {
            throw new IllegalStateException("Invalid postfix expression: stack size after evaluation is " + stack.size());
        }

        return toBoolean(stack.pop());
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.doubleValue() != 0.0;
        }
        if (value instanceof String s) {
            return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes");
        }
        return value != null;
    }

    // Evaluate a single operation
    private static boolean evaluateOperation(Object a, Object b, String operator) {
        if (operator.equals("||")) {
            return toBoolean(a) || toBoolean(b);
        }
        if (operator.equals("&&")) {
            return toBoolean(a) && toBoolean(b);
        }

        // Numerical comparisons
        if (a instanceof Number numA && b instanceof Number numB) {
            double da = numA.doubleValue();
            double db = numB.doubleValue();
            return switch (operator) {
                case "==" -> Double.compare(da, db) == 0;
                case "!=" -> Double.compare(da, db) != 0;
                case ">" -> da > db;
                case "<" -> da < db;
                case ">=" -> da >= db;
                case "<=" -> da <= db;
                default -> throw new IllegalArgumentException("Invalid operator: " + operator);
            };
        }

        // Try parsing string operands to numbers if comparing with inequality
        if (operator.equals(">") || operator.equals("<") || operator.equals(">=") || operator.equals("<=")) {
            try {
                double da = Double.parseDouble(String.valueOf(a));
                double db = Double.parseDouble(String.valueOf(b));
                return switch (operator) {
                    case ">" -> da > db;
                    case "<" -> da < db;
                    case ">=" -> da >= db;
                    case "<=" -> da <= db;
                    default -> false;
                };
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        // Equality comparisons for strings / booleans / other objects
        String strA = String.valueOf(a);
        String strB = String.valueOf(b);

        return switch (operator) {
            case "==" -> strA.equalsIgnoreCase(strB);
            case "!=" -> !strA.equalsIgnoreCase(strB);
            default -> throw new IllegalArgumentException("Invalid operator: " + operator);
        };
    }
}
