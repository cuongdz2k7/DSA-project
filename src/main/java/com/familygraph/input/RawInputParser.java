package com.familygraph.input;

import com.familygraph.input.model.GraphValidationResult;
import com.familygraph.input.model.ParseResult;
import com.familygraph.model.validation.ValidationResult;

/** Public facade for the parse, validate, and normalize input pipeline. */
public final class RawInputParser {
    private final RawTextParser parser;
    private final ParsedInputValidator validator;
    private final InputNormalizer normalizer;

    public RawInputParser() {
        this(new RawTextParser(), new ParsedInputValidator(), new InputNormalizer());
    }

    public RawInputParser(RawTextParser parser,
                          ParsedInputValidator validator,
                          InputNormalizer normalizer) {
        this.parser = parser;
        this.validator = validator;
        this.normalizer = normalizer;
    }

    public ValidationResult parseAndValidate(String rawInput) {
        ParseResult parsed = parser.parse(rawInput);
        if (!parsed.successful()) {
            return ValidationResult.failure(parsed.error());
        }

        GraphValidationResult validated = validator.validate(parsed.data());
        if (!validated.valid()) {
            return ValidationResult.failure(validated.error());
        }

        return ValidationResult.success(normalizer.normalize(validated.data()));
    }
}
