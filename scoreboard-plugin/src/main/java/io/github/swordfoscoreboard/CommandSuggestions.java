package io.github.swordfoscoreboard;
import java.util.*;
final class CommandSuggestions {
    static List<String> matching(Collection<String> choices,String prefix) {
        String lower=prefix.toLowerCase(Locale.ROOT);
        return choices.stream().filter(s->s.toLowerCase(Locale.ROOT).startsWith(lower)).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
