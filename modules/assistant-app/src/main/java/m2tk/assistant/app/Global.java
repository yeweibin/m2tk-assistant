/*
 *  Copyright (c) M2TK Project. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package m2tk.assistant.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.app.ui.template.DescriptorDecoder;
import m2tk.assistant.app.ui.template.SectionDecoder;
import m2tk.assistant.app.ui.template.TemplateReader;
import m2tk.assistant.app.ui.template.definition.M2TKTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Slf4j
public final class Global
{
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Global()
    {
    }

    public static void init()
    {
        loadInternalTemplates();
        loadUserDefinedTemplates();
    }

    private static void loadInternalTemplates()
    {
        TemplateReader reader = new TemplateReader();
        M2TKTemplate psiTemplate = reader.parse(Global.class.getResource("/template/PSITemplate.xml"));
        if (psiTemplate != null)
        {
            psiTemplate.getTableTemplates().forEach(SectionDecoder::registerTemplate);
            psiTemplate.getDescriptorTemplates().forEach(DescriptorDecoder::registerTemplate);
        }
        M2TKTemplate siTemplate = reader.parse(Global.class.getResource("/template/SITemplate.xml"));
        if (siTemplate != null)
        {
            siTemplate.getTableTemplates().forEach(SectionDecoder::registerTemplate);
            siTemplate.getDescriptorTemplates().forEach(DescriptorDecoder::registerTemplate);
        }
    }

    private static void loadUserDefinedTemplates()
    {
        Path folder = Paths.get(System.getProperty("user.dir"), "template");

        try (Stream<Path> stream = Files.list(folder))
        {
            TemplateReader reader = new TemplateReader();
            stream.forEach(path -> loadTemplate(reader, path.toFile()));
        } catch (Exception ex)
        {
            log.warn("加载自定义模板时异常：{}", ex.getMessage());
        }
    }

    public static int loadUserDefinedTemplates(File[] files)
    {
        TemplateReader reader = new TemplateReader();

        int success = 0;
        for (File file : files)
        {
            if (loadTemplate(reader, file))
                success += 1;
        }
        return success;
    }

    private static boolean loadTemplate(TemplateReader reader, File file)
    {
        if (file == null || !file.getName().endsWith(".xml"))
            return false;

        M2TKTemplate userTemplate = reader.parse(file);
        if (userTemplate == null)
            return false;

        userTemplate.getTableTemplates().forEach(SectionDecoder::registerTemplate);
        userTemplate.getDescriptorTemplates().forEach(DescriptorDecoder::registerTemplate);
        log.info("加载自定义模板：{}", file);

        return true;
    }
}
