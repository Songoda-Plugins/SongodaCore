package com.songoda.core.math;

public class Eval {
    private int pos = -1;
    private int ch;
    private final String toParse;
    private final String warningMessage;

    public Eval(String toParse, String warningMessage) {
        this.toParse = toParse;
        this.warningMessage = warningMessage + " ";
    }

    private void nextChar() {
        this.ch = (++this.pos < this.toParse.length()) ? this.toParse.charAt(this.pos) : -1;
    }

    private boolean eat(int charToEat) {
        while (this.ch == ' ') {
            nextChar();
        }

        if (this.ch == charToEat) {
            nextChar();
            return true;
        }

        return false;
    }

    public double parse() {
        nextChar();

        double x = parseExpression();
        if (this.pos < this.toParse.length()) {
            throw new RuntimeException(this.warningMessage + "Unexpected: '" + (char) this.ch + "' at position " + this.pos + " in '" + this.toParse + "'");
        }

        return x;
    }

    // Grammar:
    // expression = term | expression `+` term | expression `-` term
    // term = factor | term `*` factor | term `/` factor
    // factor = `+` factor | `-` factor | `(` expression `)`
    //        | number | functionName factor | factor `^` factor

    private double parseExpression() {
        double x = parseTerm();

        for (; ; ) {
            if (eat('+')) { // addition
                x += parseTerm();
            } else if (eat('-')) { // subtraction
                x -= parseTerm();
            } else {
                return x;
            }
        }
    }

    private double parseTerm() {
        double x = parseFactor();

        for (; ; ) {
            if (eat('*')) { // multiplication
                x *= parseFactor();
            } else if (eat('/')) { // division
                x /= parseFactor();
            } else {
                return x;
            }
        }
    }

    private double parseFactor() {
        if (eat('+')) {
            return parseFactor(); // unary plus
        }
        if (eat('-')) {
            return -parseFactor(); // unary minus
        }

        double x;
        int startPos = this.pos;
        if (eat('(')) { // parentheses
            x = parseExpression();
            eat(')');
        } else if ((this.ch >= '0' && this.ch <= '9') || this.ch == '.') { // numbers
            while ((this.ch >= '0' && this.ch <= '9') || this.ch == '.') {
                nextChar();
            }

            x = Double.parseDouble(this.toParse.substring(startPos, this.pos));
        } else if (this.ch >= 'a' && this.ch <= 'z') { // functions
            while (this.ch >= 'a' && this.ch <= 'z') {
                nextChar();
            }

            String func = this.toParse.substring(startPos, this.pos);
            x = parseFactor();

            switch (func) {
                case "sqrt":
                    x = Math.sqrt(x);
                    break;
                case "sin":
                    x = Math.sin(Math.toRadians(x));
                    break;
                case "cos":
                    x = Math.cos(Math.toRadians(x));
                    break;
                case "tan":
                    x = Math.tan(Math.toRadians(x));
                    break;
                default:
                    throw new RuntimeException(this.warningMessage + "Unknown function: " + func);
            }
        } else {
            throw new RuntimeException(this.warningMessage + "Unexpected: " + (char) this.ch);
        }

        if (eat('^')) {
            x = Math.pow(x, parseFactor()); // exponentiation
        }

        return x;
    }
}
