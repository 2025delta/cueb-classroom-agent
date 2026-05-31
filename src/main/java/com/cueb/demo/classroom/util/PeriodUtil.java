package com.cueb.demo.classroom.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.WeekFields;

/**
 * 高校节次—时间映射工具
 */
public class PeriodUtil {

    // 每节开始时间
    private static final LocalTime[] PERIOD_START = {
        null,
        LocalTime.of(8, 0),   // 第1节
        LocalTime.of(8, 55),  // 第2节
        LocalTime.of(10, 0),  // 第3节
        LocalTime.of(10, 55), // 第4节
        LocalTime.of(14, 0),  // 第5节
        LocalTime.of(14, 55), // 第6节
        LocalTime.of(16, 0),  // 第7节
        LocalTime.of(16, 55), // 第8节
        LocalTime.of(19, 0),  // 第9节
        LocalTime.of(19, 55), // 第10节
    };

    private static final int MAX_PERIOD = 10;

    /** 当前是星期几，1=周一 7=周日 */
    public static int currentWeekDay() {
        DayOfWeek dow = LocalDate.now().getDayOfWeek();
        return dow == DayOfWeek.SUNDAY ? 7 : dow.getValue();
    }

    /** 当前教学周（按9月1日起算第1周，简单算法） */
    public static int currentWeek() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        // 取当年9月1日所在的周一作为第1周起点
        LocalDate sep1 = LocalDate.of(year, 9, 1);
        LocalDate firstMonday = sep1.with(DayOfWeek.MONDAY);
        // 如果当前在9月之前，用上一年的
        if (now.isBefore(firstMonday)) {
            sep1 = LocalDate.of(year - 1, 9, 1);
            firstMonday = sep1.with(DayOfWeek.MONDAY);
        }
        return (int) java.time.temporal.ChronoUnit.WEEKS.between(firstMonday, now) + 1;
    }

    /** 当前节次，不在上课时段返回0 */
    public static int currentPeriod() {
        LocalTime now = LocalTime.now();
        for (int i = 1; i <= MAX_PERIOD; i++) {
            LocalTime start = PERIOD_START[i];
            LocalTime end = (i < MAX_PERIOD) ? PERIOD_START[i + 1] : LocalTime.of(21, 30);
            if (!now.isBefore(start) && now.isBefore(end)) {
                return i;
            }
        }
        return 0;
    }
}
