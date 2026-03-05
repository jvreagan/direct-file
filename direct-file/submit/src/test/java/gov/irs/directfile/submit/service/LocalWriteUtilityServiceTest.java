package gov.irs.directfile.submit.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalWriteUtilityServiceTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        LocalWriteUtilityService.tearDownTestNow();
    }

    @Test
    void givenValidXml_whenWriteXmlToDisk_thenFileCreated() throws Exception {
        // given
        String xmlContent = "<root><element>test data</element></root>";
        String submissionId = "sub-001";
        String fileName = "submission";

        // when
        Path result = LocalWriteUtilityService.writeXmlToDisk(xmlContent, submissionId, tempDir.toString(), fileName);

        // then
        assertThat(result).exists();
        assertThat(result.getFileName().toString()).isEqualTo("submission.xml");
        String writtenContent = Files.readString(result);
        assertThat(writtenContent).isEqualTo(xmlContent);
    }

    @Test
    void givenTestNow_whenToday_thenReturnsExpectedDate() {
        // given
        OffsetDateTime fixedNow = OffsetDateTime.of(2025, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC);
        LocalWriteUtilityService.setUpTestNow(fixedNow);

        // when
        XMLGregorianCalendar result = LocalWriteUtilityService.Today();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2025);
        assertThat(result.getMonth()).isEqualTo(6);
        assertThat(result.getDay()).isEqualTo(15);
    }

    @Test
    void givenValidDateString_whenCreateGregorianDateFromString_thenReturnsCalendar() {
        // given
        String dateString = "2025-03-20T14:30:00Z";

        // when
        XMLGregorianCalendar result = LocalWriteUtilityService.CreateGregorianDateFromString(dateString);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2025);
        assertThat(result.getMonth()).isEqualTo(3);
        assertThat(result.getDay()).isEqualTo(20);
    }

    @Test
    void givenInvalidDateString_whenCreateGregorianDateFromString_thenThrowsException() {
        // given
        String invalidDateString = "not-a-date";

        // when / then
        assertThatThrownBy(() -> LocalWriteUtilityService.CreateGregorianDateFromString(invalidDateString))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
