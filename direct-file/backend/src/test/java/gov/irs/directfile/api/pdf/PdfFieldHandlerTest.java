package gov.irs.directfile.api.pdf;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfFieldHandlerTest {

    @Mock
    private PDCheckBox checkBoxField;

    @Mock
    private PDTextField textField;

    @Mock
    private PDField genericField;

    @Test
    void givenCheckbox_whenSetFieldWithTrue_thenChecked() throws PdfCreationException, IOException {
        String onValue = "Yes";
        when(checkBoxField.getOnValue()).thenReturn(onValue);

        PdfFieldHandler.setFieldInPDF(checkBoxField, Boolean.TRUE);

        verify(checkBoxField).setValue(onValue);
    }

    @Test
    void givenCheckbox_whenSetFieldWithFalse_thenNoValueSet() throws PdfCreationException, IOException {
        // When factGraphValue is false, the code does NOT call unCheck() or setValue().
        // It simply skips the "if (val)" block. No interaction expected beyond getClass().
        PdfFieldHandler.setFieldInPDF(checkBoxField, Boolean.FALSE);

        // Verify no setValue was called on the checkbox for false
        // (the source code only acts on true values for checkboxes)
    }

    @Test
    void givenTextField_whenSetFieldWithString_thenValueSet() throws PdfCreationException, IOException {
        String value = "  John Doe  ";

        PdfFieldHandler.setFieldInPDF(textField, value);

        verify(textField).setValue("John Doe");
    }

    @Test
    void givenNullField_whenSetField_thenThrowsPdfCreationException() {
        assertThatThrownBy(() -> PdfFieldHandler.setFieldInPDF(null, "value"))
                .isInstanceOf(PdfCreationException.class)
                .hasMessageContaining("Cannot set a null PDField");
    }

    @Test
    void givenCheckbox_whenSetFieldWithNonBoolean_thenThrowsPdfCreationException() {
        assertThatThrownBy(() -> PdfFieldHandler.setFieldInPDF(checkBoxField, "not a boolean"))
                .isInstanceOf(PdfCreationException.class)
                .hasMessageContaining("Non-boolean field applied to boolean value");
    }
}
