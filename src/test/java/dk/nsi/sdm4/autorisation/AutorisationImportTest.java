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
package dk.nsi.sdm4.autorisation;

import dk.nsi.sdm4.core.parser.Parser;
import dk.nsi.sdm4.testutils.TestDbConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.apache.commons.io.FileUtils.toFile;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = {AutorisationParserTestConfig.class, TestDbConfiguration.class})
public class AutorisationImportTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Autowired
    private Parser parser;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testCanImport() throws IOException, InterruptedException {
        File dataset = createTestDataset("data/aut/valid/20090915AutDK.csv");
        parser.process(dataset);
        long cnt = jdbcTemplate.queryForLong("SELECT count(1) FROM Autorisation");
        assertEquals(4, cnt);
        Thread.sleep(1000);

        Date now = new Date();
        Timestamp modifiedDate1 =
                jdbcTemplate.queryForObject("SELECT ModifiedDate FROM Autorisation LIMIT 1", Timestamp.class);

        // Import another file and check validTo is set correctly and modified date is updated
        dataset = createTestDataset("data/aut/valid/20090918AutDK.csv");
        parser.process(dataset);
        cnt = jdbcTemplate.queryForLong("SELECT count(1) FROM Autorisation");
        assertEquals(6, cnt);
        cnt = jdbcTemplate.queryForLong("SELECT count(1) FROM Autorisation WHERE ValidTo>=?", new Timestamp(now.getTime()));
        assertEquals(5, cnt);

        Timestamp modifiedDate2 =
                jdbcTemplate.queryForObject("SELECT ModifiedDate FROM Autorisation ORDER BY ModifiedDate DESC LIMIT 1", Timestamp.class);

        assertFalse(modifiedDate1.equals(modifiedDate2));
    }

    private File createTestDataset(String filename) throws IOException {
        File dataset = temp.newFolder();
        FileUtils.copyFileToDirectory(getFile(filename), dataset);
        return dataset;
    }

    private File getFile(String filename) {
        return toFile(getClass().getClassLoader().getResource(filename));
    }

}
