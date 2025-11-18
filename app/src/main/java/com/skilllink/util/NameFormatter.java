package com.skilllink.util;

import android.text.TextUtils;

public final class NameFormatter {

    private NameFormatter() {
        // Utility class
    }

    public static Parts resolve(String fullName, String email) {
        if (!TextUtils.isEmpty(fullName)) {
            String[] tokens = sanitize(fullName).split("\\s+");
            if (tokens.length > 0) {
                String first = capitalize(tokens[0]);
                String last = tokens.length > 1 ? joinAndCapitalize(tokens, 1) : "";
                return new Parts(first, last, tokens.length > 1, Source.EXPLICIT);
            }
        }

        if (!TextUtils.isEmpty(email)) {
            int atPosition = email.indexOf('@');
            if (atPosition > 0) {
                String localPart = email.substring(0, atPosition)
                        .replace('.', ' ')
                        .replace('_', ' ');
                localPart = sanitize(localPart);

                if (!TextUtils.isEmpty(localPart)) {
                    String[] tokens = localPart.split("\\s+");
                    if (tokens.length > 0) {
                        String first = capitalize(tokens[0]);
                        String last = tokens.length > 1 ? joinAndCapitalize(tokens, 1) : "";
                        return new Parts(first, last, tokens.length > 1, Source.INFERRED_FROM_EMAIL);
                    }
                }
            }
        }

        return null;
    }

    private static String sanitize(String value) {
        String trimmed = value.trim();
        return trimmed.replaceAll("\\s+", " ");
    }

    private static String joinAndCapitalize(String[] tokens, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int index = startIndex; index < tokens.length; index++) {
            String token = capitalize(tokens[index]);
            if (!TextUtils.isEmpty(token)) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(token);
            }
        }
        return builder.toString();
    }

    private static String capitalize(String word) {
        if (TextUtils.isEmpty(word)) {
            return "";
        }

        String lower = word.toLowerCase();
        char first = Character.toUpperCase(lower.charAt(0));
        if (lower.length() == 1) {
            return String.valueOf(first);
        }
        return first + lower.substring(1);
    }

    public enum Source {
        EXPLICIT,
        INFERRED_FROM_EMAIL
    }

    public static class Parts {
        private final String firstName;
        private final String lastName;
        private final boolean hasLastName;
        private final Source source;

        Parts(String firstName, String lastName, boolean hasLastName, Source source) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.hasLastName = hasLastName && !TextUtils.isEmpty(lastName);
            this.source = source;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public boolean hasLastName() {
            return hasLastName;
        }

        public Source getSource() {
            return source;
        }

        public String getFullName() {
            if (hasLastName()) {
                return firstName + " " + lastName;
            }
            return firstName;
        }
    }
}
