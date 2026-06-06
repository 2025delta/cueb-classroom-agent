package com.cueb.demo.classroom.service;

import com.cueb.demo.classroom.context.ClassroomQueryContext;
import com.cueb.demo.classroom.entity.Classroom;
import com.cueb.demo.classroom.entity.OccupancyReport;
import com.cueb.demo.classroom.mapper.ClassroomMapper;
import com.cueb.demo.classroom.mapper.OccupancyReportMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

/**
 * V2 Agent Service — 只保留上报工具。
 * 教室查询不再由 AI 决定，改为 Controller 预注入实时数据到 Prompt，
 * AI 基于注入数据直接回答，文本与 VO 天然一致。
 */
@Service
public class ClassroomAgentServiceV2 {

    private final OccupancyReportMapper occupancyReportMapper;
    private final ClassroomMapper classroomMapper;
    private final ClassroomQueryContext context;

    public ClassroomAgentServiceV2(OccupancyReportMapper occupancyReportMapper,
                                   ClassroomMapper classroomMapper,
                                   ClassroomQueryContext context) {
        this.occupancyReportMapper = occupancyReportMapper;
        this.classroomMapper = classroomMapper;
        this.context = context;
    }

    @Tool(description = "上报教室占用情况。每次调用上报一条记录，" +
           "如需上报多个教室请多次调用此工具（每次对话最多10条）。" +
           "参数: classroomName=教室名称（如\"博学楼101\"）, isOccupied=1占用/0空闲, " +
           "peopleCount=当前人数, duration=占用时长(HH:mm:ss格式)")
    public String reportOccupancy(
            @ToolParam(description = "教室名称，如\"博学楼101\"、\"博学楼201\"", required = true) String classroomName,
            @ToolParam(description = "是否占用：1=占用 0=空闲", required = true) Integer isOccupied,
            @ToolParam(description = "教室内人数", required = true) Integer peopleCount,
            @ToolParam(description = "占用时长，格式 HH:mm:ss，如 01:30:00 表示1.5小时", required = true) String duration) {

        if (classroomName == null || classroomName.isBlank()) {
            return "上报失败：教室名称不能为空。";
        }
        Classroom classroom = classroomMapper.findByName(classroomName.trim());
        if (classroom == null) {
            return "上报失败：未找到名称为\"" + classroomName + "\"的教室，请确认教室名称是否正确。";
        }
        if (isOccupied == null || (isOccupied != 0 && isOccupied != 1)) {
            return "上报失败：占用状态无效，应为 0（空闲）或 1（占用）。";
        }
        if (peopleCount == null || peopleCount < 0) {
            return "上报失败：人数无效。";
        }

        LocalTime parsedDuration;
        try {
            parsedDuration = LocalTime.parse(duration);
        } catch (Exception e) {
            return "上报失败：时长格式无效，请使用 HH:mm:ss 格式（如 01:30:00）。";
        }

        OccupancyReport report = new OccupancyReport();
        report.setClassroomId(classroom.getId());
        report.setIsOccupied(isOccupied);
        report.setReporter(0);
        report.setPeopleCount(peopleCount);
        report.setDuration(parsedDuration);

        occupancyReportMapper.insert(report);

        String statusText = isOccupied == 1 ? "占用中" : "空闲";
        String result = String.format("上报成功：%s，状态=%s，人数=%d，时长=%s",
                classroomName, statusText, peopleCount, duration);

        context.setLastReportResult(result);

        return result;
    }
}
