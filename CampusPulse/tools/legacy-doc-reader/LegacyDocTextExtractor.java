import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;

/**
 * Extracts readable text from a legacy Microsoft Word .doc file without changing
 * the source document. This utility is for reviewing course materials only.
 */
public final class LegacyDocTextExtractor {
    private LegacyDocTextExtractor() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: LegacyDocTextExtractor <input.doc> <output.txt>");
        }

        File input = new File(args[0]);
        File output = new File(args[1]);
        if (!input.isFile()) {
            throw new IllegalArgumentException("Input file does not exist: " + input.getAbsolutePath());
        }

        try (FileInputStream inputStream = new FileInputStream(input);
             HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document);
             FileOutputStream stream = new FileOutputStream(output)) {
            StringBuilder text = new StringBuilder();
            for (String paragraph : extractor.getParagraphText()) {
                text.append(paragraph.replace('\r', '\n'));
                if (!paragraph.endsWith("\n")) {
                    text.append(System.lineSeparator());
                }
            }
            stream.write(text.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
