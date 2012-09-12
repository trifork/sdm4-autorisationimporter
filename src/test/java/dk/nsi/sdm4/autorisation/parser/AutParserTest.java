/**
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Contributor(s): Contributors are attributed in the source code
 * where applicable.
 *
 * The Original Code is "Stamdata".
 *
 * The Initial Developer of the Original Code is Trifork Public A/S.
 *
 * Portions created for the Original Code are Copyright 2011,
 * Lægemiddelstyrelsen. All Rights Reserved.
 *
 * Portions created for the FMKi Project are Copyright 2011,
 * National Board of e-Health (NSI). All Rights Reserved.
 */

package dk.nsi.sdm4.autorisation.parser;

import dk.nsi.sdm4.autorisation.model.Autorisation;
import dk.nsi.sdm4.core.parser.Parser;
import dk.nsi.sdm4.core.parser.ParserException;
import dk.nsi.sdm4.core.persistence.Persister;
import dk.sdsd.nsp.slalog.api.SLALogger;
import dk.sdsd.nsp.slalog.impl.SLALoggerDummyImpl;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class AutParserTest {
	@Configuration
	@PropertySource({"classpath:test.properties"})
	static class TestConf {
		@Bean
		public Parser parser() {
			return new AutorisationParser();
		}

		@Bean
		public SLALogger slaLogger() {
			return new SLALoggerDummyImpl();
		}

		@Bean
		public Persister persister() {
			return mock(Persister.class);
		}

		@Bean
		public JdbcTemplate jdbcTemplate() {
			return mock(JdbcTemplate.class);
		}

		@Bean
		public static PropertySourcesPlaceholderConfigurer properties(){
			return new PropertySourcesPlaceholderConfigurer();
		}
	}

    @Autowired
    AutorisationParser parser;

	@Autowired
	JdbcTemplate jdbcTemplate;

    public static File valid;
    public static File invalid;
	private File validWith3Removed;

	@Before
    public void setUp() {
        valid = FileUtils.toFile(getClass().getClassLoader().getResource("data/aut/valid/20090915AutDK.csv"));
	    validWith3Removed = FileUtils.toFile(getClass().getClassLoader().getResource("data/aut/validWith3Removed/20090919AutDK.csv"));
	    invalid = FileUtils.toFile(getClass().getClassLoader().getResource("data/aut/invalid/20090915AutDK.csv"));
    }

    @Test
    public void testParse() throws IOException {
        Autorisationsregisterudtraek auts = parser.parse(valid, new DateTime());
        assertEquals(4, auts.getEntities().size());
        Autorisation a = auts.getEntityById("0013H");
        assertNotNull(a);
        assertEquals("0101280063", a.getCpr());
        assertEquals("Tage Søgaard", a.getFornavn());
    }

	@Test
	public void doesNotAllowNumberOfAutorisationerToDecreaseMoreThanThreshold() throws IOException {
		when(jdbcTemplate.queryForInt(any(String.class))).thenReturn(5); // der er to indgange i den fil vi importer om lidt

		try {
			parser.process(validWith3Removed.getParentFile());
			fail("Expected exception from parser because number of autorisationer decreased too much");
		} catch (ParserException e) {
			assertThat(e.getMessage(), containsString(validWith3Removed.getName()));
			assertThat(e.getMessage(), containsString("1")); // max allowed reduction
			assertThat(e.getMessage(), containsString("3")); // reduktionen som antal
			assertThat(e.getMessage(), containsString("5")); // antal i databasen
		}
	}

	@Test(expected = ParserException.class)
    public void testInvalid() throws IOException {
        parser.parse(invalid, new DateTime());
    }
}
