package edu.tlu.klgd.infracstructure.persistence.jpa;

import edu.tlu.klgd.application.dto.CalculationSettingsDTO;
import edu.tlu.klgd.domain.common.util.TextNormalizer;
import edu.tlu.klgd.domain.entity.ClassRecord;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FormulaRuleEvaluator {
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile(
        "^\\s*(?:n\\u1ebfu|neu)\\s+(.+?)\\s+(?:th\\u00ec|thi)\\s+(.+?)(?:\\s*,\\s*(?:ng\\u01b0\\u1ee3c\\s+l\\u1ea1i|nguoc\\s+lai)\\s+(.+))?$",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern GROUP_SIZE_PATTERN = Pattern.compile("(?:t\\u1eebng\\s+nh\\u00f3m|tung\\s+nhom)\\s+(\\d+(?:[\\.,]\\d+)?)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private FormulaRuleEvaluator() {
    }

    static Evaluation evaluate(ClassRecord record, String ruleCode, String ruleName, String formula, CalculationSettingsDTO settings) {
        Map<String, Double> variables = baseVariables(record, settings);
        String ktnExpression = null;
        Double groupedKtnSum = null;
        Double groupedKtnAverage = null;
        Double standardHours = null;

        for (String rawLine : formula.split(";")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            String normalizedLine = TextNormalizer.normalize(line);
            if (normalizedLine.contains("tung nhom") && ktnExpression != null) {
                GroupedCoefficient groupedCoefficient = groupedCoefficient(record, settings, line, ktnExpression, variables);
                groupedKtnSum = groupedCoefficient.sum();
                groupedKtnAverage = groupedCoefficient.average();
                variables.put("KTN", groupedKtnSum);
            }
            Assignment assignment = assignmentFromLine(line, variables);
            if (assignment == null) {
                continue;
            }
            String target = variableName(assignment.target());
            String expression = prepareExpression(assignment.expression());
            double value = new ExpressionParser(expression, variables).parse();
            variables.put(target, value);
            if ("KTN".equals(target)) {
                ktnExpression = expression;
                if (expression.toUpperCase(Locale.ROOT).contains("SVNHOM") || expression.toUpperCase(Locale.ROOT).contains("SV_NHOM")) {
                    GroupedCoefficient groupedCoefficient = groupedCoefficient(record, settings, line, ktnExpression, variables);
                    groupedKtnSum = groupedCoefficient.sum();
                    groupedKtnAverage = groupedCoefficient.average();
                    variables.put("KTN", groupedKtnSum);
                }
            }
            if ("GIOCHUAN".equals(target) || "STANDARDHOURS".equals(target)) {
                standardHours = value;
            }
        }

        if (standardHours == null) {
            standardHours = variables.getOrDefault("K", 1.0) * variables.getOrDefault("TC", 0.0) * 15.0;
        }

        Double k = variables.get("K");
        Double klt = variables.get("KLT");
        Double ktn = groupedKtnAverage == null ? variables.get("KTN") : groupedKtnAverage;
        return new Evaluation(ruleCode, ruleName, k, klt, ktn, standardHours);
    }

    private static Assignment assignmentFromLine(String line, Map<String, Double> variables) {
        Matcher matcher = CONDITIONAL_PATTERN.matcher(line);
        if (matcher.matches()) {
            boolean condition = evaluateCondition(matcher.group(1), variables);
            String selected = condition ? matcher.group(2) : matcher.group(3);
            if (selected == null || selected.isBlank()) {
                return null;
            }
            return simpleAssignment(selected);
        }
        return simpleAssignment(line);
    }

    private static Assignment simpleAssignment(String line) {
        int index = line.indexOf('=');
        if (index < 0) {
            return null;
        }
        return new Assignment(line.substring(0, index), line.substring(index + 1));
    }

    private static boolean evaluateCondition(String condition, Map<String, Double> variables) {
        String prepared = prepareExpression(condition);
        for (String operator : new String[] {"<=", ">=", "==", "<", ">"}) {
            int index = prepared.indexOf(operator);
            if (index > 0) {
                double left = new ExpressionParser(prepared.substring(0, index), variables).parse();
                double right = new ExpressionParser(prepared.substring(index + operator.length()), variables).parse();
                return switch (operator) {
                    case "<=" -> left <= right;
                    case ">=" -> left >= right;
                    case "==" -> Math.abs(left - right) < 0.0000001;
                    case "<" -> left < right;
                    case ">" -> left > right;
                    default -> false;
                };
            }
        }
        return new ExpressionParser(prepared, variables).parse() != 0;
    }

    private static GroupedCoefficient groupedCoefficient(
        ClassRecord record,
        CalculationSettingsDTO settings,
        String standardLine,
        String ktnExpression,
        Map<String, Double> variables
    ) {
        double groupSize = settings.labGroupSize();
        Matcher matcher = GROUP_SIZE_PATTERN.matcher(standardLine);
        if (matcher.find()) {
            groupSize = number(matcher.group(1));
        }
        double remainingStudents = Math.max(0, value(record.getStudentCount()));
        double total = 0;
        double groups = 0;
        while (remainingStudents > 0) {
            double currentGroupStudents = Math.min(groupSize, remainingStudents);
            Map<String, Double> groupVariables = new HashMap<>(variables);
            groupVariables.put("SVNHOM", currentGroupStudents);
            groupVariables.put("SV_NHOM", currentGroupStudents);
            total += new ExpressionParser(ktnExpression, groupVariables).parse();
            groups += 1;
            remainingStudents -= currentGroupStudents;
        }
        return new GroupedCoefficient(total, groups == 0 ? null : total / groups);
    }

    private record GroupedCoefficient(double sum, Double average) {
    }

    private static Map<String, Double> baseVariables(ClassRecord record, CalculationSettingsDTO settings) {
        Map<String, Double> variables = new HashMap<>();
        double students = value(record.getStudentCount());
        double credits = value(record.getCredits());
        variables.put("SV", students);
        variables.put("TC", credits);
        variables.put("TINCHI", credits);
        variables.put("SOTUANTHUCTAPTOTNGHIEP", settings.graduationInternshipWeeks());
        variables.put("SONGAYTHUCTAPTRACDIA", settings.surveyingInternshipDays());
        variables.put("SONGAYTHUCTAPMACDINH", settings.defaultInternshipDays());
        variables.put("LABGROUPSIZE", settings.labGroupSize());
        variables.put("PI", Math.PI);
        return variables;
    }

    private static String prepareExpression(String expression) {
        return expression
            .replaceAll("(?iu)t\\u00edn\\s+ch\\u1ec9|tin\\s+chi", "TC")
            .replaceAll("(?iu)s\\u1ed1\\s+tu\\u1ea7n\\s+th\\u1ef1c\\s+t\\u1eadp\\s+t\\u1ed1t\\s+nghi\\u1ec7p|so\\s+tuan\\s+thuc\\s+tap\\s+tot\\s+nghiep", "SOTUANTHUCTAPTOTNGHIEP")
            .replaceAll("(?iu)s\\u1ed1\\s+ng\\u00e0y\\s+th\\u1ef1c\\s+t\\u1eadp\\s+tr\\u1eafc\\s+\\u0111\\u1ecba|so\\s+ngay\\s+thuc\\s+tap\\s+trac\\s+dia", "SONGAYTHUCTAPTRACDIA")
            .replaceAll("(?iu)s\\u1ed1\\s+ng\\u00e0y\\s+th\\u1ef1c\\s+t\\u1eadp\\s+m\\u1eb7c\\s+\\u0111\\u1ecbnh|so\\s+ngay\\s+thuc\\s+tap\\s+mac\\s+dinh", "SONGAYTHUCTAPMACDINH")
            .replaceAll("(?iu)SV\\s+nh\\u00f3m|SV\\s+nhom", "SVNHOM")
            .replace("\u00b2", "^2")
            .replace("\u221a(", "sqrt(")
            .replace('\u00d7', '*')
            .replace('\u00f7', '/')
            .replace("\u2264", "<=")
            .replace("\u2265", ">=")
            .replace('\u221a', ' ')
            .replace("\u03c0", "PI")
            .replaceAll("(?<=\\d),(?=\\d)", ".")
            .trim();
    }

    private static String variableName(String target) {
        String normalized = TextNormalizer.normalize(target).replace(" ", "").toUpperCase(Locale.ROOT);
        if (normalized.equals("GIOCHUAN")) {
            return "GIOCHUAN";
        }
        if (normalized.equals("KLYTHUYET") || normalized.equals("K_LT")) {
            return "KLT";
        }
        if (normalized.equals("KTHUCHANH") || normalized.equals("KTHINGHIEM") || normalized.equals("KTH") || normalized.equals("K_TH")) {
            return "KTN";
        }
        return normalized;
    }

    private static double value(Integer value) {
        return value == null ? 0 : value;
    }

    private static double value(Double value) {
        return value == null ? 0.0 : value;
    }

    private static double number(String value) {
        return Double.parseDouble(value.replace(',', '.'));
    }

    record Evaluation(String ruleCode, String ruleName, Double coefficientK, Double coefficientTheory, Double coefficientPractice, double standardHours) {
    }

    private record Assignment(String target, String expression) {
    }

    private static final class ExpressionParser {
        private final String expression;
        private final Map<String, Double> variables;
        private int position;

        private ExpressionParser(String expression, Map<String, Double> variables) {
            this.expression = expression == null ? "" : expression;
            this.variables = variables;
        }

        private double parse() {
            double value = parseExpression();
            skipWhitespace();
            return value;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value += parseTerm();
                } else if (match('-')) {
                    value -= parseTerm();
                } else {
                    return value;
                }
            }
        }

        private double parseTerm() {
            double value = parsePower();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    value *= parsePower();
                } else if (match('/')) {
                    value /= parsePower();
                } else {
                    return value;
                }
            }
        }

        private double parsePower() {
            double value = parseUnary();
            skipWhitespace();
            if (match('^')) {
                value = Math.pow(value, parsePower());
            }
            return value;
        }

        private double parseUnary() {
            skipWhitespace();
            if (match('+')) {
                return parseUnary();
            }
            if (match('-')) {
                return -parseUnary();
            }
            return parsePrimary();
        }

        private double parsePrimary() {
            skipWhitespace();
            if (match('(')) {
                double value = parseExpression();
                match(')');
                return value;
            }
            if (isNumberStart(peek())) {
                return parseNumber();
            }
            if (isIdentifierStart(peek())) {
                String identifier = parseIdentifier();
                skipWhitespace();
                if (match('(')) {
                    double first = parseExpression();
                    skipWhitespace();
                    if (match(',')) {
                        double second = parseExpression();
                        match(')');
                        return applyFunction(identifier, first, second);
                    }
                    match(')');
                    return applyFunction(identifier, first);
                }
                return variables.getOrDefault(variableName(identifier), 0.0);
            }
            return 0.0;
        }

        private double parseNumber() {
            int start = position;
            while (position < expression.length() && (Character.isDigit(expression.charAt(position)) || expression.charAt(position) == '.')) {
                position++;
            }
            return Double.parseDouble(expression.substring(start, position));
        }

        private String parseIdentifier() {
            int start = position;
            while (position < expression.length()) {
                char c = expression.charAt(position);
                if (Character.isLetterOrDigit(c) || c == '_') {
                    position++;
                } else {
                    break;
                }
            }
            return expression.substring(start, position);
        }

        private double applyFunction(String name, double first) {
            String function = variableName(name);
            return switch (function) {
                case "SQRT" -> Math.sqrt(first);
                case "LOG" -> Math.log10(first);
                default -> first;
            };
        }

        private double applyFunction(String name, double first, double second) {
            String function = variableName(name);
            return switch (function) {
                case "MIN" -> Math.min(first, second);
                case "MAX" -> Math.max(first, second);
                case "ROOT" -> Math.pow(second, 1.0 / first);
                case "LOG" -> Math.log(second) / Math.log(first);
                default -> first;
            };
        }

        private boolean match(char expected) {
            skipWhitespace();
            if (position < expression.length() && expression.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private char peek() {
            skipWhitespace();
            return position < expression.length() ? expression.charAt(position) : '\0';
        }

        private void skipWhitespace() {
            while (position < expression.length() && Character.isWhitespace(expression.charAt(position))) {
                position++;
            }
        }

        private static boolean isNumberStart(char value) {
            return Character.isDigit(value) || value == '.';
        }

        private static boolean isIdentifierStart(char value) {
            return Character.isLetter(value) || value == '_';
        }
    }
}
