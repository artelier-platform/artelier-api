package com.artelier.api.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

import java.text.Normalizer;
import java.util.Locale;

public final class SearchTerms {

    public static final char ESCAPE = '\\';

    private SearchTerms() {}

    /** Devuelve "%termino%" sin tildes, en minusculas y con % _ \ escapados; null si esta vacio. */
    public static String toLikePattern(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String n = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        n = n.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + n + "%";
    }

    public static Expression<String> normalized(CriteriaBuilder cb, Expression<String> e) {
        return cb.function("unaccent", String.class, cb.lower(e));
    }
}
