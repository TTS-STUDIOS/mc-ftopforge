import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * toLowerCase()/toUpperCase() without a Locale follow the machine language:
 * on a Turkish server "DIAMOND".toLowerCase() gives "dıamond" and lookups break.
 * Java 8 on purpose: this plugin compiles with release 8.
 */
class LocaleCaseGuardTest {

    @Test
    @DisplayName("no case change depends on the system locale")
    void caseChangesUseLocaleRoot() throws IOException {
        List<String> offenders = new ArrayList<String>();
        walk(new File("src/main/java"), offenders);
        assertTrue(offenders.isEmpty(),
            "toLowerCase()/toUpperCase() without Locale (use Locale.ROOT) in: " + offenders);
    }

    private void walk(File dir, List<String> offenders) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                walk(f, offenders);
            } else if (f.getName().endsWith(".java")) {
                String code = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                if (code.contains(".toLowerCase()") || code.contains(".toUpperCase()")) offenders.add(f.getPath());
            }
        }
    }
}
