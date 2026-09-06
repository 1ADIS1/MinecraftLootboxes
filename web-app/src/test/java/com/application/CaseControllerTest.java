package com.application;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CaseControllerTest {
    @Test
    void requiresSessionAndUsesItsPlayerRatherThanClientSuppliedIdentity() {
        Application controller = new Application();
        UserRepository repository = mock(UserRepository.class);
        CaseService service = mock(CaseService.class);
        ReflectionTestUtils.setField(controller, "userRepository", repository);
        ReflectionTestUtils.setField(controller, "caseService", service);
        MockHttpSession session = new MockHttpSession();
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> controller.onCaseOpen("armour", session)).getStatusCode().value());
        verifyNoInteractions(service, repository);

        User user = new User();
        user.setId(7);
        user.setLogin("RealPlayer");
        session.setAttribute("userId", 7);
        when(repository.findById(7)).thenReturn(Optional.of(user));
        CaseCatalog.Drop drop = CaseCatalog.items("armour").getFirst();
        when(service.open("RealPlayer", "armour")).thenReturn(drop);
        assertEquals(drop, controller.onCaseOpen("armour", session));
        verify(service).open("RealPlayer", "armour");
    }
}
