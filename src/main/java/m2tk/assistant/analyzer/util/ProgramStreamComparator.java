package m2tk.assistant.analyzer.util;

import m2tk.assistant.analyzer.domain.ElementaryStream;
import m2tk.assistant.analyzer.presets.StreamTypes;

import java.util.Comparator;
import java.util.Objects;

public class ProgramStreamComparator implements Comparator<ElementaryStream>
{
    @Override
    public int compare(ElementaryStream s1, ElementaryStream s2)
    {
        // 视频 -> 音频 -> 数据 -> 私有
        // 同类则比pid
        String c1 = s1.getCategory();
        String c2 = s2.getCategory();

        if (Objects.equals(c1, c2))
            return Integer.compare(s1.getStreamPid(), s2.getStreamPid());

        switch (c1)
        {
            case StreamTypes.CATEGORY_VIDEO:
                return -1; // 视频排在最前面
            case StreamTypes.CATEGORY_AUDIO:
                return Objects.equals(c2, StreamTypes.CATEGORY_VIDEO) ? 1 : -1; // 音频落后视频，先于其他
            case StreamTypes.CATEGORY_DATA:
                return Objects.equals(c2, StreamTypes.CATEGORY_USER_PRIVATE) ? -1 : 1; // 数据先于私有数据，落后于音视频
            case StreamTypes.CATEGORY_USER_PRIVATE:
                return 1; // 私有类型排在最后面
            default:
                return Integer.compare(s1.getStreamPid(), s2.getStreamPid()); // 保护性措施，实际并不会触发。
        }
    }
}
