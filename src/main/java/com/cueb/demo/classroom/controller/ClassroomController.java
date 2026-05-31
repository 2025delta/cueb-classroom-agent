package com.cueb.demo.classroom.controller;

import com.cueb.demo.classroom.service.ClassroomService;
import com.cueb.demo.classroom.vo.ClassroomStatusVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {

    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    /** 获取所有教室占用状态 */
    @GetMapping
    public List<ClassroomStatusVO> listAll() {
        return classroomService.listAllStatus();
    }

    /** 获取所有空闲教室 */
    @GetMapping("/free")
    public List<ClassroomStatusVO> listFree() {
        return classroomService.listByStatus("空闲");
    }

    /** 获取所有被上报占用的教室 */
    @GetMapping("/occupied")
    public List<ClassroomStatusVO> listOccupied() {
        return classroomService.listByStatus("占用中");
    }

    /** 获取所有正在上课的教室 */
    @GetMapping("/in-class")
    public List<ClassroomStatusVO> listInClass() {
        return classroomService.listByStatus("上课中");
    }

    /** 获取单个教室占用状态 */
    @GetMapping("/{id}")
    public ClassroomStatusVO getOne(@PathVariable Long id) {
        return classroomService.getStatus(id);
    }
}
