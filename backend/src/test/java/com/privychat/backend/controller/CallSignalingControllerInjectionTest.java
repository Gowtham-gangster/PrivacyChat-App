package com.privychat.backend.controller;

import com.privychat.backend.dto.call.IceConfigResponse;
import com.privychat.backend.service.CallSignalingService;
import com.privychat.backend.service.MessageService;
import com.privychat.backend.service.TurnCredentialService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CallSignalingControllerInjectionTest {

    @Configuration
    static class TestContextConfig {
        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        public TurnCredentialService turnCredentialService(
                org.springframework.beans.factory.config.AutowireCapableBeanFactory beanFactory) {
            // Let Spring resolve the bean through its autowire mechanism to prove multiple constructors resolve cleanly
            return beanFactory.createBean(TurnCredentialService.class);
        }

        @Bean
        public CallSignalingService callSignalingService() {
            return Mockito.mock(CallSignalingService.class);
        }

        @Bean
        public MessageService messageService() {
            return Mockito.mock(MessageService.class);
        }

        @Bean
        public CallSignalingController callSignalingController(
                CallSignalingService callSignalingService,
                MessageService messageService,
                TurnCredentialService turnCredentialService) {
            return new CallSignalingController(callSignalingService, messageService, turnCredentialService);
        }
    }

    @Test
    @DisplayName("Verify Spring instantiates TurnCredentialService via @Autowired and injects into CallSignalingController")
    void testSpringInstantiatesTurnCredentialServiceAndInjectsIntoCallSignalingController() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        Map<String, Object> props = new HashMap<>();
        props.put("privychat.webrtc.stun-server", "stun:stun.l.google.com:19302");
        props.put("privychat.webrtc.coturn.enabled", "false");
        props.put("privychat.webrtc.coturn.host", "");
        props.put("privychat.webrtc.coturn.port", "3478");
        props.put("privychat.webrtc.coturn.tls-port", "5349");
        props.put("privychat.webrtc.coturn.static-secret", "");
        props.put("privychat.webrtc.coturn.realm", "privychat.app");
        props.put("privychat.webrtc.coturn.ttl-seconds", "86400");
        props.put("privychat.webrtc.turn-server", "");
        props.put("privychat.webrtc.turn-username", "");
        props.put("privychat.webrtc.turn-secret", "");

        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("testProps", props));
        context.register(TestContextConfig.class);
        context.refresh();

        TurnCredentialService turnService = context.getBean(TurnCredentialService.class);
        assertNotNull(turnService, "TurnCredentialService must be instantiated by Spring without NoSuchMethodException");

        CallSignalingController controller = context.getBean(CallSignalingController.class);
        assertNotNull(controller, "CallSignalingController must be instantiated by Spring with constructor parameter 2 injected");

        UUID userId = UUID.randomUUID();
        ResponseEntity<IceConfigResponse> response = controller.getIceServers(userId);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("stun:stun.l.google.com:19302", response.getBody().getIceServers().get(0).getUrls().get(0));

        context.close();
    }
}

