package gov.irs.directfile.api.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import gov.irs.directfile.api.config.PdfServiceProperties;
import gov.irs.directfile.api.io.IOLocationService;
import gov.irs.directfile.api.loaders.service.FactGraphService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @Mock
    private PdfServiceProperties properties;

    @Mock
    private IOLocationService ioLocationService;

    @Mock
    private FactGraphService factGraphService;

    private PdfService pdfService;

    @BeforeEach
    void setUp() {
        when(properties.getConfiguredPdfs()).thenReturn(Collections.emptyList());
        pdfService = new PdfService(properties, ioLocationService, factGraphService);
    }

    @Test
    void givenEmptyDocumentList_whenMergeDocuments_thenTargetUnchanged() throws Exception {
        PDDocument targetDocument = new PDDocument();
        int initialPageCount = targetDocument.getNumberOfPages();

        ReflectionTestUtils.invokeMethod(pdfService, "mergeDocuments", List.of(), targetDocument);

        assertThat(targetDocument.getNumberOfPages()).isEqualTo(initialPageCount);
        targetDocument.close();
    }

    @Test
    void givenMultipleDocuments_whenMergeDocuments_thenAllPagesMerged() throws Exception {
        PDDocument targetDocument = new PDDocument();

        PDDocument sourceDoc1 = new PDDocument();
        sourceDoc1.addPage(new PDPage());
        sourceDoc1.addPage(new PDPage());

        PDDocument sourceDoc2 = new PDDocument();
        sourceDoc2.addPage(new PDPage());

        List<PDDocument> sourceDocuments = new ArrayList<>();
        sourceDocuments.add(sourceDoc1);
        sourceDocuments.add(sourceDoc2);

        ReflectionTestUtils.invokeMethod(pdfService, "mergeDocuments", sourceDocuments, targetDocument);

        assertThat(targetDocument.getNumberOfPages()).isEqualTo(3);
        targetDocument.close();
    }

    @Test
    void givenOpenDocuments_whenSafelyClose_thenClosesAllDocuments() throws IOException {
        PDDocument doc1 = new PDDocument();
        doc1.addPage(new PDPage());
        PDDocument doc2 = new PDDocument();
        doc2.addPage(new PDPage());

        List<PDDocument> documents = List.of(doc1, doc2);

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(pdfService, "safelyClosePDDocuments", documents))
                .doesNotThrowAnyException();
    }

    @Test
    void givenAlreadyClosedDocument_whenSafelyClose_thenHandlesGracefully() throws IOException {
        PDDocument doc1 = new PDDocument();
        doc1.close(); // already closed

        PDDocument doc2 = new PDDocument();
        doc2.addPage(new PDPage());

        List<PDDocument> documents = List.of(doc1, doc2);

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(pdfService, "safelyClosePDDocuments", documents))
                .doesNotThrowAnyException();
    }
}
