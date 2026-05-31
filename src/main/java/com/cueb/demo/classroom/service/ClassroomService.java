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

        // P2: 检查活跃占用窗口 — create_time + duration >= NOW()
        OccupancyReport active = occupancyReportMapper.findActiveByClassroomId(c.getId());
        if (active != null) {
            vo.setStatus(active.getIsOccupied() == 1 ? "占用中" : "空闲");
            vo.setReportedBy(active.getReporter());
            vo.setPeopleCount(active.getPeopleCount());
            vo.setDuration(active.getDuration());
            vo.setReportCreateTime(active.getCreateTime());
            return vo;
        }

        // P3: 无活跃窗口 → 回退到最新上报记录
        OccupancyReport latest = occupancyReportMapper.findLatestByClassroomId(c.getId());
        if (latest != null) {
            vo.setStatus(latest.getIsOccupied() == 1 ? "占用中" : "空闲");
            vo.setReportedBy(latest.getReporter());
            vo.setPeopleCount(latest.getPeopleCount());
            vo.setDuration(latest.getDuration());
            vo.setReportCreateTime(latest.getCreateTime());
        } else {
            vo.setStatus("空闲");
        }
        return vo;
    }
}
