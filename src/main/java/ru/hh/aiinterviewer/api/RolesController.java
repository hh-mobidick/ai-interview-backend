package ru.hh.aiinterviewer.api;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hh.aiinterviewer.service.RolesService;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RolesController {

  private final RolesService rolesService;

  @GetMapping("/suggest")
  public ResponseEntity<List<String>> suggest(@RequestParam("q") String query) {
    return ResponseEntity.ok(rolesService.suggest(query));
  }
}
