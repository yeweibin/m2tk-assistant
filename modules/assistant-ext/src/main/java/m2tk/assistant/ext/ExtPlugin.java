package m2tk.assistant.ext;

import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.template.DescriptorDecoder;
import m2tk.assistant.api.template.SectionDecoder;
import m2tk.assistant.api.template.TemplateReader;
import m2tk.assistant.api.template.definition.DescriptorTemplate;
import m2tk.assistant.api.template.definition.M2TKTemplate;
import m2tk.assistant.api.template.definition.TableTemplate;
import org.pf4j.Plugin;

import java.util.List;

@Slf4j
public class ExtPlugin extends Plugin
{
    @Override
    public void start()
    {
        TemplateReader reader = new TemplateReader();
        M2TKTemplate psiTemplate = reader.parse(getClass().getResource("/template/PSITemplateEx.xml"));
        if (psiTemplate != null)
        {
            List<TableTemplate> tableTemplates = psiTemplate.getTableTemplates();
            List<DescriptorTemplate> descriptorTemplates = psiTemplate.getDescriptorTemplates();
            tableTemplates.forEach(SectionDecoder::registerTemplate);
            descriptorTemplates.forEach(DescriptorDecoder::registerTemplate);
            log.info("[PSI] {} table templates and {} descriptor templates are loaded.",
                     tableTemplates.size(),
                     descriptorTemplates.size());
        }
        M2TKTemplate siTemplate = reader.parse(getClass().getResource("/template/SITemplateEx.xml"));
        if (siTemplate != null)
        {
            List<TableTemplate> tableTemplates = siTemplate.getTableTemplates();
            List<DescriptorTemplate> descriptorTemplates = siTemplate.getDescriptorTemplates();
            tableTemplates.forEach(SectionDecoder::registerTemplate);
            descriptorTemplates.forEach(DescriptorDecoder::registerTemplate);
            log.info("[SI] {} table templates and {} descriptor templates are loaded.",
                     tableTemplates.size(),
                     descriptorTemplates.size());
        }
    }
}
