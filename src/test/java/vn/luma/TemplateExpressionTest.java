package vn.luma;

import java.nio.file.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TemplateExpressionTest {
    @Test void dynamicTextDoesNotLeavePropertyReferencesOutsideThymeleafExpressions()throws Exception{
        var attributes=Pattern.compile("th:(?:text|classappend)=\"([^\"]*)\"");
        try(var files=Files.walk(Path.of("src/main/resources/templates"))){
            for(var file:files.filter(p->p.toString().endsWith(".html")).toList()){
                var matches=attributes.matcher(Files.readString(file));
                while(matches.find()){
                    String outside=matches.group(1).replaceAll("\\$\\{[^}]*\\}","").replaceAll("'[^']*'","");
                    assertThat(Pattern.compile("\\b[a-zA-Z]+\\.[A-Za-z_]").matcher(outside).find()).as("Unwrapped property in %s: %s",file,matches.group(1)).isFalse();
                }
            }
        }
    }
}
