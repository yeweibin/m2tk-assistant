# M2TK解析模板定义说明<!-- omit in toc -->


## 目录<!-- omit in toc -->

- [一、介绍](#一介绍)
  - [1.1 文档编写目的](#11-文档编写目的)
  - [1.2 文档面向人群](#12-文档面向人群)
- [二、模板定义](#二模板定义)
  - [2.1 XML声明](#21-xml声明)
  - [2.2 \<M2TKTemplate\> 标签](#22-m2tktemplate-标签)
  - [2.3 \<TableTemplate\> 标签](#23-tabletemplate-标签)
    - [2.3.1 \<TableId\> 标签](#231-tableid-标签)
    - [2.3.2 \<DisplayName\> 标签](#232-displayname-标签)
    - [2.3.3 \<TableBody\> 标签](#233-tablebody-标签)
    - [2.3.4 \<UniqueKey\> 标签](#234-uniquekey-标签)
  - [2.4 \<DescriptorTemplate\> 标签](#24-descriptortemplate-标签)
    - [2.4.1 \<DisplayName\> 标签](#241-displayname-标签)
    - [2.4.2 \<MayOccurIn\> 标签](#242-mayoccurin-标签)
    - [2.4.3 \<DescriptorBody\> 标签](#243-descriptorbody-标签)
  - [2.5 \<Syntax\> 标签](#25-syntax-标签)
    - [2.5.1 \<Field\> 标签](#251-field-标签)
      - [2.5.1.1 \<FieldPresentation\> 标签](#2511-fieldpresentation-标签)
    - [2.5.2 \<If\> 标签](#252-if-标签)


## 一、介绍

### 1.1 文档编写目的

本文档将详细介绍构造M2TK解析模板的步骤，以及M2TK解析模板的各元素含义和使用约束，帮助读者写出正确的数据解析模板文件。

### 1.2 文档面向人群

本文档主要面向 **M2TK码流分析助手** 的高级用户，即需要编写自定义数据解析器的人员。因此要求读者必须了解 **MPEG-2数据段（private_section）** 和 **描述符（descriptor）** 的定义方式，具备基本的XML语法知识（至少知道怎么组织XML结构），以及基础的Java（或其他类似的编程语言）语法知识，知道或熟悉打印函数（printf）的格式化模板用法，例如 **%d**、**%02x** 等。


## 二、模板定义

### 2.1 XML声明

```xml
<?xml version = "1.0" encoding = "UTF-8"?>
<M2TKTemplate>
  ...
</M2TKTemplate>
```

### 2.2 &lt;M2TKTemplate&gt; 标签

```xml
<M2TKTemplate xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:noNamespaceSchemaLocation="/schema/M2TKTemplate.xsd">
    <!-- body -->
</M2TKTemplate>
```

&lt;M2TKTemplate&gt;标签是模板XML的根节点，其包含一个或多个下列标签：

- &lt;TableTemplate&gt;
- &lt;DescriptorTemplate&gt;

标签可以以任意顺序排列，但不允许嵌套。

【说明】 **M2TKTemplate.xsd** 文件定义了模板XML的格式规范。


### 2.3 &lt;TableTemplate&gt; 标签

```xml
<!-- TableTemplate示例 -->
<TableTemplate name="program_association_section" standard="mpeg">
    <TableId id="0">
        <DisplayName str="PAT"/>
    </TableId>
    <TableBody>
        <Field name="table_id" length="8" encoding="uimsbf"/>
        <Field name="section_syntax_indicator" length="1" encoding="bslbf"/>
        <Field name="0" length="1" encoding="bslbf"/>
        <Field name="reserved" length="2" encoding="bslbf"/>
        <Field name="section_length" length="12" encoding="uimsbf"/>
        <Field name="transport_stream_id" length="16" encoding="uimsbf"/>
        <Field name="reserved" length="2" encoding="bslbf"/>
        <Field name="version_number" length="5" encoding="bslbf"/>
        <Field name="current_next_indicator" length="1" encoding="bslbf"/>
        <Field name="section_number" length="8" encoding="uimsbf"/>
        <Field name="last_section_number" length="8" encoding="uimsbf"/>
        <Loop name="program_association_loop" length_field="implicit" length_correction="-4">
            <LoopPresentation>
                <NoLoopHeader/>
                <LoopEmpty str="无节目描述"/>
                <LoopEntry>
                    <Prefix str="节目映射"/>
                </LoopEntry>
            </LoopPresentation>
            <Body>
                <Field name="program_number" length="16" encoding="uimsbf">
                    <FieldPresentation>
                        <Prefix str="节目号"/>
                    </FieldPresentation>
                </Field>
                <Field name="reserved" length="3" encoding="bslbf"/>
                <If>
                    <Condition>
                        <CompareWithConst field="program_number" comp_op="equals" const="0"/>
                    </Condition>
                    <Then>
                        <Field name="network_pid" length="13" encoding="uimsbf">
                            <FieldPresentation>
                                <Prefix str="NIT PID"/>
                                <Format str="0x%04X"/>
                            </FieldPresentation>
                        </Field>
                    </Then>
                    <Else>
                        <Field name="program_map_pid" length="13" encoding="uimsbf">
                            <FieldPresentation>
                                <Prefix str="PMT PID"/>
                                <Format str="0x%04X"/>
                            </FieldPresentation>
                        </Field>
                    </Else>
                </If>
            </Body>
        </Loop>
        <Field name="CRC_32" length="32" encoding="uimsbf"/>
    </TableBody>
    <UniqueKey>
        <FieldRef field="table_id"/>
        <FieldRef field="transport_stream_id"/>
        <FieldRef field="version_number"/>
        <FieldRef field="section_number"/>
    </UniqueKey>
</TableTemplate>
```

&lt;TableTemplate&gt;（段模板）标签用于描述私有段特征、语法结构和显示要求。

&lt;TableTemplate&gt;标签要求按照顺序依次描述：

- &lt;TableId&gt;标签
- &lt;TableBody&gt;标签
- &lt;UniqueKey&gt;标签

其中，允许出现多个&lt;TableId&gt;标签（用于描述具有不同TableId的分段，例如EIT），可以省略&lt;UniqueKey&gt;标签（每个解析到的分段数据都独立保存）。

&lt;TableTemplate&gt;标签包含下列属性：

- name：当前段结构的索引名称，可用于描述符定位（详见描述符模板中的MayOccurIn标签）。
- standard：来源标准。可选值有：mpeg（ISO标准）、dvb（DVB标准）、private（其他标准或自定义数据）。

【要求】name是必要属性，standard是可选属性。


#### 2.3.1 &lt;TableId&gt; 标签

```xml
<TableId id="0">
    <DisplayName str="PAT"/>
</TableId>
```

每个&lt;TableId&gt;标签描述一个特定的table_id值。它可以携带一个&lt;DisplayName&gt;标签，用来定义对应表的表头名字。

&lt;TableId&gt;标签包含唯一必要属性：

- id：table_id值


#### 2.3.2 &lt;DisplayName&gt; 标签

```xml
<DisplayName str="PAT"/>
```

&lt;DisplayName&gt;标签描述表数据的显示名称，结构与&lt;Label&gt;标签相同，解析器以字面量方式展示文字内容，不进行格式化。


#### 2.3.3 &lt;TableBody&gt; 标签

```xml
<TableBody>
    ...
</TableBody>
```

&lt;TableBody&gt;标签的结构类型详见*Syntax标签*。


#### 2.3.4 &lt;UniqueKey&gt; 标签

```xml
<UniqueKey>
    <FieldRef field="table_id"/>
    <FieldRef field="transport_stream_id"/>
    <FieldRef field="version_number"/>
    <FieldRef field="section_number"/>
</UniqueKey>
```

&lt;UniqueKey&gt;标签可以包含一个或以上&lt;FieldRef&gt;标签。&lt;FieldRef&gt;标签只有一个必要属性：field，表示被引用字段的索引名（详见&lt;Field&gt;标签描述）。

&lt;UniqueKey&gt;标签是可选标签。


### 2.4 &lt;DescriptorTemplate&gt; 标签

```xml
<!-- DescriptorTemplate示例 -->
<DescriptorTemplate tag="9" name="ca_descriptor" standard="mpeg">
    <DisplayName str="CA Descriptor"/>
    <MayOccurIn table="conditional_access_section"/>
    <MayOccurIn table="program_map_section"/>
    <DescriptorBody>
        <Field length="8" name="descriptor_tag" encoding="uimsbf">
            <FieldPresentation>
                <Prefix str="Tag"/>
                <Format str="0x%02X"/>
            </FieldPresentation>
        </Field>
        <Field length="8" name="descriptor_length" encoding="uimsbf"/>
        <Field length="16" name="CAS_ID" encoding="uimsbf">
            <FieldPresentation>
                <Format str="%04x (%s)"/>
            </FieldPresentation>
        </Field>
        <Field length="3" name="reserved" encoding="bslbf"/>
        <Field length="13" name="CA_PID" encoding="uimsbf">
            <FieldPresentation>
                <Prefix str="ECM PID"/>
                <Format str="0x%04X"/>
            </FieldPresentation>
        </Field>
        <Field length="n/a" name="private_data" encoding="octets" length_field="implicit"/>
    </DescriptorBody>
</DescriptorTemplate>
```

&lt;DescriptorTemplate&gt;（描述符模板）标签用于描述描述符特征、语法结构和显示要求。

&lt;DescriptorTemplate&gt;标签要求按照顺序依次描述&lt;DisplayName&gt;、&lt;MayOccurIn&gt;和&lt;DescriptorBody&gt;标签。其中，允许出现任意数量的&lt;MayOccurIn&gt;标签（标记允许携带该描述符的表）。

&lt;DescriptorTemplate&gt;标签包含下列属性：

- tag：descriptor_tag，其中保留值‘0’用于表示未知格式的任意描述符解析。
- tag_ext：扩展descriptor_tag。
- name：描述符索引名称。
- standard：来源标准。可选值有：mpeg（ISO标准）、dvb（DVB标准）、private（其他标准或自定义数据）。

【要求】tag、name是必要属性，其他是可选属性。


#### 2.4.1 &lt;DisplayName&gt; 标签

```xml
<DisplayName str="CA Descriptor"/>
```

&lt;DisplayName&gt;标签描述描述符的显示名称，结构与&lt;Label&gt;标签相同，解析器以字面量方式展示文字内容，不进行格式化。


#### 2.4.2 &lt;MayOccurIn&gt; 标签

```xml
<MayOccurIn table="program_map_section"/>
```
&lt;MayOccurIn&gt;标签只有一个必要属性：table，表示可能携带该描述符的表的索引名称。

&lt;MayOccurIn&gt;标签是可选标签。当未定义MayOccurIn时，描述符可以出现在任意表中。


#### 2.4.3 &lt;DescriptorBody&gt; 标签

```xml
<DescriptorBody>
    ...
</DescriptorBody>
```

&lt;DescriptorBody&gt;标签的结构类型详见*Syntax标签*。

### 2.5 &lt;Syntax&gt; 标签

**Syntax标签**表示由下列标签任意组合而成的标签集合，包括：

- Field标签：描述具体的单项数据字段。
- If标签：描述在某些情况下存在的数据字段（或字段组合）。
- Loop标签：描述循环出现的数据字段（或字段组合）。

解码器按照Syntax标签里元素定义的顺序进行解码，对于字段引用，解码器在当前字段的前序字段（即已出现的字段）中查找引用本体。对于循环中出现的引用，则仅在【当前循环内】和【整个循环体】的前序字段中寻找引用本体。


#### 2.5.1 &lt;Field&gt; 标签

```xml
<!-- Field示例 -->
<Field name="descriptor_tag" encoding="uimsbf" length="8">
  <FieldPresentation>
    <Prefix str="Tag"/>
    <Format str="0x%02X"/>
  </FieldPresentation>
</Field>
```

&lt;Field&gt;标签描述具有完整含义的简单数据，例如数值、字符串或字节数组，其在含义上完整不可分割（不允许进行拆分解释）。
&lt;Field&gt;标签包含以下属性：

- name：字段名称
- encoding：编码方式，包括：bslbf（比特流，最高位在前），uimsbf（无符号整数，最高位在前），checksum（校验码，高位在前），nibbles（半字节流），octets（字节流），text（文本）。
- string_type：文本类型，包括：dvb_text（DVB方式编码的字符串），utf16/utf8，ascii，gb2312/gbk/gb18030（中文编码）。
- length：字段长度，表示字段的编码长度（单位：比特）。通常用于描述数值，或较短的字节流、半字节流。有效范围：[1, 64]。
- length_field：长度引用字段，表示当前字段的编码长度需要用被引用的字段的值间接表示（单位：字节）。
- length_correction：长度修正，表示当前字段长度需要进行额外的修正（单位：字节）。仅在使用间接长度时存在，修正值可以是负数，表示需要减去的字节数。

【要求】name、encoding是必要属性，其他是可选属性。当字段类型为text时，必须提供合适的string_type描述。

***bslbf***、***uimsbf***、***checksum*** 都是数值类型，可以表示64位以内的任意非负整数；***nibbles*** 通常表示BCD编码；***octets*** 表示任意长度的字节数组；***text*** 表示字符串编码。字符串编码中是否包含结尾字符'\0'，由业务层规定，模板解析时并不作说明。

&lt;Field&gt;标签可以添加显示说明标签（&lt;FieldPresentation&gt;），专门描述字段的展示形式和特殊要求。

##### 2.5.1.1 &lt;FieldPresentation&gt; 标签

&lt;FieldPresentation&gt; 标签包含两个可选的 **Label** 标签：

- &lt;Prefix&gt; 标签：展示字段时，需要添加在内容前面的前缀部分。默认以当前字段的名称加冒号作为前缀显示。
- &lt;Format&gt; 标签：展示字段时，字段值的显示样式。

一个 **Label** 标签包含三个属性：

- str：表示要显示的文字或格式化模板（仅对Format标签适用）。
- color：显示内容的颜色，以“#rrggbb”的编码格式表示，其中r、g、b分别代表红、绿、蓝的颜色代码。注意，这里的颜色是前景色，而不是背景色。暂不支持设置背景色。如果没有定义color，则使用默认的前景色。
- bold：显示内容是否加粗（bool值，true/false）。

&lt;Format&gt; 标签里的格式化模板，需要遵循：

- 当应用于数值时，应符合Java语言里的字符串格式化模板，并且只接收一个输入参数，即字段的值。
- 当应用于nibbles流时，可以添加格式化说明：compact|lowercase（顺序任意，逗号分隔）。compact表示用字符串形式展示（默认是数组形式），lowercase表示字符以小写形式展示（默认大写）。
- 当应用与octets流时，可以添加格式化说明：compact|lowercase（顺序任意，逗号分隔）。compact表示以连续十六进制数字符串表示，且无前后括号包围（默认以数组形式展示），lowercase表示字符以小写形式展示（默认大写）。 

格式化标签不适用于文本内容。

#### 2.5.2 &lt;If&gt; 标签

