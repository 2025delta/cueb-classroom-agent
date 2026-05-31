package com.cueb.demo.classroom.mapper;

import com.cueb.demo.classroom.entity.Classroom;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ClassroomMapper {
    List<Classroom> findAll();
    Classroom findById(Long id);
    Classroom findByName(String name);
}
