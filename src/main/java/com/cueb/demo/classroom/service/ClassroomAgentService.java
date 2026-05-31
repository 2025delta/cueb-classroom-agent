package com.cueb.demo.classroom.service;

import com.cueb.demo.classroom.entity.Classroom;
import com.cueb.demo.classroom.entity.OccupancyReport;
import com.cueb.demo.classroom.mapper.ClassroomMapper;
import com.cueb.demo.classroom.mapper.OccupancyReportMapper;
import com.cueb.demo.classroom.vo.ClassroomStatusVO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ClassroomAgentService {

    private final ClassroomService classroomService;
    private final OccupancyReportMapper occupancyReportMapper;
    private final ClassroomMapper classroomMapper;

    public ClassroomAgentService(ClassroomService classroomService,
                                  OccupancyReportMapper occupancyReportMapper,
                                  ClassroomMapper classroomMapper) {
        this.classroomService = classroomService;
        this.occupancyReportMapper = occupancyReportMapper;
        this.classroomMapper = classroomMapper;
    }

    @Tool(description = "查询教室占用情况。可按教室名称查询单个教室，" +
           "或按教学楼名称和状态筛选。不传参数则返回所有教室。")
    public String queryClassrooms(
            @ToolParam(description = "教室名称，如\"博学楼101\"，查询特定教室时传入，可选") String classroomName,
            @ToolParam(description = "教学楼名称，如\"博学楼\"、\"慎思楼\"，可选") String building,
            @ToolParam(description = "教室状态筛选：空闲、占用中、上课中，可选") String status) {

        List<ClassroomStatusVO> list;

        if (classroomName != null && !classroomName.isBlank()) {
            Classroom c = classroomMapper.findByName(classroomName.trim());
            if (c == null) {
                return "未找到名称为\"" + classroomName + "\"的教室。";
            }
            ClassroomStatusVO vo = classroomService.getStatus(c.getId());
            if (vo == null) {
                return "未找到名称为\"" + classroomName + "\"的教室。";
            }
            list = List.of(vo);
        } else if (status != null && !status.isBlank()) {
            list = classroomService.listByStatus(status);
        } else {
            list = classroomService.listAllStatus();
        }

        if (building != null && !building.isBlank()) {
            list = list.stream()
                    .filter(v -> building.equals(v.getBuilding()))
                    .toList();
        }

        if (list.isEmpty()) {
            return "没有找到符合条件的教室。";
        }

        StringBuilder sb = new StringBuilder("查询结果如下：\n");
        LocalDateTime now = LocalDateTime.now();
        for (ClassroomStatusVO v : list) {
            sb.append("  【").append(v.getName()).append("】")
              .append(" 状态：").append(v.getStatus());
            if (v.getCourseName() != null) {
                sb.append(" 课程：").append(v.getCourseName());
            }
            if (v.getTeacherName() != null) {
                sb.append(" 教师：").append(v.getTeacherName());
            }
            if (v.getPeopleCount() != null) {
                sb.append(" 人数：").append(v.getPeopleCount());
            }
            if (v.getDuration() != null && v.getReportCreateTime() != null) {
                long elapsedMin = Duration.between(v.getReportCreateTime(), now).toMinutes();
                long remainingMin = Duration.between(now, v.getReportCreateTime().plusNanos(v.getDuration().toNanoOfDay())).toMinutes();
                sb.append(" 已占用：").append(formatMinutes(elapsedMin));
                if (remainingMin > 0) {
                    sb.append(" 剩余：").append(formatMinutes(remainingMin));
                } else {
                    sb.append(" 剩余：0分钟【已超时，数据可能有误，建议重新上报】");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
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
        return String.format("上报成功：%s，状态=%s，人数=%d，时长=%s",
                classroomName, statusText, peopleCount, duration);
    }

    private static String formatMinutes(long totalMinutes) {
        if (totalMinutes <= 0) return "0分钟";
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0) {
            return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
        }
        return minutes + "分钟";
    }
}
