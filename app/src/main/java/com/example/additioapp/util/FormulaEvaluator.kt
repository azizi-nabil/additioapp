package com.example.additioapp.util

import java.util.Stack
import kotlin.math.max
import kotlin.math.min

object FormulaEvaluator {

    fun evaluate(formula: String, variables: Map<String, Float>): Float {
        try {
            // 1. Remove spaces from expression first to handle "Test 1" vs "Test1" in formula
            var expression = formula.replace(" ", "")
            android.util.Log.d("FormulaEvaluator", "Expression after trimming: $expression")
            android.util.Log.d("FormulaEvaluator", "Variables: $variables")

            // Sort variables by length descending
            val sortedVariables = variables.entries.sortedByDescending { it.key.length }
            
            sortedVariables.forEach { (name, value) ->
                // Prepare variable name variants
                val variants = mutableListOf<String>()
                
                // 1. [Name] (with spaces removed from name if any, since expression has no spaces)
                val nameNoSpaces = name.replace(" ", "")
                variants.add("\\[$nameNoSpaces\\]")
                
                // 2. Name (no spaces)
                variants.add(java.util.regex.Pattern.quote(nameNoSpaces))
                
                variants.forEach { variant ->
                    // Use regex to replace
                    // Note: Since we removed spaces from expression, word boundaries \b might behave differently
                    // e.g. "max(Test1,Test2)". "Test1" is bounded by "(" and ",".
                    // "(" and "," are non-word chars, so \b works.
                    val regex = Regex("\\b$variant\\b")
                    if (regex.containsMatchIn(expression)) {
                         android.util.Log.d("FormulaEvaluator", "Replaced '$variant' (from '$name') with '$value'")
                    }
                    expression = expression.replace(regex, value.toString())
                }
            }
            android.util.Log.d("FormulaEvaluator", "Expression after replacement: $expression")
            
            val result = parse(expression)
            android.util.Log.d("FormulaEvaluator", "Result: $result")
            return result
            // Remove spaces
            expression = expression.replace(" ", "")
            return parse(expression)
        } catch (e: Exception) {
            e.printStackTrace()
            return 0f
        }
    }

    private fun parse(expression: String): Float {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < expression.length) expression[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Float {
                nextChar()
                val x = parseExpression()
                if (pos < expression.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Float {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }

            fun parseTerm(): Float {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) x /= parseFactor() // division
                    else return x
                }
            }

            fun parseFactor(): Float {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Float
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = expression.substring(startPos, pos).toFloat()
                } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func = expression.substring(startPos, pos)
                    if (eat('('.code)) {
                        val args = ArrayList<Float>()
                        do {
                            args.add(parseExpression())
                        } while (eat(','.code))
                        eat(')'.code)
                        
                        x = when (func) {
                            "max" -> if (args.isNotEmpty()) args.maxOrNull() ?: 0f else 0f
                            "min" -> if (args.isNotEmpty()) args.minOrNull() ?: 0f else 0f
                            "avg", "mean" -> if (args.isNotEmpty()) args.average().toFloat() else 0f
                            else -> 0f
                        }
                    } else {
                        x = 0f
                    }
                } else {
                    x = 0f
                }
                return x
            }
        }.parse()
    }
}
