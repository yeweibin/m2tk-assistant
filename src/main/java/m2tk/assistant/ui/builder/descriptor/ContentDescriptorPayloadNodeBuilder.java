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

package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.HashMap;
import java.util.Map;

public class ContentDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    private static final Map<Integer, String> contentDescriptions;

    static
    {
        contentDescriptions = new HashMap<>();
        contentDescriptions.put(0x10, "电影/电视剧/戏剧（普通）");
        contentDescriptions.put(0x11, "侦探/恐怖");
        contentDescriptions.put(0x12, "冒险/战争");
        contentDescriptions.put(0x13, "科幻");
        contentDescriptions.put(0x14, "喜剧");
        contentDescriptions.put(0x15, "肥皂剧");
        contentDescriptions.put(0x16, "言情");
        contentDescriptions.put(0x17, "古典/历史");
        contentDescriptions.put(0x20, "新闻时事（普通）");
        contentDescriptions.put(0x21, "新闻/天气预报");
        contentDescriptions.put(0x22, "新闻杂志");
        contentDescriptions.put(0x23, "资料");
        contentDescriptions.put(0x24, "讨论/访谈/辩论");
        contentDescriptions.put(0x30, "表演/游戏（普通）");
        contentDescriptions.put(0x31, "智力游戏/智力竞赛");
        contentDescriptions.put(0x32, "杂技");
        contentDescriptions.put(0x33, "脱口秀");
        contentDescriptions.put(0x40, "综合体育");
        contentDescriptions.put(0x41, "特别节目");
        contentDescriptions.put(0x42, "体育杂志");
        contentDescriptions.put(0x43, "足球/篮球/排球");
        contentDescriptions.put(0x44, "乒乓球/羽毛球/网球");
        contentDescriptions.put(0x45, "团体性运动");
        contentDescriptions.put(0x46, "田径");
        contentDescriptions.put(0x47, "自行车/赛车");
        contentDescriptions.put(0x48, "水上运动");
        contentDescriptions.put(0x49, "冬季运动");
        contentDescriptions.put(0x4A, "马术");
        contentDescriptions.put(0x4B, "武术/拳击/摔跤");
        contentDescriptions.put(0x4C, "棋牌");
        contentDescriptions.put(0x50, "青年少儿节目（综合）");
        contentDescriptions.put(0x51, "幼儿节目");
        contentDescriptions.put(0x52, "少儿节目");
        contentDescriptions.put(0x53, "青年节目");
        contentDescriptions.put(0x54, "信息/教育");
        contentDescriptions.put(0x55, "卡通/木偶戏");
        contentDescriptions.put(0x60, "音乐/舞蹈（综合）");
        contentDescriptions.put(0x61, "流行");
        contentDescriptions.put(0x62, "古典音乐/严肃音乐");
        contentDescriptions.put(0x63, "民俗音乐/民族音乐");
        contentDescriptions.put(0x64, "爵士乐");
        contentDescriptions.put(0x65, "歌舞剧/歌剧");
        contentDescriptions.put(0x66, "芭蕾舞");
        contentDescriptions.put(0x67, "戏曲/曲艺");
        contentDescriptions.put(0x70, "文化艺术（综合）");
        contentDescriptions.put(0x71, "表演艺术");
        contentDescriptions.put(0x72, "高雅艺术");
        contentDescriptions.put(0x74, "大众文化/传统艺术");
        contentDescriptions.put(0x75, "文学");
        contentDescriptions.put(0x76, "电影/电视文化");
        contentDescriptions.put(0x79, "新媒体");
        contentDescriptions.put(0x7A, "艺术/文化杂志");
        contentDescriptions.put(0x7B, "时尚");
        contentDescriptions.put(0x80, "社会/政治/经济（普通）");
        contentDescriptions.put(0x81, "杂志/报道/资讯/证券");
        contentDescriptions.put(0x82, "经济/社会咨询");
        contentDescriptions.put(0x83, "名人专题");
        contentDescriptions.put(0x90, "教育/科学/专题（普通）");
        contentDescriptions.put(0x91, "自然/动物/环境");
        contentDescriptions.put(0x92, "技术/自然科学");
        contentDescriptions.put(0x93, "医疗/生理/心理");
        contentDescriptions.put(0x94, "探险");
        contentDescriptions.put(0x95, "社会科学");
        contentDescriptions.put(0x96, "继续教育");
        contentDescriptions.put(0x97, "语言");
        contentDescriptions.put(0xA0, "休闲/业余爱好（普通）");
        contentDescriptions.put(0xA1, "旅游");
        contentDescriptions.put(0xA2, "手工");
        contentDescriptions.put(0xA3, "车趣");
        contentDescriptions.put(0xA4, "健身");
        contentDescriptions.put(0xA5, "烹饪");
        contentDescriptions.put(0xA6, "广告/购物");
        contentDescriptions.put(0xA7, "园艺");
    }

    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int count = payload.size() / 2;
        for (int i = 0; i < count; i++)
        {
            int identifier = payload.readUINT8(i * 2);
            int userByte = payload.readUINT8(i * 2 + 1);
            node.add(create(String.format("类型描述%d = %s，用户字节 = 0x%02X",
                                          i + 1,
                                          contentDescriptions.getOrDefault(identifier, "未定义"),
                                          userByte)));
        }
    }
}
