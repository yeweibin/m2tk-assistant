/*
 * Copyright (c) Ye Weibin. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package m2tk.assistant.template;

import cn.hutool.core.util.StrUtil;
import m2tk.assistant.template.definition.*;
import org.eaxy.Element;
import org.eaxy.SchemaValidationException;
import org.eaxy.Validator;
import org.eaxy.Xml;
import org.eaxy.experimental.SampleXmlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public final class TemplateReader
{
    private static final Logger logger = LoggerFactory.getLogger(TemplateReader.class);

    private final Validator validator;

    public TemplateReader()
    {
        try
        {
            URL xsd = getClass().getResource("/schema/M2TKTemplate.xsd");
            if (xsd == null)
                throw new IllegalStateException("template schema file is missing");

            SampleXmlBuilder generator = new SampleXmlBuilder(xsd, "");
            validator = generator.getValidator();
        } catch (IOException ex)
        {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    public M2TKTemplate parse(URL file)
    {
        try
        {
            Element root = Xml.read(file).getRootElement();

            // 首先验证XML结构是否合规
            validator.validate(root);
            logger.debug("文件[{}]通过Schema验证", file);

            // 解析模板内容（解析过程中将会对部分字段进行验证）
            List<TemplateDefinition> templates =
                    root.elements().stream()
                        .map(element -> switch (element.tagName())
                        {
                            case "TableTemplate" -> interpretTableTemplate(element);
                            case "DescriptorTemplate" -> interpretDescriptorTemplate(element);
                            default -> (TemplateDefinition) null;
                        })
                        .filter(Objects::nonNull)
                        .toList();
            logger.info("模板文件：{}，共解析 {} 个模板定义。", file, templates.size());

            return new M2TKTemplate(templates);
        } catch (SchemaValidationException ex)
        {
            logger.warn("文件[{}]未通过Schema验证：{}", file, ex.getMessage());
            return null;
        } catch (Exception ex)
        {
            logger.warn("执行异常：{}", ex.getMessage(), ex);
            return null;
        }
    }

    public M2TKTemplate parse(File file)
    {
        try
        {
            return parse(file.toURI().toURL());
        } catch (Exception ex)
        {
            logger.warn("执行异常：{}", ex.getMessage(), ex);
            return null;
        }
    }

    private static TableTemplate interpretTableTemplate(Element element)
    {
        TableTemplate template = new TableTemplate();
        template.setName(element.attr("name"));
        template.setGroup(element.attr("group"));
        template.setTableIds(StreamSupport.stream(element.find("TableId").spliterator(), false)
                                          .map(TemplateReader::interpretTableId)
                                          .toList());
        template.setTableSyntax(element.select("TableBody").elements().stream()
                                       .map(TemplateReader::interpretSyntaxFieldDefinition)
                                       .filter(Objects::nonNull)
                                       .toList());
        template.setUniqueKey(interpretOptional(element.find("UniqueKey").singleOrDefault(),
                                                TemplateReader::interpretUniqueKey));

        if (template.getTableSyntax().isEmpty())
        {
            logger.error("缺少有效的TableBody定义");
            return null;
        }

        return template;
    }

    private static TableId interpretTableId(Element element)
    {
        TableId tableId = new TableId();
        tableId.setId(Integer.decode(element.attr("id")));
        tableId.setDisplayName(interpretLabel(element.select("DisplayName")));
        return tableId;
    }

    private static UniqueKey interpretUniqueKey(Element element)
    {
        return UniqueKey.of(element.find("FieldRef").elements()
                                   .stream()
                                   .map(e -> e.attr("field"))
                                   .toList());
    }

    private static DescriptorTemplate interpretDescriptorTemplate(Element element)
    {
        DescriptorTemplate template = new DescriptorTemplate();
        template.setTag(Integer.decode(element.attr("tag")));
        template.setTagExtension(Integer.decode(StrUtil.nullToDefault(element.attr("tag_ext"), "-1")));
        template.setName(element.attr("name"));
        template.setDisplayName(interpretLabel(element.select("DisplayName")));
        template.setMayOccurIns(StreamSupport.stream(element.find("MayOccurIn").spliterator(), false)
                                             .map(mayOccurIn -> mayOccurIn.attr("table"))
                                             .toList());
        template.setDescriptorSyntax(element.select("DescriptorBody")
                                            .elements()
                                            .stream()
                                            .map(TemplateReader::interpretSyntaxFieldDefinition)
                                            .filter(Objects::nonNull)
                                            .toList());

        if (template.getDescriptorSyntax().isEmpty())
        {
            logger.error("缺少有效的DescriptorBody定义");
            return null;
        }

        return template;
    }

    private static Label interpretLabel(Element element)
    {
        Label label = new Label();
        label.setText(element.attr("str"));
        label.setColor(element.attr("color"));
        label.setBold(Boolean.parseBoolean(StrUtil.emptyToDefault(element.attr("bold"), "false")));
        return label;
    }

    private static SyntaxFieldDefinition interpretSyntaxFieldDefinition(Element element)
    {
        return switch (element.tagName())
        {
            case "Field" -> interpretField(element);
            case "If" -> interpretIf(element);
            case "Loop" -> interpretLoop(element);
            case "Descriptor" -> DescriptorFieldDefinition.INSTANCE;
            default -> null;
        };
    }

    private static DataFieldDefinition interpretField(Element element)
    {
        DataFieldDefinition definition = new DataFieldDefinition();
        definition.setName(element.attr("name"));
        definition.setEncoding(element.attr("encoding"));
        definition.setStringType(element.attr("string_type"));
        definition.setLength(element.attr("length"));
        definition.setLengthField(element.attr("length_field"));
        definition.setLengthCorrection(element.attr("length_correction"));
        definition.setPresentation(interpretOptional(element.find("FieldPresentation").firstOrDefault(),
                                                     TemplateReader::interpretFieldPresentation));

        if (!definition.verify())
        {
            logger.error("DataField定义不符合要求");
            return null;
        }

        return definition;
    }

    private static ConditionalFieldDefinition interpretIf(Element element)
    {
        Condition condition = null;
        Element e1 = element.find("Condition", "CompareWithConst").firstOrDefault();
        Element e2 = element.find("Condition", "CompareWithConstMulti").firstOrDefault();
        if (e1 != null)
        {
            condition = new Condition();
            condition.setType("CompareWithConst");
            condition.setField(e1.attr("field"));
            condition.setOperation(e1.attr("comp_op"));
            condition.setValue(Long.decode(e1.attr("const")));
        }
        if (e2 != null)
        {
            condition = new Condition();
            condition.setType("CompareWithConstMulti");
            condition.setField(e2.attr("field"));
            condition.setOperation(e2.attr("comp_op"));
            condition.setValues(StreamSupport.stream(e2.find("ConstValue").spliterator(), false)
                                             .mapToLong(value -> Long.decode(value.attr("const")))
                                             .toArray());
        }

        ConditionalFieldDefinition definition = new ConditionalFieldDefinition();
        definition.setCondition(condition);
        definition.setThenPart(element.find("Then").first()
                                      .elements().stream()
                                      .map(TemplateReader::interpretSyntaxFieldDefinition)
                                      .filter(Objects::nonNull)
                                      .toList());
        Optional.ofNullable(element.find("Else").firstOrDefault())
                .ifPresentOrElse(elseNode -> definition.setElsePart(elseNode.elements()
                                                                            .stream()
                                                                            .map(TemplateReader::interpretSyntaxFieldDefinition)
                                                                            .filter(Objects::nonNull)
                                                                            .toList()),
                                 () -> definition.setElsePart(Collections.emptyList()));

        if (!definition.verify())
        {
            logger.error("Condition定义不符合要求");
            return null;
        }

        return definition;
    }

    private static LoopFieldDefinition interpretLoop(Element element)
    {
        LoopFieldDefinition definition = new LoopFieldDefinition();
        definition.setName(element.attr("name"));
        definition.setLengthType(element.attr("length_type"));
        definition.setLengthField(element.attr("length_field"));
        definition.setLengthCorrection(element.attr("length_correction"));
        definition.setPresentation(interpretOptional(element.find("LoopPresentation").firstOrDefault(),
                                                     TemplateReader::interpretLoopPresentation));
        definition.setBody(element.find("Body")
                                  .first()
                                  .elements()
                                  .stream()
                                  .map(TemplateReader::interpretSyntaxFieldDefinition)
                                  .filter(Objects::nonNull)
                                  .toList());

        if (!definition.verify())
        {
            logger.error("Loop定义不符合要求");
            return null;
        }

        return definition;
    }

    private static LoopPresentation interpretLoopPresentation(Element element)
    {
        LoopPresentation presentation = new LoopPresentation();
        presentation.setNoLoopHeader(element.find("NoLoopHeader").isPresent());
        presentation.setLoopHeader(interpretOptional(element.find("LoopHeader").firstOrDefault(),
                                                     TemplateReader::interpretLabel));
        presentation.setLoopEmpty(interpretOptional(element.find("LoopEmpty").firstOrDefault(),
                                                    TemplateReader::interpretLabel));
        presentation.setLoopEntryPresentation(interpretOptional(element.find("LoopEntry").firstOrDefault(),
                                                                TemplateReader::interpretLoopEntryPresentation));
        return presentation;
    }

    private static LoopEntryPresentation interpretLoopEntryPresentation(Element element)
    {
        LoopEntryPresentation presentation = new LoopEntryPresentation();
        presentation.setFixed(interpretOptional(element.find("Fixed").firstOrDefault(),
                                                TemplateReader::interpretLabel));
        presentation.setPrefix(interpretOptional(element.find("Prefix").firstOrDefault(),
                                                 TemplateReader::interpretLabel));
        return presentation;
    }

    private static FieldPresentation interpretFieldPresentation(Element element)
    {
        FieldPresentation presentation = new FieldPresentation();
        presentation.setPrefix(interpretOptional(element.find("Prefix").firstOrDefault(),
                                                 TemplateReader::interpretLabel));
        presentation.setFormat(interpretOptional(element.find("Format").firstOrDefault(),
                                                 TemplateReader::interpretLabel));
        presentation.setValueMappings(interpretOptional(element.find("Mapping").firstOrDefault(),
                                                        TemplateReader::interpretValueMapping));
        return presentation;
    }

    private static List<ValueMapping> interpretValueMapping(Element element)
    {
        return element.elements().stream()
                      .map(e -> (ValueMapping) switch (e.tagName())
                      {
                          case "Value" -> ValueMapping.mono(Long.decode(e.attr("value")),
                                                            e.select("ValString").attr("str"));
                          case "ValueRange" -> ValueMapping.range(Long.decode(e.attr("min")),
                                                                  Long.decode(e.attr("max")),
                                                                  e.select("ValString").attr("str"));
                          case "DVBTime" -> ValueMapping.dvbTime();
                          case "Duration" -> ValueMapping.duration();
                          case "ThreeLetterCode" -> ValueMapping.threeLetterCode();
                          default -> null;
                      })
                      .filter(Objects::nonNull)
                      .toList();
    }

    private static <T> T interpretOptional(Element e, Function<Element, T> interpreter)
    {
        return (e == null) ? null : interpreter.apply(e);
    }
}