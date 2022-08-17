package m2tk.assistant.analyzer;

import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.SectionEntity;
import m2tk.multiplex.DemuxStatus;
import m2tk.util.Bytes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class SectionPrinter implements Consumer<DemuxStatus>
{
    private final DatabaseService databaseService;

    public SectionPrinter(DatabaseService service)
    {
        databaseService = Objects.requireNonNull(service);
    }

    @Override
    public void accept(DemuxStatus status)
    {
        if (status.isRunning())
            return;

        printSections("PAT");
        printSections("CAT");
        printSections("PMT");
        printSections("NIT_Actual");
        printSections("NIT_Other");
        printSections("BAT");
        printSections("SDT_Actual");
        printSections("SDT_Other");
        printSections("EIT_PF_Actual");
        printSections("EIT_PF_Other");
        printSections("EIT_Schedule_Actual");
        printSections("EIT_Schedule_Other");
        printSections("TDT");
        printSections("EMM");
    }

    private void printSections(String name)
    {
        Map<Integer, List<SectionEntity>> sectionGroups = databaseService.getSections(name)
                                                                         .stream()
                                                                         .collect(groupingBy(SectionEntity::getStream,
                                                                                             toList()));
        List<Integer> pids = new ArrayList<>(sectionGroups.keySet());
        pids.sort(Integer::compare);
        for (Integer pid : pids)
        {
            List<SectionEntity> sections = sectionGroups.get(pid);
            System.out.printf("%s(PID: %d)> Total: %d%n", name, pid, sections.size());
            for (SectionEntity section : sections)
                System.out.println("  => " + Bytes.toHexString(section.getEncoding()));
            System.out.println();
        }
    }
}
