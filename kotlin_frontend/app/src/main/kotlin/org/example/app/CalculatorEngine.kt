package org.example.app

import kotlin.math.*

/**
 * PUBLIC_INTERFACE
 * CalculatorEngine parses and evaluates mathematical expressions supporting
 * +, -, *, /, ^, parentheses, decimals, sin, cos, tan, log, ln, sqrt.
 * Throws IllegalArgumentException for invalid expressions.
 */
object CalculatorEngine {
    // Supported function names
    private val functions = setOf("sin", "cos", "tan", "log", "ln", "√")

    /**
     * PUBLIC_INTERFACE
     * Evaluates an expression string and returns the result as a Double.
     * Supported: +, -, *, /, ^, sin, cos, tan, log, ln, sqrt, parentheses.
     * Example: "sin(0) + log(10) * 2"
     */
    fun evaluate(rawExpr: String): Double {
        val expr = preprocess(rawExpr)
        val parser = Parser(expr)
        val value = parser.parseExpression()
        if (parser.hasMore()) {
            throw IllegalArgumentException("Invalid input: extra characters")
        }
        return value
    }

    // Preprocess unicode operators: ×, ÷, − to *, /, -; √(X) to sqrt(X)
    private fun preprocess(raw: String): String {
        return raw.replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("√", "sqrt")
            .replace("\\s+".toRegex(), "")
    }

    // Simple recursive descent parser for mathematical expressions
    private class Parser(val input: String) {
        var pos = 0
        val len = input.length

        fun hasMore(): Boolean = pos < len

        fun peek(): Char? = if (pos < len) input[pos] else null

        fun skipWhitespace() {
            while (hasMore() && input[pos].isWhitespace()) pos++
        }

        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipWhitespace()
                val op = peek()
                if (op == '+' || op == '-') {
                    pos++
                    val right = parseTerm()
                    value = if (op == '+') value + right else value - right
                } else {
                    break
                }
            }
            return value
        }

        fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                skipWhitespace()
                val op = peek()
                if (op == '*' || op == '/') {
                    pos++
                    val right = parseFactor()
                    value = if (op == '*') value * right else value / right
                } else {
                    break
                }
            }
            return value
        }

        fun parseFactor(): Double {
            skipWhitespace()
            var value = parseUnary()
            skipWhitespace()
            while (peek() == '^') {
                pos++
                val power = parseUnary()
                value = value.pow(power)
                skipWhitespace()
            }
            return value
        }

        fun parseUnary(): Double {
            skipWhitespace()
            val op = peek()
            return when (op) {
                '+' -> { pos++; parseUnary() }
                '-' -> { pos++; -parseUnary() }
                else -> parsePrimary()
            }
        }

        fun parsePrimary(): Double {
            skipWhitespace()
            val c = peek()
            return when {
                c == '(' -> {
                    pos++
                    val value = parseExpression()
                    if (peek() != ')') error("Missing ')'")
                    pos++
                    value
                }
                c?.isDigit() == true || c == '.' -> parseNumber()
                c?.isLetter() == true -> parseFunction()
                else -> error("Unexpected character: $c")
            }
        }

        fun parseNumber(): Double {
            val start = pos
            while (hasMore() && (input[pos].isDigit() || input[pos] == '.')) pos++
            if (start == pos) error("Number expected")
            val numStr = input.substring(start, pos)
            return numStr.toDoubleOrNull() ?: error("Invalid number: $numStr")
        }

        fun parseFunction(): Double {
            val start = pos
            while (hasMore() && (input[pos].isLetter() || input[pos] == '_')) pos++
            val name = input.substring(start, pos).lowercase()
            if (name == "sqrt") {
                // sqrt(X) or sqrtX, require parenthesis
                if (peek() == '(') {
                    pos++
                    val arg = parseExpression()
                    if (peek() != ')') error("Missing ')' after sqrt")
                    pos++
                    return sqrt(arg)
                } else {
                    error("Expected '(' after sqrt")
                }
            }
            // Single-argument functions (sin, cos, tan, log, ln)
            if (name in setOf("sin", "cos", "tan", "log", "ln")) {
                if (peek() != '(') error("Expected '(' after $name")
                pos++
                val arg = parseExpression()
                if (peek() != ')') error("Missing ')' after $name")
                pos++
                return when (name) {
                    "sin" -> sin(arg)
                    "cos" -> cos(arg)
                    "tan" -> tan(arg)
                    "log" -> log10(arg)
                    "ln" -> ln(arg)
                    else -> error("Unknown function")
                }
            }
            error("Unknown function: $name")
        }

        fun error(message: String): Nothing {
            throw IllegalArgumentException("$message at position $pos")
        }
    }
}
