package com.cueb.demo.classroom.mapper;

import com.cueb.demo.classroom.entity.OccupancyReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OccupancyReportMapper {

    int insert(OccupancyReport report);

    /** 取该教室最近一条上报记录 */
    OccupancyReport findLatestByClassroomId(@Param("classroomId") Long classroomId);

    /** 取所有教室最近一条上报 */
    List<OccupancyReport> findLatestPerClassroom();

    /** 查找当前时间窗口内活跃的上报记录（create_time + duration >= NOW()） */
    OccupancyReport findActiveByClassroomId(@Param("classroomId") Long classroomId);

    /** 批量插入上报记录 */
    int insertBatch(@Param("list") List<OccupancyReport> reports);
}
