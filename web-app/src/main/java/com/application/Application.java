package com.application;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@RestController
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseService caseService;

    private final Set<Integer> openingUsers = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostMapping("/login")
    public @ResponseBody String login(@RequestParam String login, @RequestParam String password, HttpSession session) {
        User user = userRepository.findByLogin(login).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid login or password"
        ));

        if (!user.getPassword().equals(password)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid login or password"
            );
        }

        session.setAttribute("userId", user.getId());

        return "logged-in!";
    }

    @PostMapping(path = "/add") // Map ONLY POST Requests
    public @ResponseBody String addNewUser(@RequestParam String login
            , @RequestParam String password) {
        // @ResponseBody means the returned String is the response, not a view name
        // @RequestParam means it is a parameter from the GET or POST request

        User n = new User();
        n.setLogin(login);
        n.setPassword(password);
        userRepository.save(n);
        return "Saved";
    }

    @GetMapping(path = "/all")
    public @ResponseBody Iterable<User> getAllUsers() {
        // This returns a JSON or XML with the users
        return userRepository.findAll();
    }

    @GetMapping("/api/cases")
    public Map<String, List<CaseCatalog.Drop>> cases() {
        return CaseCatalog.all();
    }

    @PostMapping("/open-case")
    public CaseCatalog.Drop onCaseOpen(@RequestParam String caseId, HttpSession session) {
        Object sessionId = session.getAttribute("userId");
        if (!(sessionId instanceof Integer userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Log in before opening a case.");
        }
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in again."));
        if (!openingUsers.add(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A case is already being opened.");
        }
        try {
            CaseCatalog.Drop reward = caseService.open(user.getLogin(), caseId);
            log.info("Delivered {} from {} case to user {}", reward.material(), caseId, userId);
            return reward;
        } finally {
            openingUsers.remove(userId);
        }
    }

    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Map<String, String>> requestError(ResponseStatusException error) {
        return org.springframework.http.ResponseEntity.status(error.getStatusCode())
                .body(Map.of("message", error.getReason() == null ? "Request failed" : error.getReason()));
    }

    @GetMapping("/hello")
    public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
        return String.format("Hello %s!", name);
    }
}
