package com.cueb.demo.classroom.vo;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class ClassroomStatusVO {
    private Long id;
    private String name;
    private String building;
    private Integer capacity;
    private String status;        // 占用中 / 空闲 / 上课中
    private String courseName;    // 当前课程名，无课为null
    private String teacherName;   // 当前教师，无课为null
    private Integer reportedBy;   // 最近上报者身份，1=学生 2=老师
    private Integer peopleCount;  // 教室内人数
    private LocalTime duration;         // 占用持续时长
    private LocalDateTime reportCreateTime; // 上报记录的创建时间

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public Integer getReportedBy() { return reportedBy; }
    public void setReportedBy(Integer reportedBy) { this.reportedBy = reportedBy; }
    public Integer getPeopleCount() { return peopleCount; }
    public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
    public LocalTime getDuration() { return duration; }
    public void setDuration(LocalTime duration) { this.duration = duration; }
    public LocalDateTime getReportCreateTime() { return reportCreateTime; }
    public void setReportCreateTime(LocalDateTime reportCreateTime) { this.reportCreateTime = reportCreateTime; }
}
