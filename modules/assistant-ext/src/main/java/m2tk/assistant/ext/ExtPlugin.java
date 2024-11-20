package m2tk.assistant.ext;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.Plugin;

@Slf4j
public class ExtPlugin extends Plugin
{
    @Override
    public void start()
    {
        log.info("m2kt-assistant.ext-plugin started.");
    }

    @Override
    public void stop()
    {
        log.info("m2kt-assistant.ext-plugin stopped.");
    }

    @Override
    public void delete()
    {
        log.info("m2kt-assistant.ext-plugin deleted.");
    }
}
