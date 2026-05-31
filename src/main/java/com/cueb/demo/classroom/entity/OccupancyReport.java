package com.cueb.demo.classroom.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class OccupancyReport {
    private Long id;
    private Long classroomId;
    private Integer isOccupied;
    private Integer reporter;
    private Integer peopleCount;
    private LocalTime duration;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }
    public Integer getIsOccupied() { return isOccupied; }
    public void setIsOccupied(Integer isOccupied) { this.isOccupied = isOccupied; }
    public Integer getReporter() { return reporter; }
    public void setReporter(Integer reporter) { this.reporter = reporter; }
    public Integer getPeopleCount() { return peopleCount; }
    public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
    public LocalTime getDuration() { return duration; }
    public void setDuration(LocalTime duration) { this.duration = duration; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
