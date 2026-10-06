package at.technikum.dms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Startet den kompletten Spring-Kontext – mit H2 statt PostgreSQL (Profil "test").
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DmsApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void openApiSpecIsGenerated() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("DMS REST API"))
				.andExpect(jsonPath("$.paths['/api/documents']").exists())
				.andExpect(jsonPath("$.paths['/api/collections/{id}/documents/{documentId}']").exists());
	}

}
