package smhy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class JsonExportConfiguration {

    @Value("${smhy.json.enabled:true}")
    private boolean enabled;

    private Boolean commandLineOverride;

    public boolean isEnabled() {
        return commandLineOverride != null ? commandLineOverride : enabled;
    }

    public void setCommandLineOverride(Boolean override) {
        this.commandLineOverride = override;
    }
}
