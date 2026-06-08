package com.otzar.sscm;

import com.otzar.sscm.entities.Client;
import com.otzar.sscm.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialContentManagerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void getContentsByClientReturnsEmptyListForExistingClientWithNoContent() throws Exception {
        Client client = new Client();
        client.setUser_id(2L);
        client.setAdmin_id(null);
        client.setBusiness_name("Client Without Content");
        client.setPhone("0500000000");

        clientRepository.save(client);

        mockMvc.perform(get("/contents/client/{clientId}", client.getClient_id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void getContentsByClientReturnsNotFoundForMissingClient() throws Exception {
        mockMvc.perform(get("/contents/client/{clientId}", 9999L))
                .andExpect(status().isNotFound());
    }

}
