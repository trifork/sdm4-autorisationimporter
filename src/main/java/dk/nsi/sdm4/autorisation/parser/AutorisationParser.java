/**
 * The MIT License
 *
 * Original work sponsored and donated by National Board of e-Health (NSI), Denmark
 * (http://www.nsi.dk)
 *
 * Copyright (C) 2011 National Board of e-Health (NSI), Denmark (http://www.nsi.dk)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
    public void process(File dataset, String identifier) throws ParserException {
        Preconditions.checkNotNull(dataset);

        File[] files = dataset.listFiles();

		if (files.length != 1) {
			throw new ParserException("Data directory " + dataset.getAbsolutePath() + " contains " + files.length + " files, but expected exactly one");
		}

        // Make sure we update transaction time
        persister.resetTransactionTime();
        SLALogItem slaLogItem = slaLogger.createLogItem(getHome()+".process", "SDM4."+getHome()+".process");
        slaLogItem.setMessageId(identifier);
        slaLogItem.addCallParameter(Parser.SLA_INPUT_NAME, dataset.getAbsolutePath());
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

            int processed = 0;
            for (File file : files) {
                Autorisationsregisterudtraek autRegisterDataset = parse(file, currentVersion);
	            guardAgainsUnacceptableReduction(file, autRegisterDataset);
                processed += autRegisterDataset.size();
		        persister.persistCompleteDataset(autRegisterDataset);
            }

            // Update the table for the STS.
            jdbcTemplate.execute("TRUNCATE TABLE autreg");
            jdbcTemplate.update("INSERT INTO autreg (cpr, given_name, surname, aut_id, edu_id) SELECT cpr, Fornavn, Efternavn, Autorisationsnummer, UddannelsesKode " + FROMCLAUSE_VALID_AUTORISATIONER);

            slaLogItem.addCallParameter(Parser.SLA_RECORDS_PROCESSED_MAME, ""+processed);
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
