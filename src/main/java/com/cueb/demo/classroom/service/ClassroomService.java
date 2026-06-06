package com.cueb.demo.classroom.service;

import com.cueb.demo.classroom.entity.Classroom;
import com.cueb.demo.classroom.entity.CourseSchedule;
import com.cueb.demo.classroom.entity.OccupancyReport;
import com.cueb.demo.classroom.mapper.ClassroomMapper;
import com.cueb.demo.classroom.mapper.CourseScheduleMapper;
import com.cueb.demo.classroom.mapper.OccupancyReportMapper;
import com.cueb.demo.classroom.util.PeriodUtil;
import com.cueb.demo.classroom.vo.ClassroomStatusVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClassroomService {

    private final ClassroomMapper classroomMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final OccupancyReportMapper occupancyReportMapper;

    public ClassroomService(ClassroomMapper classroomMapper,
                            CourseScheduleMapper courseScheduleMapper,
                            OccupancyReportMapper occupancyReportMapper) {
        this.classroomMapper = classroomMapper;
        this.courseScheduleMapper = courseScheduleMapper;
        this.occupancyReportMapper = occupancyReportMapper;
    }

    /** 查询所有教室的当前占用状态 */
    public List<ClassroomStatusVO> listAllStatus() {
        List<Classroom> classrooms = classroomMapper.findAll();
        List<ClassroomStatusVO> result = new ArrayList<>();
        int weekDay = PeriodUtil.currentWeekDay();
        int week = PeriodUtil.currentWeek();
        int period = PeriodUtil.currentPeriod();

        for (Classroom c : classrooms) {
            result.add(buildStatus(c, weekDay, week, period));
        }
        return result;
    }

    /** 查询单个教室的当前占用状态 */
    public ClassroomStatusVO getStatus(Long classroomId) {
        Classroom c = classroomMapper.findById(classroomId);
        if (c == null) return null;
        return buildStatus(c, PeriodUtil.currentWeekDay(),
                PeriodUtil.currentWeek(), PeriodUtil.currentPeriod());
    }

    /** 按状态过滤教室 */
    public List<ClassroomStatusVO> listByStatus(String status) {
        return listAllStatus().stream()
                .filter(v -> status.equals(v.getStatus()))
                .toList();
    }

    private ClassroomStatusVO buildStatus(Classroom c, int weekDay, int week, int period) {
        ClassroomStatusVO vo = new ClassroomStatusVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setBuilding(c.getBuilding());
        vo.setCapacity(c.getCapacity());

        // P1: 课表优先 — 当前时间有课 → 上课中
        if (period > 0) {
            List<CourseSchedule> courses = courseScheduleMapper
                    .findByClassroomIdAndPeriod(c.getId(), weekDay, week, period);
            if (!courses.isEmpty()) {
                CourseSchedule cs = courses.get(0);
                vo.setStatus("上课中");
                vo.setCourseName(cs.getCourseName());
                vo.setTeacherName(cs.getTeacherName());
                return vo;
            }
        }

        // P2: 取今天最新一条上报记录
        //     空闲记录永久有效，占用记录按时间窗口判断
        OccupancyReport latest = occupancyReportMapper.findLatestByClassroomId(c.getId());
        if (latest != null) {
            if (latest.getIsOccupied() == 0) {
                // 空闲记录不受时长约束，直接以该记录为准
                vo.setStatus("空闲");
            } else {
                // 占用记录：检查是否仍在有效时长内
                LocalDateTime expiry = latest.getCreateTime()
                        .plusSeconds(latest.getDuration().toSecondOfDay());
                if (expiry.isBefore(LocalDateTime.now())) {
                    vo.setStatus("空闲");   // 已过期，自动恢复为空闲
                } else {
                    vo.setStatus("占用中"); // 未过期，仍为占用中
                }
            }
            vo.setReportedBy(latest.getReporter());
            vo.setPeopleCount(latest.getPeopleCount());
            vo.setDuration(latest.getDuration());
            vo.setReportCreateTime(latest.getCreateTime());
            return vo;
        }

        // P3: 无任何上报记录 → 默认空闲
        vo.setStatus("空闲");
        return vo;
    }
}
