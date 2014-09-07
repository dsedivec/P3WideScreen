/*
 * Copyright (C) 2014  Dale Sedivec
 *
 * This file is part of P3WideScreen.
 *
 * P3WideScreen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * P3WideScreen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with P3WideScreen.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.codefu.p3widescreen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class INIPatcher {
    private static final int LINE_ENDING_READ_AHEAD = 512;

    private class INIPatch {
        public final String section;
        public final String key;
        public final String expectedValue;
        public final String newValue;

        public INIPatch(String section, String key,
                        String expectedValue, String newValue) {
            this.section = section;
            this.key = key;
            this.expectedValue = expectedValue;
            this.newValue = newValue;
        }

        @Override
        public int hashCode() {
            return section.hashCode() ^ key.hashCode();
        }
    }

    private final Pattern lineEndingPattern = Pattern.compile("\\n|\\r\\n?");
    private final Pattern sectionPattern =
        Pattern.compile("^\\[(?<name>[^\\]]+)\\]$");
    private final Pattern keyValuePattern =
        Pattern.compile("^(?<key>\\w+)=(?<value>.*)");

    private final HashMap<String, HashMap<String, INIPatch>> sections =
        new HashMap<>();
    private final HashSet<INIPatch> allPatches = new HashSet<>();

    public void addPatch(String section, String key, String expectedValue,
                         String newValue) {
        HashMap<String, INIPatch> keys = sections.get(section);
        if (keys == null) {
            keys = new HashMap<>();
            sections.put(section, keys);
        } else if (keys.containsKey(key)) {
            throw new RuntimeException(String.format(
                "already have a patch for %s in section %s", key, section));
        }
        INIPatch patch = new INIPatch(section, key, expectedValue, newValue);
        keys.put(key, patch);
        allPatches.add(patch);
    }

    private String sniffLineEndings(Reader reader) throws IOException {
        if (!reader.markSupported()) {
            throw new RuntimeException(String.format(
                "reader %s doesn't support marks", reader));
        }
        reader.mark(LINE_ENDING_READ_AHEAD);
        // -1 here because, from the sound of the mark/reset contract,
        // if you read *up to* the limit, you may not be able to
        // reset anymore.
        CharBuffer buf = CharBuffer.allocate(LINE_ENDING_READ_AHEAD - 1);
        reader.read(buf);
        reader.reset();
        buf.flip();
        Matcher matcher = lineEndingPattern.matcher(buf);
        if (matcher.find()) {
            return matcher.group();
        } else {
            throw new RuntimeException(String.format(
                "couldn't find end of line within %d byte(s)",
                LINE_ENDING_READ_AHEAD - 1));
        }
    }

    public void patch(Reader input, Writer output,
                      Map<String, Object> substitutions)
            throws IOException {
        BufferedReader bufferedInput = new BufferedReader(input);
        HashSet<INIPatch> patchesApplied = new HashSet<>();
        String lineEnding = sniffLineEndings(bufferedInput);
        HashMap<String, INIPatch> keys = null;
        while (true) {
            String line = bufferedInput.readLine();
            if (line == null) {
                break;
            }
            Matcher matcher = sectionPattern.matcher(line);
            if (matcher.matches()) {
                keys = sections.get(matcher.group("name"));
            } else if (keys != null) {
                matcher = keyValuePattern.matcher(line);
                if (matcher.matches()) {
                    String key = matcher.group("key");
                    INIPatch patch = keys.get(key);
                    if (patch != null) {
                        line = applyPatch(matcher, patch, substitutions);
                        patchesApplied.add(patch);
                    }
                }
            }
            output.write(line);
            // I hope they don't depend on any INI files having no line
            // ending at end of file.
            output.write(lineEnding);
        }
        if (!patchesApplied.equals(allPatches)) {
            HashSet<INIPatch> missingPatches = new HashSet<>(allPatches);
            missingPatches.removeAll(patchesApplied);
            List<String> descriptions = missingPatches.stream()
                .map(patch -> String.format("%s/%s", patch.section, patch.key))
                .collect(Collectors.toList());
            throw new RuntimeException("failed to apply patches: "
                                       + String.join(", ", descriptions));
        }
    }

    private final Pattern substitutionPattern = Pattern.compile("\\.|\\$\\w+");

    private String substituteValues(String template,
                                    Map<String, Object> substitutions) {
        Matcher matcher = substitutionPattern.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String matchText = matcher.group();
            String replacement;
            if (matchText.charAt(0) == '\\') {
                assert matchText.length() == 2;
                replacement = matchText.substring(1);
            } else {
                assert matchText.charAt(0) == '$';
                Object replacementObj =
                    substitutions.get(matchText.substring(1));
                if (replacementObj == null) {
                    throw new RuntimeException(String.format(
                        "no substitution given for %s in \"%s\"",
                        matchText, template));
                }
                replacement = replacementObj.toString();
            }
            matcher.appendReplacement(result,
                                      Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String applyPatch(Matcher matcher, INIPatch patch,
                              Map<String, Object> substitutions) {
        String value = matcher.group("value");
        if (value.equals(patch.expectedValue)) {
            String newValue = substituteValues(patch.newValue, substitutions);
            return String.format("%s=%s", patch.key, newValue);
        } else {
            throw new RuntimeException(String.format(
                ("expected %s in section %s to have value"
                    + " \"%s\" but it has value \"%s\" instead"),
                patch.key, patch.section, patch.expectedValue, value));
        }
    }
}
