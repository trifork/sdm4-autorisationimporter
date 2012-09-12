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

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.base.Preconditions;

import dk.nsi.sdm4.autorisation.model.Autorisation;
import dk.nsi.sdm4.core.parser.Parser;
import dk.nsi.sdm4.core.parser.ParserException;
import dk.nsi.sdm4.core.persistence.Persister;
import dk.nsi.sdm4.core.util.Dates;
import dk.sdsd.nsp.slalog.api.SLALogItem;
import dk.sdsd.nsp.slalog.api.SLALogger;

public class AutorisationParser implements Parser {
    private static final String FILENAME_DATE_FORMAT = "yyyyMMdd";
    private static final String FILE_ENCODING = "ISO8859-15";
	public static final String FROMCLAUSE_VALID_AUTORISATIONER = "FROM Autorisation WHERE ValidFrom <= NOW() AND ValidTo > NOW();";

	@Autowired
    private SLALogger slaLogger;

    @Autowired
    Persister persister;

    @Autowired
    JdbcTemplate jdbcTemplate;

	@Value("${spooler.autorisationimporter.max.allowed.reduction}")
	private int maxAllowedReduction;

	@Override
    public void process(File dataset) throws ParserException {
        Preconditions.checkNotNull(dataset);

        File[] files = dataset.listFiles();

		if (files.length != 1) {
			throw new ParserException("Data directory " + dataset.getAbsolutePath() + " contains " + files.length + " files, but expected exactly one");
		}

        SLALogItem slaLogItem = slaLogger.createLogItem("AutorisationImporter", "All Files");

        try {
            // Make sure the file set has not been imported before.
            // Check what the previous highest version is (the ValidFrom column).

            Timestamp previousVersion;
            try {
                previousVersion = jdbcTemplate.queryForObject("SELECT MAX(ValidFrom) as version FROM Autorisation", Timestamp.class);
            } catch (EmptyResultDataAccessException e) {
                previousVersion = null;
            }

            DateTime currentVersion = getDateFromFilename(files[0].getName());

            if (previousVersion != null && !currentVersion.isAfter(previousVersion.getTime())) {
                throw new Exception("The version of autorisationsregister that was placed for import was out of order. current_version='"
                                + previousVersion + "', new_version='" + currentVersion + "'.");
            }

            for (File file : files) {
                Autorisationsregisterudtraek autRegisterDataset = parse(file, currentVersion);
	            guardAgainsUnacceptableReduction(file, autRegisterDataset);
		        persister.persistCompleteDataset(autRegisterDataset);
            }

            // Update the table for the STS.
            jdbcTemplate.execute("TRUNCATE TABLE autreg");
            jdbcTemplate.update("INSERT INTO autreg (cpr, given_name, surname, aut_id, edu_id) SELECT cpr, Fornavn, Efternavn, Autorisationsnummer, UddannelsesKode " + FROMCLAUSE_VALID_AUTORISATIONER);
            
            slaLogItem.setCallResultOk();
            slaLogItem.store();
        } catch (Exception e) {
            slaLogItem.setCallResultError("AutorisationImporter failed - Cause: " + e.getMessage());
            slaLogItem.store();
	        if (e instanceof ParserException) {
		        // do not wrap more than we need to
		        throw (ParserException) e;
	        } else {
                throw new ParserException(e);
	        }
        }
    }

	private void guardAgainsUnacceptableReduction(File file, Autorisationsregisterudtraek autRegisterDataset) {
		int currentNumberOfValidAutorisationer = jdbcTemplate.queryForInt("SELECT COUNT(AutorisationPID) " + FROMCLAUSE_VALID_AUTORISATIONER);
		int reduction = currentNumberOfValidAutorisationer - autRegisterDataset.size();
		if (reduction > maxAllowedReduction) {
	        throw new ParserException("Number of autorisationer in file " + file.getAbsolutePath() +
			        " is a reduction of " + reduction + " compared to the current number of active autorisationer " + currentNumberOfValidAutorisationer + " in the database. " +
			        "This is more than the threshold of " + maxAllowedReduction + ", so file is not imported");
		}
	}

	protected DateTime getDateFromFilename(String filename) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(FILENAME_DATE_FORMAT);
        return formatter.parseDateTime(filename.substring(0, 8));
    }

    public Autorisationsregisterudtraek parse(File file, DateTime validFrom) throws IOException {
        Autorisationsregisterudtraek dataset = new Autorisationsregisterudtraek(validFrom.toDate());

        LineIterator lineIterator = FileUtils.lineIterator(file, FILE_ENCODING);

        while (lineIterator.hasNext()) {
            String line = lineIterator.nextLine();

	        Autorisation autorisation = new Autorisation();

	        try {
		        StringTokenizer st = new StringTokenizer(line, ";");

		        autorisation.setAutorisationnummer(st.nextToken());
		        autorisation.setCpr(st.nextToken());
		        autorisation.setEfternavn(st.nextToken());
		        autorisation.setFornavn(st.nextToken());
		        autorisation.setUddannelsesKode(st.nextToken());
	        } catch (RuntimeException e) {
		        throw new ParserException("Unable to parse line " + line, e);
	        }

	        autorisation.setValidFrom(validFrom.toDate());
            autorisation.setValidTo(Dates.THE_END_OF_TIME);

            dataset.add(autorisation);
        }

        return dataset;
    }

    @Override
    public String getHome() {
        return "autorisationimporter";
    }

}
