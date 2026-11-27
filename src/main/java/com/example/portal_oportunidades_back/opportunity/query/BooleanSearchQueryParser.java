package com.example.portal_oportunidades_back.opportunity.query;

import com.example.portal_oportunidades_back.exception.BadRequestException;

import java.util.ArrayList;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public final class BooleanSearchQueryParser {
    private static final int MAX_TOKENS = 32;
    private static final int MAX_DEPTH = 8;

    public sealed interface SearchNode permits Term, Phrase, And, Or, Not {}
    public record Term(String value) implements SearchNode {}
    public record Phrase(String value) implements SearchNode {}
    public record And(SearchNode left, SearchNode right) implements SearchNode {}
    public record Or(SearchNode left, SearchNode right) implements SearchNode {}
    public record Not(SearchNode child) implements SearchNode {}

    private enum Type { WORD, PHRASE, AND, OR, NOT, LPAREN, RPAREN, END }
    private record Token(Type type, String text) {}

    public SearchNode parse(String input) {
        if (input == null || input.isBlank()) {
            throw invalid("Search query is empty");
        }
        Parser parser = new Parser(tokenize(input));
        SearchNode result = parser.parseOr(0);
        if (!parser.check(Type.END)) throw invalid("Unexpected token in search query");
        if (!hasPositiveTerm(result, false)) throw invalid("Search must contain a positive term");
        return result;
    }

    private ArrayList<Token> tokenize(String input) {
        ArrayList<Token> result = new ArrayList<>();
        for (int i = 0; i < input.length();) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '(') { result.add(new Token(Type.LPAREN, "(")); i++; continue; }
            if (c == ')') { result.add(new Token(Type.RPAREN, ")")); i++; continue; }
            if (c == '"') {
                int end = input.indexOf('"', i + 1);
                if (end < 0) throw invalid("Unclosed phrase in search query");
                String phrase = input.substring(i + 1, end).strip();
                if (phrase.isEmpty()) throw invalid("Search phrase cannot be empty");
                result.add(new Token(Type.PHRASE, phrase));
                i = end + 1;
                continue;
            }
            int start = i;
            while (i < input.length() && !Character.isWhitespace(input.charAt(i))
                    && input.charAt(i) != '(' && input.charAt(i) != ')' && input.charAt(i) != '"') i++;
            String word = input.substring(start, i);
            Type type = switch (word.toUpperCase(Locale.ROOT)) {
                case "AND" -> Type.AND;
                case "OR" -> Type.OR;
                case "NOT" -> Type.NOT;
                default -> Type.WORD;
            };
            result.add(new Token(type, word));
            if (result.size() > MAX_TOKENS) throw invalid("Search query has too many terms");
        }
        if (result.size() > MAX_TOKENS) throw invalid("Search query has too many terms");
        result.add(new Token(Type.END, ""));
        return result;
    }

    private boolean hasPositiveTerm(SearchNode node, boolean negated) {
        return switch (node) {
            case Term ignored -> !negated;
            case Phrase ignored -> !negated;
            case Not not -> hasPositiveTerm(not.child(), !negated);
            case And and -> hasPositiveTerm(and.left(), negated) || hasPositiveTerm(and.right(), negated);
            case Or or -> hasPositiveTerm(or.left(), negated) || hasPositiveTerm(or.right(), negated);
        };
    }

    private static BadRequestException invalid(String message) {
        return new BadRequestException(message);

    }

    private static final class Parser {
        private final ArrayList<Token> tokens;
        private int position;

        private Parser(ArrayList<Token> tokens) { this.tokens = tokens; }

        private SearchNode parseOr(int depth) {
            SearchNode node = parseAnd(depth);
            while (match(Type.OR)) node = new Or(node, parseAnd(depth));
            return node;
        }

        private SearchNode parseAnd(int depth) {
            SearchNode node = parseUnary(depth);
            while (true) {
                if (match(Type.AND)) {
                    if (!startsUnary(peek().type())) throw invalid("AND must be followed by a term");
                    node = new And(node, parseUnary(depth));
                } else if (startsUnary(peek().type())) {
                    node = new And(node, parseUnary(depth)); // implicit AND
                } else {
                    return node;
                }
            }
        }

        private SearchNode parseUnary(int depth) {
            if (match(Type.NOT)) {
                if (!startsUnary(peek().type())) throw invalid("NOT must be followed by a term");
                return new Not(parseUnary(depth));
            }
            return parsePrimary(depth);
        }

        private SearchNode parsePrimary(int depth) {
            if (match(Type.WORD)) return new Term(previous().text());
            if (match(Type.PHRASE)) return new Phrase(previous().text());
            if (match(Type.LPAREN)) {
                if (depth >= MAX_DEPTH) throw invalid("Search query is nested too deeply");
                SearchNode node = parseOr(depth + 1);
                if (!match(Type.RPAREN)) throw invalid("Unclosed parenthesis in search query");
                return node;
            }
            throw invalid("Expected a search term");
        }

        private boolean startsUnary(Type type) {
            return type == Type.WORD || type == Type.PHRASE || type == Type.NOT || type == Type.LPAREN;
        }
        private boolean check(Type type) { return peek().type() == type; }
        private boolean match(Type type) {
            if (!check(type)) return false;
            position++;
            return true;
        }
        private Token peek() { return tokens.get(position); }
        private Token previous() { return tokens.get(position - 1); }
    }

}
