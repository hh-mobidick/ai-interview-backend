package ru.hh.aiinterviewer.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hh.aiinterviewer.domain.model.Role;
import ru.hh.aiinterviewer.domain.model.RoleSynonym;
import ru.hh.aiinterviewer.domain.repository.RoleRepository;
import ru.hh.aiinterviewer.domain.repository.RoleSynonymRepository;

@Service
@RequiredArgsConstructor
public class RolesService {

  private final RoleRepository roleRepository;
  private final RoleSynonymRepository roleSynonymRepository;
  private static final int MAX_RESULTS = 20;

  public List<String> suggest(String query) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    String q = normalize(query);

    // 1) LIKE search on roles and synonyms
    List<Role> rolesByLike = roleRepository.searchByNormalizedContains(q);
    List<RoleSynonym> synonymsByLike = roleSynonymRepository.searchByNormalizedContains(q);

    Set<String> candidates = new LinkedHashSet<>();
    candidates.addAll(rolesByLike.stream().map(Role::getName).collect(Collectors.toList()));
    candidates.addAll(synonymsByLike.stream().map(s -> s.getRole().getName()).collect(Collectors.toList()));

    // 2) Fuzzy ranking using simple Jaro-Winkler
    List<String> ranked = new ArrayList<>(candidates);
    ranked.sort(Comparator.comparingDouble((String r) -> jaroWinkler(normalize(r), q)).reversed());

    return ranked.stream().limit(MAX_RESULTS).collect(Collectors.toList());
  }

  private String normalize(String s) {
    return s.toLowerCase(Locale.ROOT).trim();
  }

  // Minimal Jaro-Winkler similarity
  private double jaroWinkler(String s1, String s2) {
    if (s1.equals(s2)) {
      return 1.0;
    }
    int[] mtp = matches(s1, s2);
    double m = mtp[0];
    if (m == 0) {
      return 0.0;
    }
    double j = (m / s1.length() + m / s2.length() + (m - mtp[1]) / m) / 3.0;
    double jw = j < 0.7 ? j : j + Math.min(0.1, 1.0 / mtp[3]) * mtp[2] * (1.0 - j);
    return jw;
  }

  private int[] matches(String s1, String s2) {
    String max = s1.length() > s2.length() ? s1 : s2;
    String min = s1.length() > s2.length() ? s2 : s1;
    int range = Math.max(max.length() / 2 - 1, 0);
    boolean[] matchFlags = new boolean[max.length()];
    boolean[] matchFlagsMin = new boolean[min.length()];

    int matches = 0;
    for (int i = 0; i < min.length(); i++) {
      int start = Math.max(0, i - range);
      int end = Math.min(i + range + 1, max.length());
      for (int j = start; j < end; j++) {
        if (!matchFlags[j] && min.charAt(i) == max.charAt(j)) {
          matchFlagsMin[i] = true;
          matchFlags[j] = true;
          matches++;
          break;
        }
      }
    }

    if (matches == 0) {
      return new int[] {0, 0, 0, Math.max(s1.length(), s2.length())};
    }

    char[] ms1 = new char[matches];
    char[] ms2 = new char[matches];
    int si = 0;
    for (int i = 0; i < min.length(); i++) {
      if (matchFlagsMin[i]) {
        ms1[si++] = min.charAt(i);
      }
    }
    si = 0;
    for (int i = 0; i < max.length(); i++) {
      if (matchFlags[i]) {
        ms2[si++] = max.charAt(i);
      }
    }

    int transpositions = 0;
    for (int i = 0; i < matches; i++) {
      if (ms1[i] != ms2[i]) {
        transpositions++;
      }
    }
    int prefix = 0;
    int last = Math.min(4, Math.min(s1.length(), s2.length()));
    for (; prefix < last && s1.charAt(prefix) == s2.charAt(prefix); prefix++) {
      // count prefix
    }
    return new int[] {matches, transpositions / 2, prefix, max.length()};
  }
}
