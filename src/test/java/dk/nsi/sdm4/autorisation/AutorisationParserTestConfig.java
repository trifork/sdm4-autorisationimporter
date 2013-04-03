package dk.nsi.sdm4.autorisation;

import dk.nsi.sdm4.autorisation.parser.AutorisationParser;
import dk.nsi.sdm4.core.parser.Parser;
import dk.nsi.sdm4.core.persistence.AuditingPersister;
import dk.nsi.sdm4.core.persistence.Persister;
import dk.sdsd.nsp.slalog.api.SLALogger;
import dk.sdsd.nsp.slalog.impl.SLALoggerDummyImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@PropertySource({"classpath:test.properties"})
public class AutorisationParserTestConfig {

    @Bean
    public Parser parser() {
        return new AutorisationParser();
    }

    @Bean
    public Persister persister() {
        return new AuditingPersister();
    }

    @Bean
    public SLALogger slaLogger() {
        return new SLALoggerDummyImpl();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties(){
        return new PropertySourcesPlaceholderConfigurer();
    }
}
