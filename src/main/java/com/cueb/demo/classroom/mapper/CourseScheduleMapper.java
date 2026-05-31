package com.cueb.demo.classroom.mapper;

import com.cueb.demo.classroom.entity.CourseSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseScheduleMapper {

    List<CourseSchedule> findByClassroomId(@Param("classroomId") Long classroomId);

    /**
     * 查当前时间是否有课（用于占用判断）：星期几 + 周次 + 节次 匹配
     */
    List<CourseSchedule> findByClassroomIdAndPeriod(@Param("classroomId") Long classroomId,
                                                    @Param("weekDay") Integer weekDay,
                                                    @Param("week") Integer week,
                                                    @Param("period") Integer period);
}
